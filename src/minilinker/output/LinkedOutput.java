package minilinker.output;

import minilinker.linker.SectionLayout;
import minilinker.linker.SymbolTable;
import minilinker.model.ObjectFile;
import minilinker.model.Section;
import minilinker.model.SectionType;
import minilinker.util.HexUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Linkleme işlemi sonucu oluşan bellek görüntüsünü temsil eder.
 *
 * <p>Object dosyalarının section verilerini yerleşim planına göre
 * tek bir bellek dizisinde birleştirir. Hex dump çıktısı üretebilir.</p>
 */
public class LinkedOutput {

    private final List<ObjectFile> objectFiles;
    private final SectionLayout sectionLayout;
    private final SymbolTable symbolTable;
    private final int[] memory;

    public LinkedOutput(
            List<ObjectFile> objectFiles,
            SectionLayout sectionLayout,
            SymbolTable symbolTable
    ) {
        Objects.requireNonNull(
                objectFiles,
                "Object file listesi boş olamaz."
        );

        Objects.requireNonNull(
                sectionLayout,
                "Section layout boş olamaz."
        );

        Objects.requireNonNull(
                symbolTable,
                "Symbol table boş olamaz."
        );

        if (objectFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "En az bir object file bulunmalıdır."
            );
        }

        List<ObjectFile> copiedObjectFiles = new ArrayList<>();

        for (ObjectFile objectFile : objectFiles) {
            if (objectFile == null) {
                throw new IllegalArgumentException(
                        "Object file boş olamaz."
                );
            }

            copiedObjectFiles.add(objectFile);
        }

        this.objectFiles = Collections.unmodifiableList(
                copiedObjectFiles
        );

        this.sectionLayout = sectionLayout;
        this.symbolTable = symbolTable;
        this.memory = new int[sectionLayout.getTotalSize()];

        buildMemoryImage();
    }

    private void buildMemoryImage() {
        for (ObjectFile objectFile : objectFiles) {
            for (Section section : objectFile.getSections()) {
                SectionType sectionType = section.getType();

                int baseAddress =
                        sectionLayout.getSectionAddress(
                                objectFile,
                                sectionType
                        );

                for (int offset = 0;
                     offset < section.getWordCount();
                     offset++) {

                    int address = baseAddress + offset;

                    if (address < 0 || address >= memory.length) {
                        throw new IllegalArgumentException(
                                "Bellek adresi geçersiz: " + address
                        );
                    }

                    memory[address] = section.getWord(offset);
                }
            }
        }
    }

    public List<ObjectFile> getObjectFiles() {
        return objectFiles;
    }

    public SectionLayout getSectionLayout() {
        return sectionLayout;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public int getWord(int address) {
        validateAddress(address);
        return memory[address];
    }

    public int[] getMemory() {
        return memory.clone();
    }

    public List<Integer> getWords() {
        List<Integer> result = new ArrayList<>();

        for (int word : memory) {
            result.add(word);
        }

        return Collections.unmodifiableList(result);
    }

    public int getMemorySize() {
        return memory.length;
    }

    public int getTotalSize() {
        return memory.length;
    }

    public boolean isEmpty() {
        return memory.length == 0;
    }

    public boolean containsAddress(int address) {
        return address >= 0 && address < memory.length;
    }

    public int getSectionWord(
            ObjectFile objectFile,
            SectionType sectionType,
            int offset
    ) {
        Objects.requireNonNull(
                objectFile,
                "Object file boş olamaz."
        );

        Objects.requireNonNull(
                sectionType,
                "Section türü boş olamaz."
        );

        Section section = objectFile.getSection(sectionType);

        if (section == null) {
            throw new IllegalArgumentException(
                    "Section bulunamadı: " + sectionType.getName()
            );
        }

        if (offset < 0 || offset >= section.getWordCount()) {
            throw new IndexOutOfBoundsException(
                    "Section offset sınırları dışında: " + offset
            );
        }

        int baseAddress =
                sectionLayout.getSectionAddress(
                        objectFile,
                        sectionType
                );

        return getWord(baseAddress + offset);
    }

    public int getSymbolAddress(
            ObjectFile objectFile,
            String symbolName
    ) {
        return symbolTable.resolveAddress(
                objectFile,
                symbolName
        );
    }

    public String toHexDump() {
        StringBuilder result = new StringBuilder();

        for (int address = 0;
             address < memory.length;
             address += 8) {

            if (address > 0) {
                result.append(System.lineSeparator());
            }

            result.append(formatHex(address))
                    .append(":");

            int end = Math.min(address + 8, memory.length);

            for (int current = address;
                 current < end;
                 current++) {

                result.append(" ")
                        .append(formatHex(memory[current]));
            }
        }

        return result.toString();
    }

    private void validateAddress(int address) {
        if (address < 0 || address >= memory.length) {
            throw new IndexOutOfBoundsException(
                    "Bellek adresi sınırları dışında: " + address
            );
        }
    }

    private static String formatHex(int value) {
        return HexUtils.formatWord(value);
    }

    @Override
    public String toString() {
        return toHexDump();
    }
}