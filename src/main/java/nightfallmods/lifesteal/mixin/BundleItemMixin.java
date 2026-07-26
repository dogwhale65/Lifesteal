package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
public class BundleItemMixin {

    @Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockBundleInsert(ItemStack bundle, Slot slot, ClickAction action, Player player,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (StorageRestrictionHandler.isStorageRestricted(slot.getItem())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockBundleReceive(ItemStack bundle, ItemStack other, Slot slot, ClickAction action,
                                              Player player, SlotAccess access, CallbackInfoReturnable<Boolean> cir) {
        if (StorageRestrictionHandler.isStorageRestricted(other)) {
            cir.setReturnValue(true);
        }
    }
}

