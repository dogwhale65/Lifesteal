package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartTNT.class)
public class TNTCartDisableMixin {

    @Inject(method = "primeFuse", at = @At("HEAD"), cancellable = true)
    private void lifesteal$disableTNTCarts(DamageSource source, CallbackInfo ci) {
        if (ServerConfig.getInstance().disableTNTCarts) {
            ci.cancel();
        }
    }
}

