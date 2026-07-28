package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.manager.CraftedHeartTracker;
import nightfallmods.lifesteal.manager.EGAEffectStripper;
import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CraftedHeart extends Item {

    public CraftedHeart(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.pass(itemStack);
        }

        ServerPlayer player = (ServerPlayer) user;
        ServerConfig cfg = ServerConfig.getInstance();

        // The click that reached here may have been a refused item frame placement; that already
        // told the player why it failed, so the heart cap is not worth mentioning on top of it.
        if (StorageRestrictionHandler.wasRejectedByItemFrame(player)) {
            return InteractionResultHolder.fail(itemStack);
        }

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return InteractionResultHolder.fail(itemStack);
        }

        double cap = cfg.craftedHeartCap * Constants.HEART_VALUE;
        if (attr.getBaseValue() >= cap) {
            player.sendSystemMessage(
                    Component.literal("Crafted Hearts cannot raise your health beyond "
                                    + cfg.craftedHeartCap + " hearts.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(itemStack);
        }

        if (attr.getBaseValue() >= cfg.getMaxHealth()) {
            player.sendSystemMessage(
                    Component.literal("You cannot have more than " + cfg.maxHearts + " hearts.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(itemStack);
        }

        attr.setBaseValue(attr.getBaseValue() + Constants.HEART_VALUE);
        if (cfg.fullHeartOnGain)
            player.setHealth(player.getHealth() + (float) Constants.HEART_VALUE);

        CraftedHeartTracker.increment(player.getUUID());

        if (attr.getBaseValue() / Constants.HEART_VALUE > cfg.egaHeartThreshold)
            EGAEffectStripper.stripIfSnapshotExists(player);

        itemStack.shrink(1);

        level.playSound(null, player.blockPosition(),
                SoundEvent.createVariableRangeEvent(new ResourceLocation(cfg.heartEquipSound)),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int cap = ServerConfig.getInstance().craftedHeartCap;
        tooltip.add(Items.line()
                .append("Consume to gain a ")
                .append(Items.colored("Heart", ChatFormatting.DARK_RED))
                .append(". Cannot be applied above ")
                .append(Items.colored(cap + " hearts", ChatFormatting.GOLD))
                .append("."));
    }
}
