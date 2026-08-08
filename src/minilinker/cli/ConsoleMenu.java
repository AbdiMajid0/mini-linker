package minilinker.cli;

import minilinker.linker.MiniLinker;
import minilinker.model.MiniObjectModule;
import minilinker.model.ObjectSection;
import minilinker.model.RelocationEntry;
import minilinker.model.SymbolDefinition;
import minilinker.parser.ObjectFileParser;
import minilinker.util.Messages;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Kullanıcı ile etkileşimi sağlayan konsol menüsü.
 *
 * <p>Modül yükleme, görüntüleme, doğrulama ve linkleme işlemlerini
 * menü üzerinden yapmaya olanak tanır.</p>
 */
public class ConsoleMenu {

    private static final Logger LOGGER =
            Logger.getLogger(ConsoleMenu.class.getName());
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Scanner scanner;
    private final ObjectFileParser parser;
    private final List<MiniObjectModule> loadedModules;

    public ConsoleMenu() {
        this(new Scanner(System.in), new ObjectFileParser());
    }

    public ConsoleMenu(
            Scanner scanner,
            ObjectFileParser parser
    ) {
        this.scanner = Objects.requireNonNull(
                scanner,
                "Scanner boş olamaz."
        );
        this.parser = Objects.requireNonNull(
                parser,
                "Parser boş olamaz."
        );
        this.loadedModules = new ArrayList<>();
    }

    /** Menüyü sürekli çalıştırır. */
    public void start() {
        boolean running = true;

        printWelcome();

        while (running) {
            printMenu();
            String choice = readLine("Seçiminiz: ");

            try {
                switch (choice) {
                    case "1" -> loadObjectFile();
                    case "2" -> listModules();
                    case "3" -> showModuleDetails();
                    case "4" -> validateModules();
                    case "5" -> removeModule();
                    case "6" -> linkModules();
                    case "0" -> running = false;
                    default -> System.out.println(
                            "Gecersiz secim. Lutfen menudeki bir secenegi girin."
                    );
                }
            } catch (IOException | IllegalArgumentException |
                     IllegalStateException exception) {
                System.out.println("Hata: " + exception.getMessage());
            }

            if (running) {
                System.out.println();
            }
        }

        System.out.println("Program sonlandirildi.");
    }

    /** start() için okunabilir bir alternatif isim. */
    public void run() {
        start();
    }

    /** ConsoleMenu doğrudan çalıştırılabilsin diye giriş noktası. */
    public static void main(String[] args) {
        new ConsoleMenu().start();
    }

    private void printWelcome() {
        System.out.println("========================================");
        System.out.println("          MINI LINKER CONSOLE           ");
        System.out.println("========================================");
    }

    private void printMenu() {
        System.out.println("\n--------------- MENU ------------------");
        System.out.println("1 - MINIOBJ dosyasi yukle");
        System.out.println("2 - Yuklu modulleri listele");
        System.out.println("3 - Modul ayrintilarini goster");
        System.out.println("4 - Modulleri dogrula");
        System.out.println("5 - Yuklu modulu kaldir");
        System.out.println("6 - Modulleri linkle");
        System.out.println("0 - Cikis");
        System.out.println("----------------------------------------");
    }

    private void loadObjectFile() throws IOException {
        String fileName = readLine(
                "Yuklenecek .obj dosyasinin yolu: "
        );

        if (fileName.isBlank()) {
            LOGGER.warning("Kullanici bos dosya yolu girdi.");
            throw new IllegalArgumentException(
                    "Dosya yolu bos birakilamaz."
            );
        }

        if (!fileName.toLowerCase().endsWith(".obj")) {
            LOGGER.warning("Hatali dosya uzantisi: " + fileName);
            throw new IllegalArgumentException(
                    "Dosya .obj uzantili olmalidir."
            );
        }

        Path path = Path.of(removeOptionalQuotes(fileName));
        
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            LOGGER.warning("Dosya bulunamadi veya normal bir dosya degil: " + path);
            throw new IllegalArgumentException("Dosya bulunamadi: " + path);
        }
        
        if (Files.size(path) > MAX_FILE_SIZE) {
            LOGGER.warning("Dosya boyutu cok buyuk: " + path);
            throw new IllegalArgumentException("Dosya boyutu cok buyuk (max 10MB).");
        }

        LOGGER.info("Modul yukleniyor: " + path);
        MiniObjectModule module = parser.parse(path);

        for (MiniObjectModule loadedModule : loadedModules) {
            if (loadedModule.getModuleName().equals(
                    module.getModuleName()
            )) {
                throw new IllegalArgumentException(
                        "Bu modul zaten yuklu: "
                                + module.getModuleName()
                );
            }
        }

        loadedModules.add(module);

