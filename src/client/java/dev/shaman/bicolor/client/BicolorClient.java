package dev.shaman.bicolor.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BicolorClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockEntityRenderers.register(BlockEntityType.SHULKER_BOX, BicolorBoxRenderer::new);
    }
}
