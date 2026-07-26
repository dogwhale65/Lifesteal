package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
public class EquippableMixin {

    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockElytraEquip(ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ServerConfig.getInstance().disableElytras) return;
        if (!stack.is(Items.ELYTRA)) return;

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Elytras are disabled.")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
            sp.containerMenu.sendAllDataToRemote();
        }
        cir.setReturnValue(InteractionResult.FAIL);
    }
}

