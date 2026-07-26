package nightfallmods.lifesteal.manager;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import nightfallmods.lifesteal.mixin.accessor.MobEffectInstanceAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EGAEffectStripper {

    // MobEffects entries are plain MobEffect objects until 1.20.5 wraps them in Holder.
    private static final Set<MobEffect> EGA_EFFECT_TYPES = Set.of(
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.ABSORPTION
    );

    private static final long SNAPSHOT_MAX_AGE_TICKS = 6000;

    private record Snapshot(Map<MobEffect, MobEffectInstance> effects, long gameTime) {}

    private static final Map<UUID, Snapshot> preEgaSnapshots = new HashMap<>();

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                preEgaSnapshots.remove(handler.getPlayer().getUUID())
        );
    }

    public static void snapshotBeforeEGA(ServerPlayer player) {
        Map<MobEffect, MobEffectInstance> snap = new HashMap<>();
        for (MobEffect effect : EGA_EFFECT_TYPES) {
            MobEffectInstance active = player.getEffect(effect);
            if (active != null) snap.put(effect, new MobEffectInstance(active));
        }
        preEgaSnapshots.put(player.getUUID(), new Snapshot(snap, player.level().getGameTime()));
    }

    public static void stripIfSnapshotExists(ServerPlayer player) {
        Snapshot snapshot = preEgaSnapshots.remove(player.getUUID());
        if (snapshot == null) return;
        if (player.level().getGameTime() - snapshot.gameTime() > SNAPSHOT_MAX_AGE_TICKS) return;

        for (MobEffect effect : EGA_EFFECT_TYPES) {
            stripOne(player, effect, snapshot.effects().get(effect));
        }
    }

    private static void stripOne(ServerPlayer player, MobEffect effect, MobEffectInstance before) {
        MobEffectInstance active = player.getEffect(effect);
        if (active == null) return;

        if (before != null) {

            player.removeEffect(effect);
            player.addEffect(new MobEffectInstance(before));
        } else {

            MobEffectInstance hidden = ((MobEffectInstanceAccessor) active).getHiddenEffect();
            player.removeEffect(effect);
            if (hidden != null) player.addEffect(new MobEffectInstance(hidden));
        }
    }
}
