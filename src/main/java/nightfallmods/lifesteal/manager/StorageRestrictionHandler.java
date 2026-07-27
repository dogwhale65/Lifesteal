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

    private static final int SUPPRESSION_WINDOW_TICKS = 2;

    private static final Map<UUID, Integer> suppressedUses = new HashMap<>();

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
        suppressUse(player);
        player.sendSystemMessage(
                Component.literal(stack.getHoverName().getString() + " cannot be stored.")
                        .withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false)),
                true
        );
    }

    /**
     * A blocked entity interaction (item frame, decorated pot) does not stop the client from
     * falling through to a plain item use, which would apply the heart the player was only
     * trying to place. Blocking the interaction arms a one-shot veto that the heart items check.
     */
    private static void suppressUse(ServerPlayer player) {
        suppressedUses.put(player.getUUID(), player.tickCount);
    }

    /** Returns true if this use follows a just-blocked placement and should be dropped. */
    public static boolean consumeUseSuppression(ServerPlayer player) {
        Integer tick = suppressedUses.remove(player.getUUID());
        return tick != null && player.tickCount - tick <= SUPPRESSION_WINDOW_TICKS;
    }
}

