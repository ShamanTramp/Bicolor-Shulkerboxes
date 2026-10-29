package dev.shaman.bicolor.mixin;

import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ColoredBox;
import dev.shaman.bicolor.ItemData;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxDropsMixin {
    @Inject(method = "getDrops", at = @At("RETURN"))
    private void bicolor$preserveColors(BlockState state, LootParams.Builder context, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ColoredBox box) {
            ColorPair pair = box.bicolor$getColors();
            if (pair != null) for (ItemStack stack : cir.getReturnValue()) if (ItemData.isBox(stack)) ItemData.applyPair(stack, pair);
        }
    }
}
