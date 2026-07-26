package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.manager.EliminatedPlayersTracker;
import nightfallmods.lifesteal.manager.RevivedPlayersManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.server.players.UserNameToIdResolver;

import java.util.Optional;
import java.util.UUID;

public class ReviveLogic {

    private final MinecraftServer server;
    private final Player reviver;

    public ReviveLogic(MinecraftServer server, Player reviver) {
        this.server  = server;
        this.reviver = reviver;
    }

    public boolean revivePlayer(String playerName) {

        UserNameToIdResolver cache = server.services().nameToIdCache();
        if (cache == null) {
            sendError("Profile cache unavailable.");
            return false;
        }

        Optional<NameAndId> profileOpt = cache.get(playerName);
        if (profileOpt.isEmpty()) {
            sendError("Player '" + playerName + "' not found in server cache.");
            return false;
        }

        return execute(profileOpt.get());
    }

    private boolean execute(NameAndId profile) {
        UUID id = profile.id();
        ServerPlayer online = id != null
                ? server.getPlayerList().getPlayer(id)
                : null;

        if (online != null && online.isSpectator() && EliminatedPlayersTracker.isEliminated(online.getUUID())) {
            return reviveSpectator(online);
        }
        return reviveOffline(profile);
    }

    private boolean reviveSpectator(ServerPlayer player) {
        applyReviveHearts(player);
        player.setGameMode(GameType.SURVIVAL);
        EliminatedPlayersTracker.clearEliminated(player.getUUID());
        Lifesteal.LOGGER.info("[Revival] {} revived from spectator.", player.getName().getString());
        return true;
    }

    private boolean reviveOffline(NameAndId profile) {
        UUID id = profile.id();

        if (id == null || !EliminatedPlayersTracker.isEliminated(id)) {
            sendError(profile.name() + " is not eliminated.");
            return false;
        }

        UserBanList banList = server.getPlayerList().getBans();
        if (banList.isBanned(profile)) banList.remove(profile);
        EliminatedPlayersTracker.clearEliminated(id);

        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            applyReviveHearts(online);
        } else {
            RevivedPlayersManager.markRevived(id);
        }

        Lifesteal.LOGGER.info("[Revival] {} revived; hearts applied or queued for next join.", profile.name());
        return true;
    }

    private void applyReviveHearts(ServerPlayer player) {
        double health = ServerConfig.getInstance().getReviveHealth();
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.setBaseValue(health);
        player.setHealth((float) health);
        nightfallmods.lifesteal.manager.CraftedHeartTracker.reset(player.getUUID());
    }

    private void sendError(String message) {
        Lifesteal.LOGGER.warn("[Revival] {}", message);
        if (reviver != null) {
            reviver.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        }
    }
}

