# Port: 26.2 → 1.20.1

Read `README.md` first. This is the largest port. Apply EVERYTHING in
`PORT-1.20.2-to-1.20.4.md` (NBT conversion, Java 17, feature removals — including the
1.20.2-only removals: no Crafter, no decorated-pot storage) **plus** the deltas below.

## Build
- `minecraft_version=1.20.1`; Java 17; `JAVA_17`; `options.release = 17`;
  `mappings loom.officialMojangMappings()`.
- Known-good line: fabric-loader `0.15.x/0.16.x`, fabric-api `0.92.x+1.20.1`
  (1.20.1 is an LTS-ish modding target; 0.92.x is its terminal fabric-api line).

## Deltas vs 1.20.2–1.20.4

### 1. No `RecipeHolder` (added 1.20.2)
Recipes are `Recipe<?>` carrying their own id (`recipe.getId()`). This only affects the
custom-recipe injection — the JSON-merge approach dodges it entirely: merge raw
`JsonElement`s into `RecipeManager.apply`'s map (same `@ModifyVariable` mixin as the
1.21 port file) and vanilla parses them. Verify the `apply` descriptor:
`apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V`.

### 2. Block interaction is a single `use` method
The `useItemOn`/`useWithoutItem` split arrived in 1.20.5, and 1.20.1 has no pot storage
or crafter anyway (both deleted for this target) — so no block-use mixins remain except
none. Nothing to do beyond the deletions.

### 3. `GameProfileCache` details
`server.getProfileCache().get(name)` → `Optional<GameProfile>` ✔ (same). Op check:
`server.getPlayerList().isOp(GameProfile)` ✔.

### 4. Sundry 1.20.1 signatures (verify each with javap; expected shapes listed)
- `HopperBlockEntity.addItem(Container, ItemEntity)` ✔ static, same.
- `Slot.mayPlace/mayPickup`, `ResultSlot.onTake`, `ItemCombinerMenu.mayPickup(Player,
  boolean)`, `SmithingMenu.onTake` ✔ same shapes.
- `ShulkerBoxSlot` ✔ exists.
- `BundleItem.overrideStackedOnOther(ItemStack, Slot, ClickAction, Player)` and
  `overrideOtherStackedOnMe(ItemStack, ItemStack, Slot, ClickAction, Player, SlotAccess)`
  ✔ same (bundles are experimental content here but the item class exists).
- `MinecartTNT.primeFuse()` ✔ no-arg (README).
- `LivingEntity.checkTotemDeathProtection(DamageSource)` ✔ same name.
- `LivingEntity.addEffect(MobEffectInstance, Entity)` ✔ same.
- `EndDragonFight.setDragonKilled(EnderDragon)` with the podium
  `setBlockAndUpdate(..., Blocks.DRAGON_EGG...)` inside ✔ — same redirect approach; the
  level field may be typed `ServerLevel` already; confirm the exact
  `setBlockAndUpdate` owner in the bytecode (`ServerLevel` vs `Level`) and match the
  redirect descriptor.
- `EnderpearlItem.use` → `InteractionResultHolder<ItemStack>` (as in all pre-1.21.2).
- `Explosion` mixin per the 1.21 port file; in 1.20.1 the fields are
  `source`/`damageSource` — verify with javap. If `Holder.getRegisteredName` is absent,
  use `damageSource.is(DamageTypes.BAD_RESPAWN_POINT)`.
- `Inventory.getContainerSize()` = 41 (README note).
- `new ResourceLocation(ns, path)` / `new ResourceLocation(s)` — the static `parse`/
  `fromNamespaceAndPath` factories may not exist in 1.20.1; constructors are safe.

### 5. Enchantment ids
`BuiltInRegistries.ENCHANTMENT.getKey(enchant).toString()` for config keys;
`enchant.getMaxLevel()` ✔. `EnchantmentHelper.getEnchantments/setEnchantments` for
clamping (per the 1.20.2–1.20.4 file). Note 1.20.1 has 38-ish enchantments (no density/
breach/wind burst) — the registry-driven config handles this automatically.

### 6. Unique items on 1.20.1
With no Mace, the default `uniqueItems` list is just `minecraft:netherite_chestplate`
(smithing-gated — smithing menu APIs are stable) and the Dragon Egg flow. The generic
system needs no structural change.

## Verify
README checklist (expect `[Enchants] Limits active for ~38 enchantment(s)`), plus a full
manual pass of the revive GUI — this port touches every rendering path (NBT names/lore).
