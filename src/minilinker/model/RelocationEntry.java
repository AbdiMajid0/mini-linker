package minilinker.model;

import java.util.Objects;

/**
 * Pipeline v1'de kullanılan RELOC kaydı.
 *
 * <p>Section, offset, relocation türü (ABS16/REL16) ve
 * hedef sembol adını tutar.</p>
 */
public final class RelocationEntry {

    private final SectionType section;
    private final int wordOffset;
    private final RelocationType type;
    private final String symbolName;

    public RelocationEntry(
            SectionType section,
            int wordOffset,
            RelocationType type,
            String symbolName
    ) {
        this.section = Objects.requireNonNull(
                section,
                "Section boş olamaz"
        );

        this.type = Objects.requireNonNull(
                type,
                "Relocation türü boş olamaz"
        );

        if (wordOffset < 0) {
            throw new IllegalArgumentException(
                    "Relocation offset'i negatif olamaz: "
                            + wordOffset
            );
        }

        if (symbolName == null || symbolName.isBlank()) {
            throw new IllegalArgumentException(
                    "Relocation sembolü boş olamaz"
            );
        }

        this.wordOffset = wordOffset;
        this.symbolName = symbolName.trim();
    }

    public SectionType getSection() {
        return section;
    }

    public int getWordOffset() {
        return wordOffset;
    }

    public RelocationType getType() {
        return type;
    }

    public String getSymbolName() {
        return symbolName;
    }

    @Override
    public String toString() {
        return section + "["
                + wordOffset + "] "
                + type + " "
                + symbolName;
    }
}