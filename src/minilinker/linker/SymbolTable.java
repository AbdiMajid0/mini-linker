package minilinker.linker;

import minilinker.model.ObjectFile;
import minilinker.model.Relocation;
import minilinker.model.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bağlama sürecinde global ve yerel sembolleri yöneten sembol tablosu.
 *
 * <p>Her sembolün mutlak adresini hesaplar, çakışmaları tespit eder
 * ve EXTERN sembollerin çözümlenebilirliğini doğrular.</p>
 */
public class SymbolTable {

    private final Map<String, SymbolEntry> globalSymbols;
    private final Map<ObjectFile, Map<String, SymbolEntry>> localSymbols;
    private boolean built;

    public SymbolTable() {
        this.globalSymbols = new LinkedHashMap<>();
        this.localSymbols = new IdentityHashMap<>();
        this.built = false;
    }

    public void build(
            List<ObjectFile> objectFiles,
            SectionLayout layout
    ) {
        Objects.requireNonNull(
                objectFiles,
                "Object file listesi boş olamaz."
        );

        Objects.requireNonNull(
                layout,
                "Section layout boş olamaz."
        );

        clear();

        Map<String, SymbolEntry> newGlobalSymbols =
                new LinkedHashMap<>();

        Map<ObjectFile, Map<String, SymbolEntry>> newLocalSymbols =
                new IdentityHashMap<>();

        for (ObjectFile objectFile : objectFiles) {
            if (objectFile == null) {
                throw new IllegalArgumentException(
                        "Object file boş olamaz."
                );
            }

            Map<String, SymbolEntry> moduleLocalSymbols =
                    new LinkedHashMap<>();

            newLocalSymbols.put(
                    objectFile,
                    moduleLocalSymbols
            );

            for (Symbol symbol : objectFile.getSymbols()) {

                if (symbol.isExtern()) {
                    continue;
                }

                if (!symbol.isDefined()) {
                    throw new IllegalArgumentException(
                            "Tanımsız sembol bulundu: "
                                    + symbol.getName()
                    );
                }

                int absoluteAddress =
                        layout.getAbsoluteAddress(
                                objectFile,
                                symbol
                        );

                SymbolEntry entry = new SymbolEntry(
                        objectFile,
                        symbol,
                        absoluteAddress
                );

                if (symbol.isLocal()) {
                    if (moduleLocalSymbols.containsKey(
                            symbol.getName()
                    )) {
                        throw new IllegalArgumentException(
                                "Aynı module içinde sembol tekrarı: "
                                        + symbol.getName()
                        );
                    }

                    moduleLocalSymbols.put(
                            symbol.getName(),
                            entry
                    );
                } else if (symbol.isGlobal()) {
                    SymbolEntry previous =
                            newGlobalSymbols.putIfAbsent(
                                    symbol.getName(),
                                    entry
                            );

                    if (previous != null) {
                        throw new IllegalArgumentException(
                                "Global sembol birden fazla tanımlanmış: "
                                        + symbol.getName()
                                        + " ("
                                        + previous.getModuleName()
                                        + " ve "
                                        + objectFile.getModuleName()
                                        + ")"
                        );
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Geçersiz sembol binding türü: "
                                    + symbol.getName()
                    );
                }
            }
        }

        validateExternalSymbols(
                objectFiles,
                newGlobalSymbols
        );

        validateRelocations(
                objectFiles,
                newLocalSymbols,
                newGlobalSymbols
        );

        globalSymbols.putAll(newGlobalSymbols);
        localSymbols.putAll(newLocalSymbols);
        built = true;
    }

    private void validateExternalSymbols(
            List<ObjectFile> objectFiles,
            Map<String, SymbolEntry> globalSymbols
    ) {
        for (ObjectFile objectFile : objectFiles) {
            for (Symbol symbol : objectFile.getSymbols()) {

                if (symbol.isExtern()
                        && !globalSymbols.containsKey(
                        symbol.getName()
                )) {
                    throw new IllegalArgumentException(
                            "Çözülemeyen EXTERN sembol: "
                                    + symbol.getName()
                                    + " (module: "
                                    + objectFile.getModuleName()
                                    + ")"
                    );
                }
            }
        }
    }

