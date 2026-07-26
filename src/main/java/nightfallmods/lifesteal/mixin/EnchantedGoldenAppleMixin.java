package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.manager.EGAEffectStripper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.item.Item.class)
public class EnchantedGoldenAppleMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockEGAUse(Level level, Player user, InteractionHand hand,
                                       CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide()) return;
        if (!(user instanceof ServerPlayer player)) return;

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return;

        if (ServerConfig.getInstance().disableEGA) {
            player.sendSystemMessage(Component.literal(
                    "Enchanted Golden Apples are disabled.").withStyle(ChatFormatting.RED), true);
            cir.setReturnValue(InteractionResultHolder.fail(stack));
            return;
        }

        if (!ServerConfig.getInstance().egaHeartLimitEnabled) return;

        double hearts = player.getAttribute(Attributes.MAX_HEALTH).getBaseValue() / Constants.HEART_VALUE;

        if (hearts > ServerConfig.getInstance().egaHeartThreshold) {
            player.sendSystemMessage(Component.literal(
                    "You cannot use an Enchanted Golden Apple above "
                            + ServerConfig.getInstance().egaHeartThreshold + " hearts."
            ).withStyle(ChatFormatting.RED));
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void lifesteal$snapshotOnEGAConsumed(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (!ServerConfig.getInstance().egaHeartLimitEnabled) return;
        if (level.isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return;

        EGAEffectStripper.snapshotBeforeEGA(player);
    }
}
