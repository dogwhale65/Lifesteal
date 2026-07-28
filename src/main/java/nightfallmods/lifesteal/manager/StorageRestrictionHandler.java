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

    public static void notifyRestricted(ServerPlayer player, ItemStack stack) {
        player.sendSystemMessage(
                Component.literal(plainName(stack) + " cannot be stored.")
                        .withStyle(style -> style.withColor(ChatFormatting.RED)),
                true
        );
    }

    /**
     * Item names carry their own colour, and the vanilla ones resolve to legacy §-coded strings —
     * either would repaint part of the warning. Strip both so the whole line reads red.
     */
    private static String plainName(ItemStack stack) {
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        return name == null ? "" : name;
    }
}

