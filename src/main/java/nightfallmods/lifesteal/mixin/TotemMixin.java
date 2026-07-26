package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class TotemMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void lifesteal$disableTotems(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!ServerConfig.getInstance().disableTotems) return;
        if ((Object) this instanceof ServerPlayer) {
            cir.setReturnValue(false);
        }
    }
}