    private void validateRelocations(
            List<ObjectFile> objectFiles,
            Map<ObjectFile, Map<String, SymbolEntry>> localSymbols,
            Map<String, SymbolEntry> globalSymbols
    ) {
        for (ObjectFile objectFile : objectFiles) {

            Map<String, SymbolEntry> moduleLocalSymbols =
                    localSymbols.get(objectFile);

            for (Relocation relocation :
                    objectFile.getRelocations()) {

                String symbolName =
                        relocation.getSymbolName();

                boolean localFound =
                        moduleLocalSymbols.containsKey(
                                symbolName
                        );

                boolean globalFound =
                        globalSymbols.containsKey(
                                symbolName
                        );

                if (!localFound && !globalFound) {
                    throw new IllegalArgumentException(
                            "Relocation sembolü çözülemedi: "
                                    + symbolName
                                    + " (module: "
                                    + objectFile.getModuleName()
                                    + ")"
                    );
                }
            }
        }
    }

    public SymbolEntry lookup(
            ObjectFile objectFile,
            String symbolName
    ) {
        requireBuilt();

        Objects.requireNonNull(
                objectFile,
                "Object file boş olamaz."
        );

        validateSymbolName(symbolName);

        if (!localSymbols.containsKey(objectFile)) {
            throw new IllegalArgumentException(
                    "Bu object file symbol table içinde yok: "
                            + objectFile.getModuleName()
            );
        }

        Map<String, SymbolEntry> moduleLocalSymbols =
                localSymbols.get(objectFile);

        SymbolEntry localEntry =
                moduleLocalSymbols.get(symbolName);

        if (localEntry != null) {
            return localEntry;
        }

        return globalSymbols.get(symbolName);
    }

    public int resolveAddress(
            ObjectFile objectFile,
            String symbolName
    ) {
        SymbolEntry entry =
                lookup(objectFile, symbolName);

        if (entry == null) {
            throw new IllegalArgumentException(
                    "Sembol çözümlenemedi: "
                            + symbolName
            );
        }

        return entry.getAbsoluteAddress();
    }

    public boolean hasSymbol(
            ObjectFile objectFile,
            String symbolName
    ) {
        if (!built || objectFile == null
                || symbolName == null
                || symbolName.isBlank()) {
            return false;
        }

        return lookup(objectFile, symbolName) != null;
    }

    public boolean hasGlobalSymbol(String symbolName) {
        if (!built
                || symbolName == null
                || symbolName.isBlank()) {
            return false;
        }

        return globalSymbols.containsKey(symbolName);
    }

    public SymbolEntry getGlobalSymbol(String symbolName) {
        requireBuilt();
        validateSymbolName(symbolName);

        return globalSymbols.get(symbolName);
    }

    public Map<String, SymbolEntry> getGlobalSymbols() {
        requireBuilt();

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(globalSymbols)
        );
    }

    public Map<String, Integer> getGlobalAddresses() {
        requireBuilt();

        Map<String, Integer> addresses =
                new LinkedHashMap<>();

        for (Map.Entry<String, SymbolEntry> entry :
                globalSymbols.entrySet()) {

            addresses.put(
                    entry.getKey(),
                    entry.getValue().getAbsoluteAddress()
            );
        }

        return Collections.unmodifiableMap(addresses);
    }

    public List<SymbolEntry> getEntries() {
        requireBuilt();

        return Collections.unmodifiableList(
                new ArrayList<>(globalSymbols.values())
        );
    }

    public int size() {
        return globalSymbols.size();
    }

    public boolean isBuilt() {
        return built;
    }

    public void clear() {
        globalSymbols.clear();
        localSymbols.clear();
        built = false;
    }

    private void requireBuilt() {
        if (!built) {
            throw new IllegalStateException(
                    "Symbol table henüz oluşturulmadı."
            );
        }
    }

    private static void validateSymbolName(
            String symbolName
    ) {
        if (symbolName == null || symbolName.isBlank()) {
            throw new IllegalArgumentException(
                    "Sembol adı boş olamaz."
            );
        }
    }

    public static final class SymbolEntry {

        private final ObjectFile objectFile;
        private final Symbol symbol;
        private final int absoluteAddress;

        private SymbolEntry(
                ObjectFile objectFile,
                Symbol symbol,
                int absoluteAddress
        ) {
            this.objectFile = objectFile;
            this.symbol = symbol;
            this.absoluteAddress = absoluteAddress;
        }

        public ObjectFile getObjectFile() {
            return objectFile;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public String getName() {
            return symbol.getName();
        }

        public String getModuleName() {
            return objectFile.getModuleName();
        }

        public int getAbsoluteAddress() {
            return absoluteAddress;
        }

        @Override
        public String toString() {
            return "SymbolEntry{" +
                    "name='" + getName() + '\'' +
                    ", module='" + getModuleName() + '\'' +
                    ", absoluteAddress=0x" +
                    String.format(
                            "%04X",
                            absoluteAddress
                    ) +
                    '}';
        }
    }
}