package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Equippable component (1.21.2+) and the ArmorSlot class (1.21+) both post-date this version,
 * so the right-click equip path is gated on ElytraItem#use instead. Dragging an elytra into the
 * chest slot by hand or having a dispenser equip one is still caught by the InventoryEnforcer scan.
 */
@Mixin(ElytraItem.class)
public class ElytraEquipMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockEquip(Level level, Player user, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!ServerConfig.getInstance().disableElytras) return;
        if (level.isClientSide()) return;

        if (user instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Elytras are disabled.").withStyle(ChatFormatting.RED), true);
            sp.containerMenu.sendAllDataToRemote();
        }
        cir.setReturnValue(InteractionResultHolder.fail(user.getItemInHand(hand)));
    }
}
