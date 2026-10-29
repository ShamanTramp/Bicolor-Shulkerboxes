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
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** Each half owns its visibility mask, including during deferred rendering. */
public final class TwoToneRenderer {
    private final HalfModel lid;
    private final HalfModel base;
    private final MaterialSet materials;
    public TwoToneRenderer(EntityModelSet models, MaterialSet materials) {
        this.materials = materials;
        lid = new HalfModel(models.bakeLayer(ModelLayers.SHULKER_BOX), true);
        base = new HalfModel(models.bakeLayer(ModelLayers.SHULKER_BOX), false);
    }
    public void submit(ColorPair colors, Direction facing, float progress, PoseStack poses, SubmitNodeCollector nodes,
                       int light, int overlay, int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        poses.pushPose();
        poses.translate(.5F, .5F, .5F);
        poses.scale(.9995F, .9995F, .9995F);
        poses.mulPose(facing.getRotation());
        poses.scale(1, -1, -1);
        poses.translate(0, -1, 0);
        submitHalf(lid, colors.lid(), progress, poses, nodes, light, overlay, outline, breaking);
        submitHalf(base, colors.base(), progress, poses, nodes, light, overlay, outline, breaking);
        poses.popPose();
    }
    private void submitHalf(HalfModel model, ShellColor color, float progress, PoseStack poses, SubmitNodeCollector nodes,
                            int light, int overlay, int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        Material material = color == ShellColor.NATURAL ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxMaterial(ItemData.dye(color));
        nodes.submitModel(model, progress, poses, material.renderType(model::renderType), light, overlay, -1, materials.get(material), outline, breaking);
    }
    private static final class HalfModel extends Model<Float> {
        private final ModelPart lid;
        HalfModel(ModelPart root, boolean drawLid) {
            super(root, RenderTypes::entityCutoutNoCull);
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
