package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Protection granted after respawning: while a player's grace is running, damage dealt by another
 * player cannot reach them. Mobs, fall damage and everything else still hurt, so grace is a shield
 * against being farmed on the way back — not invincibility.
 *
 * It is deliberately cheap to lose. Putting on armour or throwing the first punch ends it, which
 * stops it from being carried into a fight the player picked themselves.
 *
 * Expiries are wall-clock and held in memory only: they keep counting down while a player is
 * offline, and are dropped entirely if the server restarts.
 */
public class GracePeriodManager {

    private static final Map<UUID, Long> expiries = new HashMap<>();

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::allowDamage);
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::tick);
        Lifesteal.LOGGER.info("[Grace] Manager registered.");
    }

    /** Starts a fresh grace period. Does nothing while the feature is switched off. */
    public static void grant(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        player.sendSystemMessage(Component.literal(
                        "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                                + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
        Lifesteal.LOGGER.info("[Grace] {} granted {}s of grace.",
                player.getName().getString(), cfg.gracePeriodSeconds);
    }

    public static boolean isActive(UUID playerId) {
        Long expiry = expiries.get(playerId);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;
        if (!(entity instanceof ServerPlayer victim) || victim == attacker) return true;

        // Swinging at someone forfeits your own grace whether or not the blow actually lands.
        end(attacker, "you attacked " + victim.getName().getString() + ".");

        if (!isActive(victim.getUUID())) return true;

        attacker.sendSystemMessage(Component.literal(
                        victim.getName().getString() + " is still in their grace period.")
                .withStyle(ChatFormatting.YELLOW), true);
        return false;
    }

    private static void tick(MinecraftServer server) {
        if (expiries.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> entries = expiries.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Long> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (entry.getValue() <= now) {
                entries.remove();
                if (player != null) notifyEnded(player, "it ran out.");
            } else if (player != null && isWearingArmour(player)) {
                entries.remove();
                notifyEnded(player, "you equipped armour.");
            }
        }
    }

    /** Ends grace early. Silent for players who had none, so callers need not check first. */
    private static void end(ServerPlayer player, String reason) {
        if (!isActive(player.getUUID())) {
            expiries.remove(player.getUUID());
            return;
        }
        expiries.remove(player.getUUID());
        notifyEnded(player, reason);
    }

    private static void notifyEnded(ServerPlayer player, String reason) {
        player.sendSystemMessage(Component.literal("Your grace period ended.")
                .withStyle(ChatFormatting.RED));
        Lifesteal.LOGGER.info("[Grace] {} lost grace: {}", player.getName().getString(), reason);
    }

    private static boolean isWearingArmour(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private static String describe(int seconds) {
        if (seconds >= 60 && seconds % 60 == 0) {
            int minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return seconds + (seconds == 1 ? " second" : " seconds");
    }
}
