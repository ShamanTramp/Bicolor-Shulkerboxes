# Bicolor Shulkerboxes

A Fabric mod for **Minecraft Java 1.21.11**. Dye shulker shells, then combine two shells around a chest to choose a shulker box's lid and base colors.

**One identical JAR for servers, clients and singleplayer. Installing it on multiplayer clients is optional.** Players without the mod can join, craft and use the boxes; they see a vanilla box in its base color, with a tooltip describing both colors.

## Installation

Requirements: Java 21, Minecraft Java **1.21.11**, Fabric Loader **0.18.4 or newer**, and Fabric API **0.141.6+1.21.11**.

1. Install Fabric and Fabric API for 1.21.11.
2. Put `bicolor-shulkerboxes-0.1.0.jar` into `mods/` on the server.
3. Optionally put **the exact same file** into `mods/` on clients to see colored shells and bicolor boxes.
4. For singleplayer, install that same file and Fabric API on the client.

No resource pack download is required. A client with this mod can also join a vanilla server: normal vanilla behavior is retained there.

## Recipes

- **Shell + dye**, shapeless: colors one shell. Already colored shells can be dyed again; applying the same color has no recipe.
- **Shell / chest / shell**, vertically in any crafting-table or crafter column: the upper shell colors the lid, the lower shell colors the base.
- **Bicolor box + dye**, shapeless: produces a uniform vanilla box while keeping contents and its custom name. This also works when the dye matches its existing base color.

The palette is all 16 dyes plus the original natural shell appearance. Natural is distinct from purple. Matching halves produce the corresponding ordinary vanilla box. Two natural shells keep the original vanilla recipe.

Shell dyeing and box recoloring also work in the player's 2×2 grid. Box assembly requires a 3×3 grid. All recipes work in the redstone crafter. The assembly recipe book preview is a generic box; the actual output slot reflects the shells placed in the grid. Book autofill may choose any matching shells from the inventory; place them manually to select exact colors.

The mod preserves the original shell texture's pixel outline and shading, removing its purple hue only in the dye mask. Natural shells retain the untouched vanilla texture. Boxes reuse vanilla geometry, textures and lid animation. The visible split follows the actual lid/base geometry rather than an artificial 50/50 stripe.

## Build and test

Use JDK 21. Gradle is supplied through the checked-in wrapper.

```sh
./gradlew build
```

The single distributable is `build/libs/bicolor-shulkerboxes-0.1.0.jar`. Test-only helper JARs live separately in `build/testmods/`; do not install or distribute those.

The full verification suite additionally requires Node.js 22 and, on headless Linux, Xvfb and Mesa:

```sh
cd tests/vanilla
npm ci --ignore-scripts
cd ../..
./gradlew runProductionClientGameTest
python3 tools/vanilla_smoke.py
python3 tools/vanilla_server_client.py
```

On a desktop Linux machine without `CI=true`, run graphical commands inside `xvfb-run -a` to keep test windows off the desktop. In CI, Loom starts Xvfb automatically. The scripts create disposable, loopback-only test servers under `build/`; they do not modify an existing Minecraft installation.

See [test coverage](docs/TESTING.md), [data and networking](docs/ARCHITECTURE.md), and [verification evidence](docs/VALIDATION.md).

## Compatibility

- No custom item, block, recipe serializer or data-component registrations are required on vanilla clients.
- Storage stays at 27 slots. Redstone, hoppers, facing, opening clearance and shulker nesting restrictions use vanilla implementations.
- Server-installed mod data travels through standard item components and block-entity packets. There is no mandatory custom network channel.
- Recipe datapacks overriding the same shulker recipe files can change this mod's behavior.
- Only Fabric 1.21.11 is targeted. Other loaders, shaders and mods replacing shulker rendering or inventories are not certified.
- Keep the mod installed on the server while using bicolor blocks. Removing it is not a conversion procedure: a vanilla save may discard their extra block data.

## Official references

- [Mojang: Minecraft Java 1.21.11 technical changes](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)
- [Fabric for Minecraft 1.21.11](https://fabricmc.net/2025/12/05/12111.html)
- [Fabric automated testing](https://docs.fabricmc.net/1.21.11/develop/automatic-testing)
- [Fabric block entity rendering](https://docs.fabricmc.net/1.21.11/develop/blocks/block-entity-renderer)
- [Fabric production run tasks](https://docs.fabricmc.net/1.21.11/develop/loom/production-run-tasks)

Code is MIT licensed. Minecraft and its original artwork belong to Mojang/Microsoft; the shell dye mask derives from the game's original texture. This project is not affiliated with Mojang or Microsoft.
