package minilinker.linker;

import minilinker.architecture.Mini16Architecture;
import minilinker.model.MiniObjectModule;
import minilinker.model.ObjectSection;
import minilinker.model.RelocationEntry;
import minilinker.model.RelocationType;
import minilinker.model.SectionType;
import minilinker.model.SymbolDefinition;
import minilinker.parser.ObjectFileParser;
import minilinker.util.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Mini16 mimarisi için ana linker sınıfı.
 *
 * <p>Birden fazla MINIOBJ modülünü alır, section yerleşimi yapar,
 * sembol tablosu oluşturur, relocation uygular ve bağlı bellek
 * görüntüsünü üretir.</p>
 *
 * <p>Çıktı olarak {@code memory.hex} ve {@code link.map} dosyaları
 * oluşturulabilir.</p>
 */
public final class MiniLinker {

    private static final Logger LOGGER =
            Logger.getLogger(MiniLinker.class.getName());

    private final ObjectFileParser parser;

    public MiniLinker() {
        this(new ObjectFileParser());
    }

    public MiniLinker(ObjectFileParser parser) {
        this.parser = Objects.requireNonNull(
                parser,
                "Parser boş olamaz."
        );
    }

    public LinkResult link(List<Path> objectPaths)
            throws IOException {

        if (objectPaths == null || objectPaths.isEmpty()) {
            throw new IllegalArgumentException(
                    "En az bir object dosyası verilmelidir."
            );
        }

        List<MiniObjectModule> modules = new ArrayList<>();

        for (Path path : objectPaths) {
            if (path == null) {
                throw new IllegalArgumentException(
                        "Object dosyası yolu boş olamaz."
                );
            }

            modules.add(parser.parse(path));
        }

        return linkModules(modules);
    }

    public LinkResult link(Path... objectPaths)
            throws IOException {

        if (objectPaths == null) {
            throw new IllegalArgumentException(
                    "Object dosyaları boş olamaz."
            );
        }

        return link(Arrays.asList(objectPaths));
    }

    public LinkResult linkModules(
            List<MiniObjectModule> inputModules
    ) {
        if (inputModules == null || inputModules.isEmpty()) {
            throw new IllegalArgumentException(
                    "Link edilecek modül bulunamadı."
            );
        }

        List<MiniObjectModule> modules =
                new ArrayList<>(inputModules);

        LOGGER.info("Starting link process for " + modules.size() + " modules");

        LOGGER.info("Validating modules...");
        validateModules(modules);

        LOGGER.info("Creating layout...");
        Layout layout = createLayout(modules);

        LOGGER.info("Creating symbol table...");
        Map<String, Integer> symbolAddresses =
                createSymbolTable(modules, layout);

        LOGGER.info("Resolving EXTERN references...");
        resolveExternalSymbols(modules, symbolAddresses);

        LOGGER.info("Applying relocations...");
        applyRelocations(
                modules,
                layout,
                symbolAddresses
        );

        LOGGER.info("Generating linked memory image...");
        List<Integer> memoryWords =
                createLinkedMemory(modules, layout);

        LOGGER.info("Link process completed successfully");
        return new LinkResult(
                modules,
                memoryWords,
                symbolAddresses,
                layout.moduleSectionBases
        );
    }

    public LinkResult linkAndWrite(
            List<Path> objectPaths,
            Path memoryHexPath,
            Path mapPath
    ) throws IOException {

        LinkResult result = link(objectPaths);

        writeOutputs(
                result,
                memoryHexPath,
                mapPath
        );

        return result;
    }

    public void writeOutputs(
            LinkResult result,
            Path memoryHexPath,
            Path mapPath
    ) throws IOException {

        Objects.requireNonNull(
                result,
                "Link sonucu boş olamaz."
        );

        writeText(
                memoryHexPath,
                result.toMemoryHex()
        );

        writeText(
                mapPath,
                result.toMapText()
        );
    }

