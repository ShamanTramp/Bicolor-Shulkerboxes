# Minecraft 26.3 validation — 30 October 2026

Target: Minecraft Java 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, Java 25.

Distribution: `bicolor-shulkerboxes-0.2.0+mc26.3.jar` (53,683 bytes). The same file is used on servers and optional clients.

SHA-256: `284ea932de19ce8054d060134e02cb0aef8a62e1901385f66a5b606d003a1485`.

| Check | Local result |
| --- | --- |
| Common and client entrypoints in one JAR; test helpers excluded | PASS |
| Unit tests | 7 passed |
| Server GameTests | 12 project scenarios + 1 Fabric framework scenario passed |
| Recipe matrices | 867 assemblies + 272 shell-dye attempts + 4,352 box recolorings passed |
| Stackability, names, contents, foreign data and unknown schemas | PASS |
| Crafter, disabled slots, pulses, hopper refill and ejection | PASS |
| Vanilla shell texture pixel silhouette and shading | PASS |
| Production client rendering, lid animation, inventory, hand and dropped items | PASS |
| Resource reload, chunk unload/reload, saved-world close/reopen | PASS |
| Actual modded and official unmodded clients connected simultaneously | PASS |
| Color updates, dimension travel and reconnect | PASS |
| Official vanilla client on a production Fabric server | PASS |
| Version-1 color envelope and 37 diamonds retained after process stop/restart | PASS |
| Modded client on an official server.jar without Fabric or mods | PASS |

The official vanilla client is launched directly from Mojang's release binaries and libraries, without Fabric, test mods or this mod. Its downloads are checksum-verified. The server smoke report records the tested mod checksum.

Crafting behavior is exercised by Minecraft's actual server recipe manager and crafter. The old Mineflayer network-click test was replaced because that library did not support 26.3. The current vanilla-client check proves connection, marked-item compatibility and persistence; it does not automate crafting clicks in the vanilla GUI.

Screenshots in [the gallery](screenshots/) were refreshed using the 26.3 client. [The 1.21.11 validation record](https://github.com/ShamanTramp/Bicolor-Shulkerboxes/blob/v0.1.0/docs/VALIDATION.md) remains available with release 0.1.0.

The checked-in GitHub Actions workflow repeats these checks. Remote CI success is reported separately once the run completes.
