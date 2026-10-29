package dev.shaman.bicolor.mixin;

import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ItemData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapedRecipe.class)
public abstract class BoxAssemblyRecipeMixin {
    @Shadow @Final private String group;
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void bicolor$assemble(CraftingInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        if (group.equals("bicolor_shulkerboxes:box_assembly") && input.width() == 1 && input.height() == 3) {
            cir.setReturnValue(ItemData.box(new ColorPair(ItemData.shellColor(input.getItem(0)), ItemData.shellColor(input.getItem(2)))));
        }
    }
}
