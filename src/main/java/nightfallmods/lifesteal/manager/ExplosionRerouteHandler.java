package nightfallmods.lifesteal.manager;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public class ExplosionRerouteHandler {

    private static final ThreadLocal<Boolean> suppressing = ThreadLocal.withInitial(() -> false);

    public static void setSuppressing(boolean value) { suppressing.set(value); }
    public static boolean isSuppressing()            { return suppressing.get(); }

    public static boolean shouldReroute(Entity source, DamageSource damageSource) {
        if (source instanceof EndCrystal) return true;
        if (damageSource == null) return false;
        // Holder#getRegisteredName does not exist on 1.20.2; match the damage type key directly.
        return damageSource.is(DamageTypes.BAD_RESPAWN_POINT);
    }
}
