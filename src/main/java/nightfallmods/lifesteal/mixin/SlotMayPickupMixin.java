package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotMayPickupMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lifesteal$gateUniqueCraft(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) return;

        if (UniqueItemManager.blocksCrafting(self.getItem())) {
            cir.setReturnValue(false);
        }
    }
}

