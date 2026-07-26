package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class DragonEggExplosionMixin {

    @Inject(method = "wasExploded", at = @At("HEAD"))
    private void lifesteal$onEggExploded(Level level, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        if ((Object) this == Blocks.DRAGON_EGG) {
            UniqueItemManager.onUniqueDestroyed(new ItemStack(Items.DRAGON_EGG));
        }
    }
}

