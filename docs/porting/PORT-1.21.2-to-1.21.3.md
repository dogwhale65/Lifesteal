# Port: 26.2 → 1.21.2 / 1.21.3

Read `README.md` first. Apply everything in `PORT-1.21.4.md` (chains upward: global
renames, Shelf removal, TooltipDisplay removal). 1.21.2 is the version that INTRODUCED
the surfaces the newer ports rely on, so they are all still present here:
- `ServerExplosion` (ExplosionDamageMixin target) ✔
- `LivingEntity.hurtServer(ServerLevel, DamageSource, float)` ✔
- `Equippable` component (EquippableMixin) ✔
- `RecipeMap` + reworked `RecipeManager` (RecipeManagerMixin) ✔ — this manager was NEW
  in 1.21.2; double-check `prepare`'s return type and `fromJson`'s parameters with javap,
  as the earliest iteration may differ slightly from 26.2's shape.
- Unified `InteractionResult` for `Item.use` ✔
- `Item.Properties().setId(ResourceKey)` ✔ (required from here up)
- `CraftingInput` for the Crafter redirect ✔

## Build
- `minecraft_version=1.21.3` (or .2); Java 21; `JAVA_21`;
  `mappings loom.officialMojangMappings()`.
- fabric-api `0.114.x+1.21.3` era (check fabricmc.net/develop).

## Deltas vs 1.21.4
- None expected beyond routine verification. 1.21.2 → 1.21.4 kept these APIs stable.
- `Level.getMinY()` exists from 1.21.2 (the rename happened here) — the
  `ItemEntityDestructionMixin` void check ports unchanged.

## Verify
README checklist. Pay extra attention to boot-time mixin application for
`RecipeManagerMixin` and `ExplosionDamageMixin` (earliest versions of their targets).