    private static void validateModules(
            List<MiniObjectModule> modules
    ) {
        Set<String> moduleNames =
                new LinkedHashSet<>();

        for (MiniObjectModule module : modules) {
            Objects.requireNonNull(
                    module,
                    "Modül boş olamaz."
            );

            if (!moduleNames.add(module.getModuleName())) {
                throw new IllegalStateException(
                        "Aynı modül adı birden fazla kullanılmış: "
                                + module.getModuleName()
                );
            }

            module.validate();
        }
    }

    private static Layout createLayout(
            List<MiniObjectModule> modules
    ) {
        IdentityHashMap<
                MiniObjectModule,
                EnumMap<SectionType, Integer>
                > sectionBases = new IdentityHashMap<>();

        for (MiniObjectModule module : modules) {
            sectionBases.put(
                    module,
                    new EnumMap<>(SectionType.class)
            );
        }

        int nextAddress = 0;

        /*
         * Yerleşim sırası:
         *
         * 1. Bütün .text section'ları
         * 2. Bütün .data section'ları
         */
        for (SectionType sectionType : SectionType.values()) {
            for (MiniObjectModule module : modules) {
                ObjectSection section =
                        module.getSection(sectionType);

                if (section == null) {
                    continue;
                }

                long endAddress =
                        (long) nextAddress
                                + section.wordCount();

                if (endAddress
                        > Mini16Architecture.ADDRESS_COUNT) {
                    throw new IllegalStateException(
                            "Program 16-bit adres alanını aşıyor."
                    );
                }

                sectionBases
                        .get(module)
                        .put(sectionType, nextAddress);

                nextAddress = (int) endAddress;
            }
        }

        return new Layout(
                sectionBases,
                nextAddress
        );
    }

    private static Map<String, Integer> createSymbolTable(
            List<MiniObjectModule> modules,
            Layout layout
    ) {
        Map<String, Integer> symbols =
                new LinkedHashMap<>();

        Map<String, String> symbolOwners =
                new LinkedHashMap<>();

        for (MiniObjectModule module : modules) {
            for (SymbolDefinition definition
                    : module.getDefinitions().values()) {

                int sectionBase = getSectionBase(
                        layout,
                        module,
                        definition.getSection()
                );

                int address = sectionBase
                        + definition.getWordOffset();

                Mini16Architecture.validateAddress(address);

                String symbolName =
                        definition.getName();

                if (symbols.containsKey(symbolName)) {
                    throw new IllegalStateException(
                            "Sembol birden fazla modülde tanımlanmış: "
                                    + symbolName
                                    + " ("
                                    + symbolOwners.get(symbolName)
                                    + " ve "
                                    + module.getModuleName()
                                    + ")"
                    );
                }

                symbols.put(symbolName, address);
                symbolOwners.put(
                        symbolName,
                        module.getModuleName()
                );
            }
        }

        return symbols;
    }

    private static void resolveExternalSymbols(
            List<MiniObjectModule> modules,
            Map<String, Integer> symbolAddresses
    ) {
        for (MiniObjectModule module : modules) {
            for (String external
                    : module.getExternals()) {

                if (!symbolAddresses.containsKey(external)) {
                    throw new IllegalStateException(
                            "Çözülemeyen harici sembol: "
                                    + external
                                    + " | Modül: "
                                    + module.getModuleName()
                    );
                }
            }
        }
    }

