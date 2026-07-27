package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public class ItemFrameMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockFraming(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemFrame self = (ItemFrame) (Object) this;
        if (!self.getItem().isEmpty()) return;

        if (StorageRestrictionHandler.isStorageRestricted(player.getItemInHand(hand))) {
            if (player instanceof ServerPlayer sp) {
                StorageRestrictionHandler.notifyRestricted(sp, player.getItemInHand(hand));
            }
            // CONSUME, not FAIL: the client only stops at an entity interaction that consumed the
            // action. On FAIL it falls through to using the item, which eats the heart and prints
            // the heart-cap message on top of the action bar warning sent above.
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}

