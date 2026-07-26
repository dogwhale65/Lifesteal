# Port: 26.2 → 1.21.6 / 1.21.7 / 1.21.8

Read `README.md` first, then apply everything in `PORT-1.21.9-to-1.21.11.md` — this
target is identical to that port **plus** the following removals (pre–Copper Age).

## Build
- `minecraft_version=1.21.8` (or .6/.7); Java 21; `JAVA_21` mixin level;
  `mappings loom.officialMojangMappings()`.
- Known-good reference: fabric-api `0.129.x+1.21.8` era (check fabricmc.net/develop).

## Removals vs the 1.21.9 port
- **Shelf blocks do not exist**: delete `ShelfMixin.java` and its entry in
  `lifesteal.mixins.json`. No other shelf references exist.
- **Copper chests do not exist**: no code change (they were never referenced by name —
  the container blocking works by `BlockEntity` type).

## Everything else
Identical to `PORT-1.21.9-to-1.21.11.md`:
- All README global renames.
- `ResolvableProfile`: on this line use the record constructor
  `new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap())` —
  `createUnresolved` does not exist here.
- `TooltipDisplay` exists (keep the PROFILE-hiding line). NOTE: on this line the
  unresolved-profile head may not render a "Dynamic" tooltip line at all; keeping the
  hide is harmless either way.
- `RecipeMap`/`prepare` mixin shape exists; verify `fromJson` signature via javap.
- Keep `.setId(...)` in item settings; `InteractionResult` unified; `hurtServer`,
  `ServerExplosion`, `Equippable`, `ArmorSlot` all present.

## Verify
README checklist; confirm decorated pots refuse restricted items (shelves N/A here).
