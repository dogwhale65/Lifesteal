package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartTNT.class)
public class TNTCartDisableMixin {

    // primeFuse takes no DamageSource before 26.x.
    @Inject(method = "primeFuse", at = @At("HEAD"), cancellable = true)
    private void lifesteal$disableTNTCarts(CallbackInfo ci) {
        if (ServerConfig.getInstance().disableTNTCarts) {
            ci.cancel();
        }
    }
}
