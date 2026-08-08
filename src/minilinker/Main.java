package minilinker;

import minilinker.cli.ConsoleMenu;
import minilinker.gui.MainApp;
import minilinker.linker.MiniLinker;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Mini Linker uygulamasının ana giriş noktası.
 *
 * <p>Komut satırı argümanı verilmezse interaktif konsol menüsünü başlatır.
 * Argüman verilirse doğrudan linkleme işlemi yapar.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            MainApp.main(args);
        } else {
            MiniLinker.main(args);
        }
    }
}
