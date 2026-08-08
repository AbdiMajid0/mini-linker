package minilinker.model;

import java.util.Objects;

/**
 * Pipeline v2'de kullanılan relocation kaydı.
 *
 * <p>Bir section içinde belirli bir offset'teki word'ün,
 * belirtilen sembolün adresiyle güncellenmesi gerektiğini belirtir.</p>
 */
public class Relocation {

    private final SectionType section;
    private final int offset;
    private final RelocationType type;
    private final String symbolName;

    public Relocation(
            SectionType section,
            int offset,
            RelocationType type,
            String symbolName
    ) {
        this.section = Objects.requireNonNull(
                section,
                "Relocation section bilgisi boş olamaz."
        );

        if (offset < 0 || offset > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Relocation offset 16-bit aralıkta olmalıdır."
            );
        }

        this.offset = offset;

        this.type = Objects.requireNonNull(
                type,
                "Relocation türü boş olamaz."
        );

        if (symbolName == null || symbolName.isBlank()) {
            throw new IllegalArgumentException(
                    "Relocation sembol adı boş olamaz."
            );
        }

        this.symbolName = symbolName;
    }

    public SectionType getSection() {
        return section;
    }

    public String getSectionName() {
        return section.getName();
    }

    public int getOffset() {
        return offset;
    }

    public RelocationType getType() {
        return type;
    }

    public String getSymbolName() {
        return symbolName;
    }

    @Override
    public String toString() {
        return "Relocation{" +
                "section=" + section +
                ", offset=" + offset +
                ", type=" + type +
                ", symbolName='" + symbolName + '\'' +
                '}';
    }
}