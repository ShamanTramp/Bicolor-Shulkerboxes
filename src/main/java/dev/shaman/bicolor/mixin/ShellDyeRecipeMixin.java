package dev.shaman.bicolor.mixin;

import dev.shaman.bicolor.ItemData;
import dev.shaman.bicolor.ShellColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapelessRecipe.class)
public abstract class ShellDyeRecipeMixin {
    @Shadow @Final private ItemStackTemplate result;
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"), cancellable = true)
    private void bicolor$rejectSameColor(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!((NormalCraftingRecipe)(Object)this).group().equals("bicolor_shulkerboxes:shell_dye") || !cir.getReturnValue()) return;
        ShellColor target = ItemData.shellColor(result.create());
        for (ItemStack stack : input.items()) if (stack.is(Items.SHULKER_SHELL) && ItemData.shellColor(stack) == target) cir.setReturnValue(false);
    }
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void bicolor$preserveShellData(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
        if (!((NormalCraftingRecipe)(Object)this).group().equals("bicolor_shulkerboxes:shell_dye")) return;
        for (ItemStack stack : input.items()) if (stack.is(Items.SHULKER_SHELL)) {
            cir.setReturnValue(ItemData.dyeShell(stack, ItemData.shellColor(result.create()))); return;
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
