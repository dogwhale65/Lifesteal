package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.ExplosionRerouteHandler;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ServerExplosion does not exist before 1.21.2 — entity damage happens inside
 * {@link Explosion#explode()} here, so the suppression window wraps that method rather than a
 * separate hurtEntities pass. The source and damage source are read through the public getters
 * instead of shadowing private final fields.
 */
@Mixin(Explosion.class)
public class ExplosionDamageMixin {

    @Inject(method = "explode", at = @At("HEAD"))
    private void lifesteal$setExplosionSuppression(CallbackInfo ci) {
        Explosion self = (Explosion) (Object) this;
        if (ExplosionRerouteHandler.shouldReroute(self.getDirectSourceEntity(), self.getDamageSource()))
            ExplosionRerouteHandler.setSuppressing(true);
    }

    @Inject(method = "explode", at = @At("RETURN"))
    private void lifesteal$clearExplosionSuppression(CallbackInfo ci) {
        ExplosionRerouteHandler.setSuppressing(false);
    }
}
