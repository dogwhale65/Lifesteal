package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import nightfallmods.lifesteal.mixin.accessor.ItemCombinerMenuAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemCombinerMenu#mayPickup is abstract on 1.20.2 (it only gains a body in later versions), so the
 * unique-item gate attaches to the SmithingMenu implementation directly rather than the base class.
 */
@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lifesteal$gateUniqueSmithing(Player player, boolean canTake, CallbackInfoReturnable<Boolean> cir) {
        ItemCombinerMenuAccessor self = (ItemCombinerMenuAccessor) this;
        if (UniqueItemManager.blocksCrafting(self.getResultSlots().getItem(0))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void lifesteal$tagUnique(Player player, ItemStack stack, CallbackInfo ci) {
        UniqueItemManager.tagCrafted(stack);
    }
}
