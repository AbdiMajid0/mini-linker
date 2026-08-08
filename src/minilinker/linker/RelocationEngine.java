package minilinker.linker;

import minilinker.model.ObjectFile;
import minilinker.model.Relocation;
import minilinker.model.RelocationType;
import minilinker.model.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Relocation kayıtlarını işleyerek section verilerini günceller.
 *
 * <p>ABS16 ve REL16 relocation türlerini destekler. ABS16 mutlak adres,
 * REL16 göreli adres (hedef - (mevcut + 1)) hesaplar.</p>
 */
public class RelocationEngine {

    private static final int MIN_SIGNED_16 = -32768;
    private static final int MAX_SIGNED_16 = 32767;

    public void apply(
            List<ObjectFile> objectFiles,
            SectionLayout layout,
            SymbolTable symbolTable
    ) {
        Objects.requireNonNull(
                objectFiles,
                "Object file listesi boş olamaz."
        );

        Objects.requireNonNull(
                layout,
                "Section layout boş olamaz."
        );

        Objects.requireNonNull(
                symbolTable,
                "Symbol table boş olamaz."
        );

        List<RelocationPatch> patches = new ArrayList<>();

        for (ObjectFile objectFile : objectFiles) {
            if (objectFile == null) {
                throw new IllegalArgumentException(
                        "Object file boş olamaz."
                );
            }

            for (Relocation relocation :
                    objectFile.getRelocations()) {

                patches.add(
                        preparePatch(
                                objectFile,
                                relocation,
                                layout,
                                symbolTable
                        )
                );
            }
        }

        for (RelocationPatch patch : patches) {
            patch.section.setWord(
                    patch.offset,
                    patch.value
            );
        }
    }

    private RelocationPatch preparePatch(
            ObjectFile objectFile,
            Relocation relocation,
            SectionLayout layout,
            SymbolTable symbolTable
    ) {
        Section section =
                objectFile.getSection(
                        relocation.getSection()
                );

        if (section == null) {
            throw new IllegalArgumentException(
                    "Relocation section bulunamadı: "
                            + relocation.getSectionName()
            );
        }

        int offset = relocation.getOffset();

        if (offset < 0 || offset >= section.getWordCount()) {
            throw new IllegalArgumentException(
                    "Relocation offset section sınırları dışında: "
                            + offset
            );
        }

        int sectionBase =
                layout.getSectionAddress(
                        objectFile,
                        relocation.getSection()
                );

        int placeAddress = sectionBase + offset;

        int symbolAddress =
                symbolTable.resolveAddress(
                        objectFile,
                        relocation.getSymbolName()
                );

        int relocatedValue;

        if (relocation.getType() == RelocationType.ABS16) {
            relocatedValue = calculateAbsoluteValue(
                    symbolAddress,
                    relocation
            );
        } else if (relocation.getType() == RelocationType.REL16) {
            relocatedValue = calculateRelativeValue(
                    symbolAddress,
                    placeAddress,
                    relocation
            );
        } else {
            throw new IllegalArgumentException(
                    "Desteklenmeyen relocation türü: "
                            + relocation.getType()
            );
        }

        return new RelocationPatch(
                section,
                offset,
                relocatedValue & 0xFFFF
        );
    }

    private int calculateAbsoluteValue(
            int symbolAddress,
            Relocation relocation
    ) {
        if (symbolAddress < 0 || symbolAddress > 0xFFFF) {
            throw new IllegalArgumentException(
                    "ABS16 adresi 16-bit aralıkta değil: "
                            + relocation.getSymbolName()
            );
        }

        return symbolAddress;
    }

    private int calculateRelativeValue(
            int symbolAddress,
            int placeAddress,
            Relocation relocation
    ) {
        int nextWordAddress = placeAddress + 1;
        int relativeValue = symbolAddress - nextWordAddress;

        if (relativeValue < MIN_SIGNED_16
                || relativeValue > MAX_SIGNED_16) {

            throw new IllegalArgumentException(
                    "REL16 değeri 16-bit signed aralığı aşıyor: "
                            + relocation.getSymbolName()
            );
        }

        return relativeValue;
    }

    private static final class RelocationPatch {

        private final Section section;
        private final int offset;
        private final int value;

        private RelocationPatch(
                Section section,
                int offset,
                int value
        ) {
            this.section = section;
            this.offset = offset;
            this.value = value;
        }
    }
}