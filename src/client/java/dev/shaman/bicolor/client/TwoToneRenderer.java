package dev.shaman.bicolor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ItemData;
import dev.shaman.bicolor.ShellColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** Each half owns its visibility mask, including during deferred rendering. */
public final class TwoToneRenderer {
    private final HalfModel lid;
    private final HalfModel base;
    private final SpriteGetter sprites;
    public TwoToneRenderer(EntityModelSet models, SpriteGetter sprites) {
        this.sprites = sprites;
        lid = new HalfModel(models.bakeLayer(ModelLayers.SHULKER_BOX), true);
        base = new HalfModel(models.bakeLayer(ModelLayers.SHULKER_BOX), false);
    }
    public void submit(ColorPair colors, Direction facing, float progress, PoseStack poses, SubmitNodeCollector nodes,
                       int light, int overlay, int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        poses.pushPose();
        poses.mulPose(ShulkerBoxRenderer.modelTransform(facing));
        submitTransformed(colors, progress, poses, nodes, light, overlay, outline, breaking);
        poses.popPose();
    }
    /** Item models in 26.3 already supply the shulker transform. */
    public void submitTransformed(ColorPair colors, float progress, PoseStack poses, SubmitNodeCollector nodes,
                                  int light, int overlay, int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        submitHalf(lid, colors.lid(), progress, poses, nodes, light, overlay, outline, breaking);
        submitHalf(base, colors.base(), progress, poses, nodes, light, overlay, outline, breaking);
    }
    private void submitHalf(HalfModel model, ShellColor color, float progress, PoseStack poses, SubmitNodeCollector nodes,
                            int light, int overlay, int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        SpriteId sprite = color == ShellColor.NATURAL ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(ItemData.dye(color));
        nodes.submitModel(model, progress, poses, light, overlay, -1, sprite, sprites, outline);
        if (breaking != null) nodes.order(1).submitCrumblingOverlay(model, progress, poses, sprite.renderType(model.renderType()), light, overlay, -1, breaking);
    }
    private static final class HalfModel extends Model<Float> {
        private final ModelPart lid;
        HalfModel(ModelPart root, boolean drawLid) {
            super(root, RenderTypes::entityCutout);
            lid = root.getChild("lid");
            root.getChild(drawLid ? "base" : "lid").visible = false;
        }
        @Override public void setupAnim(Float progress) {
            super.setupAnim(progress);
            lid.setPos(0, 24 - progress * 8, 0);
            lid.yRot = progress * (float)(Math.PI * 1.5);
        }
    }
}
