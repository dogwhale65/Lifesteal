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
(components era) and `keySet()` is keyed by `Holder<Enchantment>`, so
`InventoryEnforcer.clampComponent` largely survives — **but** `ItemEnchantments.getLevel`
and `Mutable.set` take a bare `Enchantment` on this version, not the holder. Inside the
`keySet()` loop, pass `holder.value()` to both. Change `EnchantmentsConfig.init`:
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
`CraftingInput` arrives in 1.21; here recipes assemble from a container. Verified against
the 1.20.5 jar, the call site in `CrafterBlock.dispenseFrom` is **erased to `Container`**:
```
Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;
```
so the `@Redirect` target and its handler parameter must both say `Container` (cast to
`CraftingContainer` inside to make the real call). Targeting `CraftingContainer` in the
descriptor matches nothing and fails the injection. The gate/tag logic is unchanged.

### 5. Recipe RESULTS are already modern — INGREDIENTS are not
1.20.5 recipe results use `{"id": "...", "count": n}`, so the `result` blocks in
`CustomRecipeLoader` port unchanged. (That flips at 1.20.4 and below — see that file.)
The **`key` entries do not**: the bare-string ingredient form
```json
"n": ["minecraft:netherite_ingot"]
```
is 1.21+ syntax, and this version's `Ingredient` codec rejects it with
`Not a JSON object: [...]; Item array cannot be empty`. Every key must be an object:
```json
"n": {"item": "minecraft:netherite_ingot"}
```
Rewrite all three default templates. The failure is silent at boot — the recipes log as
loaded by the mixin and then fail during vanilla parsing, so check the log for
`Parsing error loading recipe lifesteal:*` rather than trusting the count line.

### 6. `ItemCombinerMenu.mayPickup` is abstract here — move the gate to `SmithingMenu`
`mayPickup` only gains a body on `ItemCombinerMenu` in 1.21.2. Injecting into the abstract
declaration fails at APPLY and, with `defaultRequire: 1`, crashes the boot. Delete
`ItemCombinerMenuMixin` and fold its handler into `SmithingMenuMixin` — the handler already
early-returned on anything that was not a `SmithingMenu`, so nothing is lost.

Do **not** carry the `@Shadow @Final protected ResultContainer resultSlots` across with it:
Mixin does not resolve `@Shadow` against a field declared in a *superclass* of the target and
throws `@Shadow field resultSlots was not located in the target class`. Read the result
through the public pair instead:
```java
SmithingMenu self = (SmithingMenu) (Object) this;
ItemStack result = self.getSlot(self.getResultSlot()).getItem();
```

### 7. Misc
- `MobEffects.*` are `Holder<MobEffect>` here (changed in 1.20.5) — `EGAEffectStripper`
  ports unchanged. Same for `player.getEffect(Holder)`.
- `ResolvableProfile` exists (introduced 1.20.5): record ctor with
  `Optional.of(name)`.
- `TooltipDisplay` doesn't exist — drop the PROFILE-hide line (see PORT-1.21.4).
- `DecoratedPotBlock.useItemOn` returns `ItemInteractionResult` (as in 1.21 port).
- Shelves don't exist — `ShelfMixin` deleted (as in newer ports).
- `RecipeHolder` exists (since 1.20.2); the RecipeManager JSON-merge approach from the
  1.21 port file applies verbatim — `apply(Map, ResourceManager, ProfilerFiller)` is the
  declared overload to target.
- `Explosion`'s `source`/`damageSource` are `final`, so `ExplosionDamageMixin` needs
  `@Shadow @Final`. Entity damage is inline in `explode()`; there is no `hurtEntities`.
- `Block.wasExploded` takes a plain `Level` here (it narrows to `ServerLevel` in 1.21.2).
- `Level.getMinY()` is `getMinBuildHeight()` (inherited from `LevelHeightAccessor`).
- `ResourceLocation.parse` / `fromNamespaceAndPath` do **not** exist yet — use the public
  constructors `new ResourceLocation(s)` / `new ResourceLocation(ns, path)`. `tryParse` is fine.

## Verify
README checklist; specifically confirm the enchant config generates the full 1.20.5/6
enchantment list (**42** on 1.20.5) and that a crafter refuses a second mace.
Boot result on 1.20.5: `Done`, `[Recipes] Loaded 3 custom recipe(s) from config`,
`Loaded 1178 recipes` (1175 vanilla + 3), `[Enchants] Limits active for 42 enchantment(s)`.
