package dev.shaman.bicolor.mixin;

import dev.shaman.bicolor.ColorPair;
import dev.shaman.bicolor.ColoredBox;
import dev.shaman.bicolor.ItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxDataMixin extends RandomizableContainerBlockEntity implements ColoredBox {
    @Unique private CustomData bicolor$data = CustomData.EMPTY;
    protected ShulkerBoxDataMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public @Nullable ColorPair bicolor$getColors() {
        return ItemData.readPair(bicolor$data.copyTag().getCompoundOrEmpty(ItemData.KEY)).orElse(null);
    }
    @Override public void bicolor$setColors(@Nullable ColorPair pair) {
        CompoundTag tag = bicolor$data.copyTag();
        if (pair == null) tag.remove(ItemData.KEY); else tag.put(ItemData.KEY, ItemData.pairTag(pair));
        bicolor$data = CustomData.of(tag);
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void applyImplicitComponents(DataComponentGetter data) {
        super.applyImplicitComponents(data);
        bicolor$data = data.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!bicolor$data.isEmpty()) builder.set(DataComponents.CUSTOM_DATA, bicolor$data);
    }
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void bicolor$load(ValueInput input, CallbackInfo ci) {
        bicolor$data = CustomData.of(input.read(ItemData.KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void bicolor$save(ValueOutput output, CallbackInfo ci) {
        if (!bicolor$data.isEmpty()) output.store(ItemData.KEY, CompoundTag.CODEC, bicolor$data.copyTag());
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Do not send container contents, locks or foreign metadata to nearby clients.
        CompoundTag tag = new CompoundTag();
        ColorPair pair = bicolor$getColors();
        if (pair != null) {
            CompoundTag data = new CompoundTag(); data.put(ItemData.KEY, ItemData.pairTag(pair)); tag.put(ItemData.KEY, data);
        }
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
