package minilinker.parser;

import minilinker.model.MiniObjectModule;
import minilinker.model.ObjectSection;
import minilinker.model.RelocationEntry;
import minilinker.model.RelocationType;
import minilinker.model.SectionType;
import minilinker.model.SymbolDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * MINIOBJ formatındaki object dosyalarını ayrıştıran parser.
 *
 * <p>Desteklenen komutlar: MODULE, SECTION, WORDS, ENDSECTION,
 * DEFINE, EXTERN, RELOC, END. İlk satır "MINIOBJ 1" olmalıdır.</p>
 *
 * <p>Ayrıştırma sonucunda bir {@link MiniObjectModule} nesnesi döner.</p>
 */
public final class ObjectFileParser {

    private static final String HEADER = "MINIOBJ 1";

    public MiniObjectModule parse(Path path) throws IOException {
        Objects.requireNonNull(path, "Dosya yolu boş olamaz.");

        if (!Files.exists(path)) {
            throw new IOException("Dosya bulunamadı: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException("Geçerli bir dosya değil: " + path);
        }

        List<String> lines = Files.readAllLines(
                path,
                StandardCharsets.UTF_8
        );

        return parse(lines, path.toString());
    }

    public MiniObjectModule parse(String fileName)
            throws IOException {

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Dosya adı boş olamaz."
            );
        }

