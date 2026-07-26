package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderpearlItem.class)
public class EnderPearlMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void lifesteal$disablePearls(Level level, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ServerConfig.getInstance().disableEnderPearls) return;
        if (level.isClientSide()) return;

        if (user instanceof ServerPlayer player) {
            player.sendSystemMessage(
                    Component.literal("Ender Pearls are disabled.").withStyle(ChatFormatting.RED), true);

            player.containerMenu.sendAllDataToRemote();
        }
        cir.setReturnValue(InteractionResult.FAIL);
    }
}

