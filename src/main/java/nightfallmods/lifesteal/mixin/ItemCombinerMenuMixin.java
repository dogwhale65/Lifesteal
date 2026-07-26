package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCombinerMenu.class)
public class ItemCombinerMenuMixin {

    @Shadow @Final protected ResultContainer resultSlots;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lifesteal$gateUniqueSmithing(Player player, boolean canTake, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof SmithingMenu)) return;

        if (UniqueItemManager.blocksCrafting(resultSlots.getItem(0))) {
            cir.setReturnValue(false);
        }
    }
}