        return parse(Path.of(fileName));
    }

    public MiniObjectModule parse(List<String> lines) {
        return parse(lines, "<memory>");
    }

    private MiniObjectModule parse(
            List<String> lines,
            String sourceName
    ) {
        Objects.requireNonNull(
                lines,
                "Dosya satırları boş olamaz."
        );

        if (lines.isEmpty()) {
            throw error(sourceName, 1, "Dosya boş olamaz.");
        }

        String header = normalizeLine(lines.get(0), 0);

        if (!HEADER.equals(header)) {
            throw error(
                    sourceName,
                    1,
                    "İlk satır MINIOBJ 1 olmalıdır."
            );
        }

        MiniObjectModule module = null;
        ObjectSection currentSection = null;
        boolean ended = false;

        for (int index = 1; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String line = normalizeLine(lines.get(index), index);

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (ended) {
                throw error(
                        sourceName,
                        lineNumber,
                        "END komutundan sonra başka komut bulunamaz."
                );
            }

            String[] tokens = line.split("\\s+");
            String command = tokens[0];

            switch (command) {
                case "MODULE":
                    requireTokenCount(
                            tokens,
                            2,
                            sourceName,
                            lineNumber,
                            command
                    );

                    if (module != null) {
                        throw error(
                                sourceName,
                                lineNumber,
                                "MODULE yalnızca bir kez kullanılabilir."
                        );
                    }

                    if (currentSection != null) {
                        throw error(
                                sourceName,
                                lineNumber,
                                "Açık section varken MODULE kullanılamaz."
                        );
                    }

                    module = new MiniObjectModule(tokens[1]);
                    break;

                case "SECTION":
                    requireModule(
                            module,
                            sourceName,
                            lineNumber
                    );

                    requireTokenCount(
                            tokens,
                            2,
                            sourceName,
                            lineNumber,
                            command
                    );

                    if (currentSection != null) {
                        throw error(
                                sourceName,
                                lineNumber,
                                "İç içe SECTION kullanılamaz."
                        );
                    }

                    SectionType sectionType =
                            parseSectionType(
                                    tokens[1],
                                    sourceName,
                                    lineNumber
                            );

                    ObjectSection section =
                            new ObjectSection(sectionType);

                    try {
                        module.addSection(section);
                    } catch (IllegalArgumentException exception) {
                        throw error(
                                sourceName,
                                lineNumber,
                                exception.getMessage()
                        );
                    }

                    currentSection = section;
                    break;

                case "WORDS":
                    requireModule(
                            module,
                            sourceName,
                            lineNumber
                    );

                    if (currentSection == null) {
                        throw error(
                                sourceName,
                                lineNumber,
                                "WORDS yalnızca açık section içinde kullanılabilir."
                        );
                    }

                    if (tokens.length < 2) {
                        throw error(
                                sourceName,
                                lineNumber,
                                "WORDS en az bir hexadecimal değer içermelidir."
                        );
                    }

                    for (int tokenIndex = 1;
                         tokenIndex < tokens.length;
                         tokenIndex++) {

                        currentSection.addWord(
                                parseHexWord(
                                        tokens[tokenIndex],
                                        sourceName,
                                        lineNumber
                                )
                        );
                    }
                    break;

                case "ENDSECTION":
                    requireTokenCount(
                            tokens,
                            1,
                            sourceName,
                            lineNumber,
                            command
                    );

                    if (currentSection == null) {
                        throw error(
                                sourceName,
                                lineNumber,
                                "Kapatılacak açık section yok."
                        );
                    }

                    currentSection = null;
                    break;

                case "DEFINE":
                    requireModule(
                            module,
                            sourceName,
                            lineNumber
                    );

                    requireOutsideSection(
                            currentSection,
                            sourceName,
                            lineNumber
                    );

                    requireTokenCount(
                            tokens,
                            4,
                            sourceName,
                            lineNumber,
                            command
                    );

                    SectionType definitionSection =
                            parseSectionType(
                                    tokens[2],
                                    sourceName,
                                    lineNumber
                            );

                    int definitionOffset =
                            parseDecimalOffset(
                                    tokens[3],
                                    sourceName,
                                    lineNumber
                            );

                    try {
                        module.defineSymbol(
                                new SymbolDefinition(
                                        tokens[1],
                                        definitionSection,
                                        definitionOffset
                                )
                        );
                    } catch (IllegalArgumentException exception) {
                        throw error(
                                sourceName,
                                lineNumber,
                                exception.getMessage()
                        );
                    }
                    break;

                case "EXTERN":
                    requireModule(
                            module,
                            sourceName,
                            lineNumber
                    );

                    requireOutsideSection(
                            currentSection,
                            sourceName,
                            lineNumber
                    );

                    requireTokenCount(
                            tokens,
                            2,
                            sourceName,
                            lineNumber,
                            command
                    );

                    try {
                        module.addExternal(tokens[1]);
                    } catch (IllegalArgumentException exception) {
                        throw error(
                                sourceName,
                                lineNumber,
                                exception.getMessage()
                        );
                    }
                    break;

                case "RELOC":
                    requireModule(
                            module,
                            sourceName,
                            lineNumber
                    );

                    requireOutsideSection(
                            currentSection,
                            sourceName,
                            lineNumber
                    );

                    requireTokenCount(
                            tokens,
                            5,
                            sourceName,
                            lineNumber,
                            command
                    );

                    SectionType relocationSection =
                            parseSectionType(
                                    tokens[1],
                                    sourceName,
                                    lineNumber
                            );

                    int relocationOffset =
                            parseDecimalOffset(
                                    tokens[2],
                                    sourceName,
                                    lineNumber
                            );

                    RelocationType relocationType =
                            parseRelocationType(
                                    tokens[3],
                                    sourceName,
                                    lineNumber
                            );

                    module.addRelocation(
                            new RelocationEntry(
                                    relocationSection,
                                    relocationOffset,
                                    relocationType,
                                    tokens[4]
                            )
                    );
                    break;

                case "END":
                    requireTokenCount(
                            tokens,
                            1,
                            sourceName,
                            lineNumber,
                            command
                    );

                    requireModule(
                            module,
                            sourceName,
                            lineNumber
                    );

                    requireOutsideSection(
                            currentSection,
                            sourceName,
                            lineNumber
                    );

                    ended = true;
                    break;

                default:
                    throw error(
                            sourceName,
                            lineNumber,
                            "Bilinmeyen komut: " + command
                    );
            }
        }

        if (module == null) {
            throw error(
                    sourceName,
                    lines.size(),
                    "MODULE komutu bulunamadı."
            );
        }

        if (currentSection != null) {
            throw error(
                    sourceName,
                    lines.size(),
                    "SECTION ENDSECTION ile kapatılmamış."
            );
        }

        if (!ended) {
            throw error(
                    sourceName,
                    lines.size(),
                    "Dosya END komutuyla bitmelidir."
            );
        }

        module.validate();
        return module;
    }

    private static void requireModule(
            MiniObjectModule module,
            String sourceName,
            int lineNumber
    ) {
        if (module == null) {
            throw error(
                    sourceName,
                    lineNumber,
                    "Önce MODULE komutu kullanılmalıdır."
            );
        }
    }

    private static void requireOutsideSection(
            ObjectSection section,
            String sourceName,
            int lineNumber
    ) {
        if (section != null) {
            throw error(
                    sourceName,
                    lineNumber,
                    "Bu komut açık section içinde kullanılamaz."
            );
        }
    }

    private static void requireTokenCount(
            String[] tokens,
            int expected,
            String sourceName,
            int lineNumber,
            String command
    ) {
        if (tokens.length != expected) {
            throw error(
                    sourceName,
                    lineNumber,
                    command + " komutu " + (expected - 1)
                            + " parametre almalıdır."
            );
        }
    }

    private static SectionType parseSectionType(
            String value,
            String sourceName,
            int lineNumber
    ) {
        try {
            return SectionType.fromName(value);
        } catch (IllegalArgumentException exception) {
            throw error(
                    sourceName,
                    lineNumber,
                    exception.getMessage()
            );
        }
    }

    private static RelocationType parseRelocationType(
            String value,
            String sourceName,
            int lineNumber
    ) {
        try {
            return RelocationType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw error(
                    sourceName,
                    lineNumber,
                    "Desteklenmeyen relocation türü: " + value
            );
        }
    }

    private static int parseHexWord(
            String value,
            String sourceName,
            int lineNumber
    ) {
        if (!value.matches("[0-9A-Fa-f]{4}")) {
            throw error(
                    sourceName,
                    lineNumber,
                    "WORD değeri dört basamaklı hexadecimal olmalıdır: "
                            + value
            );
        }

        return Integer.parseInt(value, 16);
    }

    private static int parseDecimalOffset(
            String value,
            String sourceName,
            int lineNumber
    ) {
        if (!value.matches("[0-9]+")) {
            throw error(
                    sourceName,
                    lineNumber,
                    "Offset decimal ve negatif olmayan bir sayı olmalıdır: "
                            + value
            );
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw error(
                    sourceName,
                    lineNumber,
                    "Offset değeri çok büyük: " + value
            );
        }
    }

    private static String normalizeLine(
            String line,
            int index
    ) {
        if (line == null) {
            throw new IllegalArgumentException(
                    "Satır " + (index + 1) + " boş olamaz."
            );
        }

        String normalized = line.strip();

        if (index == 0 && normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1).strip();
        }

        return normalized;
    }

    private static IllegalArgumentException error(
            String sourceName,
            int lineNumber,
            String message
    ) {
        return new IllegalArgumentException(
                sourceName + ":" + lineNumber + " - " + message
        );
    }
}