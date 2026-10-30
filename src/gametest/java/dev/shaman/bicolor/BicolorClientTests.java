package dev.shaman.bicolor;

import dev.shaman.bicolor.client.BicolorBoxRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class BicolorClientTests implements FabricClientGameTest {
    private static final BlockPos BOX = new BlockPos(2, -60, 0);
    private static final ColorPair PAIR = new ColorPair(ShellColor.RED, ShellColor.BLUE);
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    @Override public void runTest(ClientGameTestContext context) {
        if (System.getProperty("bicolor.externalVanillaPort") != null) { testVanillaServer(context); return; }
        context.runOnClient(mc -> {
            try (var originalStream = mc.getResourceManager().getResourceOrThrow(net.minecraft.resources.Identifier.withDefaultNamespace("textures/item/shulker_shell.png")).open();
                 var maskStream = mc.getResourceManager().getResourceOrThrow(net.minecraft.resources.Identifier.fromNamespaceAndPath(BicolorShulkerboxes.ID, "textures/item/shulker_shell.png")).open();
                 var original = com.mojang.blaze3d.platform.NativeImage.read(originalStream);
                 var mask = com.mojang.blaze3d.platform.NativeImage.read(maskStream)) {
                check(original.getWidth() == mask.getWidth() && original.getHeight() == mask.getHeight(), "Shell texture size changed");
                for (int y = 0; y < original.getHeight(); y++) for (int x = 0; x < original.getWidth(); x++) {
                    int source = original.getPixel(x, y), pixel = mask.getPixel(x, y);
                    check((source >>> 24) == (pixel >>> 24), "Shell silhouette changed");
                    int shade = Math.max((source >> 16) & 255, Math.max((source >> 8) & 255, source & 255));
                    check((pixel & 0xffffff) == ((shade << 16) | (shade << 8) | shade), "Shell shading changed");
                }
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        });
        TestWorldSave save;
        try (var world = context.worldBuilder().create()) {
            save = world.getWorldSave();
            world.getServer().runCommand("fill -6 -61 -6 12 -61 12 minecraft:white_concrete");
            world.getServer().runCommand("tp @a 3 -60 7 180 12");
            world.getServer().runCommand("time set 6000");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int i = 0; i < 6; i++) {
                    ColorPair pair = i == 1 ? PAIR : new ColorPair(ShellColor.values()[i + 1], ShellColor.values()[16 - i]);
                    ItemStack item = ItemData.box(pair);
                    BlockPos pos = new BlockPos(i * 2, -60, 0);
                    level.setBlockAndUpdate(pos, Block.byItem(item.getItem()).defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.values()[i]));
                    var box = (ShulkerBoxBlockEntity)level.getBlockEntity(pos);
                    box.applyComponentsFromItemStack(item);
                    if (i == 1) box.setItem(0, new ItemStack(Items.DIAMOND, 37));
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int i = 1; i < ShellColor.values().length; i++) player.getInventory().setItem(i - 1, ItemData.shell(ShellColor.values()[i]));
                player.getInventory().setItem(17, ItemData.box(PAIR));
                player.getInventory().setItem(18, ItemData.box(new ColorPair(ShellColor.NATURAL, ShellColor.GREEN)));
                player.containerMenu.broadcastChanges();
            });
            world.getConnection().waitForChunksRender();
            waitForPair(context);
            context.runOnClient(mc -> {
                var entity = (ShulkerBoxBlockEntity)mc.level.getBlockEntity(BOX);
                var renderer = mc.getBlockEntityRenderDispatcher().<ShulkerBoxBlockEntity, net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState>getRenderer(entity);
                check(renderer instanceof BicolorBoxRenderer, "Bicolor renderer not installed");
                var state = renderer.createRenderState();
                renderer.extractRenderState(entity, state, 0, net.minecraft.world.phys.Vec3.ZERO, null);
                check(PAIR.equals(((BicolorBoxRenderer.State)state).colors), "Render state lost pair");
            });
            context.takeScreenshot("bicolor-world-six-orientations");
            world.getServer().runOnServer(server -> {
                var box = (ShulkerBoxBlockEntity)server.overworld().getBlockEntity(BOX);
                box.startOpen(server.getPlayerList().getPlayers().getFirst());
            });
            context.waitTicks(15);
            context.waitFor(mc -> ((ShulkerBoxBlockEntity)mc.level.getBlockEntity(BOX)).getProgress(0) > .95f);
            context.takeScreenshot("bicolor-open-lid");
            world.getServer().runOnServer(server -> ((ShulkerBoxBlockEntity)server.overworld().getBlockEntity(BOX)).stopOpen(server.getPlayerList().getPlayers().getFirst()));
            context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.waitTicks(3);
            context.takeScreenshot("bicolor-inventory");
            context.setScreen(() -> null);
            var reload = context.computeOnClient(mc -> mc.reloadResourcePacks());
            context.waitFor(mc -> reload.isDone(), 1200);
            reload.join();
            waitForPair(context);
            context.takeScreenshot("bicolor-after-resource-reload");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(0, ItemData.box(PAIR));
                player.containerMenu.broadcastChanges();
                var dropped = new net.minecraft.world.entity.item.ItemEntity(server.overworld(), 3, -59, 4, ItemData.box(PAIR));
                dropped.setNeverPickUp();
                server.overworld().addFreshEntity(dropped);
            });
            context.waitTicks(10);
            context.takeScreenshot("bicolor-box-in-hand-and-dropped");
            world.getServer().runCommand("tp @a 1600 -60 1600");
            context.waitFor(mc -> mc.level.getBlockEntity(BOX) == null, 1200);
            world.getServer().runCommand("tp @a 3 -60 7 180 12");
            world.getConnection().waitForChunksRender();
            waitForPair(context);
        }
        try (var reopened = save.open()) {
            reopened.getConnection().waitForChunksRender();
            waitForPair(context);
            reopened.getServer().runOnServer(server -> {
                var entity = (ShulkerBoxBlockEntity)server.overworld().getBlockEntity(BOX);
                check(PAIR.equals(((ColoredBox)entity).bicolor$getColors()), "Restart lost colors");
                check(entity.getItem(0).is(Items.DIAMOND) && entity.getItem(0).getCount() == 37, "Restart lost contents");
            });
            context.takeScreenshot("bicolor-after-world-restart");
        }
        testMixedPlayers(context);
    }
    private static void testVanillaServer(ClientGameTestContext context) {
        String address = "127.0.0.1:" + System.getProperty("bicolor.externalVanillaPort");
        context.runOnClient(mc -> net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
            new net.minecraft.client.gui.screens.TitleScreen(), mc,
            net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
            new net.minecraft.client.multiplayer.ServerData("Vanilla compatibility test", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null));
        context.waitFor(mc -> mc.level != null && mc.player != null, 1200);
        context.waitFor(mc -> mc.level.getBlockEntity(BOX) instanceof ShulkerBoxBlockEntity, 1200);
        context.runOnClient(mc -> check(((ColoredBox)mc.level.getBlockEntity(BOX)).bicolor$getColors() == null, "Vanilla box unexpectedly modified"));
        context.waitTicks(10);
        context.takeScreenshot("modded-client-on-vanilla-server");
        context.runOnClient(mc -> mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false));
    }
    private static void testMixedPlayers(ClientGameTestContext context) {
        if (System.getProperty("bicolor.projectDir") == null) return;
        java.util.Properties props = new java.util.Properties();
        props.setProperty("max-players", "2"); props.setProperty("server-ip", "127.0.0.1");
        props.setProperty("white-list", "false"); // Disposable loopback server; 26.3 defaults to an allowlist.
        props.setProperty("server-port", "25578"); props.setProperty("view-distance", "2");
        props.setProperty("level-name", "mixed-player-world");
        Process guest = null;
        try (var server = context.worldBuilder().createServer(props)) {
            try (var connection = server.connect()) {
                var path = java.nio.file.Path.of(System.getProperty("bicolor.projectDir"), "tools", "vanilla_client.py");
                guest = new ProcessBuilder("python3", path.toString(), "--port", "25578", "--username", "VanillaGuest").redirectErrorStream(true)
                    .redirectOutput(java.nio.file.Path.of("vanilla-guest.log").toFile()).start();
                context.waitFor(mc -> mc.getConnection() != null && mc.getConnection().getOnlinePlayers().size() == 2, 4800);
                server.runOnServer(s -> {
                    ItemStack stack = ItemData.box(PAIR);
                    s.overworld().setBlockAndUpdate(BOX, Block.byItem(stack.getItem()).defaultBlockState());
                    ((ShulkerBoxBlockEntity)s.overworld().getBlockEntity(BOX)).applyComponentsFromItemStack(stack);
                    for (var player : s.getPlayerList().getPlayers()) { player.getInventory().setItem(0, stack.copy()); player.containerMenu.broadcastChanges(); }
                });
                server.runCommand("tp @a 3 -60 7 180 12");
                connection.waitForChunksRender(); waitForPair(context);
                context.takeScreenshot("mixed-modded-and-vanilla-players");
                ColorPair changed = new ColorPair(ShellColor.GREEN, ShellColor.BLUE);
                server.runOnServer(s -> ((ColoredBox)s.overworld().getBlockEntity(BOX)).bicolor$setColors(changed));
                context.waitFor(mc -> changed.equals(((ColoredBox)mc.level.getBlockEntity(BOX)).bicolor$getColors()), 1200);
                server.runCommand("execute as @a[name=!VanillaGuest] in minecraft:the_nether run tp @s 0 80 0");
                context.waitFor(mc -> mc.level != null && mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER), 1200);
                server.runCommand("execute as @a[name=!VanillaGuest] in minecraft:overworld run tp @s 3 -60 7 180 12");
                context.waitFor(mc -> mc.level != null && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                    && mc.level.getBlockEntity(BOX) instanceof ColoredBox box && changed.equals(box.bicolor$getColors()), 1200);
            }
            try (var reconnected = server.connect()) {
                reconnected.waitForChunksRender();
                context.waitFor(mc -> mc.level.getBlockEntity(BOX) instanceof ColoredBox box
                    && new ColorPair(ShellColor.GREEN, ShellColor.BLUE).equals(box.bicolor$getColors()), 1200);
            }
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        finally { if (guest != null) guest.destroy(); }
    }
    private static void waitForPair(ClientGameTestContext context) {
        context.waitFor(mc -> mc.level != null && mc.level.getBlockEntity(BOX) instanceof ColoredBox box && PAIR.equals(box.bicolor$getColors()), 1200);
    }
}
