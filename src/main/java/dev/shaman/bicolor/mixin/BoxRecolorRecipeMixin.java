package dev.shaman.bicolor.mixin;

import dev.shaman.bicolor.ItemData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransmuteRecipe.class)
public abstract class BoxRecolorRecipeMixin {
    @Shadow @Final private Ingredient input;
    @Shadow @Final private Ingredient material;
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void bicolor$allowBaseColor(CraftingInput grid, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!((NormalCraftingRecipe)(Object)this).group().equals("bicolor_shulkerboxes:box_recolor") || grid.ingredientCount() != 2) return;
        boolean box = false, dye = false;
        for (ItemStack stack : grid.items()) {
            if (stack.isEmpty()) continue;
            if (!box && input.test(stack) && ItemData.pair(stack).isPresent()) box = true;
            else if (!dye && material.test(stack)) dye = true;
            else return;
        }
        if (box && dye) cir.setReturnValue(true);
    }
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void bicolor$clearColors(CraftingInput grid, CallbackInfoReturnable<ItemStack> cir) {
        if (((NormalCraftingRecipe)(Object)this).group().equals("bicolor_shulkerboxes:box_recolor")) cir.setReturnValue(ItemData.clearBicolor(cir.getReturnValue()));
    }
}
