# Port: 26.2 → 1.20.2 / 1.20.3 / 1.20.4

Read `README.md` first. Apply the global renames, everything structural from
`PORT-1.21-to-1.21.1.md` (`InteractionResultHolder`, `hurt`, `Explosion` mixin,
RecipeManager JSON-merge, no `setId`), and the non-component parts of
`PORT-1.20.5-to-1.20.6.md` (static enchantment registry, no ArmorSlot). **The defining
work of this port is that item components do not exist — everything component-based
becomes NBT.** Java is back to 17.

## Build
- `minecraft_version=1.20.4` (or .2/.3); **Java 17**; `JAVA_17` mixin level;
  `options.release = 17`; `mappings loom.officialMojangMappings()`.
- fabric-api `0.91.x+1.20.4` era (check fabricmc.net/develop).

## Feature removals for this target
- **Mace + Wind Charge don't exist (1.20.5 additions)**: remove `minecraft:mace` from the
  default `uniqueItems`, delete `WindChargeMixin` + `disableWindCharges`. The generic
  unique/craft-gate code stays (chestplate + egg still use it).
- **1.20.2 only**: Crafter (added 1.20.3) → delete `CrafterBlockMixin`; decorated-pot
  storage (added 1.20.3) → delete `DecoratedPotMixin`. Keep both for 1.20.3/1.20.4, but
  note the pot's interaction method is a single `use(...)` pre-1.20.5 (no
  `useItemOn`/`useWithoutItem` split) — retarget the mixin accordingly (javap it).
- Shelves, TooltipDisplay, Equippable: gone as in newer ports.

## Component → NBT conversion map

| 26.2 component code | 1.20.2–1.20.4 NBT |
|---|---|
| `stack.get(DataComponents.CUSTOM_DATA)` / `CustomData.of(tag)` (UniqueItemManager) | `stack.getTag()` / `stack.getOrCreateTag()`; write with `tag.putString("unique", id)` directly on the live tag (no copy/set dance) |
| `stack.set(DataComponents.CUSTOM_NAME, comp)` (ReviveItemFactory) | `stack.setHoverName(comp)` |
| `stack.has/get(DataComponents.CUSTOM_NAME)` (menu click handlers) | `stack.hasCustomHoverName()` / `stack.getHoverName()` |
| `DataComponents.PROFILE` + `ResolvableProfile` (heads) | `stack.getOrCreateTag().putString("SkullOwner", playerName)` — the client resolves the skin |
| `DataComponents.ITEM_NAME` (colored default names in `Items`) | Not possible at registration. Move names to `assets/lifesteal/lang/en_us.json` (plain text; color via item class override of `getName(ItemStack)` returning the styled Component — add this override to `Heart`, `CraftedHeart`, `BeaconOfLife`, and a tiny `HeartFragmentItem` class) |
| `DataComponents.LORE` at registration | Not possible at registration. Override `appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)` in the same four item classes and add the styled lore lines there |
| `DataComponents.ENCHANTMENTS` / `STORED_ENCHANTMENTS` + `ItemEnchantments.Mutable` (InventoryEnforcer) | `EnchantmentHelper.getEnchantments(stack)` → `Map<Enchantment,Integer>`; clamp values; write back `EnchantmentHelper.setEnchantments(map, stack)`. For enchanted books the same helper pair works against the stored-enchantments tag in these versions — verify with javap; otherwise manipulate `EnchantedBookItem.getEnchantments(stack)` (ListTag) directly |
| `DataComponents.TOOLTIP_DISPLAY` | delete (N/A) |
| `MobEffects.*` as `Holder<MobEffect>` (EGAEffectStripper) | Plain `MobEffect` constants: `Set<MobEffect>`, `player.getEffect(MobEffect)`, `removeEffect(MobEffect)`. `MobEffectInstance` copy-constructor and the `hiddenEffect` accessor mixin are unchanged |
| `effect.getEffect().getRegisteredName()` (EffectBlockMixin) | `BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).toString()` |

## Recipe format change (IMPORTANT)
Shaped-recipe RESULTS use the pre-1.20.5 format:
```json
"result": { "item": "lifesteal:heart_fragment", "count": 1 }
```
Update all three default templates in `CustomRecipeLoader` (`"id"` → `"item"`). **Ingredients
also change**: the array-of-strings form is NOT valid here — `Ingredient.Value.CODEC` expects an
object, so each key becomes `"n": { "item": "minecraft:netherite_ingot" }`. A recipe using the
array form is rejected silently (the failure is only logged at debug level). Verified against
`data/minecraft/recipes/beacon.json` in the 1.20.2 jar. The
RecipeManager JSON-merge mixin from the 1.21 port file works here (`RecipeHolder`
exists since 1.20.2).

## Misc for this target
- `GameProfileCache.get(name)` returns `Optional<GameProfile>` — same shape as newer.
- `UserBanListEntry(GameProfile, Date, String, Date, String)` ✔.
- `Level.getMinBuildHeight()` (as in the 1.21 port).
- `Block.wasExploded(Level, BlockPos, Explosion)` ✔.
- Elytra: `ElytraItem.use` gate from the 1.21 port file works (Equipable interface
  exists); no ArmorSlot — scan safety-net covers slots.
- `SoundEvent.createVariableRangeEvent(ResourceLocation)` ✔ unchanged.
- `player.playNotifySound` ✔.
- Fabric item groups: `ItemGroupEvents.modifyEntriesEvent(...)` (README table).

## Verify
README checklist, plus: hover a Heart (name+lore render from the overrides), enchant a
book past a cap and confirm the stored enchant clamps, revive-menu heads show skins via
SkullOwner.
