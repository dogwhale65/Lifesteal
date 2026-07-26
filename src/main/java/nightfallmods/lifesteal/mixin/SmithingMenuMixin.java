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
 * Both halves of the unique-item gate for smithing live here: on this version
 * {@code ItemCombinerMenu#mayPickup} is abstract, so the pickup gate has to be applied to the
 * concrete {@link SmithingMenu} override rather than to the shared superclass.
 */
@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lifesteal$gateUniqueSmithing(Player player, boolean canTake, CallbackInfoReturnable<Boolean> cir) {
        SmithingMenu self = (SmithingMenu) (Object) this;

        if (UniqueItemManager.blocksCrafting(self.getSlot(self.getResultSlot()).getItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void lifesteal$tagUnique(Player player, ItemStack stack, CallbackInfo ci) {
        UniqueItemManager.tagCrafted(stack);
    }
}
