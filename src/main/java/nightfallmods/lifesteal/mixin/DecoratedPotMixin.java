package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlock.class)
public class DecoratedPotMixin {

    // useItemOn returns ItemInteractionResult on 1.20.5–1.21.1; it merges back into
    // InteractionResult in 1.21.2.
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockPotStash(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit,
                                         CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!StorageRestrictionHandler.isStorageRestricted(stack)) return;

        if (player instanceof ServerPlayer sp) StorageRestrictionHandler.notifyRestricted(sp, stack);
        cir.setReturnValue(ItemInteractionResult.FAIL);
    }
}

