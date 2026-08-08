package minilinker.util;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 16-bit hexadecimal değerler için merkezi formatlama ve ayrıştırma yardımcıları.
 *
 * <p>Tüm proje genelinde hex formatlama bu sınıf üzerinden yapılmalıdır.</p>
 */
public final class HexUtils {

    private static final int MIN_VALUE = 0x0000;
    private static final int MAX_VALUE = 0xFFFF;

    private HexUtils() {
    }

    public static boolean isValid16Bit(int value) {
        return value >= MIN_VALUE && value <= MAX_VALUE;
    }

    public static void validate16Bit(int value) {
        if (!isValid16Bit(value)) {
            throw new IllegalArgumentException(
                    "Değer 16-bit aralığında değil: " + value
            );
        }
    }

    public static String formatWord(int value) {
        validate16Bit(value);

        return String.format(
                Locale.ROOT,
                "%04X",
                value
        );
    }

    public static String formatAddress(int address) {
        validate16Bit(address);

        return String.format(
                Locale.ROOT,
                "%04X",
                address
        );
    }

    public static int parseWord(String value) {
        return parse16Bit(value, "word");
    }

    public static int parseAddress(String value) {
        return parse16Bit(value, "adres");
    }

    private static int parse16Bit(
            String value,
            String description
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    description + " boş olamaz."
            );
        }

        String normalized = value.trim();

        if (normalized.startsWith("0x")
                || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }

        if (!normalized.matches("[0-9A-Fa-f]{1,4}")) {
            throw new IllegalArgumentException(
                    "Geçersiz hexadecimal " + description + ": "
                            + value
            );
        }

        return Integer.parseInt(normalized, 16);
    }

    public static String formatWords(
            List<Integer> words
    ) {
        Objects.requireNonNull(
                words,
                "Word listesi boş olamaz."
        );

        StringJoiner result = new StringJoiner(" ");

        for (Integer word : words) {
            if (word == null) {
                throw new IllegalArgumentException(
                        "Word değeri boş olamaz."
                );
            }

            result.add(formatWord(word));
        }

        return result.toString();
    }

    public static String toHexDump(
            List<Integer> words
    ) {
        Objects.requireNonNull(
                words,
                "Word listesi boş olamaz."
        );

        if (words.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int address = 0;
             address < words.size();
             address += 8) {

            if (address > 0) {
                result.append(System.lineSeparator());
            }

            result.append(formatAddress(address))
                    .append(":");

            int end = Math.min(
                    address + 8,
                    words.size()
            );

            for (int current = address;
                 current < end;
                 current++) {

                Integer word = words.get(current);

                if (word == null) {
                    throw new IllegalArgumentException(
                            "Word değeri boş olamaz."
                    );
                }

                result.append(" ")
                        .append(formatWord(word));
            }
        }

        return result.toString();
    }
}