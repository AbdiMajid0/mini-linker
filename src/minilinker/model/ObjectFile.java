package minilinker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/**
 * Linker pipeline v2 tarafından kullanılan object dosyası temsili.
 *
 * <p>Section, Symbol ve Relocation nesnelerini barındırır.
 * {@link ModuleConverter} aracılığıyla {@link MiniObjectModule}
 * nesnesinden dönüştürülebilir.</p>
 */
public class ObjectFile {

    private final String moduleName;
    private final EnumMap<SectionType, Section> sections;
    private final List<Symbol> symbols;
    private final List<Relocation> relocations;

    public ObjectFile(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            throw new IllegalArgumentException(
                    "Modül adı boş olamaz."
            );
        }

        this.moduleName = moduleName.trim();
        this.sections = new EnumMap<>(SectionType.class);
        this.symbols = new ArrayList<>();
        this.relocations = new ArrayList<>();
    }

    public String getModuleName() {
        return moduleName;
    }

    public void addSection(Section section) {
        Objects.requireNonNull(
                section,
                "Section boş olamaz."
        );

        SectionType type = section.getType();

        if (sections.containsKey(type)) {
            throw new IllegalArgumentException(
                    "Bu section zaten mevcut: " + type.getName()
            );
        }

        sections.put(type, section);
    }

    public Section getOrCreateSection(SectionType type) {
        Objects.requireNonNull(
                type,
                "Section türü boş olamaz."
        );

        return sections.computeIfAbsent(
                type,
                Section::new
        );
    }

    public Section getSection(SectionType type) {
        Objects.requireNonNull(
                type,
                "Section türü boş olamaz."
        );

        return sections.get(type);
    }

    public boolean hasSection(SectionType type) {
        return type != null && sections.containsKey(type);
    }

    public List<Section> getSections() {
        List<Section> result = new ArrayList<>();

        for (SectionType type : SectionType.values()) {
            Section section = sections.get(type);

            if (section != null) {
                result.add(section);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public void addSymbol(Symbol symbol) {
        Objects.requireNonNull(
                symbol,
                "Symbol boş olamaz."
        );

        if (findSymbol(symbol.getName()) != null) {
            throw new IllegalArgumentException(
                    "Aynı sembol birden fazla kez tanımlanamaz: "
                            + symbol.getName()
            );
        }

        symbols.add(symbol);
    }

    public Symbol findSymbol(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        for (Symbol symbol : symbols) {
            if (symbol.getName().equals(name)) {
                return symbol;
            }
        }

        return null;
    }

    public boolean hasSymbol(String name) {
        return findSymbol(name) != null;
    }

    public List<Symbol> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    public void addRelocation(Relocation relocation) {
        Objects.requireNonNull(
                relocation,
                "Relocation boş olamaz."
        );

        relocations.add(relocation);
    }

    public List<Relocation> getRelocations() {
        return Collections.unmodifiableList(relocations);
    }

    @Override
    public String toString() {
        return "ObjectFile{" +
                "moduleName='" + moduleName + '\'' +
                ", sections=" + sections.values() +
                ", symbols=" + symbols +
                ", relocations=" + relocations +
                '}';
    }
}