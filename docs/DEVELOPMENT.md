# Development

Target: Minecraft Java **26.3**, Java **25**, Fabric Loader **0.20.1**, Fabric API **0.165.0+26.3**.

Use JDK 25 and the checked-in Gradle wrapper:

```sh
./gradlew build
```

The distributable is `build/libs/bicolor-shulkerboxes-0.2.0+mc26.3.jar`. It contains both the server and optional client code. Test helpers live in `build/testmods/` and must not be distributed.

Minecraft is unobfuscated in this release. The build uses `net.fabricmc.fabric-loom`; there is no mappings dependency or remapping step.

## Full verification

On headless Linux, install Xvfb and Mesa. The tests use OpenGL and run the official Minecraft client in an isolated directory, without Fabric or mods, to check vanilla compatibility. Java 25 and Python 3 are required; Node.js is no longer needed.

```sh
./gradlew build
xvfb-run -a -s '-screen 0 1280x720x24 -noreset' ./gradlew runProductionClientGameTest
xvfb-run -a -s '-screen 0 1280x720x24 -noreset' python3 tools/vanilla_smoke.py
xvfb-run -a -s '-screen 0 1280x720x24 -noreset' python3 tools/vanilla_server_client.py
```

On Linux, set `SDL_VIDEODRIVER=x11` when using Xvfb. Each caller owns one display shared by its test clients. All test servers bind to loopback and use disposable worlds under `build/`; these settings do not affect a user's Minecraft installation.

- [Architecture and data format](ARCHITECTURE.md)
- [Test coverage](TESTING.md)
- [Validation evidence](VALIDATION.md)

## Compatibility

The version-1 color metadata from the 1.21.11 release is retained. The 26.3 build requires Minecraft 26.3; use release 0.1.0 for Minecraft 1.21.11.

Recipe datapacks overriding the same shulker recipes can change the mod's behavior. Other loaders, shaders and mods replacing shulker rendering or inventories have not been certified.

The assembly recipe book preview is generic, and autofill may choose any shells. The actual output slot reflects the selected ingredients. Choose shells manually when the arrangement matters.

Removing the mod is not a conversion procedure: a vanilla save may discard extra block data.

## References

- [Mojang: Minecraft Java 26.3](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)
- [Fabric for Minecraft 26.3](https://fabricmc.net/2026/09/15/263.html)
- [Official Fabric example for 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3)
- [Fabric automated testing](https://docs.fabricmc.net/develop/automatic-testing)

Code is MIT licensed. Minecraft and its original artwork belong to Mojang/Microsoft; the shell dye mask derives from the game's original texture. This project is not affiliated with Mojang or Microsoft.
