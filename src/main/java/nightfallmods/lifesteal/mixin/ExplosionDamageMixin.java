package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.ExplosionRerouteHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerExplosion.class)
public class ExplosionDamageMixin {

    @Shadow @Final private Entity source;
    @Shadow @Final private DamageSource damageSource;

    @Inject(method = "hurtEntities", at = @At("HEAD"))
    private void lifesteal$setExplosionSuppression(CallbackInfo ci) {
        if (ExplosionRerouteHandler.shouldReroute(this.source, this.damageSource))
            ExplosionRerouteHandler.setSuppressing(true);
    }

    @Inject(method = "hurtEntities", at = @At("RETURN"))
    private void lifesteal$clearExplosionSuppression(CallbackInfo ci) {
        ExplosionRerouteHandler.setSuppressing(false);
    }
}