    private static void applyRelocations(
            List<MiniObjectModule> modules,
            Layout layout,
            Map<String, Integer> symbolAddresses
    ) {
        for (MiniObjectModule module : modules) {
            for (RelocationEntry relocation
                    : module.getRelocations()) {

                String symbolName =
                        relocation.getSymbolName();

                Integer symbolAddress =
                        symbolAddresses.get(symbolName);

                if (symbolAddress == null) {
                    throw new IllegalStateException(
                            Messages.ERR_SYMBOL_NOT_RESOLVED
                                    + symbolName
                    );
                }

                ObjectSection section =
                        module.getSection(
                                relocation.getSection()
                        );

                if (section == null) {
                    throw new IllegalStateException(
                            Messages.ERR_SECTION_NOT_FOUND
                                    + relocation.getSection()
                    );
                }

                int relocatedValue;

                if (relocation.getType()
                        == RelocationType.ABS16) {

                    relocatedValue = symbolAddress;

                } else if (relocation.getType()
                        == RelocationType.REL16) {

                    int sectionBase = getSectionBase(
                            layout,
                            module,
                            relocation.getSection()
                    );

                    int placeAddress = sectionBase
                            + relocation.getWordOffset();

                    int nextWordAddress = placeAddress + 1;
                    relocatedValue =
                            (symbolAddress - nextWordAddress)
                                    & 0xFFFF;

                } else {
                    throw new IllegalStateException(
                            Messages.ERR_UNSUPPORTED_RELOC
                                    + relocation.getType()
                    );
                }

                section.setWord(
                        relocation.getWordOffset(),
                        relocatedValue
                );
            }
        }
    }


    private static List<Integer> createLinkedMemory(
            List<MiniObjectModule> modules,
            Layout layout
    ) {
        List<Integer> memory =
                new ArrayList<>();

        for (int index = 0;
             index < layout.totalWords;
             index++) {
            memory.add(0);
        }

        for (MiniObjectModule module : modules) {
            for (SectionType sectionType
                    : SectionType.values()) {

                ObjectSection section =
                        module.getSection(sectionType);

                if (section == null) {
                    continue;
                }

                int baseAddress = getSectionBase(
                        layout,
                        module,
                        sectionType
                );

                for (int offset = 0;
                     offset < section.wordCount();
                     offset++) {

                    memory.set(
                            baseAddress + offset,
                            section.getWord(offset)
                    );
                }
            }
        }

        return memory;
    }

    private static int getSectionBase(
            Layout layout,
            MiniObjectModule module,
            SectionType sectionType
    ) {
        EnumMap<SectionType, Integer> bases =
                layout.moduleSectionBases.get(module);

        if (bases == null
                || !bases.containsKey(sectionType)) {
            throw new IllegalStateException(
                    "Section yerleşimi bulunamadı: "
                            + module.getModuleName()
                            + " "
                            + sectionType
            );
        }

        return bases.get(sectionType);
    }

