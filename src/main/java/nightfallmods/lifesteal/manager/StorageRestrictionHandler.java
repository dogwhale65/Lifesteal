package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageRestrictionHandler {

    private static final Map<UUID, Long> notifiedAtTick = new HashMap<>();

    public static boolean isStorageRestricted(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() == Items.HEART
                || stack.getItem() == Items.CRAFTED_HEART
                || stack.getItem() == Items.HEART_FRAGMENT
                || stack.getItem() == Items.BEACON_OF_LIFE) return true;

        return UniqueItemManager.isTrackedUnique(stack);
    }

    public static boolean isBlockedContainer(Container container) {
        return container instanceof BlockEntity
                || container instanceof CompoundContainer
                || container instanceof PlayerEnderChestContainer
                || container instanceof ContainerEntity;
    }

    public static void notifyRestricted(ServerPlayer player, ItemStack stack) {
        player.sendSystemMessage(
                Component.literal(plainName(stack) + " cannot be stored.")
                        .withStyle(ChatFormatting.RED),
                true
        );
        notifiedAtTick.put(player.getUUID(), player.level().getGameTime());
    }

    /**
     * The item's name with any legacy section-sign colours stripped, so the whole message renders
     * red rather than picking up the item's own colour part-way through.
     */
    private static String plainName(ItemStack stack) {
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        return name == null ? "" : name;
    }

    /**
     * True if this player was told an item cannot be stored on the current tick.
     *
     * Right-clicking an item frame sends an interact packet and a use-item packet in the same tick,
     * so a Heart bounced off a frame would otherwise also report the heart cap. One refusal per
     * action is enough, and the frame's is the one that explains what happened.
     */
    public static boolean notifiedThisTick(ServerPlayer player) {
        Long tick = notifiedAtTick.get(player.getUUID());
        return tick != null && tick == player.level().getGameTime();
    }
}

