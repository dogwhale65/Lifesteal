package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.ExplosionRerouteHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class ExplosionEntityDamageMixin {

    // hurtServer arrives in 1.21.2; hurt runs on both sides here, so the client is filtered out.
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void lifesteal$suppressExplosionDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity) (Object) this).level().isClientSide()) return;

        if (ExplosionRerouteHandler.isSuppressing()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
