package minilinker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link MiniObjectModule} nesnesini {@link ObjectFile} nesnesine dönüştürür.
 *
 * <p>Bu sınıf, parser tarafından üretilen v1 modeli (MiniObjectModule) ile
 * linker pipeline v2 (SectionLayout, SymbolTable, RelocationEngine, LinkedOutput,
 * MapFileWriter) tarafından kullanılan v2 modeli (ObjectFile) arasında
 * köprü görevi görür.</p>
 */
public final class ModuleConverter {

    private ModuleConverter() {
    }

    /**
     * Bir {@link MiniObjectModule} nesnesini {@link ObjectFile} nesnesine dönüştürür.
     *
     * @param module dönüştürülecek modül (boş olamaz)
     * @return dönüştürülmüş ObjectFile nesnesi
     * @throws NullPointerException module null ise
     */
    public static ObjectFile convert(MiniObjectModule module) {
        Objects.requireNonNull(module, "Modül boş olamaz.");

        ObjectFile objectFile = new ObjectFile(module.getModuleName());

        convertSections(module, objectFile);
        convertSymbols(module, objectFile);
        convertRelocations(module, objectFile);

        return objectFile;
    }

    /**
     * Birden fazla {@link MiniObjectModule} nesnesini dönüştürür.
     *
     * @param modules dönüştürülecek modül listesi
     * @return dönüştürülmüş ObjectFile listesi
     */
    public static List<ObjectFile> convertAll(
            List<MiniObjectModule> modules
    ) {
        Objects.requireNonNull(modules, "Modül listesi boş olamaz.");

        List<ObjectFile> result = new ArrayList<>();

        for (MiniObjectModule module : modules) {
            result.add(convert(module));
        }

        return result;
    }

    /**
     * Section'ları dönüştürür (ObjectSection → Section).
     */
    private static void convertSections(
            MiniObjectModule module,
            ObjectFile objectFile
    ) {
        for (Map.Entry<SectionType, ObjectSection> entry
                : module.getSections().entrySet()) {

            ObjectSection objectSection = entry.getValue();
            Section section = new Section(objectSection.getType());

            section.addWords(objectSection.getWords());
            objectFile.addSection(section);
        }
    }

    /**
     * DEFINE sembollerini ve EXTERN sembollerini dönüştürür.
     *
     * <p>DEFINE sembolleri GLOBAL binding ile, EXTERN sembolleri
     * EXTERN binding ile oluşturulur.</p>
     */
    private static void convertSymbols(
            MiniObjectModule module,
            ObjectFile objectFile
    ) {
        for (SymbolDefinition definition
                : module.getDefinitions().values()) {

            Symbol symbol = Symbol.defined(
                    definition.getName(),
                    SymbolBinding.GLOBAL,
                    definition.getSection(),
                    definition.getWordOffset()
            );

            objectFile.addSymbol(symbol);
        }

        for (String external : module.getExternals()) {
            objectFile.addSymbol(Symbol.extern(external));
        }
    }

    /**
     * RelocationEntry nesnelerini Relocation nesnelerine dönüştürür.
     */
    private static void convertRelocations(
            MiniObjectModule module,
            ObjectFile objectFile
    ) {
        for (RelocationEntry entry : module.getRelocations()) {
            Relocation relocation = new Relocation(
                    entry.getSection(),
                    entry.getWordOffset(),
                    entry.getType(),
                    entry.getSymbolName()
            );

            objectFile.addRelocation(relocation);
        }
    }
}
