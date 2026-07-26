package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemCombinerMenu#mayPickup is abstract on 1.20.1, so the unique-craft gate that lived in
 * ItemCombinerMenuMixin moves onto the concrete SmithingMenu override — the same menu it
 * already filtered for. resultSlots belongs to the superclass and cannot be shadowed from
 * here, so the result stack is read through the public slot accessors.
 */
@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void lifesteal$tagUnique(Player player, ItemStack stack, CallbackInfo ci) {
        UniqueItemManager.tagCrafted(stack);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lifesteal$gateUniqueSmithing(Player player, boolean canTake, CallbackInfoReturnable<Boolean> cir) {
        ItemCombinerMenu self = (ItemCombinerMenu) (Object) this;
        if (UniqueItemManager.blocksCrafting(self.getSlot(self.getResultSlot()).getItem())) {
            cir.setReturnValue(false);
        }
    }
}
