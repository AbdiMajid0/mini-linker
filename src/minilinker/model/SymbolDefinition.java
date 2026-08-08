package minilinker.model;

import java.util.Objects;

/**
 * Pipeline v1'de kullanılan DEFINE sembol tanımı.
 *
 * <p>Sembol adını, ait olduğu section türünü ve
 * section içindeki word offset'ini tutar.</p>
 */
public final class SymbolDefinition {

    private final String name;
    private final SectionType section;
    private final int wordOffset;

    public SymbolDefinition(
            String name,
            SectionType section,
            int wordOffset
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Sembol adı boş olamaz"
            );
        }

        if (wordOffset < 0) {
            throw new IllegalArgumentException(
                    "Sembol offset'i negatif olamaz: "
                            + wordOffset
            );
        }

        this.name = name.trim();
        this.section = Objects.requireNonNull(
                section,
                "Section boş olamaz"
        );
        this.wordOffset = wordOffset;
    }

    public String getName() {
        return name;
    }

    public SectionType getSection() {
        return section;
    }

    public int getWordOffset() {
        return wordOffset;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof SymbolDefinition)) {
            return false;
        }

        SymbolDefinition other =
                (SymbolDefinition) object;

        return wordOffset == other.wordOffset
                && name.equals(other.name)
                && section == other.section;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                section,
                wordOffset
        );
    }

    @Override
    public String toString() {
        return name + "="
                + section + ":"
                + wordOffset;
    }
}