package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxSlot.class)
public class ShulkerBoxSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void lifesteal$restrictStorage(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (StorageRestrictionHandler.isStorageRestricted(stack)) {
            cir.setReturnValue(false);
        }
    }
}

