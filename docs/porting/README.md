# Lifesteal Backporting Guide (26.2 → 1.20.1 … 1.21.11)

This folder contains one porting plan per target version cluster. Each plan lists every
API change needed to port the mod **from its current Minecraft 26.2 codebase backward**
to that target. Hand the README plus ONE port file to the model doing the port.

## How to use these documents

1. Read this README fully — the rename tables and feature matrix apply to every port.
2. Open the port file for your target version. It lists file-by-file changes and
   version-specific gotchas. Where a port file says "see README table", apply the
   corresponding global rename.
3. Ports are cumulative going backward: the 1.21.4 file describes the delta from 26.2
   directly (not from 1.21.5) — each file is self-contained against the 26.2 source.
4. After porting: `gradlew compileJava`, then `gradlew runServer` and confirm the server
   reaches `Done` — this project uses `defaultRequire: 1` in `lifesteal.mixins.json`, so
   any failed mixin injection crashes the boot (this is deliberate; keep it).

## Mappings strategy (important)

The 26.2 project uses **Mojang official mappings** (Loom's default in 26.x — note there
is NO `mappings` line in build.gradle). For every backport, add an explicit mappings line
and KEEP Mojang mappings rather than switching to Yarn:

```gradle
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    ...
}
```

All class/method names in these documents are Mojang mappings for that version.

## Build configuration per target

Check exact fabric-loader / fabric-api / loom versions on https://fabricmc.net/develop
(the numbers below are known-good ballparks; the site is authoritative).

| Target | Java | Loom | mixins.json `compatibilityLevel` | `options.release` |
|---|---|---|---|---|
| 1.20.1 | 17 | 1.4+ | JAVA_17 | 17 |
| 1.20.2–1.20.4 | 17 | 1.4+ | JAVA_17 | 17 |
| 1.20.5–1.20.6 | 21 | 1.6+ | JAVA_21 | 21 |
| 1.21–1.21.1 | 21 | 1.6+ | JAVA_21 | 21 |
| 1.21.2–1.21.3 | 21 | 1.8+ | JAVA_21 | 21 |
| 1.21.4 | 21 | 1.8+ | JAVA_21 | 21 |
| 1.21.5 | 21 | 1.9+ | JAVA_21 | 21 |
| 1.21.6–1.21.8 | 21 | 1.10+ | JAVA_21 | 21 |
| 1.21.9–1.21.11 | 21 | 1.11+ | JAVA_21 | 21 |

Also update in every port: `gradle.properties` (`minecraft_version`, `loader_version`,
`fabric_api_version`), `fabric.mod.json` (`"minecraft"` depends range, `"java"` depends),
and `build.gradle` (`options.release`, `sourceCompatibility`/`targetCompatibility`).
`src/main/resources/lifesteal.mixins.json`: set the compatibility level from the table.

Language note: the codebase uses records, switch expressions, and text blocks — all fine
on Java 17. Nothing needs rewriting for the Java 17 targets.

## Global renames (apply in EVERY backport)

These are 26.x-only names; every target below 26.x uses the older name.

