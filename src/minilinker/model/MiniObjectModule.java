package minilinker.model;

import java.util.ArrayList;
import minilinker.util.Messages;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parser tarafından üretilen object modül temsili (v1).
 *
 * <p>Bir MINIOBJ dosyasından okunan section, sembol tanımları (DEFINE),
 * dış referanslar (EXTERN) ve relocation kayıtlarını tutar.</p>
 *
 * @see minilinker.parser.ObjectFileParser
 * @see minilinker.model.ModuleConverter
 */
public final class MiniObjectModule {

    private final String moduleName;
    private final Map<SectionType, ObjectSection> sections;
    private final Map<String, SymbolDefinition> definitions;
    private final Set<String> externals;
    private final List<RelocationEntry> relocations;

    public MiniObjectModule(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            throw new IllegalArgumentException(Messages.ERR_MODULE_NAME_EMPTY);
        }

        this.moduleName = moduleName;
        this.sections = new EnumMap<>(SectionType.class);
        this.definitions = new LinkedHashMap<>();
        this.externals = new LinkedHashSet<>();
        this.relocations = new ArrayList<>();
    }

    public String getModuleName() {
        return moduleName;
    }

    public void addSection(ObjectSection section) {
        Objects.requireNonNull(section, "Section boş olamaz");

        if (sections.putIfAbsent(section.getType(), section) != null) {
            throw new IllegalArgumentException(
                    "Section birden fazla tanımlanamaz: "
                            + section.getType()
            );
        }
    }

    public ObjectSection getSection(SectionType type) {
        return sections.get(
                Objects.requireNonNull(type, "Section türü boş olamaz")
        );
    }

    public Map<SectionType, ObjectSection> getSections() {
        return Collections.unmodifiableMap(sections);
    }

    public void defineSymbol(SymbolDefinition definition) {
        Objects.requireNonNull(
                definition,
                "Sembol tanımı boş olamaz"
        );

        if (externals.contains(definition.getName())) {
            throw new IllegalArgumentException(
                    "Sembol hem EXTERN hem DEFINE olamaz: "
                            + definition.getName()
            );
        }

        if (definitions.putIfAbsent(
                definition.getName(),
                definition
        ) != null) {
            throw new IllegalArgumentException(
                    "Sembol birden fazla tanımlanamaz: "
                            + definition.getName()
            );
        }
    }

    public Map<String, SymbolDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public void addExternal(String symbolName) {
        if (symbolName == null || symbolName.isBlank()) {
            throw new IllegalArgumentException(
                    "Sembol adı boş olamaz"
            );
        }

        if (definitions.containsKey(symbolName)) {
            throw new IllegalArgumentException(
                    "Sembol hem DEFINE hem EXTERN olamaz: "
                            + symbolName
            );
        }

        if (!externals.add(symbolName)) {
            throw new IllegalArgumentException(
                    "EXTERN sembolü tekrar edemez: "
                            + symbolName
            );
        }
    }

    public Set<String> getExternals() {
        return Collections.unmodifiableSet(externals);
    }

    public void addRelocation(RelocationEntry relocation) {
        relocations.add(
                Objects.requireNonNull(
                        relocation,
                        "Relocation boş olamaz"
                )
        );
    }

    public List<RelocationEntry> getRelocations() {
        return Collections.unmodifiableList(relocations);
    }

    public void validate() {
        if (sections.isEmpty()) {
            throw new IllegalStateException(
                    "Modülde en az bir section bulunmalıdır"
            );
        }

        for (SymbolDefinition definition :
                definitions.values()) {

            ObjectSection section =
                    sections.get(definition.getSection());

            if (section == null) {
                throw new IllegalStateException(
                        "Sembolün section'ı bulunamadı: "
                                + definition
                );
            }

            if (definition.getWordOffset()
                    >= section.wordCount()) {
                throw new IllegalStateException(
                        "Sembol offset'i section sınırları dışında: "
                                + definition
                );
            }
        }

        for (RelocationEntry relocation : relocations) {
            ObjectSection section =
                    sections.get(relocation.getSection());

            if (section == null) {
                throw new IllegalStateException(
                        "Relocation section'ı bulunamadı: "
                                + relocation
                );
            }

            if (relocation.getWordOffset()
                    >= section.wordCount()) {
                throw new IllegalStateException(
                        "Relocation offset'i section sınırları dışında: "
                                + relocation
                );
            }

            String symbolName =
                    relocation.getSymbolName();

            if (!definitions.containsKey(symbolName)
                    && !externals.contains(symbolName)) {
                throw new IllegalStateException(
                        "Relocation sembolü tanımsız: "
                                + symbolName
                );
            }
        }
    }
}