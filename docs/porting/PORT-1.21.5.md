# Port: 26.2 → 1.21.5

Read `README.md` first. Identical scope to `PORT-1.21.6-to-1.21.8.md` (apply all of it,
including the Shelf removal) — 1.21.5 is the version that INTRODUCED `TooltipDisplay`,
so that component is still available and the `ReviveItemFactory` PROFILE-hide keeps
working.

## Build
- `minecraft_version=1.21.5`; Java 21; `JAVA_21`; `mappings loom.officialMojangMappings()`.
- fabric-api `0.119.x+1.21.5` era (check fabricmc.net/develop).

## Deltas (beyond the 1.21.6–1.21.8 file)
- None known. 1.21.5 ↔ 1.21.8 are API-compatible for every surface this mod touches:
  components (`ITEM_NAME`, `LORE`, `PROFILE`, `CUSTOM_DATA`, `ENCHANTMENTS`,
  `STORED_ENCHANTMENTS`, `TOOLTIP_DISPLAY`), `ServerExplosion`, `hurtServer`,
  `Equippable`, `ArmorSlot`, `RecipeMap`, unified `InteractionResult`, `setId` item
  registration.
- As always: verify `RecipeManager.fromJson` and `ResolvableProfile` construction shapes
  with javap against the mapped 1.21.5 jar before assuming.

## Verify
README checklist.
