# Verification workflow

Changes follow **red → green → regression**: first reproduce a missing or incorrect behavior, implement it, then run the targeted checks and relevant surrounding scenarios. Initial color and item-data tests were compiled before those production classes existed; they failed, then passed after implementation. The shell artwork correction has an additional runtime pixel-level regression assertion.

## Unit tests

`./gradlew test` uses Fabric Loader JUnit and initializes Minecraft registries for stack/component tests. It covers:

- 17 colors, ordered pairs, inverse pairs and malformed color names;
- vanilla item identities, shell stackability and all 289 pair encodings;
- preservation of contents, names and foreign data during recoloring;
- refusal to reinterpret or delete unknown future metadata.

Loom also attaches the server GameTests to the build/test lifecycle.

## Server GameTests

The project's GameTests use actual recipes loaded from resources, not a duplicate recipe implementation:

- all 289 pairs in each of three columns: 867 assemblies;
- 17 source shell colors × 16 dyes: 272 dye attempts, including same-color rejection;
- 272 mixed boxes × 16 dyes: 4,352 recolorings, preserving named container contents;
- invalid grid shapes and ingredients;
- placement through `BlockItem`, save/load, drops and replacement;
- six facings, 27-slot capacity, comparator output and nesting restrictions;
- real redstone crafter pulses, disabled slots and exact consumption;
- invalid same-color crafter input consuming nothing;
- hopper refill of an existing recipe and colored-shell ejection.

There are also Fabric's own framework validation tests; these are reported separately from project scenarios in the validation summary.

## Production client GameTest

`./gradlew runProductionClientGameTest` loads the same remapped distribution JAR installed on the production server. A separate, non-distributed test helper supplies assertions and screenshots.

It verifies the original shell texture's silhouette and shading pixel by pixel, color synchronization, all six box orientations, render-state extraction, lid animation, inventory rendering, hand/dropped rendering, resource reload, chunk unload/reload, and save/close/reopen with contents intact.

An in-process dedicated server then hosts the real modded client alongside a separate Node vanilla-protocol client with no Fabric or mod code. It checks concurrent players, initial color state, subsequent color updates, dimension travel and reconnect.

The production helper contains only the client test entrypoint. Server GameTest annotations remain in the development GameTest harness; loading their published annotation defaults in an intermediary production environment caused an enum annotation error during setup. This separation keeps that test-framework issue out of the production client test and the shipped mod. Production network tests also apply Fabric’s documented `disableNetworkSynchronizer=true` workaround, because the additional protocol client does not participate in the test framework’s internal synchronizer.

## Mod-free client against a production server

```sh
python3 tools/vanilla_smoke.py
```

Starts an actual Fabric launcher with the distribution JAR on loopback, then drives manual inventory clicks through Mineflayer using protocol 1.21.11. It dyes two shells, assembles a box at a table, reads tooltip metadata, places it, deposits 37 diamonds, breaks/picks up/replaces it, stops the server, restarts the same world, checks contents/colors, and recolors the recovered box.

This is a **protocol-client** test, not a screenshot test of the official vanilla GUI. It loads neither Fabric nor this mod in the client. The probe explicitly sends Minecraft's `player_loaded` acknowledgement after chunks arrive, because the pinned Mineflayer release does not yet send it automatically.

Reports include the SHA-256 of the tested mod. `--phase restart` is available for targeted diagnosis against the already-created disposable world.

## Modded client against a vanilla server

```sh
python3 tools/vanilla_server_client.py
```

Runs Mojang's `server.jar` directly, with no Fabric and no mods, then connects a production client carrying the universal mod. The client verifies that the ordinary shulker block has no bicolor state and captures a screenshot.

## Evidence and CI

GitHub Actions runs generated-resource checks, unit/server tests, production graphics, both protocol smoke phases and the actual vanilla-server compatibility scenario. It stores the one distributable JAR separately from logs, reports and screenshots. No test helper or Node dependency is bundled in the distributable.

Tests use disposable worlds and fixed loopback ports 25576–25578. Stop other test runs using those ports before running the suite. Graphical tests need OpenGL; headless Linux uses Xvfb and Mesa. The local validation record distinguishes executed checks from CI configuration; remote CI is only confirmed once its run has finished successfully.
