package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShelfBlock.class)
public class ShelfMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockShelfStash(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (!StorageRestrictionHandler.isStorageRestricted(stack)) return;

        if (player instanceof ServerPlayer sp) StorageRestrictionHandler.notifyRestricted(sp, stack);
        cir.setReturnValue(InteractionResult.FAIL);
    }
}

