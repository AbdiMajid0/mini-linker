package minilinker.architecture;

/**
 * Mini16 mimarisinin sabitlerini ve doğrulama yardımcılarını tanımlar.
 *
 * <p>Mini16, 16 bitlik word'ler ve 16 bitlik adres alanı (0x0000–0xFFFF)
 * kullanan bir öğretim amaçlı mimaridir.</p>
 */
public final class Mini16Architecture {

    public static final int WORD_BITS = 16;
    public static final int BYTES_PER_WORD = 2;

    public static final int MIN_WORD_VALUE = 0x0000;
    public static final int MAX_WORD_VALUE = 0xFFFF;

    public static final int MIN_ADDRESS = 0x0000;
    public static final int MAX_ADDRESS = 0xFFFF;
    public static final int ADDRESS_COUNT = 65_536;
    public static final int MEMORY_SIZE_BYTES =
            ADDRESS_COUNT * BYTES_PER_WORD;

    private Mini16Architecture() {
    }

    public static boolean isValidWord(int value) {
        return value >= MIN_WORD_VALUE
                && value <= MAX_WORD_VALUE;
    }

    public static void validateWord(int value) {
        if (!isValidWord(value)) {
            throw new IllegalArgumentException(
                    "Geçersiz 16-bit word: " + value
            );
        }
    }

    public static boolean isValidAddress(int address) {
        return address >= MIN_ADDRESS
                && address <= MAX_ADDRESS;
    }

    public static void validateAddress(int address) {
        if (!isValidAddress(address)) {
            throw new IllegalArgumentException(
                    "Geçersiz adres: " + address
            );
        }
    }

    public static String formatWord(int value) {
        validateWord(value);
        return String.format("%04X", value);
    }

    public static String formatAddress(int address) {
        validateAddress(address);
        return String.format("%04X", address);
    }
}