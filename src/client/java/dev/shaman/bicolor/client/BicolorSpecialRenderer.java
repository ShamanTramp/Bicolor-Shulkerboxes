package dev.shaman.bicolor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ItemData;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class BicolorSpecialRenderer implements SpecialModelRenderer<ColorPair> {
    private final ShulkerBoxSpecialRenderer vanilla;
    private final TwoToneRenderer renderer;
    private final Direction direction;
    private final float openness;
    public BicolorSpecialRenderer(ShulkerBoxSpecialRenderer vanilla, BakingContext context, Direction direction, float openness) {
        this.vanilla = vanilla; this.renderer = new TwoToneRenderer(context.entityModelSet(), context.materials());
        this.direction = direction; this.openness = openness;
    }
    @Override public @Nullable ColorPair extractArgument(ItemStack stack) { return ItemData.pair(stack).orElse(null); }
    @Override public void getExtents(Consumer<Vector3fc> consumer) { vanilla.getExtents(consumer); }
    @Override public void submit(@Nullable ColorPair colors, ItemDisplayContext context, PoseStack poses, SubmitNodeCollector nodes,
                                 int light, int overlay, boolean foil, int outline) {
        if (colors == null) vanilla.submit(context, poses, nodes, light, overlay, foil, outline);
        else renderer.submit(colors, direction, openness, poses, nodes, light, overlay, outline, null);
    }
}
