package minilinker.model;

/**
 * Section türlerini tanımlayan enum.
 *
 * <p>{@code .text} (kod) ve {@code .data} (veri) section'ları
 * desteklenir.</p>
 */
public enum SectionType {

    TEXT(".text"),
    DATA(".data");

    private final String name;

    SectionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SectionType fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Section adı boş olamaz."
            );
        }

        for (SectionType type : values()) {
            if (type.name.equals(name.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Desteklenmeyen section: " + name
        );
    }

    @Override
    public String toString() {
        return name;
    }
}