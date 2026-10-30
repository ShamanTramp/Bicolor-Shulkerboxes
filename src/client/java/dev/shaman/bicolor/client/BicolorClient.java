package dev.shaman.bicolor.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public final class BicolorClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockEntityRenderers.register(BlockEntityTypes.SHULKER_BOX, BicolorBoxRenderer::new);
    }
}
