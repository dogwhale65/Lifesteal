package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotMayPlaceMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void lifesteal$restrictStorage(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (StorageRestrictionHandler.isStorageRestricted(stack)
                && StorageRestrictionHandler.isBlockedContainer(self.container)) {
            cir.setReturnValue(false);
        }
    }
}

