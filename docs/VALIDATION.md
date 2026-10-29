# Local validation — 29 October 2026

Target: Minecraft Java 1.21.11, Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11, Java 21.

Distribution: `bicolor-shulkerboxes-0.1.0.jar` (53,758 bytes).

SHA-256: `bea26f18fb0e2d13882f568b3e20546a83c576772ffe25050eadb195bc879c6e`.

## Completed checks

| Check | Result |
| --- | --- |
| Universal artifact contains common and client entrypoints, no test classes | PASS |
| JUnit tests | 7 passed |
| Server GameTests | 8 project scenarios + 1 Fabric framework scenario passed |
| Recipe matrices | 867 assemblies + 272 shell-dye attempts + 4,352 box recolorings passed |
| Hopper refill, disabled crafter slots, redstone pulses, ejection and consumption | PASS |
| Vanilla shell outline, transparency and shading retained pixel by pixel | PASS |
| Production client: animation, inventory, hand, dropped item, resource reload | PASS |
| Chunk unload/reload and world close/reopen | PASS |
| Real modded client + separate mod-free protocol guest simultaneously | PASS |
| Color update, dimension changes and reconnect | PASS |
| Mod-free protocol client: manual crafting and real container interactions | PASS |
| Complete dedicated-server process stop/restart, contents/colors retained, recoloring | PASS |
| Universal mod installed on client connecting to Mojang server.jar without Fabric/mods | PASS |

The production graphics test uses the same remapped mod artifact as the dedicated-server smoke test. The latter's report records its checksum. Temporary test helper JARs are separate and are not distributed.

The mod-free client is Mineflayer 4.37.1 speaking vanilla protocol 1.21.11. This verifies protocol compatibility and gameplay interactions, not the official vanilla GUI's pixel appearance. The complementary production client test runs an actual Minecraft GUI against a truly unmodded server.

## Captures

- [Original shell texture recolored, and box items](screenshots/bicolor-inventory.png)
- [Boxes in six orientations](screenshots/bicolor-world-six-orientations.png)
- [Animated lid](screenshots/bicolor-open-lid.png)
- [Concurrent modded and mod-free players](screenshots/mixed-modded-and-vanilla-players.png)
- [Modded client on an actual vanilla server](screenshots/modded-client-on-vanilla-server.png)

## Continuous integration

The checked-in workflow reproduces these checks and uploads the JAR, logs and screenshots. Remote GitHub Actions status is recorded separately once its first run completes; local success does not imply remote success.
