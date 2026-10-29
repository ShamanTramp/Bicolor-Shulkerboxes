# Data, recipes and rendering

## A single universal artifact

The main initializer uses common Minecraft classes only. The client initializer and client mixins live in `src/client/` and are marked client-only in `fabric.mod.json`. Loom combines both source sets into the one remapped distribution JAR. Dedicated servers do not initialize the graphics code.

There are no new item, block, component or recipe serializer registry entries. Shells use `minecraft:shulker_shell`. Boxes use the existing shulker box matching their base color. This permits unmodified clients to decode every item and block they receive.

## Version 1 metadata

Inside the vanilla `minecraft:custom_data` component, the mod owns only the `bicolor_shulkerboxes` key:

```json
{"bicolor_shulkerboxes":{"version":1,"kind":"shell","color":"red"}}
```

```json
{"bicolor_shulkerboxes":{"version":1,"kind":"box","lid":"red","base":"blue"}}
```

Colors are `natural` or vanilla dye names such as `light_blue`. Unknown schema versions and invalid pairs are not interpreted as valid mod metadata. Whole-box recoloring removes only recognized box metadata and the mod's own translated lore line; foreign custom data and other lore remain intact.

Natural shells and uniform boxes need no mod metadata. Shell names and box lore use standard translated text components, with bilingual fallbacks for clients that do not have the translations.

## Placed boxes

A mixin on the existing shulker block entity reads and collects the vanilla custom-data component. Its persistent extension stores that envelope under a `bicolor_shulkerboxes` root field, while the normal container implementation saves inventory and names. Thus a placed box contains `bicolor_shulkerboxes.bicolor_shulkerboxes` in its saved tag. This nested envelope intentionally retains other custom-data keys too.

The block's vanilla identity always matches its base color. Survival drops attach the color pair to the ordinary vanilla drop; creative container drops use component collection. No replacement container or second hidden inventory exists.

`getUpdateTag` and the normal block-entity update packet include only the recognized color pair. They do not expose inventory, locks or unrelated metadata to observers. Vanilla clients ignore the unknown tag fields; modded clients read them. Chunk loading naturally supplies initial state, and block updates supply changes. No per-player cache or channel negotiation is necessary, and there is no client-to-server color mutation packet.

## Recipes

The mod ships ordinary vanilla JSON recipes. Small mixins extend only recipes with explicitly namespaced groups:

- Shapeless shell dye recipes reject the current color and preserve shell custom data when producing a new shell.
- The original shaped shulker recipe is replaced with the same vanilla pattern plus a namespaced group; assembly reads upper/lower shell metadata.
- The 16 box recoloring recipes use vanilla transmutation. The extension also permits matching the existing base color when the lid differs, and clears bicolor metadata after conversion.

Minecraft remains responsible for slot matching, shift-click handling, ingredient consumption, remainder handling, crafter redstone execution and hopper transfers. Actual recipes loaded through the server recipe manager are exercised by tests, including the crafter cache.

## Client rendering

A local copy of a dyed shell stack selects a bundled tinted item model; no client-only model identifier is written back to inventory or sent by the server. The mask retains the original vanilla texture's alpha and per-pixel shading.

For placed boxes, a renderer captures the color pair alongside vanilla render state. Each half has its own baked model and fixed visibility mask, avoiding cross-contamination during deferred rendering. The transforms, textures and opening movement follow vanilla behavior. The special item renderer uses the same two-half rendering for inventory, hands and dropped entities, and includes the color pair in the item render identity.

All models and renderer objects are rebuilt by the normal resource reload process. Unmarked vanilla stacks and block entities use the vanilla render path.
