package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Post-death protection from other players. A player who respawns is immune to anything another
 * player initiates until the timer runs out, they put armour on, or they swing at someone.
 */
public class GracePeriodManager {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final EquipmentSlot[] ARMOUR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Player UUID -> epoch millis at which their protection lapses. */
    private static final Map<UUID, Long> expiries = new HashMap<>();

    private static int tickCounter = 0;

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) begin(newPlayer);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // getEntity() resolves to the owner for arrows, potions and TNT, so this covers indirect
            // player damage as well as melee.
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;
            if (entity == attacker) return true;

            if (entity instanceof ServerPlayer) end(attacker);

            if (!(entity instanceof ServerPlayer victim) || !isProtected(victim)) return true;

            attacker.sendSystemMessage(
                    Component.literal(victim.getName().getString() + " is still in their grace period.")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return false;
        });

        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);

        Lifesteal.LOGGER.info("[Grace] Grace period manager registered.");
    }

    /** Starts protection for a freshly respawned player, unless they are eliminated or it is switched off. */
    public static void begin(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;
        if (EliminatedPlayersTracker.isEliminated(player.getUUID())) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        player.sendSystemMessage(Component.literal(
                        "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                                + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    public static boolean isProtected(Player player) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return false;
        Long expiry = expiries.get(player.getUUID());
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /** Ends protection and tells the player. A no-op if they had none. */
    public static void end(ServerPlayer player) {
        if (expiries.remove(player.getUUID()) == null) return;
        announceEnd(player);
    }

    private static void onTick(MinecraftServer server) {
        if (expiries.isEmpty()) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        if (!ServerConfig.getInstance().gracePeriodEnabled) {
            expiries.clear();
            return;
        }

        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Long>> it = expiries.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null) {
                if (now >= entry.getValue()) it.remove();
                continue;
            }
            if (now >= entry.getValue() || wearingArmour(player)) {
                it.remove();
                announceEnd(player);
            }
        }
    }

    private static void announceEnd(ServerPlayer player) {
        player.sendSystemMessage(
                Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    private static boolean wearingArmour(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /** Renders a duration the way a player would say it: "30 minutes", "1 hour 30 minutes", "45 seconds". */
    public static String describe(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int hours     = seconds / 3600;
        int minutes   = (seconds % 3600) / 60;
        int remainder = seconds % 60;

        StringBuilder text = new StringBuilder();
        if (hours > 0)   appendUnit(text, hours, "hour");
        if (minutes > 0) appendUnit(text, minutes, "minute");
        if (remainder > 0 || text.isEmpty()) appendUnit(text, remainder, "second");

        return text.toString();
    }

    private static void appendUnit(StringBuilder text, int value, String unit) {
        if (!text.isEmpty()) text.append(' ');
        text.append(value).append(' ').append(unit);
        if (value != 1) text.append('s');
    }
}
