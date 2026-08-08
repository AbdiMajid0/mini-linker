package minilinker.model;

import java.util.Objects;

/**
 * Bir object dosyasındaki sembol tanımını veya dış referansı temsil eder.
 *
 * <p>LOCAL, GLOBAL veya EXTERN binding türüne sahip olabilir.
 * EXTERN semboller section ve offset içermez.</p>
 */
public class Symbol {

    private final String name;
    private final SymbolBinding binding;
    private final SectionType section;
    private final int offset;

    public Symbol(
            String name,
            SymbolBinding binding,
            SectionType section,
            int offset
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Sembol adı boş olamaz."
            );
        }

        this.name = name;
        this.binding = Objects.requireNonNull(
                binding,
                "Sembol bağlanma türü boş olamaz."
        );

        if (binding == SymbolBinding.EXTERN) {
            if (section != null || offset != -1) {
                throw new IllegalArgumentException(
                        "EXTERN sembol section ve offset içeremez."
                );
            }
        } else {
            if (section == null) {
                throw new IllegalArgumentException(
                        "Tanımlı sembolün section bilgisi olmalıdır."
                );
            }

            if (offset < 0 || offset > 0xFFFF) {
                throw new IllegalArgumentException(
                        "Offset 16-bit aralıkta olmalıdır."
                );
            }
        }

        this.section = section;
        this.offset = offset;
    }

    public static Symbol defined(
            String name,
            SymbolBinding binding,
            SectionType section,
            int offset
    ) {
        if (binding == SymbolBinding.EXTERN) {
            throw new IllegalArgumentException(
                    "EXTERN sembol defined olarak oluşturulamaz."
            );
        }

        return new Symbol(name, binding, section, offset);
    }

    public static Symbol extern(String name) {
        return new Symbol(
                name,
                SymbolBinding.EXTERN,
                null,
                -1
        );
    }

    public String getName() {
        return name;
    }

    public SymbolBinding getBinding() {
        return binding;
    }

    public SectionType getSection() {
        return section;
    }

    public String getSectionName() {
        return section == null ? null : section.getName();
    }

    public int getOffset() {
        return offset;
    }

    public boolean isExtern() {
        return binding == SymbolBinding.EXTERN;
    }

    public boolean isDefined() {
        return section != null;
    }

    public boolean isGlobal() {
        return binding == SymbolBinding.GLOBAL;
    }

    public boolean isLocal() {
        return binding == SymbolBinding.LOCAL;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", binding=" + binding +
                ", section=" + section +
                ", offset=" + offset +
                '}';
    }
}