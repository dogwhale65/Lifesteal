# Port: 26.2 → 1.21.9 / 1.21.10 / 1.21.11 ("Copper Age" line)

Read `README.md` first. This is the smallest port — every feature of the mod exists in
this target (including Shelf blocks and Copper Chests, added in 1.21.9). The work is
almost entirely the README's **Global renames** table plus the items below.

## Build
- `gradle.properties`: `minecraft_version=1.21.11` (or .9/.10), matching yarn-era loader
  + `fabric_api_version` from fabricmc.net/develop; keep loom current for this line.
- `build.gradle`: add `mappings loom.officialMojangMappings()`, `options.release = 21`.
- `lifesteal.mixins.json`: `"compatibilityLevel": "JAVA_21"`.
- `fabric.mod.json`: `"minecraft": "~1.21.9"` (or exact), `"java": ">=21"`.

## Apply all README global renames
Every entry in the README table applies. The most mechanical sweep:
`Identifier` → `ResourceLocation` (imports + `parse`/`fromNamespaceAndPath`/`tryParse`),
`NameAndId` → `GameProfile`, `ContainerInput` → `ClickType`, GameRules key system,
`.requires(s -> s.hasPermission(3))`, `EnderDragonFight` → `EndDragonFight`,
`playSoundFor` → `player.playNotifySound(...)`, `tag.getString(...)` loses `Optional`,
`ItemFrame.interact` two-arg, `primeFuse()` no-arg, concrete/glass block constants.

## File-specific notes

- **DeathEventHandler**: replace the `ClientboundSoundPacket` helper entirely:
  ```java
  private static void playSoundFor(ServerPlayer player, String soundId) {
      player.playNotifySound(
          SoundEvent.createVariableRangeEvent(ResourceLocation.parse(soundId)),
          SoundSource.PLAYERS, 1.0f, 1.0f);
  }
  ```
  `UserBanListEntry` takes `GameProfile`: `new UserBanListEntry(player.getGameProfile(), ...)`.
- **ReviveLogic / Deathban / ReviveScreenHandler**: profile cache is
  `server.getProfileCache()` (`GameProfileCache`), returning `Optional<GameProfile>`;
  op check `server.getPlayerList().isOp(sp.getGameProfile())`;
  `profile.getId()` / `profile.getName()`.
- **ReviveItemFactory**: `ResolvableProfile.createUnresolved(name)` — VERIFY this factory
  exists in the target (it may be 26.x-only). If absent, use the record constructor:
  `new ResolvableProfile(Optional.of(playerName), Optional.empty(), new PropertyMap())`
  (`com.mojang.authlib.properties.PropertyMap`). `TooltipDisplay` exists (1.21.5+) — keep
  the hidden-PROFILE line as-is.
- **RecipeManagerMixin**: `RecipeMap` and the `prepare(...)RecipeMap` shape exist in this
  line. VERIFY the exact `fromJson` signature with javap against the target jar — if it is
  `fromJson(ResourceKey<Recipe<?>>, JsonObject, HolderLookup.Provider)` the mixin ports
  unchanged (minus `Identifier`→`ResourceLocation`); if it still takes a
  `ResourceLocation` id, adjust the `@Shadow` and call accordingly.
- **Items**: keep `.setId(ResourceKey...)` in `settings(...)` (required since 1.21.2).
- **All `use`-based mixins** (EGA, pearl, wind charge) and `Heart`/`CraftedHeart`/
  `BeaconOfLife`: unchanged — `InteractionResult` unified since 1.21.2.
- **Explosion mixins**: `ServerExplosion` + `hurtServer` exist — unchanged.
- **EnderDragonFightMixin**: class renames to `EndDragonFight`; method `setDragonKilled`
  and the single `setBlockAndUpdate` call inside it are the same — update the mixin's
  target class and the `spawnNewGateway` owner in the injection target string.
- **ShelfMixin / DecoratedPotMixin / CrafterBlockMixin / EquippableMixin /
  ArmorSlotMixin**: all targets exist; only global renames apply. VERIFY
  `ShelfBlock.useItemOn`'s parameter list with javap (new block, signature may shift
  between 1.21.9 and 26.2).

## Verify (beyond README checklist)
- Shelf + copper chest + decorated pot all refuse a Heart.
- `/reload` picks up an edited recipe file.
