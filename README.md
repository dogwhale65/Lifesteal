# Lifesteal

A Fabric server mod inspired by the LifeSteal SMP gamemode, for Minecraft 1.21.4.

Kill players to steal their hearts. Run out of hearts and you're eliminated, either
banned or sent to spectator. Get revived by an admin, or a player holding a Beacon of
Life, to come back with a fresh start.

## Items

- **Heart** - right-click to gain a heart, raising your max health.
- **Crafted Heart** - same as a Heart, but capped at a set limit.
- **Heart Fragment** - crafting ingredient for a Crafted Heart.
- **Beacon of Life** - right-click to open the revival menu, a GUI listing every
  eliminated player.

## Commands

- `/withdraw <amount>` - converts hearts back into Heart/Crafted Heart items.
- `/revive` - opens the revival menu for admins.
- `/deathban <name|uuid>` - eliminates a player by name or UUID.

## Unique items

The Mace, Netherite Chestplate, and Dragon Egg are limited to one copy on the server at a
time. While one exists, crafting or smithing another is blocked, and destroying the
existing one reopens the recipe. The Dragon Egg has no recipe, it's earned by defeating
the Ender Dragon. None of these items, along with the Heart items and Beacon of Life, can
be stored in containers, so they only ever live on players or the ground.

## Configuration

Hearts, death handling, combat toggles, enchant limits, item bans, and recipes are all
adjustable through config files generated on first launch:

- `config/lifesteal.json` - core settings
- `config/lifesteal-enchantments.json` - per-enchantment level caps
- `config/lifesteal-recipes/` - crafting recipes as editable JSON

### Minimum hearts

`minHeartsEnabled` puts a floor of `minHearts` under every player. At the floor a death costs
nothing, the killer earns nothing, and `/withdraw` will not take you below it - which in practice
means nobody is ever eliminated. Off by default.

### Grace period

`gracePeriodEnabled` gives players `gracePeriodSeconds` (30 minutes by default) of immunity to
player-dealt damage after respawning. Wearing armour or attacking another player ends it early.
Off by default.


## License

MIT
