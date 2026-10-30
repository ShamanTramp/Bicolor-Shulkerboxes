package dev.shaman.bicolor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Stack prototypes are resource-loaded in 26.3; stack behavior is covered by ItemDataGameTests. */
class ItemDataTest {
    @Test void allMetadataPairsRetainTheirOrderedColors() {
        for (ShellColor lid : ShellColor.values()) for (ShellColor base : ShellColor.values()) {
            var tag = ItemData.pairTag(new ColorPair(lid, base));
            assertEquals(1, tag.getIntOr("version", -1));
            assertEquals("box", tag.getStringOr("kind", ""));
            assertEquals(lid.id(), tag.getStringOr("lid", ""));
            assertEquals(base.id(), tag.getStringOr("base", ""));
            assertEquals(new ColorPair(lid, base), ItemData.readPair(tag).orElseThrow());
        }
    }
    @Test void futureOrMalformedMetadataIsNotInterpreted() {
        CompoundTag tag = ItemData.pairTag(new ColorPair(ShellColor.RED, ShellColor.BLUE));
        tag.putInt("version", 99); assertTrue(ItemData.readPair(tag).isEmpty());
        tag.putInt("version", 1); tag.putString("kind", "shell"); assertTrue(ItemData.readPair(tag).isEmpty());
        tag.putString("kind", "box"); tag.putString("base", "BLUE"); assertTrue(ItemData.readPair(tag).isEmpty());
        assertTrue(ItemData.readPair(new CompoundTag()).isEmpty());
    }
    @Test void naturalAppearanceIsDistinctFromPurpleDye() {
        assertNull(ItemData.dye(ShellColor.NATURAL));
        assertEquals(DyeColor.PURPLE, ItemData.dye(ShellColor.PURPLE));
        assertEquals(ShellColor.NATURAL, ItemData.color(null));
        assertEquals(ShellColor.PURPLE, ItemData.color(DyeColor.PURPLE));
    }
    @Test void versionOneLegacyMetadataRemainsReadable() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("version", 1); legacy.putString("kind", "box");
        legacy.putString("lid", "red"); legacy.putString("base", "blue");
        assertEquals(new ColorPair(ShellColor.RED, ShellColor.BLUE), ItemData.readPair(legacy).orElseThrow());
    }
}
