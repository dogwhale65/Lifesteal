package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.manager.CraftedHeartTracker;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CraftedHeart extends LifestealItem {

    public CraftedHeart(Properties properties) {
        super(properties, ChatFormatting.GOLD);
    }

    @Override
    protected Component loreLine() {
        return line()
                .append("Consume to gain a ")
                .append(colored("Heart", ChatFormatting.DARK_RED))
                .append(". Cannot be applied above ")
                .append(colored(ServerConfig.getInstance().craftedHeartCap + " hearts", ChatFormatting.GOLD))
                .append(".");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.pass(itemStack);
        }

        ServerPlayer player = (ServerPlayer) user;
        ServerConfig cfg = ServerConfig.getInstance();

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (attr.getBaseValue() >= cfg.getMaxHealth()) {
            deny(player, "You cannot have more than " + cfg.maxHearts + " hearts.");
            return InteractionResultHolder.fail(itemStack);
        }

        double cap = cfg.craftedHeartCap * Constants.HEART_VALUE;
        if (attr.getBaseValue() >= cap) {
            deny(player, "Crafted Hearts cannot raise your health beyond "
                    + cfg.craftedHeartCap + " hearts.");
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
}
