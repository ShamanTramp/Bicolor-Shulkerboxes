package dev.shaman.bicolor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ItemData;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class BicolorSpecialRenderer implements SpecialModelRenderer<ColorPair> {
    private final ShulkerBoxSpecialRenderer vanilla;
    private final TwoToneRenderer renderer;
    private final float openness;
    public BicolorSpecialRenderer(ShulkerBoxSpecialRenderer vanilla, BakingContext context, float openness) {
        this.vanilla = vanilla;
        renderer = new TwoToneRenderer(context.entityModelSet(), context.sprites());
        this.openness = openness;
    }
    @Override public @Nullable ColorPair extractArgument(ItemStack stack) { return ItemData.pair(stack).orElse(null); }
    @Override public void getExtents(Consumer<Vector3fc> consumer) { vanilla.getExtents(consumer); }
    @Override public void submit(@Nullable ColorPair colors, PoseStack poses, SubmitNodeCollector nodes,
                                 int light, int overlay, boolean foil, int outline) {
        if (colors == null) vanilla.submit(poses, nodes, light, overlay, foil, outline);
        else renderer.submitTransformed(colors, openness, poses, nodes, light, overlay, outline, null);
    }
}
