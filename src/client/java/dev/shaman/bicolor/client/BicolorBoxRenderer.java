package dev.shaman.bicolor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ColoredBox;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BicolorBoxRenderer extends ShulkerBoxRenderer {
    private final TwoToneRenderer twoTone;
    public BicolorBoxRenderer(BlockEntityRendererProvider.Context context) {
        super(context); twoTone = new TwoToneRenderer(context.entityModelSet(), context.materials());
    }
    public static final class State extends ShulkerBoxRenderState { public @Nullable ColorPair colors; }
    @Override public ShulkerBoxRenderState createRenderState() { return new State(); }
    @Override public void extractRenderState(ShulkerBoxBlockEntity entity, ShulkerBoxRenderState state, float tickProgress,
                                            Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        super.extractRenderState(entity, state, tickProgress, camera, breaking);
        ((State)state).colors = ((ColoredBox)entity).bicolor$getColors();
    }
    @Override public void submit(ShulkerBoxRenderState state, PoseStack poses, SubmitNodeCollector nodes, CameraRenderState camera) {
        ColorPair colors = ((State)state).colors;
        if (colors == null) { super.submit(state, poses, nodes, camera); return; }
        twoTone.submit(colors, state.direction, state.progress, poses, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
    }
}
