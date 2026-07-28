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

    private static final Map<UUID, Long> frameRejections = new HashMap<>();
    private static final long FRAME_REJECTION_WINDOW_TICKS = 1;

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
    }

    /**
     * The item names live in the lang file as legacy colour codes ("§4Heart"), and getString hands
     * those back verbatim. Splicing them into a message re-colours everything that follows, which
     * is why the refusal used to render in the item's colour instead of red — so they are stripped.
     */
    private static String plainName(ItemStack stack) {
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        return name == null ? "" : name;
    }

    /**
     * Right-clicking an item frame can still reach the held item's own use handler, which would
     * follow the refusal above with an unrelated "cannot be applied" message. The refusal is
     * recorded so those handlers can bow out; the window covers the tick the click landed on and
     * the next one, which is well inside vanilla's four-tick right-click delay.
     */
    public static void markFrameRejection(ServerPlayer player) {
        long now = player.level().getGameTime();
        frameRejections.values().removeIf(tick -> now - tick > FRAME_REJECTION_WINDOW_TICKS);
        frameRejections.put(player.getUUID(), now);
    }

    public static boolean wasRejectedByItemFrame(ServerPlayer player) {
        Long tick = frameRejections.get(player.getUUID());
        return tick != null && player.level().getGameTime() - tick <= FRAME_REJECTION_WINDOW_TICKS;
    }
}

