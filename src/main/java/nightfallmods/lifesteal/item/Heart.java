package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.manager.EGAEffectStripper;
import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Heart extends Item {

    public Heart(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;

        ServerPlayer player = (ServerPlayer) user;
        if (StorageRestrictionHandler.consumeUseSuppression(player)) return InteractionResult.FAIL;

        ServerConfig cfg = ServerConfig.getInstance();

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return InteractionResult.FAIL;
        if (attr.getBaseValue() >= cfg.getMaxHealth()) {
            player.sendSystemMessage(
                    Component.literal("You cannot have more than " + cfg.maxHearts + " hearts.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResult.FAIL;
        }

        attr.setBaseValue(attr.getBaseValue() + Constants.HEART_VALUE);
        if (cfg.fullHeartOnGain)
            player.setHealth(player.getHealth() + (float) Constants.HEART_VALUE);

        if (attr.getBaseValue() / Constants.HEART_VALUE > cfg.egaHeartThreshold)
            EGAEffectStripper.stripIfSnapshotExists(player);

        player.getItemInHand(hand).shrink(1);

        level.playSound(null, player.blockPosition(),
                SoundEvent.createVariableRangeEvent(ResourceLocation.parse(cfg.heartEquipSound)),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        return InteractionResult.SUCCESS;
    }
}
