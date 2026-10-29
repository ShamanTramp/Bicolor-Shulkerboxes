package dev.shaman.bicolor.client.mixin;

import dev.shaman.bicolor.BicolorShulkerboxes;
import dev.shaman.bicolor.ItemData;
import dev.shaman.bicolor.ShellColor;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public abstract class ShellItemModelMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true)
    private ItemStack bicolor$localModel(ItemStack stack) {
        if (!stack.is(Items.SHULKER_SHELL)) return stack;
        ShellColor color = ItemData.shellColor(stack);
        if (color == ShellColor.NATURAL) return stack;
        ItemStack visual = stack.copy();
        visual.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(BicolorShulkerboxes.ID, "shell_" + color.id()));
        return visual;
    }
}
