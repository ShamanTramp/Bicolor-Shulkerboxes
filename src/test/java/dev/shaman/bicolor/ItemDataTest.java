package dev.shaman.bicolor;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ItemDataTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void shellsHaveVanillaIdsAndAreStackableByColor() {
        ItemStack red = ItemData.shell(ShellColor.RED);
        assertTrue(red.is(Items.SHULKER_SHELL));
        assertEquals(ShellColor.RED, ItemData.shellColor(red));
        assertTrue(ItemStack.isSameItemSameComponents(red, ItemData.shell(ShellColor.RED)));
        assertFalse(ItemStack.isSameItemSameComponents(red, ItemData.shell(ShellColor.BLUE)));
        assertEquals(64, red.getMaxStackSize());
    }
    @Test void everyPairUsesTheCorrectVanillaBaseAndOnlyMixedPairsCarryMetadata() {
        for (ShellColor lid : ShellColor.values()) for (ShellColor base : ShellColor.values()) {
            ColorPair pair = new ColorPair(lid, base);
            ItemStack box = ItemData.box(pair);
            assertEquals(ItemData.boxItem(base), box.getItem());
            assertEquals(!pair.isUniform(), ItemData.pair(box).isPresent());
            if (!pair.isUniform()) assertEquals(pair, ItemData.pair(box).orElseThrow());
        }
    }
    @Test void recoloringPreservesContentsNameAndForeignData() {
        ItemStack box = ItemData.box(new ColorPair(ShellColor.RED, ShellColor.BLUE));
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 37))));
        box.set(DataComponents.CUSTOM_NAME, Component.literal("Treasures"));
        CompoundTag data = box.get(DataComponents.CUSTOM_DATA).copyTag();
        data.putString("other_mod", "keep me");
        box.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        ItemStack result = ItemData.clearBicolor(box.transmuteCopy(Items.GREEN_SHULKER_BOX));
        assertEquals(box.get(DataComponents.CONTAINER), result.get(DataComponents.CONTAINER));
        assertEquals(box.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
        assertEquals("keep me", result.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("other_mod", ""));
        assertTrue(ItemData.pair(result).isEmpty());
    }
    @Test void unknownFutureMetadataIsNotInterpretedOrDeleted() {
        ItemStack box = new ItemStack(Items.BLUE_SHULKER_BOX);
        CompoundTag future = new CompoundTag(); future.putInt("version", 99);
        future.putString("lid", "red"); future.putString("base", "blue");
        CompoundTag data = new CompoundTag(); data.put(ItemData.KEY, future);
        box.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        assertTrue(ItemData.pair(box).isEmpty());
        assertEquals(box.get(DataComponents.CUSTOM_DATA), ItemData.clearBicolor(box.copy()).get(DataComponents.CUSTOM_DATA));
    }
}
