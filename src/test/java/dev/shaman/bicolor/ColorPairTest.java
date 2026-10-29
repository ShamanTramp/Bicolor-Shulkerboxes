package dev.shaman.bicolor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorPairTest {
    @Test void naturalIsNotPurple() {
        assertNotEquals(ShellColor.NATURAL, ShellColor.PURPLE);
        assertEquals(17, ShellColor.values().length);
    }
    @Test void allOrderedPairsRoundTripAndInversionIsMeaningful() {
        int distinct = 0;
        for (ShellColor lid : ShellColor.values()) for (ShellColor base : ShellColor.values()) {
            ColorPair pair = new ColorPair(lid, base);
            assertEquals(pair, ColorPair.parse(lid.id(), base.id()).orElseThrow());
            assertEquals(lid == base, pair.isUniform());
            if (lid != base) {
                distinct++;
                assertNotEquals(pair, new ColorPair(base, lid));
            }
        }
        assertEquals(272, distinct);
    }
    @Test void invalidColorsAreRejectedRatherThanSilentlyChanged() {
        assertTrue(ColorPair.parse("red", "bogus").isEmpty());
        assertTrue(ColorPair.parse("", "blue").isEmpty());
        assertTrue(ColorPair.parse("RED", "blue").isEmpty());
        assertThrows(NullPointerException.class, () -> new ColorPair(null, ShellColor.BLUE));
    }
}
