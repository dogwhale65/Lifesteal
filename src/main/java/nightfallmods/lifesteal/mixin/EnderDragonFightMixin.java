package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public class EnderDragonFightMixin {

    @Redirect(method = "setDragonKilled",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lifesteal$suppressPodiumEgg(ServerLevel level, BlockPos pos, BlockState state) {
        return true;
    }

    @Inject(method = "setDragonKilled",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;spawnNewGateway()V",
                    shift = At.Shift.AFTER))
    private void lifesteal$dropUniqueEgg(EnderDragon dragon, CallbackInfo ci) {
        UniqueItemManager.onDragonDefeated(dragon.level().getServer());
    }
}

