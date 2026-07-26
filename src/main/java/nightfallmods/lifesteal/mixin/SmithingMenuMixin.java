package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The pickup gate lived on ItemCombinerMenu in newer versions, but mayPickup is still abstract
 * there on 1.20.5 — there is no body to inject into. It is applied to SmithingMenu directly
 * instead, which is the only combiner the gate ever acted on. The result stack is read through
 * the public getResultSlot()/getSlot() pair rather than the inherited resultSlots field, because
 * Mixin does not resolve @Shadow against fields declared in a superclass of the target.
 */
@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lifesteal$gateUniqueSmithing(Player player, boolean canTake, CallbackInfoReturnable<Boolean> cir) {
        SmithingMenu self = (SmithingMenu) (Object) this;
        ItemStack result = self.getSlot(self.getResultSlot()).getItem();

        if (UniqueItemManager.blocksCrafting(result)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void lifesteal$tagUnique(Player player, ItemStack stack, CallbackInfo ci) {
        UniqueItemManager.tagCrafted(stack);
    }
}
