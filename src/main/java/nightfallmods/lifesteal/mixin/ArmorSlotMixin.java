package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ArmorSlot is package-private on 26.1 (it only became public in 26.2), so it cannot be
// referenced as a class literal from this package. Target it by name instead — mixin
// resolves the string at transform time and does not need compile-time access.
@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public class ArmorSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockElytra(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ServerConfig.getInstance().disableElytras && stack.is(Items.ELYTRA)) {
            cir.setReturnValue(false);
        }
    }
}

