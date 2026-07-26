package nightfallmods.lifesteal.manager;

import net.minecraft.server.players.NameAndId;
import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.item.Items;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
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
            if (source.getEntity() instanceof ServerPlayer killer && killer != player) {
                handleKillReward(killer);
            }
            handleDeath(player);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damage) -> {
            if (entity instanceof ServerPlayer player && isAtFinalHeart(player)) {
                beginDeathMessageSuppression(player);
            }
            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID id = newPlayer.getUUID();
            if (pendingHealthReduction.remove(id)) applyHeartLoss(newPlayer);
            if (pendingSpectator.remove(id))       transitionToSpectator(newPlayer);
        });

        Lifesteal.LOGGER.info("[Death] Event handlers registered.");
    }

    private static void handleDeath(ServerPlayer player) {
        if (isAtFinalHeart(player)) {
            handleFinalDeath(player);
        } else {
            pendingHealthReduction.add(player.getUUID());
        }
    }

    private static void handleFinalDeath(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        endDeathMessageSuppression(player);
        broadcastElimination(player);
        broadcastSound(player, cfg.finalDeathSound);
        if (cfg.isDeathBanTypeSpectator()) {
            EliminatedPlayersTracker.markEliminated(player.getUUID(), player.getGameProfile().name(), false);
            pendingSpectator.add(player.getUUID());
        } else {
            banPlayer(player, cfg);
        }
    }

    private static void applyHeartLoss(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        double reduced = attr.getBaseValue() - Constants.HEART_VALUE;
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
                new NameAndId(player.getUUID(), player.getGameProfile().name()), new Date(), "Lifesteal", null, "You have run out of hearts."
        );
        player.level().getServer().getPlayerList().getBans().add(entry);

        EliminatedPlayersTracker.markEliminated(player.getUUID(), player.getGameProfile().name(), true);

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(cfg.getStartingHealth());
        CraftedHeartTracker.reset(player.getUUID());

        player.connection.disconnect(
                Component.literal("You have been banned.").withStyle(ChatFormatting.RED)
        );
        Lifesteal.LOGGER.info("[Death] {} eliminated.", player.getName().getString());
    }

    public static void deathban(MinecraftServer server, NameAndId profile) {
        UserBanList bans = server.getPlayerList().getBans();
        if (!bans.isBanned(profile)) {
            bans.add(new UserBanListEntry(
                    profile, new Date(), "Lifesteal", null, "You have run out of hearts."
            ));
        }

        if (profile.id() != null) {
            EliminatedPlayersTracker.markEliminated(profile.id(), profile.name(), true);
        }

        ServerPlayer online = profile.id() != null
                ? server.getPlayerList().getPlayer(profile.id())
                : null;
        if (online != null) {
            AttributeInstance attr = online.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(ServerConfig.getInstance().getStartingHealth());
            online.connection.disconnect(
                    Component.literal("You have been banned.").withStyle(ChatFormatting.RED)
            );
        }
        Lifesteal.LOGGER.info("[Death] {} deathbanned via command.", profile.name());
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
                    SoundEvent.createVariableRangeEvent(Identifier.parse(cfg.heartEquipSound)),
                    SoundSource.PLAYERS, 1.0f, 1.0f
            );
        }
    }

    private static boolean isAtFinalHeart(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        return attr == null || attr.getBaseValue() <= Constants.HEART_VALUE;
    }

    private static void beginDeathMessageSuppression(ServerPlayer player) {
        if (savedShowDeathMessages != null) return;
        var rules = player.level().getGameRules();
        savedShowDeathMessages = rules.get(GameRules.SHOW_DEATH_MESSAGES);
        rules.set(GameRules.SHOW_DEATH_MESSAGES, false, player.level().getServer());
    }

    private static void endDeathMessageSuppression(ServerPlayer player) {
        if (savedShowDeathMessages == null) return;
        player.level().getGameRules().set(GameRules.SHOW_DEATH_MESSAGES, savedShowDeathMessages, player.level().getServer());
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
        SoundEvent sound = SoundEvent.createVariableRangeEvent(Identifier.parse(soundId));
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound), SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0f, 1.0f, player.getRandom().nextLong()
        ));
    }
}

