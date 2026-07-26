package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.ExplosionRerouteHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On this version there is no separate {@code hurtEntities} pass — {@code Explosion#explode}
 * both collects blocks and damages entities, so suppression is scoped to that whole method.
 */
@Mixin(Explosion.class)
public class ExplosionDamageMixin {

    @Shadow @Final @Nullable private Entity source;
    @Shadow @Final private DamageSource damageSource;

    @Inject(method = "explode", at = @At("HEAD"))
    private void lifesteal$setExplosionSuppression(CallbackInfo ci) {
        if (ExplosionRerouteHandler.shouldReroute(this.source, this.damageSource))
            ExplosionRerouteHandler.setSuppressing(true);
    }

    @Inject(method = "explode", at = @At("RETURN"))
    private void lifesteal$clearExplosionSuppression(CallbackInfo ci) {
        ExplosionRerouteHandler.setSuppressing(false);
    }
}
