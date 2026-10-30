package dev.shaman.bicolor;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Only vanilla components go over the network. Unknown versions are left untouched. */
public final class ItemData {
    public static final String KEY = BicolorShulkerboxes.ID;
    private static final String LORE_KEY = KEY + ".colors";
    private ItemData() { }

    public static DyeColor dye(ShellColor color) {
        return color == ShellColor.NATURAL ? null : DyeColor.byName(color.id(), DyeColor.PURPLE);
    }
    public static ShellColor color(DyeColor dye) {
        return dye == null ? ShellColor.NATURAL : ShellColor.parse(dye.getName()).orElseThrow();
    }
    public static Item boxItem(ShellColor color) { return color == ShellColor.NATURAL ? Items.SHULKER_BOX : BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.id() + "_shulker_box")); }
    public static boolean isBox(ItemStack stack) { return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock; }
    public static ShellColor shellColor(ItemStack stack) {
        if (!stack.is(Items.SHULKER_SHELL)) return ShellColor.NATURAL;
        CompoundTag tag = ownTag(stack);
        if (!valid(tag, "shell")) return ShellColor.NATURAL;
        return ShellColor.parse(tag.getStringOr("color", "natural")).orElse(ShellColor.NATURAL);
    }
    public static Optional<ColorPair> pair(ItemStack stack) {
        return isBox(stack) ? readPair(ownTag(stack)) : Optional.empty();
    }
    public static Optional<ColorPair> readPair(CompoundTag tag) {
        if (!valid(tag, "box")) return Optional.empty();
        return ColorPair.parse(tag.getStringOr("lid", ""), tag.getStringOr("base", ""));
    }
    private static boolean valid(CompoundTag tag, String kind) {
        return tag.getIntOr("version", -1) == 1 && tag.getStringOr("kind", "").equals(kind);
    }
    private static CompoundTag ownTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty(KEY);
    }
    public static CompoundTag pairTag(ColorPair pair) {
        CompoundTag tag = header("box");
        tag.putString("lid", pair.lid().id()); tag.putString("base", pair.base().id());
        return tag;
    }
    private static CompoundTag header(String kind) {
        CompoundTag tag = new CompoundTag(); tag.putInt("version", 1); tag.putString("kind", kind); return tag;
    }
    private static void put(ItemStack stack, CompoundTag own) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.put(KEY, own); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
    public static ItemStack shell(ShellColor color) { return dyeShell(new ItemStack(Items.SHULKER_SHELL), color); }
    public static ItemStack dyeShell(ItemStack input, ShellColor color) {
        ItemStack result = input.copyWithCount(1);
        if (color == ShellColor.NATURAL) return result;
        CompoundTag tag = header("shell"); tag.putString("color", color.id()); put(result, tag);
        result.set(DataComponents.ITEM_NAME, Component.translatableWithFallback(KEY + ".shell", "Shulker shell / Carapace: %s", colorName(color)));
        return result;
    }
    public static ItemStack box(ColorPair pair) {
        ItemStack stack = new ItemStack(boxItem(pair.base()));
        return pair.isUniform() ? stack : applyPair(stack, pair);
    }
    public static ItemStack applyPair(ItemStack stack, ColorPair pair) {
        put(stack, pairTag(pair));
        var lines = new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        lines.removeIf(ItemData::isColorLore);
        if (lines.size() < ItemLore.MAX_LINES) lines.add(Component.translatableWithFallback(LORE_KEY,
            "Lid / Couvercle: %s | Base: %s", colorName(pair.lid()), colorName(pair.base())));
        if (!lines.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }
    public static ItemStack clearBicolor(ItemStack stack) {
        if (pair(stack).isEmpty()) return stack;
        CompoundTag data = stack.get(DataComponents.CUSTOM_DATA).copyTag(); data.remove(KEY);
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var lines = new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        lines.removeIf(ItemData::isColorLore);
        if (lines.isEmpty()) stack.remove(DataComponents.LORE);
        else stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }
    private static boolean isColorLore(Component line) {
        return line.getContents() instanceof TranslatableContents t && t.getKey().equals(LORE_KEY);
    }
    private static Component colorName(ShellColor color) {
        return Component.translatableWithFallback(KEY + ".color." + color.id(), color.english() + " / " + color.french());
    }
}