| 26.2 (current code) | 1.20.1 – 1.21.11 | Notes |
|---|---|---|
| `net.minecraft.resources.Identifier` | `net.minecraft.resources.ResourceLocation` | 26.x renamed the class. `Identifier.parse(s)` → `ResourceLocation.parse(s)` (1.21+) or `new ResourceLocation(s)` (1.20.x); `Identifier.fromNamespaceAndPath(ns,p)` → `ResourceLocation.fromNamespaceAndPath(ns,p)` (1.21+) or `new ResourceLocation(ns,p)` (1.20.x); `Identifier.tryParse` → `ResourceLocation.tryParse` |
| `net.minecraft.server.players.NameAndId` | `com.mojang.authlib.GameProfile` | Ban list, op checks, profile cache all take `GameProfile`. `profile.id()`/`profile.name()` → `profile.getId()`/`profile.getName()` |
| `server.services().nameToIdCache()` (`UserNameToIdResolver`) | `server.getProfileCache()` (`GameProfileCache`) | `cache.get(name)` returns `Optional<GameProfile>` on both |
| `player.nameAndId()` | `player.getGameProfile()` | e.g. `PlayerList.isOp(GameProfile)` |
| `net.minecraft.world.inventory.ContainerInput` | `net.minecraft.world.inventory.ClickType` | `AbstractContainerMenu.clicked(int, int, ClickType, Player)`; `ContainerInput.PICKUP` → `ClickType.PICKUP` |
| `net.minecraft.world.level.gamerules.GameRules` (typed `GameRule<T>` keys) | `net.minecraft.world.level.GameRules` (Key/Value system) | `rules.get(GameRules.SHOW_DEATH_MESSAGES)` → `rules.getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)`; `rules.set(KEY, v, server)` → `rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(v, server)` |
| `Commands.hasPermission(Commands.LEVEL_ADMINS)` (PermissionSet system) | `.requires(source -> source.hasPermission(3))` | 26.x PermissionSet/PermissionCheck does not exist earlier. LEVEL_ADMINS ≈ level 3, LEVEL_GAMEMASTERS ≈ 2 |
| `net.minecraft.world.level.dimension.end.EnderDragonFight` | `net.minecraft.world.level.dimension.end.EndDragonFight` | Class rename only; `setDragonKilled` + podium-egg placement structure is the same |
| `player.level().getServer()` returning non-null server directly | Same method exists; on 1.20.x it can be null-annotated — keep the code as-is, it already runs server-side only |
| Per-player sound: `ClientboundSoundPacket` via `player.connection.send(...)` | `player.playNotifySound(SoundEvent, SoundSource, float, float)` | Older versions still HAVE `playNotifySound` (it was removed in 26.x). Replace the whole `playSoundFor` packet helper in `DeathEventHandler` with one `playNotifySound` call — simpler than keeping the packet |
| `Level.playSound(Entity, BlockPos, SoundEvent, SoundSource, f, f)` | `Level.playSound(Player, BlockPos, SoundEvent, SoundSource, f, f)` | First param type changed in 26.x. Call sites pass `null`, so only the resolved overload changes — usually compiles unchanged |
| `tag.getString(key)` returning `Optional<String>` | returns `String` directly | `UniqueItemManager.getUniqueType`: drop the `.orElse("")` |
| `Inventory.getContainerSize()` = 36 main slots only (equipment separate) | = 41 (36 main + 4 armor: 36–39 + offhand: 40) | Scans over `getContainerSize()` on old versions ALREADY include armor+offhand. The explicit `EQUIPMENT_SLOTS` walk in `InventoryEnforcer`/`UniqueItemManager` becomes redundant for armor/offhand but is harmless — keep it (still needed after 26.x forward-ports, and `setItemSlot` re-sync still applies) |
| `ItemFrame.interact(Player, InteractionHand, Vec3)` | `ItemFrame.interact(Player, InteractionHand)` | Drop the `Vec3` param from `ItemFrameMixin` handler signature |
| `MinecartTNT.primeFuse(DamageSource)` | `MinecartTNT.primeFuse()` (no args) | `TNTCartDisableMixin`: drop the `DamageSource` param |
| `Blocks.CONCRETE.lime()` / `Blocks.STAINED_GLASS_PANE.gray()` | `Blocks.LIME_CONCRETE` / `Blocks.RED_CONCRETE` / `Blocks.GRAY_STAINED_GLASS_PANE` | 26.x grouped color-block accessors don't exist earlier (`ReviveItemFactory`) |
| `stack.getMaxStackSize()` | same | unchanged, listed for completeness |

## Feature availability matrix (verified against version jars)

If a feature's backing content doesn't exist in the target, DELETE the feature cleanly:
remove the mixin from `lifesteal.mixins.json`, delete the mixin class, remove the config
field, and remove any references. Do not leave dead config keys.

