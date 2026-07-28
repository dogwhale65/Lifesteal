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
import java.util.Map;
import java.util.UUID;

/**
 * Short window of immunity handed out after a death. While it lasts, nothing a player does can
 * hurt the protected player; mobs, fall damage and the rest still apply. It ends when the timer
 * runs out, when the player puts on armour, or when they swing at another player.
 *
 * <p>Grace is held in memory only, so it survives a relog but not a server restart. It is keyed by
 * wall clock, so logging out does not pause it.
 */
public final class GracePeriodManager {

    private static final int  CHECK_INTERVAL_TICKS       = 20;
    private static final long BLOCKED_NOTICE_INTERVAL_MS = 2_000L;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<UUID, Long> expiries          = new HashMap<>();
    private static final Map<UUID, Long> lastBlockedNotice = new HashMap<>();

    private static int tickCounter = 0;

    private GracePeriodManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::allowDamage);
        Lifesteal.LOGGER.info("[Grace] Grace period manager registered.");
    }

    /** Begins a grace period for a player who has just respawned, if the feature is on. */
    public static void start(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        // Armour is one of the two things that ends a grace period, so a player who respawns
        // already wearing some (keepInventory) has nothing to be granted — announcing protection
        // and revoking it a tick later would only be noise.
        if (isWearingArmor(player)) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        lastBlockedNotice.remove(player.getUUID());

        player.sendSystemMessage(Component.literal(
                        "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                                + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));

        Lifesteal.LOGGER.info("[Grace] {} protected for {}s.",
                player.getName().getString(), cfg.gracePeriodSeconds);
    }

    public static boolean isProtected(ServerPlayer player) {
        Long expiry = expiries.get(player.getUUID());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            end(player, true);
            return false;
        }
        return true;
    }

    public static void end(ServerPlayer player, boolean announce) {
        if (expiries.remove(player.getUUID()) == null) return;
        lastBlockedNotice.remove(player.getUUID());
        if (announce) {
            player.sendSystemMessage(
                    Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
        }
    }

    /** Renders a duration the way the start message reads it: "30 minutes", "45 seconds", "1 minute 30 seconds". */
    public static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60   && seconds % 60   == 0) return plural(seconds / 60,   "minute");
        if (seconds < 60)                           return plural(seconds,        "second");
        return plural(seconds / 60, "minute") + " " + plural(seconds % 60, "second");
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        // getEntity() is the player behind the blow, so an arrow or a thrown potion resolves to
        // whoever loosed it rather than to the projectile.
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;
        if (!(entity instanceof ServerPlayer victim) || victim == attacker) return true;

        // Swinging at another player is a choice to fight, so it costs the attacker their own
        // protection whether or not the blow actually lands.
        end(attacker, true);

        if (!isProtected(victim)) return true;
        notifyBlocked(attacker, victim);
        return false;
    }

    private static void onTick(MinecraftServer server) {
        if (expiries.isEmpty() && lastBlockedNotice.isEmpty()) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!expiries.containsKey(player.getUUID())) continue;
            if (now >= expiries.get(player.getUUID()) || isWearingArmor(player)) end(player, true);
        }

        // Offline players get no message; their entry just lapses.
        expiries.values().removeIf(expiry -> now >= expiry);
        lastBlockedNotice.values().removeIf(sent -> now - sent > BLOCKED_NOTICE_INTERVAL_MS);
    }

    /** Told once every couple of seconds, so holding down attack does not flood the attacker's chat. */
    private static void notifyBlocked(ServerPlayer attacker, ServerPlayer victim) {
        long now = System.currentTimeMillis();
        Long last = lastBlockedNotice.get(attacker.getUUID());
        if (last != null && now - last < BLOCKED_NOTICE_INTERVAL_MS) return;

        lastBlockedNotice.put(attacker.getUUID(), now);
        attacker.sendSystemMessage(
                Component.literal(victim.getName().getString() + " is still in their grace period.")
                        .withStyle(ChatFormatting.YELLOW));
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private static String plural(int amount, String unit) {
        return amount + " " + unit + (amount == 1 ? "" : "s");
    }
}
