package minilinker.linker;

import minilinker.model.ObjectFile;
import minilinker.model.Section;
import minilinker.model.SectionType;
import minilinker.model.Symbol;
import minilinker.util.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Object dosyalarının section'larını 16-bit adres alanına yerleştirir.
 *
 * <p>Standart linker konvansiyonuna göre önce bütün .text section'ları,
 * ardından bütün .data section'ları sıralı olarak yerleştirilir.</p>
 */
public class SectionLayout {

    public static final int START_ADDRESS = 0x0000;
    public static final int MAX_ADDRESS = 0xFFFF;

    private static final int ADDRESS_LIMIT = 0x10000;

    private final Map<ObjectFile, EnumMap<SectionType, Integer>> sectionBases;
    private int totalSize;

    public SectionLayout() {
        this.sectionBases = new LinkedHashMap<>();
        this.totalSize = 0;
    }

    /**
     * Verilen object dosyalarının section'larını belleğe yerleştirir.
     *
     * <p>Yerleşim sırası: önce bütün .text section'ları,
     * sonra bütün .data section'ları (standart linker konvansiyonu).</p>
     *
     * @param objectFiles yerleştirilecek object dosyaları
     */
    public void layout(List<ObjectFile> objectFiles) {
        Objects.requireNonNull(
                objectFiles,
                "Object file listesi boş olamaz."
        );

        Map<ObjectFile, EnumMap<SectionType, Integer>> newLayout =
                new LinkedHashMap<>();

        Set<String> moduleNames = new HashSet<>();
        int currentAddress = START_ADDRESS;

        for (ObjectFile objectFile : objectFiles) {
            if (objectFile == null) {
                throw new IllegalArgumentException(
                        Messages.ERR_MODULE_NULL
                );
            }

            if (!moduleNames.add(objectFile.getModuleName())) {
                throw new IllegalArgumentException(
                        Messages.ERR_DUPLICATE_MODULE
                                + objectFile.getModuleName()
                );
            }

            newLayout.put(
                    objectFile,
                    new EnumMap<>(SectionType.class)
            );
        }

        /*
         * Yerleşim sırası (standart linker konvansiyonu):
         *
         * 1. Bütün .text section'ları
         * 2. Bütün .data section'ları
         */
        for (SectionType sectionType : SectionType.values()) {
            for (ObjectFile objectFile : objectFiles) {
                currentAddress = placeSection(
                        objectFile,
                        sectionType,
                        newLayout.get(objectFile),
                        currentAddress
                );
            }
        }

        sectionBases.clear();
        sectionBases.putAll(newLayout);
        totalSize = currentAddress;
    }


    private int placeSection(
            ObjectFile objectFile,
            SectionType sectionType,
            EnumMap<SectionType, Integer> bases,
            int currentAddress
    ) {
        Section section = objectFile.getSection(sectionType);

        if (section == null) {
            return currentAddress;
        }

        long endAddress =
                (long) currentAddress + section.getWordCount();

        if (endAddress > ADDRESS_LIMIT) {
            throw new IllegalArgumentException(
                    "Bellek adres alanı aşıldı. Module: "
                            + objectFile.getModuleName()
                            + ", section: "
                            + sectionType.getName()
            );
        }

        bases.put(sectionType, currentAddress);

        return (int) endAddress;
    }

    public int getSectionAddress(
            ObjectFile objectFile,
            SectionType sectionType
    ) {
        Objects.requireNonNull(
                objectFile,
                "Object file boş olamaz."
        );

        Objects.requireNonNull(
                sectionType,
                "Section türü boş olamaz."
        );

        EnumMap<SectionType, Integer> bases =
                sectionBases.get(objectFile);

        if (bases == null) {
            throw new IllegalArgumentException(
                    "Bu object file için layout yapılmamış: "
                            + objectFile.getModuleName()
            );
        }

        Integer address = bases.get(sectionType);

        if (address == null) {
            throw new IllegalArgumentException(
                    "Section bulunamadı: "
                            + sectionType.getName()
            );
        }

        return address;
    }

    public int getAbsoluteAddress(
            ObjectFile objectFile,
            Symbol symbol
    ) {
        Objects.requireNonNull(
                symbol,
                "Symbol boş olamaz."
        );

        if (!symbol.isDefined()) {
            throw new IllegalArgumentException(
                    "EXTERN sembolün section adresi çözülemez: "
                            + symbol.getName()
            );
        }

        int sectionAddress = getSectionAddress(
                objectFile,
                symbol.getSection()
        );

        long absoluteAddress =
                (long) sectionAddress + symbol.getOffset();

        if (absoluteAddress > MAX_ADDRESS) {
            throw new IllegalArgumentException(
                    "Sembol adresi 16-bit adres alanını aşıyor: "
                            + symbol.getName()
            );
        }

        return (int) absoluteAddress;
    }

    public boolean hasSectionAddress(
            ObjectFile objectFile,
            SectionType sectionType
    ) {
        if (objectFile == null || sectionType == null) {
            return false;
        }

        EnumMap<SectionType, Integer> bases =
                sectionBases.get(objectFile);

        return bases != null && bases.containsKey(sectionType);
    }

    public int getTotalSize() {
        return totalSize;
    }

    public boolean isEmpty() {
        return sectionBases.isEmpty();
    }

    public List<ObjectFile> getObjectFiles() {
        return Collections.unmodifiableList(
                new ArrayList<>(sectionBases.keySet())
        );
    }

    @Override
    public String toString() {
        return "SectionLayout{" +
                "sectionBases=" + sectionBases +
                ", totalSize=" + totalSize +
                '}';
    }
}