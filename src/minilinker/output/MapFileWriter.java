package minilinker.output;

import minilinker.linker.SectionLayout;
import minilinker.linker.SymbolTable;
import minilinker.model.ObjectFile;
import minilinker.model.Relocation;
import minilinker.model.Section;
import minilinker.model.Symbol;
import minilinker.util.HexUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Linkleme sonuçlarını okunabilir bir map dosyasına yazan sınıf.
 *
 * <p>Section yerleşimi, sembol tablosu, relocation bilgileri ve
 * bellek görüntüsünü içeren detaylı bir rapor üretir.</p>
 */
public class MapFileWriter {

    public void write(
            LinkedOutput linkedOutput,
            String fileName
    ) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Map dosyası adı boş olamaz."
            );
        }

        write(linkedOutput, Path.of(fileName));
    }

    public void write(
            LinkedOutput linkedOutput,
            Path path
    ) throws IOException {
        Objects.requireNonNull(
                linkedOutput,
                "Linked output boş olamaz."
        );

        Objects.requireNonNull(
                path,
                "Map dosyası yolu boş olamaz."
        );

        String mapText = createMap(linkedOutput);

        Files.writeString(
                path,
                mapText,
                StandardCharsets.UTF_8
        );
    }

    public String createMap(LinkedOutput linkedOutput) {
        Objects.requireNonNull(
                linkedOutput,
                "Linked output boş olamaz."
        );

        SectionLayout layout =
                linkedOutput.getSectionLayout();

        SymbolTable symbolTable =
                linkedOutput.getSymbolTable();

        StringBuilder result = new StringBuilder();

        appendLine(result, "MINILINKER MAP FILE");
        appendLine(result, "===================");
        appendLine(result, "");

        appendLine(result, "MEMORY");
        appendLine(result, "------");

        appendLine(
                result,
                "Total words: "
                        + formatHex(linkedOutput.getMemorySize())
        );

        if (linkedOutput.getMemorySize() > 0) {
            appendLine(
                    result,
                    "Address range: 0000 - "
                            + formatHex(
                            linkedOutput.getMemorySize() - 1
                    )
            );
        } else {
            appendLine(result, "Address range: EMPTY");
        }

        appendLine(result, "");

        appendSectionLayout(
                result,
                linkedOutput,
                layout
        );

        appendSymbolTable(
                result,
                linkedOutput,
                symbolTable
        );

        appendRelocations(
                result,
                linkedOutput,
                layout,
                symbolTable
        );

        appendLine(result, "MEMORY IMAGE");
        appendLine(result, "------------");

        String hexDump = linkedOutput.toHexDump();

        if (hexDump.isBlank()) {
            appendLine(result, "EMPTY");
        } else {
            result.append(hexDump)
                    .append(System.lineSeparator());
        }

        return result.toString();
    }

    private void appendSectionLayout(
            StringBuilder result,
            LinkedOutput linkedOutput,
            SectionLayout layout
    ) {
        appendLine(result, "SECTION LAYOUT");
        appendLine(result, "--------------");

        appendLine(
                result,
                String.format(
                        Locale.ROOT,
                        "%-20s %-10s %-8s %-8s %-8s",
                        "MODULE",
                        "SECTION",
                        "BASE",
                        "SIZE",
                        "END"
                )
        );

        boolean sectionFound = false;

        for (ObjectFile objectFile :
                linkedOutput.getObjectFiles()) {

            for (Section section :
                    objectFile.getSections()) {

                sectionFound = true;

                int baseAddress =
                        layout.getSectionAddress(
                                objectFile,
                                section.getType()
                        );

                int wordCount =
                        section.getWordCount();

                String endAddress =
                        wordCount == 0
                                ? "----"
                                : formatHex(
                                baseAddress + wordCount - 1
                        );

                appendLine(
                        result,
                        String.format(
                                Locale.ROOT,
                                "%-20s %-10s %-8s %-8d %-8s",
                                objectFile.getModuleName(),
                                section.getType().getName(),
                                formatHex(baseAddress),
                                wordCount,
                                endAddress
                        )
                );
            }
        }

        if (!sectionFound) {
            appendLine(result, "No sections.");
        }

        appendLine(result, "");
    }

    private void appendSymbolTable(
            StringBuilder result,
            LinkedOutput linkedOutput,
            SymbolTable symbolTable
    ) {
        appendLine(result, "SYMBOL TABLE");
        appendLine(result, "------------");

        appendLine(
                result,
                String.format(
                        Locale.ROOT,
                        "%-20s %-20s %-10s %-10s %-8s %-8s",
                        "MODULE",
                        "NAME",
                        "BINDING",
                        "SECTION",
                        "OFFSET",
                        "ADDRESS"
                )
        );

        boolean symbolFound = false;

        for (ObjectFile objectFile :
                linkedOutput.getObjectFiles()) {

            for (Symbol symbol :
                    objectFile.getSymbols()) {

                symbolFound = true;

                String sectionName =
                        symbol.isDefined()
                                ? symbol.getSection().getName()
                                : "EXTERN";

                String offset =
                        symbol.isDefined()
                                ? String.valueOf(symbol.getOffset())
                                : "-";

                int absoluteAddress =
                        symbolTable.resolveAddress(
                                objectFile,
                                symbol.getName()
                        );

                appendLine(
                        result,
                        String.format(
                                Locale.ROOT,
                                "%-20s %-20s %-10s %-10s %-8s %-8s",
                                objectFile.getModuleName(),
                                symbol.getName(),
                                getBindingName(symbol),
                                sectionName,
                                offset,
                                formatHex(absoluteAddress)
                        )
                );
            }
        }

        if (!symbolFound) {
            appendLine(result, "No symbols.");
        }

        appendLine(result, "");
    }

    private void appendRelocations(
            StringBuilder result,
            LinkedOutput linkedOutput,
            SectionLayout layout,
            SymbolTable symbolTable
    ) {
        appendLine(result, "RELOCATIONS");
        appendLine(result, "-----------");

        appendLine(
                result,
                String.format(
                        Locale.ROOT,
                        "%-20s %-10s %-8s %-10s %-20s %-8s",
                        "MODULE",
                        "SECTION",
                        "OFFSET",
                        "PLACE",
                        "SYMBOL",
                        "VALUE"
                )
        );

        boolean relocationFound = false;

        for (ObjectFile objectFile :
                linkedOutput.getObjectFiles()) {

            List<Relocation> relocations =
                    objectFile.getRelocations();

            for (Relocation relocation : relocations) {
                relocationFound = true;

                int sectionBase =
                        layout.getSectionAddress(
                                objectFile,
                                relocation.getSection()
                        );

                int placeAddress =
                        sectionBase + relocation.getOffset();

                int relocatedValue =
                        linkedOutput.getWord(placeAddress);

                symbolTable.resolveAddress(
                        objectFile,
                        relocation.getSymbolName()
                );

                appendLine(
                        result,
                        String.format(
                                Locale.ROOT,
                                "%-20s %-10s %-8d %-10s %-20s %-8s",
                                objectFile.getModuleName(),
                                relocation.getSectionName(),
                                relocation.getOffset(),
                                formatHex(placeAddress),
                                relocation.getSymbolName(),
                                formatHex(relocatedValue)
                        )
                );
            }
        }

        if (!relocationFound) {
            appendLine(result, "No relocations.");
        }

        appendLine(result, "");
    }

    private static String getBindingName(Symbol symbol) {
        if (symbol.isExtern()) {
            return "EXTERN";
        }

        if (symbol.isGlobal()) {
            return "GLOBAL";
        }

        if (symbol.isLocal()) {
            return "LOCAL";
        }

        return "UNKNOWN";
    }

    private static String formatHex(int value) {
        return HexUtils.formatWord(value);
    }

    private static void appendLine(
            StringBuilder builder,
            String line
    ) {
        builder.append(line)
                .append(System.lineSeparator());
    }
}