        LOGGER.info("Modul basariyla yuklendi: " + module.getModuleName());
        System.out.println(
                "Dosya basariyla yuklendi: "
                        + module.getModuleName()
        );
    }

    private void listModules() {
        if (loadedModules.isEmpty()) {
            System.out.println(Messages.CLI_NO_MODULES);
            return;
        }

        System.out.println("Yuklu moduller:");

        for (int index = 0; index < loadedModules.size(); index++) {
            MiniObjectModule module = loadedModules.get(index);

            System.out.println(
                    (index + 1) + ". "
                            + module.getModuleName()
                            + " | section: "
                            + module.getSections().size()
                            + " | sembol: "
                            + module.getDefinitions().size()
                            + " | relocation: "
                            + module.getRelocations().size()
            );
        }
    }

    private void showModuleDetails() {
        MiniObjectModule module = selectModule();

        if (module == null) {
            return;
        }

        System.out.println("\n========== MODUL AYRINTISI ==========");
        System.out.println("Modul Adi: " + module.getModuleName());

        printSections(module);
        printDefinitions(module);
        printExternals(module);
        printRelocations(module);

        System.out.println("======================================");
    }

    private void printSections(MiniObjectModule module) {
        System.out.println("\nSection'lar:");

        if (module.getSections().isEmpty()) {
            System.out.println("  Section yok.");
            return;
        }

        for (Map.Entry<minilinker.model.SectionType, ObjectSection> entry
                : module.getSections().entrySet()) {
            ObjectSection section = entry.getValue();

            System.out.println(
                    "  " + section.getType().getName()
                            + " | word sayısı: "
                            + section.wordCount()
                            + " | WORDS: "
                            + section.toHexWords()
            );
        }
    }

    private void printDefinitions(MiniObjectModule module) {
        System.out.println("\nDEFINE sembolleri:");

        if (module.getDefinitions().isEmpty()) {
            System.out.println("  DEFINE yok.");
            return;
        }

        for (SymbolDefinition definition
                : module.getDefinitions().values()) {
            System.out.println(
                    "  " + definition.getName()
                            + " -> "
                            + definition.getSection().getName()
                            + "["
                            + definition.getWordOffset()
                            + "]"
            );
        }
    }

    private void printExternals(MiniObjectModule module) {
        System.out.println("\nEXTERN sembolleri:");

        if (module.getExternals().isEmpty()) {
            System.out.println("  EXTERN yok.");
            return;
        }

        for (String external : module.getExternals()) {
            System.out.println("  " + external);
        }
    }

    private void printRelocations(MiniObjectModule module) {
        System.out.println("\nRELOC kayıtları:");

        if (module.getRelocations().isEmpty()) {
            System.out.println("  RELOC yok.");
            return;
        }

        for (RelocationEntry relocation : module.getRelocations()) {
            System.out.println(
                    "  " + relocation.getSection().getName()
                            + "[" + relocation.getWordOffset() + "] "
                            + relocation.getType()
                            + " -> "
                            + relocation.getSymbolName()
            );
        }
    }

    private void validateModules() {
        if (loadedModules.isEmpty()) {
            System.out.println("Doğrulanacak modül yok.");
            return;
        }

        for (MiniObjectModule module : loadedModules) {
            module.validate();
            System.out.println(
                    "OK: " + module.getModuleName()
            );
        }

        System.out.println(Messages.CLI_MODULES_VALID);
    }

    private void linkModules() throws IOException {
        if (loadedModules.size() < 2) {
            System.out.println(
                    "Link icin en az 2 modul yuklu olmalidir. "
                            + "Su an: " + loadedModules.size()
            );
            return;
        }

        String hexFile = readLine(
                "Cikti memory hex dosyasi (varsayilan: memory.hex): "
        );

        if (hexFile.isBlank()) {
            hexFile = "memory.hex";
        }

        String mapFile = readLine(
                "Cikti map dosyasi (varsayilan: link.map): "
        );

        if (mapFile.isBlank()) {
            mapFile = "link.map";
        }

        MiniLinker linker = new MiniLinker();

        MiniLinker.LinkResult result =
                linker.linkModules(loadedModules);

        linker.writeOutputs(
                result,
                Path.of(hexFile),
                Path.of(mapFile)
        );

        System.out.println(Messages.CLI_LINK_SUCCESS);
        System.out.println(
                "Olusturulan dosyalar: " + hexFile + ", " + mapFile
        );
    }

    private void removeModule() {
        MiniObjectModule module = selectModule();

        if (module == null) {
            return;
        }

        loadedModules.remove(module);

        System.out.println(
                "Modul kaldirildi: " + module.getModuleName()
        );
    }

    private MiniObjectModule selectModule() {
        if (loadedModules.isEmpty()) {
            System.out.println(Messages.CLI_NO_MODULES);
            return null;
        }

        listModules();
        int selectedIndex = readInteger(
                "Modül numarası: ",
                1,
                loadedModules.size()
        );

        return loadedModules.get(selectedIndex - 1);
    }

    private int readInteger(
            String prompt,
            int minimum,
            int maximum
    ) {
        String value = readLine(prompt);

        try {
            int number = Integer.parseInt(value);

            if (number < minimum || number > maximum) {
                throw new IllegalArgumentException(
                        "Lütfen " + minimum + " ile "
                                + maximum + " arasında bir sayı girin."
                );
            }

            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Geçerli bir sayı girilmelidir."
            );
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);

        if (!scanner.hasNextLine()) {
            return "0";
        }

        return scanner.nextLine().trim();
    }

    private static String removeOptionalQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"")
                && value.endsWith("\""))
                || (value.startsWith("'")
                && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}
