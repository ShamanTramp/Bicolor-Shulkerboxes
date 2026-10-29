package dev.shaman.bicolor.client.mixin;

import dev.shaman.bicolor.client.BicolorSpecialRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxSpecialRenderer.Unbaked.class)
public abstract class ShulkerSpecialModelMixin {
    @Shadow public abstract Direction orientation();
    @Shadow public abstract float openness();
    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private void bicolor$wrap(SpecialModelRenderer.BakingContext context, CallbackInfoReturnable<SpecialModelRenderer<?>> cir) {
        if (cir.getReturnValue() instanceof ShulkerBoxSpecialRenderer vanilla)
            cir.setReturnValue(new BicolorSpecialRenderer(vanilla, context, orientation(), openness()));
    }
}
