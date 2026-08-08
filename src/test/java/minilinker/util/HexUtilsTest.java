package minilinker.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HexUtilsTest {

    @Test
    void testFormatWord() {
        assertEquals("0000", HexUtils.formatWord(0));
        assertEquals("FFFF", HexUtils.formatWord(0xFFFF));
        assertEquals("000F", HexUtils.formatWord(15));
    }

    @Test
    void testParseHex() {
        assertEquals(0x1234, HexUtils.parseWord("1234"));
        assertEquals(0xFFFF, HexUtils.parseWord("FFFF"));
    }
}
