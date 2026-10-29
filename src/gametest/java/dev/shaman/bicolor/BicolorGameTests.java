package dev.shaman.bicolor;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BicolorGameTests {
    static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    static ItemStack dye(ShellColor color) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.id() + "_dye"))); }
    static ItemStack craft(GameTestHelper h, CraftingInput grid) {
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
            .map(r -> r.value().assemble(grid, h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }
    static CraftingInput grid(int column, ShellColor lid, ShellColor base) {
        var slots = new ArrayList<ItemStack>(); for (int i = 0; i < 9; i++) slots.add(ItemStack.EMPTY);
        slots.set(column, ItemData.shell(lid)); slots.set(column + 3, new ItemStack(Items.CHEST)); slots.set(column + 6, ItemData.shell(base));
        return CraftingInput.of(3, 3, slots);
    }
    @GameTest public void all289PairsInAllThreeColumns(GameTestHelper h) {
        for (ShellColor lid : ShellColor.values()) for (ShellColor base : ShellColor.values()) for (int column = 0; column < 3; column++) {
            ItemStack result = craft(h, grid(column, lid, base));
            check(result.is(ItemData.boxItem(base)), "Wrong base: " + lid + "/" + base + " column " + column);
            check(result.getCount() == 1, "Wrong output count");
            check(lid == base ? ItemData.pair(result).isEmpty() : ItemData.pair(result).orElseThrow().equals(new ColorPair(lid, base)), "Wrong pair");
        }
        check(craft(h, CraftingInput.of(3, 1, List.of(ItemData.shell(ShellColor.RED), new ItemStack(Items.CHEST), ItemData.shell(ShellColor.BLUE)))).isEmpty(), "Horizontal recipe must fail");
        check(craft(h, CraftingInput.of(1, 2, List.of(ItemData.shell(ShellColor.RED), ItemData.shell(ShellColor.BLUE)))).isEmpty(), "Missing chest must fail");
        h.succeed();
    }
    @GameTest public void allShellDyesAndRejections(GameTestHelper h) {
        for (ShellColor source : ShellColor.values()) for (ShellColor target : ShellColor.values()) {
            if (target == ShellColor.NATURAL) continue;
            CraftingInput input = CraftingInput.of(2, 1, List.of(ItemData.shell(source), dye(target)));
            ItemStack result = craft(h, input);
            check(source == target ? result.isEmpty() : result.is(Items.SHULKER_SHELL) && ItemData.shellColor(result) == target, "Dye mismatch " + source + " -> " + target);
        }
        check(craft(h, CraftingInput.of(3, 1, List.of(ItemData.shell(ShellColor.RED), dye(ShellColor.BLUE), dye(ShellColor.GREEN)))).isEmpty(), "Extra dye must fail");
        h.succeed();
    }
    @GameTest public void recolorEveryPairWithoutLosingData(GameTestHelper h) {
        for (ShellColor lid : ShellColor.values()) for (ShellColor base : ShellColor.values()) for (ShellColor target : ShellColor.values()) {
            if (lid == base || target == ShellColor.NATURAL) continue;
            ItemStack box = ItemData.box(new ColorPair(lid, base));
            box.set(DataComponents.CUSTOM_NAME, Component.literal("Named box"));
            box.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 37))));
            ItemStack result = craft(h, CraftingInput.of(2, 1, List.of(box, dye(target))));
            check(result.is(ItemData.boxItem(target)), "Recolor failed " + lid + "/" + base + " -> " + target);
            check(ItemData.pair(result).isEmpty(), "Stale colors after recolor");
            check(box.get(DataComponents.CONTAINER).equals(result.get(DataComponents.CONTAINER)), "Lost contents");
            check(box.get(DataComponents.CUSTOM_NAME).equals(result.get(DataComponents.CUSTOM_NAME)), "Lost name");
        }
        h.succeed();
    }
    static ShulkerBoxBlockEntity place(GameTestHelper h, BlockPos pos, ItemStack stack) {
        h.setBlock(pos.below(), Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        BlockPos support = h.absolutePos(pos.below());
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false));
        ((BlockItem)stack.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(context));
        return h.getBlockEntity(pos, ShulkerBoxBlockEntity.class);
    }
    @GameTest public void placeSaveDropAndReplacePreservesEverything(GameTestHelper h) {
        ColorPair pair = new ColorPair(ShellColor.NATURAL, ShellColor.BLUE);
        BlockPos pos = new BlockPos(2, 2, 2);
        ItemStack original = ItemData.box(pair); original.set(DataComponents.CUSTOM_NAME, Component.literal("Treasure"));
        ShulkerBoxBlockEntity box = place(h, pos, original);
        check(pair.equals(((ColoredBox)box).bicolor$getColors()), "Placement lost colors");
        box.setItem(0, new ItemStack(Items.DIAMOND, 37));
        check(box.getContainerSize() == 27, "Changed capacity");
        check(!box.canPlaceItemThroughFace(1, original, Direction.UP), "Nested shulker accepted");
        check(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(box) > 0, "Comparator signal missing");
        var saved = box.saveWithFullMetadata(h.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(box.getBlockPos(), box.getBlockState(), saved, h.getLevel().registryAccess());
        check(pair.equals(((ColoredBox)loaded).bicolor$getColors()), "Save/load lost colors");
        check(((ShulkerBoxBlockEntity)loaded).getItem(0).getCount() == 37, "Save/load lost items");
        var updateTag = box.getUpdateTag(h.getLevel().registryAccess());
        check(!updateTag.toString().contains("diamond"), "Contents leaked in visual update");
        var drops = Block.getDrops(box.getBlockState(), h.getLevel(), box.getBlockPos(), box);
        check(drops.size() == 1, "Wrong drops");
        ItemStack drop = drops.getFirst();
        check(pair.equals(ItemData.pair(drop).orElseThrow()), "Drop lost colors");
        h.setBlock(pos, Blocks.AIR);
        ShulkerBoxBlockEntity replaced = place(h, pos, drop);
        check(pair.equals(((ColoredBox)replaced).bicolor$getColors()), "Replacement lost colors");
        check(replaced.getItem(0).is(Items.DIAMOND) && replaced.getItem(0).getCount() == 37, "Replacement lost contents");
        check(replaced.getCustomName().getString().equals("Treasure"), "Replacement lost name");
        for (Direction direction : Direction.values()) {
            h.setBlock(pos, replaced.getBlockState().setValue(ShulkerBoxBlock.FACING, direction));
            check(pair.equals(((ColoredBox)h.getBlockEntity(pos, ShulkerBoxBlockEntity.class)).bicolor$getColors()), "Rotation lost colors");
        }
        h.succeed();
    }
    @GameTest(maxTicks = 100) public void redstoneCrafterConsumesOneOfEach(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP));
        h.setBlock(pos.north(), Blocks.CHEST);
        CrafterBlockEntity crafter = h.getBlockEntity(pos, CrafterBlockEntity.class);
        crafter.setItem(0, ItemData.shell(ShellColor.RED).copyWithCount(2));
        crafter.setItem(3, new ItemStack(Items.CHEST, 2));
        crafter.setItem(6, ItemData.shell(ShellColor.BLUE).copyWithCount(2));
        for (int slot : new int[]{1, 2, 4, 5, 7, 8}) crafter.setSlotState(slot, false);
        h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            ChestBlockEntity output = h.getBlockEntity(pos.north(), ChestBlockEntity.class);
            check(ItemData.pair(output.getItem(0)).orElseThrow().equals(new ColorPair(ShellColor.RED, ShellColor.BLUE)), "Crafter wrong output");
            check(crafter.getItem(0).getCount() == 1 && crafter.getItem(3).getCount() == 1 && crafter.getItem(6).getCount() == 1, "Crafter consumed wrong quantities");
            h.setBlock(pos.east(), Blocks.AIR);
        });
        h.runAfterDelay(15, () -> h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK));
        h.runAfterDelay(25, () -> {
            check(crafter.isEmpty(), "Second pulse did not consume remaining inputs");
            ChestBlockEntity output = h.getBlockEntity(pos.north(), ChestBlockEntity.class);
            check(ItemData.pair(output.getItem(1)).isPresent(), "Second pulse lost output");
            h.succeed();
        });
    }
    @GameTest(maxTicks = 50) public void sameColorCrafterConsumesNothing(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3); h.setBlock(pos, Blocks.CRAFTER);
        CrafterBlockEntity crafter = h.getBlockEntity(pos, CrafterBlockEntity.class);
        crafter.setItem(0, ItemData.shell(ShellColor.RED)); crafter.setItem(1, dye(ShellColor.RED));
        h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> { check(crafter.getItem(0).getCount() == 1 && crafter.getItem(1).getCount() == 1, "Invalid craft consumed ingredients"); h.succeed(); });
    }
    @GameTest(maxTicks = 100) public void hopperRefillsShellDyeRecipe(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP));
        h.setBlock(pos.north(), Blocks.CHEST); h.setBlock(pos.above(), Blocks.HOPPER);
        var crafter = h.getBlockEntity(pos, CrafterBlockEntity.class);
        crafter.setItem(0, ItemData.shell(ShellColor.NATURAL)); crafter.setItem(1, dye(ShellColor.BLUE));
        for (int slot = 2; slot < 9; slot++) crafter.setSlotState(slot, false);
        var hopper = h.getBlockEntity(pos.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, ItemData.shell(ShellColor.NATURAL)); hopper.setItem(1, dye(ShellColor.BLUE));
        h.runAfterDelay(30, () -> {
            check(crafter.getItem(0).getCount() == 2 && crafter.getItem(1).getCount() == 2, "Hopper did not refill recipe");
            h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        });
        h.runAfterDelay(40, () -> {
            var chest = h.getBlockEntity(pos.north(), ChestBlockEntity.class);
            check(ItemData.shellColor(chest.getItem(0)) == ShellColor.BLUE, "Hopper-fed recipe failed");
            check(crafter.getItem(0).getCount() == 1 && crafter.getItem(1).getCount() == 1, "Wrong hopper-fed consumption");
            h.succeed();
        });
    }
    @GameTest(maxTicks = 50) public void crafterEjectsOneColoredShell(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP));
        var crafter = h.getBlockEntity(pos, CrafterBlockEntity.class);
        crafter.setItem(0, ItemData.shell(ShellColor.NATURAL)); crafter.setItem(1, dye(ShellColor.GREEN));
        h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            var items = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3),
                entity -> entity.getItem().is(Items.SHULKER_SHELL) && ItemData.shellColor(entity.getItem()) == ShellColor.GREEN);
            check(items.size() == 1 && items.getFirst().getItem().getCount() == 1, "Ejected output was lost or duplicated");
            check(crafter.isEmpty(), "Ejection did not consume inputs"); h.succeed();
        });
    }

}
