package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.item.Items;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathEventHandler {

    private static final Set<UUID> pendingHealthReduction = new HashSet<>();
    private static final Set<UUID> pendingSpectator       = new HashSet<>();

    private static Boolean savedShowDeathMessages = null;

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer player)) return;

            ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp && sp != player ? sp : null;
            boolean heartLost = handleDeath(player);

            if (killer == null) return;
            if (heartLost) handleKillReward(killer);
            else           announceNothingStolen(player, killer);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damage) -> {
            if (entity instanceof ServerPlayer player && isAtFinalHeart(player) && !isAtHeartFloor(player)) {
                beginDeathMessageSuppression(player);
            }
            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID id = newPlayer.getUUID();
            if (pendingHealthReduction.remove(id)) applyHeartLoss(newPlayer);
            if (pendingSpectator.remove(id)) {
                transitionToSpectator(newPlayer);
                return;
            }
            if (!alive) GracePeriodManager.start(newPlayer);
        });

        Lifesteal.LOGGER.info("[Death] Event handlers registered.");
    }

    /** Queues the consequences of a death; returns false when the heart floor absorbed it. */
    private static boolean handleDeath(ServerPlayer player) {
        if (isAtHeartFloor(player)) return false;

        if (isAtFinalHeart(player)) {
            handleFinalDeath(player);
        } else {
            pendingHealthReduction.add(player.getUUID());
        }
        return true;
    }

    private static void announceNothingStolen(ServerPlayer victim, ServerPlayer attacker) {
        victim.sendSystemMessage(Component.literal(
                "You did not lose any hearts to " + attacker.getName().getString())
                .withStyle(ChatFormatting.YELLOW));

        attacker.sendSystemMessage(Component.literal(
                victim.getName().getString() + " had no hearts to steal.")
                .withStyle(ChatFormatting.RED));
    }

    private static void handleFinalDeath(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        endDeathMessageSuppression(player);
        broadcastElimination(player);
        broadcastSound(player, cfg.finalDeathSound);
        if (cfg.isDeathBanTypeSpectator()) {
            EliminatedPlayersTracker.markEliminated(player.getUUID(), player.getGameProfile().getName(), false);
            pendingSpectator.add(player.getUUID());
        } else {
            banPlayer(player, cfg);
        }
    }

    private static void applyHeartLoss(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        double reduced = Math.max(attr.getBaseValue() - Constants.HEART_VALUE,
                                  ServerConfig.getInstance().getMinimumHealth());
        attr.setBaseValue(reduced);
        player.setHealth((float) reduced);
        CraftedHeartTracker.clampTo(player.getUUID(), (int) (reduced / Constants.HEART_VALUE));

        playSoundFor(player, ServerConfig.getInstance().heartDeathSound);
    }

    private static void transitionToSpectator(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        player.setGameMode(GameType.SPECTATOR);
        CraftedHeartTracker.reset(player.getUUID());

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double health = cfg.getStartingHealth();
            attr.setBaseValue(health);
            player.setHealth((float) health);
        }

        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(player.getName().getString())
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(" has run out of hearts.").withStyle(ChatFormatting.GRAY)),
                false
        );
    }

    private static void banPlayer(ServerPlayer player, ServerConfig cfg) {
        UserBanListEntry entry = new UserBanListEntry(
                player.getGameProfile(), new Date(), "Lifesteal", null, "You have run out of hearts."
        );
        player.level().getServer().getPlayerList().getBans().add(entry);

        EliminatedPlayersTracker.markEliminated(player.getUUID(), player.getGameProfile().getName(), true);

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(cfg.getStartingHealth());
        CraftedHeartTracker.reset(player.getUUID());

        player.connection.disconnect(
                Component.literal("You have been banned.").withStyle(ChatFormatting.RED)
        );
        Lifesteal.LOGGER.info("[Death] {} eliminated.", player.getName().getString());
    }

    public static void deathban(MinecraftServer server, GameProfile profile) {
        UserBanList bans = server.getPlayerList().getBans();
        if (!bans.isBanned(profile)) {
            bans.add(new UserBanListEntry(
                    profile, new Date(), "Lifesteal", null, "You have run out of hearts."
            ));
        }

        if (profile.getId() != null) {
            EliminatedPlayersTracker.markEliminated(profile.getId(), profile.getName(), true);
        }

        ServerPlayer online = profile.getId() != null
                ? server.getPlayerList().getPlayer(profile.getId())
                : null;
        if (online != null) {
            AttributeInstance attr = online.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(ServerConfig.getInstance().getStartingHealth());
            online.connection.disconnect(
                    Component.literal("You have been banned.").withStyle(ChatFormatting.RED)
            );
        }
        Lifesteal.LOGGER.info("[Death] {} deathbanned via command.", profile.getName());
    }

    public static void handleKillReward(ServerPlayer attacker) {
        AttributeInstance attr = attacker.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        ServerConfig cfg = ServerConfig.getInstance();
        if (attr.getBaseValue() >= cfg.getMaxHealth()) {
            ItemStack heart = new ItemStack(Items.HEART, 1);
            if (!attacker.addItem(heart)) attacker.drop(heart, false);
            Lifesteal.LOGGER.info("[Death] {} at max hearts — dropped Heart item.", attacker.getName().getString());
        } else {
            attr.setBaseValue(attr.getBaseValue() + Constants.HEART_VALUE);
            if (cfg.fullHeartOnGain)
                attacker.setHealth(attacker.getHealth() + (float) Constants.HEART_VALUE);

            if (attr.getBaseValue() / Constants.HEART_VALUE > cfg.egaHeartThreshold)
                EGAEffectStripper.stripIfSnapshotExists(attacker);

            attacker.level().playSound(
                    null, attacker.blockPosition(),
                    SoundEvent.createVariableRangeEvent(ResourceLocation.parse(cfg.heartEquipSound)),
                    SoundSource.PLAYERS, 1.0f, 1.0f
            );
        }
    }

    private static boolean isAtFinalHeart(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        return attr == null || attr.getBaseValue() <= Constants.HEART_VALUE;
    }

    /** True when losing a heart would drop the player under the configured floor. */
    private static boolean isAtHeartFloor(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.minimumHeartsEnabled) return false;

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return true;
        return attr.getBaseValue() - Constants.HEART_VALUE < cfg.getMinimumHealth();
    }

    // Game rules live on the server here, not on the level: Level#getGameRules() does not exist
    // on this version, so both the read and the write go through MinecraftServer#getGameRules().
    private static void beginDeathMessageSuppression(ServerPlayer player) {
        if (savedShowDeathMessages != null) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        GameRules rules = server.getGameRules();
        savedShowDeathMessages = rules.getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);
    }

    private static void endDeathMessageSuppression(ServerPlayer player) {
        if (savedShowDeathMessages == null) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        server.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES)
                .set(savedShowDeathMessages, server);
        savedShowDeathMessages = null;
    }

    private static void broadcastElimination(ServerPlayer player) {
        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(player.getName().getString())
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(" has been eliminated.").withStyle(ChatFormatting.GRAY)),
                false
        );
    }

    private static void broadcastSound(ServerPlayer source, String soundId) {
        for (ServerPlayer p : source.level().getServer().getPlayerList().getPlayers()) {
            playSoundFor(p, soundId);
        }
    }

    private static void playSoundFor(ServerPlayer player, String soundId) {
        player.playNotifySound(
                SoundEvent.createVariableRangeEvent(ResourceLocation.parse(soundId)),
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}

