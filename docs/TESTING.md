# Verification workflow

Changes follow **red → green → regression**. Tests use the actual Minecraft recipe manager, component prototypes and container implementations.

## Unit tests

`./gradlew test` runs seven JUnit tests covering the 17-color palette, ordered pairs, serialization, validation, unknown schemas, and readability of version-1 legacy metadata.

In 26.3, mutable stack prototypes are bound when resources load. Tests of actual stack behavior therefore run as `ItemDataGameTests` on a real server, rather than mocking component prototypes in JUnit.

## Server GameTests

`./gradlew build` runs the project's server GameTests plus Fabric's framework check:

- 289 color pairs in three columns: 867 assemblies;
- 17 source shell colors × 16 dyes: 272 dye attempts, including same-color rejection;
- 272 mixed boxes × 16 dyes: 4,352 recolorings, preserving named container contents;
- invalid grid shapes and ingredients;
- real placement, save/load, drops and replacement;
- six facings, 27 slots, comparator output and nesting restrictions;
- redstone pulses, disabled slots, exact consumption, hopper refill and ejection;
- vanilla item identities, shell stackability, foreign data preservation and unknown metadata.

## Production client GameTest

`runProductionClientGameTest` loads the distributable JAR with a separate, non-distributed test helper. It verifies the original shell texture pixel by pixel, rendered colors, all six orientations, lid animation, inventory/hand/dropped rendering, resource reload, chunk unload/reload, and closing/reopening a saved world.

It then starts a loopback dedicated server with two actual Minecraft clients: the modded test client and Mojang's official client, launched directly without Fabric or mods. It checks simultaneous connection, initial colors, updates, dimension travel and reconnection.

The production harness uses Fabric's documented `disableNetworkSynchronizer=true` setting, because the vanilla client does not participate in Fabric's internal test synchronization. The official client is launched in offline developer mode only against an isolated loopback server; no real account or credentials are used.

## Vanilla client and server restart

`tools/vanilla_smoke.py` runs the Fabric launcher with the distributable JAR. It verifies the exact version-1 color envelope used by the 1.21.11 mod, and a container holding 37 diamonds. An official unmodded client joins and receives marked items through vanilla packets. The script stops the server completely, restarts the same world, checks colors and contents, and connects the vanilla client again.

This is a vanilla connection and persistence check. Manual crafting and inventory clicks over the network are no longer driven by Mineflayer, which did not support 26.3 at the time of this port. Crafting and container behavior remain covered by the real server GameTests. The obsolete Node probe is retained in the 0.1.0 Git tag, not the current build.

The launcher obtains Minecraft and its libraries from Mojang's official release metadata and verifies download checksums. It never loads Fabric or this mod into the vanilla client.

## Modded client on a vanilla server

`tools/vanilla_server_client.py` launches Mojang's server.jar directly, without Fabric or mods. It connects a client carrying the universal mod and verifies that an ordinary shulker box has no bicolor metadata.

## CI and evidence

GitHub Actions runs resource generation checks, the build, server and client GameTests, the vanilla client restart scenario, and the actual vanilla-server scenario. It stores the distributable separately from test helpers, logs and screenshots.

Fixed loopback ports 25576–25578 are reserved for these disposable tests. Headless graphics use Xvfb and Mesa/OpenGL. Validation records distinguish local runs from completed GitHub Actions runs.
