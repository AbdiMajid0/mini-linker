package minilinker.util;

/**
 * Proje genelinde kullanılan hata ve bilgilendirme mesajları.
 *
 * <p>Sabit metinleri merkezileştirerek uluslararasılaştırma (i18n)
 * veya kolay metin değişikliği için temel oluşturur.</p>
 */
public final class Messages {

    private Messages() {
    }

    // --- Hata Mesajları ---
    public static final String ERR_MODULE_NAME_EMPTY = "Modul adi bos olamaz.";
    public static final String ERR_WORD_EMPTY = "Word degeri bos olamaz.";
    public static final String ERR_INVALID_16BIT = "Gecersiz 16-bit word: ";
    public static final String ERR_MODULE_NULL = "Modul bos olamaz.";
    public static final String ERR_DUPLICATE_MODULE = "Ayni module adi birden fazla kullanilamaz: ";
    public static final String ERR_SYMBOL_NOT_RESOLVED = "Relocation sembolu cozulemedi: ";
    public static final String ERR_SECTION_NOT_FOUND = "Relocation section'i bulunamadi: ";
    public static final String ERR_UNSUPPORTED_RELOC = "Desteklenmeyen relocation turu: ";

    // --- CLI Mesajları ---
    public static final String CLI_NO_MODULES = "Henuz yuklenmis modul yok.";
    public static final String CLI_MODULES_VALID = "Tum moduller gecerli.";
    public static final String CLI_LINK_SUCCESS = "Link islemi basarili!";

}
