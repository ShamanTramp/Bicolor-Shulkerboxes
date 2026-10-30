package dev.shaman.bicolor.client.mixin;

import dev.shaman.bicolor.client.BicolorSpecialRenderer;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wrap the generic baking call: vanilla's shulker bake now returns a concrete renderer. */
@Mixin(SpecialModelWrapper.Unbaked.class)
public abstract class ShulkerSpecialModelMixin {
    @Redirect(method = "bake", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/special/SpecialModelRenderer$Unbaked;bake(Lnet/minecraft/client/renderer/special/SpecialModelRenderer$BakingContext;)Lnet/minecraft/client/renderer/special/SpecialModelRenderer;"))
    private SpecialModelRenderer<?> bicolor$wrap(SpecialModelRenderer.Unbaked<?> definition, SpecialModelRenderer.BakingContext context) {
        SpecialModelRenderer<?> renderer = definition.bake(context);
        if (renderer instanceof ShulkerBoxSpecialRenderer vanilla && definition instanceof ShulkerBoxSpecialRenderer.Unbaked shulker)
            return new BicolorSpecialRenderer(vanilla, context, shulker.openness());
        return renderer;
    }
}
