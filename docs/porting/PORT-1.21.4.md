# Port: 26.2 → 1.21.4

Read `README.md` first. Apply everything in `PORT-1.21.5.md` (which chains to the
1.21.6–1.21.8 file: all global renames + Shelf removal), **plus** the TooltipDisplay
removal below. 1.21.4 sits after the big 1.21.2 restructuring, so `ServerExplosion`,
`hurtServer`, `Equippable`, `RecipeMap`, unified `InteractionResult`, and `setId` item
registration are all present.

## Build
- `minecraft_version=1.21.4`; Java 21; `JAVA_21`; `mappings loom.officialMojangMappings()`.
- fabric-api `0.115.x+1.21.4` era (check fabricmc.net/develop).

## Deltas vs 1.21.5
- **`TooltipDisplay` does not exist** (introduced 1.21.5). In
  `ReviveItemFactory.applySkin`, delete the `TOOLTIP_DISPLAY` line and its imports.
  Pre-26.x profiles don't render a "Dynamic" tooltip line, so nothing needs hiding.
  (If a stray tooltip line does appear on heads in testing, the 1.20.5–1.21.4 mechanism
  for hiding component tooltips is the per-component `show_in_tooltip` flag, which does
  not exist for PROFILE — so there is genuinely nothing to hide here.)
- `ResolvableProfile`: record constructor
  `new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap())`.

## Verify
README checklist; check revive-menu heads show skins with a clean tooltip.