| Feature | Exists since | Action below that version |
|---|---|---|
| Crafter block (`CrafterBlockMixin`) | 1.20.3 | 1.20.1–1.20.2: delete mixin (no crafter to gate) |
| Mace (unique item) | 1.20.5 | below: remove `minecraft:mace` from default `uniqueItems`, delete `SlotMayPickupMixin`+`ResultSlotMixin` only if mace was their sole purpose — they are generic now, so instead just remove mace from the default config list |
| Wind Charge (`WindChargeMixin`, `disableWindCharges`) | 1.20.5 | below: delete mixin + config field |
| Item components (`DataComponents.*`) | 1.20.5 | below: rewrite all component usage as NBT (see 1.20.x port files) |
| `ArmorSlot` class (`ArmorSlotMixin`) | 1.21 | 1.20.x: armor slots are anonymous inner classes of `InventoryMenu`; see port files for the alternative elytra gate |
| Data-driven enchantments (`Holder<Enchantment>`) | 1.21 | 1.20.x: `Enchantment` is a registry object; see port files |
| `ServerExplosion` (`ExplosionDamageMixin`) | 1.21.2 | below: target `Explosion` instead; injection point differs (see port files) |
| `Equippable` component (`EquippableMixin`) | 1.21.2 | below: elytra equip gate moves to `ElytraItem.use` + armor-slot handling |
| `RecipeMap`/`RecipeManager.fromJson(ResourceKey,...)` (`RecipeManagerMixin`) | 1.21.2 | below: RecipeManager loads `Map<ResourceLocation, JsonElement>`; different injection (see port files) |
| `InteractionResult` unified (Item.use returns `InteractionResult`) | 1.21.2 | below: `Item.use` returns `InteractionResultHolder<ItemStack>`; block `useItemOn` returns `ItemInteractionResult` (1.20.5–1.21.1) |
| `Item.Properties().setId(ResourceKey)` required | 1.21.2 | below: remove `.setId(...)` from `Items.settings` — plain `new Item.Properties()` |
| `TooltipDisplay` component | 1.21.5 | 1.20.5–1.21.4: no "Dynamic" tooltip line exists to hide → just remove the `TOOLTIP_DISPLAY` set in `ReviveItemFactory.applySkin`. 1.20.1–1.20.4: N/A (NBT heads) |
| Shelf block (`ShelfMixin`), Copper chest | 1.21.9 (Copper Age) | below: delete `ShelfMixin`; copper chests need no code (they're just absent) |
| Decorated pot item storage (`DecoratedPotMixin`) | 1.20.3 | 1.20.1–1.20.2: delete mixin (pots can't store items) |
| `hurtServer(ServerLevel, DamageSource, float)` | 1.21.2 | below: `hurt(DamageSource, float)` — affects `TNTCartExplosionMixin`, `ExplosionEntityDamageMixin` |
| `RecipeHolder` | 1.20.2 | 1.20.1: recipes are `Recipe<?>` with `getId()` |
| `ResolvableProfile` | 1.20.5 | 26.x `createUnresolved(name)` is new; 1.20.5–1.21.11 use `new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap())`. 1.20.1–1.20.4: `SkullOwner` NBT string |

## Fabric API differences

| 26.2 code | 1.20.1–1.21.11 |
|---|---|
| `net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(tab)` | `net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(ITEM))` |
| `ServerLivingEntityEvents.AFTER_DEATH` / `ALLOW_DEATH` | same (exists throughout) |
| `ServerPlayerEvents.AFTER_RESPAWN` | same |
| `ServerPlayConnectionEvents.JOIN` / `DISCONNECT` | same |
| `ServerTickEvents.END_SERVER_TICK` | same |
| `ServerLifecycleEvents.SERVER_STARTED` | same |
| `CommandRegistrationCallback.EVENT` | same |

## Files that need NO changes in any port

`Constants.java`, `ServerConfig.java`*, `ClientSettingsManager.java`,
`CombatCooldownManager.java`, `CraftedHeartTracker.java`, `EliminatedPlayersTracker.java`,
`RevivedPlayersManager.java`†, `EGAEffectStripper.java`†, `PageManager.java`,
`ReviveSort.java`, `PlayerCollector.java`, `HeartFeedback` (n/a — never shipped),
`LifestealClient.java`, `CustomRecipeLoader.java`.

\* minus deleted config fields per the feature matrix.
† uses `Holder<MobEffect>` — on 1.20.x targets `MobEffects.REGENERATION` etc. are plain
`MobEffect`; the generic types change mechanically (see 1.20.x port files).

## Verification checklist for every port

1. `gradlew clean build` passes.
2. `gradlew runServer` reaches `Done` with zero mixin errors (require=1 crashes if not).
3. `[Enchants] Limits active for N enchantment(s)` logs with the right N for the version.
4. `[Recipes] Loaded 3 custom recipe(s) from config` logs.
5. In-game smoke test: heart use, kill reward, /withdraw (crafted-first), /revive menu
   (heads + sort + confirmation), /deathban, banned-item deletion, enchant clamp on an
   over-limit item, storage restriction on a Heart into a chest, and (if enabled)
   totem/pearl/EGA/TNT-cart toggles.

## Known bugs (carry these into every port)

These are open bugs in the 26.2 source. They will be copied verbatim into every
backport unless fixed — fix them in the port, or at minimum do not "clean them up"
into a different shape that hides them.

### Beacon of Life is consumed on drop but the revive still goes through

**Repro:** open the `/revive` menu with a Beacon of Life in hand, then drop the beacon
item (Q / inventory drop) while the menu is still open, then confirm the revive.

**Actual:** the revive completes. The player is brought back AND the dropped beacon still
exists as an item entity on the ground — the beacon is effectively duplicated, and a
player with one beacon can revive repeatedly.

**Cause:** the menu captures the beacon reference (or just a "player had a beacon" flag)
when the GUI is opened, and the confirmation handler never re-validates that the stack is
still in the player's inventory before applying the revive. The consume path and the
validate path are separated by an arbitrary amount of wall-clock time.

**Fix direction:** re-check the held/inventory stack at confirmation time, shrink it in the
same tick as the revive, and close the menu if the beacon is no longer present. Do not
rely on the reference captured at open time.

**Port note:** the inventory/slot APIs involved differ across targets (`ItemStack.shrink`
is stable, but the screen-handler and `Player#getItemInHand` plumbing shifts on 1.20.5+
with the component rework). Whatever the target, the validation must happen at confirm,
not at open.
