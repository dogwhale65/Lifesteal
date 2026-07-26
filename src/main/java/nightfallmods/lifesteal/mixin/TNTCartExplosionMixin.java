package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.TNTCartDamageHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class TNTCartExplosionMixin {

    private static final ThreadLocal<Boolean> LIFESTEAL_TNT_CAPPING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void lifesteal$capTNTCartDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (LIFESTEAL_TNT_CAPPING.get()) return;

        if (!(((LivingEntity)(Object) this) instanceof ServerPlayer target)) return;

        Entity directSource = source.getDirectEntity();
        Entity indirectSource = source.getEntity();

        if (!(directSource instanceof MinecartTNT)) return;
        if (!(indirectSource instanceof ServerPlayer trigger)) return;

        float capped = TNTCartDamageHandler.applyCapIfNeeded(amount, trigger, target);
        if (capped < amount) {
            boolean hurt;
            LIFESTEAL_TNT_CAPPING.set(true);
            try {
                hurt = target.hurtServer(level, source, capped);
            } finally {
                LIFESTEAL_TNT_CAPPING.set(false);
            }
            cir.setReturnValue(hurt);
        }
    }
}
