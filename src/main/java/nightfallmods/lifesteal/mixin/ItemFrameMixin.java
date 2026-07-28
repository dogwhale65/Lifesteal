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
                StorageRestrictionHandler.refuseFraming(sp, player.getItemInHand(hand));
            }
            // CONSUME, not FAIL: a result that doesn't consume the action makes the client fall
            // through from the entity interaction to using the item, so a Heart or Crafted Heart
            // would be eaten right after this and answer with its own cap message. Vanilla clients
            // don't run this mixin at all, which is what refuseFraming covers on the server side.
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}

