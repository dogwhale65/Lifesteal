# Port: 26.2 → 1.20.5 / 1.20.6

Read `README.md` first, then apply ALL of `PORT-1.21-to-1.21.1.md` (this target shares
its `InteractionResultHolder`, `hurt`, `Explosion`, elytra, RecipeManager-merge, and
item-registration changes) **plus** the deltas below. Item components DO exist here
(1.20.5 introduced them), so lore/name/profile/custom-data code ports with renames only.
Mace, Wind Charge, and the Crafter all exist (verified against the 1.20.5 jar).

## Build
- `minecraft_version=1.20.6` (or .5); **Java 21** (1.20.5 raised the requirement);
  `JAVA_21`; `mappings loom.officialMojangMappings()`.
- fabric-api `0.100.x+1.20.6` era (check fabricmc.net/develop).

## Deltas vs the 1.21/1.21.1 port

### 1. Enchantments are NOT data-driven yet (1.21 change)
`Enchantment`s live in the static registry. `ItemEnchantments` + `Mutable` exist
(components era) and are still keyed by `Holder<Enchantment>`, so
`InventoryEnforcer.clampComponent` largely survives. Change `EnchantmentsConfig.init`:
```java
// instead of registries.lookupOrThrow(Registries.ENCHANTMENT):
Registry<Enchantment> reg = BuiltInRegistries.ENCHANTMENT;
for (Enchantment enchant : reg) {
    ResourceLocation id = reg.getKey(enchant);
    ...same putIfAbsent(id.toString(), enchant.getMaxLevel())...
}
```
`init` can then run in `onInitialize` directly (no server registry access needed), but
keeping it in `SERVER_STARTED` also works. `holder.getRegisteredName()` and
`holder.value().getMaxLevel()` behave the same.

### 2. `ArmorSlot` does not exist (1.21 extracted it)
Delete `ArmorSlotMixin`. Armor slots are anonymous inner classes of `InventoryMenu`
here. The elytra armor-slot path is then covered by two remaining layers: the
`ElytraItem.use` gate (from the 1.21 port file) and the `InventoryEnforcer` scan
unequip. If a hard slot-block is wanted anyway, mixin
`net.minecraft.world.inventory.InventoryMenu$1` (verify the anonymous class index with
javap) — usually not worth it; document the 1-second scan window instead.

### 3. `ArmorSlot`-adjacent: `Inventory` size
`getContainerSize()` is 41 here (armor 36–39, offhand 40) — see README. Scans already
cover equipment; keep the explicit equipment walk for the `setItemSlot` re-sync.

### 4. Crafter redirect signature
`CraftingRecipe.assemble` in 1.20.5/6 takes `(CraftingInput?, ...)` — NO:
`CraftingInput` arrives in 1.21. Here recipes assemble from a container:
`assemble(CraftingContainer, HolderLookup.Provider)`. Check `CrafterBlock.dispenseFrom`
with `javap -c` and mirror the real call in the `@Redirect`; the gate/tag logic inside
the handler is unchanged.

### 5. Recipe JSON format is ALREADY modern
1.20.5 recipe results use `{"id": "...", "count": n}` — the shipped default templates in
`CustomRecipeLoader` work unchanged. (This flips at 1.20.4 and below — see that file.)

### 6. Misc
- `MobEffects.*` are `Holder<MobEffect>` here (changed in 1.20.5) — `EGAEffectStripper`
  ports unchanged. Same for `player.getEffect(Holder)`.
- `ResolvableProfile` exists (introduced 1.20.5): record ctor with
  `Optional.of(name)`.
- `TooltipDisplay` doesn't exist — drop the PROFILE-hide line (see PORT-1.21.4).
- `DecoratedPotBlock.useItemOn` returns `ItemInteractionResult` (as in 1.21 port).
- Shelves don't exist — `ShelfMixin` deleted (as in newer ports).
- `RecipeHolder` exists (since 1.20.2); the RecipeManager JSON-merge approach from the
  1.21 port file applies verbatim.

## Verify
README checklist; specifically confirm the enchant config generates the full 1.20.6
enchantment list and that a crafter refuses a second mace.
