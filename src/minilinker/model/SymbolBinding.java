package minilinker.model;

/**
 * Sembol bağlanma türleri.
 *
 * <ul>
 *   <li>{@code LOCAL} — Modül içi görünürlük</li>
 *   <li>{@code GLOBAL} — Tüm modüllere açık (DEFINE)</li>
 *   <li>{@code EXTERN} — Başka modülde tanımlı dış referans</li>
 * </ul>
 */
public enum SymbolBinding {
    LOCAL,
    GLOBAL,
    EXTERN
}