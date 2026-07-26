# Port: 26.2 → 1.21 / 1.21.1

Read `README.md` first and apply all global renames + the Shelf and TooltipDisplay
removals from the newer port files. **This is the first big cliff going backward** —
1.21.2's restructuring has not happened yet. Item components DO exist (since 1.20.5) and
enchantments ARE data-driven (since 1.21), so `Items.java`, the lore/name components,
`EnchantmentsConfig`, and `InventoryEnforcer`'s clamp logic port with renames only.

## Build
- `minecraft_version=1.21.1` (or 1.21); Java 21; `JAVA_21`;
  `mappings loom.officialMojangMappings()`.
- fabric-api `0.116.x+1.21.1` era (check fabricmc.net/develop).

## 1. `Item.use` returns `InteractionResultHolder<ItemStack>` (not `InteractionResult`)
Affects: `Heart`, `CraftedHeart`, `BeaconOfLife`, `EnchantedGoldenAppleMixin`,
`EnderPearlMixin`, `WindChargeMixin`.
```java
// item classes:
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
    ItemStack stack = user.getItemInHand(hand);
    if (level.isClientSide()) return InteractionResultHolder.pass(stack);
    ...
    return InteractionResultHolder.fail(stack);     // was InteractionResult.FAIL
    return InteractionResultHolder.success(stack);  // was InteractionResult.SUCCESS
}
// mixins: CallbackInfoReturnable<InteractionResultHolder<ItemStack>>, and cancel with
// cir.setReturnValue(InteractionResultHolder.fail(user.getItemInHand(hand)));
```

## 2. `hurtServer` → `hurt`
`TNTCartExplosionMixin` and `ExplosionEntityDamageMixin` target
`hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z` on `LivingEntity` (no
`ServerLevel` param). The re-dispatch inside `TNTCartExplosionMixin` becomes
`target.hurt(source, capped)`. Guard against client: `hurt` runs on both sides in
≤1.21.1 — add `if (target.level().isClientSide()) return;` at the top.

## 3. `ServerExplosion` → `Explosion`
`ExplosionDamageMixin` targets `net.minecraft.world.level.Explosion`. Entity damage
happens inside `explode()` here (there is no separate `hurtEntities`):
```java
@Mixin(Explosion.class)
public class ExplosionDamageMixin {
    @Shadow @Final @Nullable private Entity source;       // verify field names via javap
    @Shadow @Final private DamageSource damageSource;
    @Inject(method = "explode", at = @At("HEAD")) ...set suppression...
    @Inject(method = "explode", at = @At("RETURN")) ...clear suppression...
}
```
`damageSource.typeHolder().getRegisteredName()` works here; if `getRegisteredName` is
missing on this version's `Holder`, use `damageSource.is(DamageTypes.BAD_RESPAWN_POINT)`.

## 4. `Equippable` component does not exist → elytra gate moves
Delete `EquippableMixin`. Replace with a mixin on `ElytraItem`:
```java
@Mixin(ElytraItem.class)
public class ElytraEquipMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockEquip(Level level, Player user, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!ServerConfig.getInstance().disableElytras) return;
        if (level.isClientSide()) return;
        ... message + resync ...
        cir.setReturnValue(InteractionResultHolder.fail(user.getItemInHand(hand)));
    }
}
```
`ArmorSlotMixin` still works (ArmorSlot exists since 1.21). The `InventoryEnforcer`
scan safety-net stays and covers dispenser equips.

## 5. `RecipeManagerMixin` — pre-RecipeMap loader
`RecipeManager` here extends the JSON reload listener with
`apply(Map<ResourceLocation, JsonElement>, ResourceManager, ProfilerFiller)`. The
SIMPLEST correct approach (recommended — replaces the whole current mixin body): merge
the raw config JSON into that map and let vanilla parse it:
```java
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true)
    private Map<ResourceLocation, JsonElement> lifesteal$injectConfigRecipes(Map<ResourceLocation, JsonElement> map) {
        Map<ResourceLocation, JsonElement> merged = new HashMap<>(map);
        for (var e : CustomRecipeLoader.loadConfigured().entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            if (id != null) merged.put(id, e.getValue()); // overrides datapack/vanilla
        }
        return merged;
    }
}
```
No `fromJson` shadow, no `RecipeMap`, no `registries` field needed. Log the count.
(Verify the exact `apply` descriptor with javap — it may be the erased bridge; target the
declared `apply` overload.)

## 6. Item registration
Remove `.setId(ResourceKey.create(...))` from `Items.settings` — `new Item.Properties()`
alone, registered via `Registry.register(BuiltInRegistries.ITEM, ResourceLocation..., item)`.

## 7. Misc for this target
- `Level.getMinY()` → `Level.getMinBuildHeight()` (`ItemEntityDestructionMixin`).
- `Block.wasExploded(ServerLevel, ...)` → `wasExploded(Level, BlockPos, Explosion)` —
  adjust `DragonEggExplosionMixin`'s handler parameters.
- `DecoratedPotBlock.useItemOn` returns **`ItemInteractionResult`** on 1.20.5–1.21.1:
  `CallbackInfoReturnable<ItemInteractionResult>`, fail with
  `ItemInteractionResult.FAIL`.
- `CrafterBlockMixin`: the crafter exists, but `CraftingRecipe.assemble` takes
  `(CraftingInput, HolderLookup.Provider)` in 1.21–1.21.1 — check the exact call in
  `CrafterBlock.dispenseFrom` with `javap -c` and mirror the descriptor in the
  `@Redirect`. (CraftingInput exists since 1.21.)
- `ResolvableProfile`: record ctor `new ResolvableProfile(Optional.of(name),
  Optional.empty(), new PropertyMap())`.
- Enchantments: data-driven (`Holder<Enchantment>`, dynamic registry) — the 26.2 code's
  `registryAccess().lookupOrThrow(Registries.ENCHANTMENT)` pattern works. Keep it.

## Verify
README checklist, plus: throw a pearl with the toggle on (checks the resync path with
`InteractionResultHolder`), and blow up a bad-respawn-anchor near a mob (explosion
suppression via the reworked mixin).