    private static void writeText(
            Path path,
            String content
    ) throws IOException {

        Objects.requireNonNull(
                path,
                "Çıktı dosyası yolu boş olamaz."
        );

        Path parent =
                path.toAbsolutePath()
                        .normalize()
                        .getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8
        );
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(
                    "Kullanım: MiniLinker "
                            + "<module1.obj> <module2.obj> ..."
            );
            return;
        }

        List<Path> inputFiles =
                new ArrayList<>();

        for (String argument : args) {
            inputFiles.add(Path.of(argument));
        }

        try {
            MiniLinker linker =
                    new MiniLinker();

            LinkResult result =
                    linker.link(inputFiles);

            linker.writeOutputs(
                    result,
                    Path.of("memory.hex"),
                    Path.of("link.map")
            );

            System.out.println(
                    "Link işlemi başarılı."
            );
            System.out.println(
                    "Oluşturulan dosya: memory.hex"
            );
            System.out.println(
                    "Oluşturulan dosya: link.map"
            );

        } catch (IOException
                 | IllegalArgumentException
                 | IllegalStateException exception) {

            System.err.println(
                    "Linker hatası: "
                            + exception.getMessage()
            );
        }
    }

    private static final class Layout {

        private final IdentityHashMap<
                MiniObjectModule,
                EnumMap<SectionType, Integer>
                > moduleSectionBases;

        private final int totalWords;

        private Layout(
                IdentityHashMap<
                        MiniObjectModule,
                        EnumMap<SectionType, Integer>
                        > moduleSectionBases,
                int totalWords
        ) {
            this.moduleSectionBases =
                    moduleSectionBases;
            this.totalWords = totalWords;
        }
    }

    public static final class LinkResult {

        private final List<MiniObjectModule> modules;
        private final List<Integer> memoryWords;
        private final Map<String, Integer> symbolAddresses;
        private final Map<
                String,
                Map<SectionType, Integer>
                > moduleSectionBases;

        private LinkResult(
                List<MiniObjectModule> modules,
                List<Integer> memoryWords,
                Map<String, Integer> symbolAddresses,
                IdentityHashMap<
                        MiniObjectModule,
                        EnumMap<SectionType, Integer>
                        > sectionBases
        ) {
            this.modules =
                    Collections.unmodifiableList(
                            new ArrayList<>(modules)
                    );

            this.memoryWords =
                    Collections.unmodifiableList(
                            new ArrayList<>(memoryWords)
                    );

            this.symbolAddresses =
                    Collections.unmodifiableMap(
                            new LinkedHashMap<>(
                                    symbolAddresses
                            )
                    );

            Map<String, Map<SectionType, Integer>>
                    copiedBases = new LinkedHashMap<>();

            for (MiniObjectModule module : modules) {
                EnumMap<SectionType, Integer>
                        copiedSectionBases =
                        new EnumMap<>(
                                SectionType.class
                        );

                copiedSectionBases.putAll(
                        sectionBases.get(module)
                );

                copiedBases.put(
                        module.getModuleName(),
                        Collections.unmodifiableMap(
                                copiedSectionBases
                        )
                );
            }

            this.moduleSectionBases =
                    Collections.unmodifiableMap(
                            copiedBases
                    );
        }

        public List<MiniObjectModule> getModules() {
            return modules;
        }

        public List<Integer> getMemoryWords() {
            return memoryWords;
        }

        public Map<String, Integer>
        getSymbolAddresses() {
            return symbolAddresses;
        }

        public Map<
                String,
                Map<SectionType, Integer>
                > getModuleSectionBases() {
            return moduleSectionBases;
        }

        public String toMemoryHex() {
            StringBuilder result =
                    new StringBuilder();

            for (int address = 0;
                 address < memoryWords.size();
                 address++) {

                result.append(
                        Mini16Architecture.formatAddress(
                                address
                        )
                );

                result.append(": ");

                result.append(
                        Mini16Architecture.formatWord(
                                memoryWords.get(address)
                        )
                );

                result.append('\n');
            }

            return result.toString();
        }

        public String toMapText() {
            StringBuilder result =
                    new StringBuilder();

            result.append("# MINI LINKER MAP\n\n");

            result.append("SECTIONS\n");

            for (Map.Entry<
                    String,
                    Map<SectionType, Integer>
                    > moduleEntry
                    : moduleSectionBases.entrySet()) {

                result.append(
                        moduleEntry.getKey()
                ).append('\n');

                for (Map.Entry<
                        SectionType,
                        Integer
                        > sectionEntry
                        : moduleEntry.getValue()
                        .entrySet()) {

                    result.append("  ")
                            .append(
                                    sectionEntry
                                            .getKey()
                                            .getName()
                            )
                            .append(" = ")
                            .append(
                                    Mini16Architecture
                                            .formatAddress(
                                                    sectionEntry
                                                            .getValue()
                                            )
                            )
                            .append('\n');
                }
            }

            result.append("\nSYMBOLS\n");

            if (symbolAddresses.isEmpty()) {
                result.append("  Sembol yok.\n");
            } else {
                for (Map.Entry<
                        String,
                        Integer
                        > symbolEntry
                        : symbolAddresses.entrySet()) {

                    result.append("  ")
                            .append(symbolEntry.getKey())
                            .append(" = ")
                            .append(
                                    Mini16Architecture
                                            .formatAddress(
                                                    symbolEntry
                                                            .getValue()
                                            )
                            )
                            .append('\n');
                }
            }

            return result.toString();
        }
    }
}