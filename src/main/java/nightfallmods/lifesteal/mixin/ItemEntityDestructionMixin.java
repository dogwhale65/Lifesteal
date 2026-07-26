package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class ItemEntityDestructionMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void lifesteal$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity item) {
            ItemStack stack = item.getItem();
            if (!UniqueItemManager.isTrackedUnique(stack)) return;

            boolean destroyed = reason == Entity.RemovalReason.KILLED
                    || (reason == Entity.RemovalReason.DISCARDED && !stack.isEmpty());
            if (destroyed) UniqueItemManager.onUniqueDestroyed(stack);
            return;
        }

        if ((Object) this instanceof FallingBlockEntity falling
                && falling.getBlockState().is(Blocks.DRAGON_EGG)) {
            boolean destroyed = reason == Entity.RemovalReason.KILLED
                    || (reason == Entity.RemovalReason.DISCARDED
                            && falling.getY() < falling.level().getMinBuildHeight());
            if (destroyed) UniqueItemManager.onUniqueDestroyed(new ItemStack(Items.DRAGON_EGG));
        }
    }
}

