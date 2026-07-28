package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.manager.EGAEffectStripper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Heart extends Item {

    public Heart(Properties properties) {
        super(properties);
    }

    // Item#use returns InteractionResultHolder here; the unified InteractionResult return arrives in 1.21.2.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        ServerPlayer player = (ServerPlayer) user;
        ServerConfig cfg = ServerConfig.getInstance();

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return InteractionResultHolder.fail(stack);

        if (attr.getBaseValue() >= cfg.getMaxHealth()) {
            player.sendSystemMessage(
                    Component.literal("You cannot go above " + cfg.maxHearts + " hearts.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(stack);
        }

        attr.setBaseValue(attr.getBaseValue() + Constants.HEART_VALUE);
        if (cfg.fullHeartOnGain)
            player.setHealth(player.getHealth() + (float) Constants.HEART_VALUE);

        if (attr.getBaseValue() / Constants.HEART_VALUE > cfg.egaHeartThreshold)
            EGAEffectStripper.stripIfSnapshotExists(player);

        stack.shrink(1);

        level.playSound(null, player.blockPosition(),
                SoundEvent.createVariableRangeEvent(new ResourceLocation(cfg.heartEquipSound)),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        return InteractionResultHolder.success(stack);
    }
}
