package minilinker.model;

/**
 * Desteklenen relocation türleri.
 *
 * <ul>
 *   <li>{@code ABS16} — Mutlak 16-bit adres</li>
 *   <li>{@code REL16} — Göreli 16-bit adres (hedef - mevcut - 1)</li>
 * </ul>
 */
public enum RelocationType {
    ABS16(2),
    REL16(2);

    private final int byteSize;

    RelocationType(int byteSize) {
        this.byteSize = byteSize;
    }

    public int getByteSize() {
        return byteSize;
    }
}