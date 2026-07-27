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
        // The whole line is the error, so it is all red — the item's own name colour (and any legacy
        // section codes baked into it) must not leak in and recolour the rest of the sentence.
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        player.sendSystemMessage(
                Component.literal(name + " cannot be stored.").withStyle(ChatFormatting.RED),
                true
        );
    }
}

