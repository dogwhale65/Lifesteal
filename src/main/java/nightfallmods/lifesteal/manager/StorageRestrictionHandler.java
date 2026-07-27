package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageRestrictionHandler {

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

    /**
     * Blocking an interaction only cancels that interaction — the client falls through to a normal
     * item use on the same right-click, which would fire the item's own "cannot apply" message on top
     * of ours. Items consult {@link #wasJustBlocked} to stay quiet for the rest of the tick.
     */
    private static final Map<UUID, Long> blockedAt = new HashMap<>();

    public static void notifyRestricted(ServerPlayer player, ItemStack stack) {
        blockedAt.put(player.getUUID(), player.level().getGameTime());

        // Built with an explicit style rather than an inherited one so the whole line reads red
        // instead of picking up the item name's colour.
        player.sendSystemMessage(
                Component.literal(stack.getHoverName().getString() + " cannot be stored.")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withItalic(false)),
                true
        );
    }

    public static boolean wasJustBlocked(Player player) {
        Long tick = blockedAt.get(player.getUUID());
        if (tick == null) return false;
        // Usually the same tick, but allow one of slack in case the two packets straddle a boundary.
        // The client's own right-click delay is longer than that, so a real use can never be eaten.
        long elapsed = player.level().getGameTime() - tick;
        return elapsed >= 0 && elapsed <= 1;
    }
}

