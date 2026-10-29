# Development

Use JDK 21. The Gradle wrapper and Minecraft/Fabric dependency versions are checked into the repository.

```sh
./gradlew build
```

The distributable is `build/libs/bicolor-shulkerboxes-0.1.0.jar`. Test helper JARs are written separately to `build/testmods/` and must not be distributed with the mod.

## Full verification

Requires Node.js 22 and, on headless Linux, Xvfb and Mesa.

```sh
cd tests/vanilla
npm ci --ignore-scripts
cd ../..
./gradlew runProductionClientGameTest
python3 tools/vanilla_smoke.py
python3 tools/vanilla_server_client.py
```

On desktop Linux, use `xvfb-run -a` for graphical commands to keep test windows off the desktop. In CI, Loom starts Xvfb automatically. Scripts create disposable test worlds under `build/` and bind test servers to loopback.

- [Architecture and data format](ARCHITECTURE.md)
- [Test coverage and workflow](TESTING.md)
- [Local validation and screenshots](VALIDATION.md)

## Compatibility notes

Recipe datapacks overriding the same shulker recipes can change the mod's behavior. Other loaders, shaders and mods replacing shulker rendering or inventories have not been certified.

The assembly recipe book preview is generic, and autofill may choose any shells. The actual result slot reflects the selected ingredients. Choose shells manually when the color arrangement matters.

Keep the mod on the server while using bicolor blocks. Removing it is not a conversion procedure: a vanilla save may discard their extra block data.

## References

- [Mojang: Minecraft Java 1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)
- [Fabric for Minecraft 1.21.11](https://fabricmc.net/2025/12/05/12111.html)
- [Automated testing](https://docs.fabricmc.net/1.21.11/develop/automatic-testing)
- [Block entity rendering](https://docs.fabricmc.net/1.21.11/develop/blocks/block-entity-renderer)
- [Production run tasks](https://docs.fabricmc.net/1.21.11/develop/loom/production-run-tasks)

Code is MIT licensed. Minecraft and its original artwork belong to Mojang/Microsoft; the shell dye mask derives from the game's original texture. This project is not affiliated with Mojang or Microsoft.
