package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.item.Items;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageRestrictionHandler {

    /** Ticks a frame refusal keeps suppressing item use. The two packets arrive together, so one spare tick is plenty. */
    private static final long REFUSAL_WINDOW_TICKS = 1L;

    private static final Map<UUID, Long> frameRefusals = new HashMap<>();

    public static void register() {
        // A vanilla client has none of this mod's mixins, so once the server refuses the item
        // frame the client goes straight on to sending its use-item packet — which would consume
        // the Heart and answer with its own cap message, or open the revive menu for a Beacon.
        // The refusal is remembered briefly so that follow-up use can be dropped.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide() && wasRefusedAtFrame(player)) {
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
        Lifesteal.LOGGER.info("[Storage] Restriction handler registered.");
    }

    /** Refuses a restricted item at an item frame: tells the player, and blocks the use that would follow. */
    public static void refuseFraming(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        frameRefusals.entrySet().removeIf(entry -> now - entry.getValue() > REFUSAL_WINDOW_TICKS);
        frameRefusals.put(player.getUUID(), now);
        notifyRestricted(player, stack);
    }

    private static boolean wasRefusedAtFrame(Player player) {
        Long refusedAt = frameRefusals.get(player.getUUID());
        if (refusedAt == null) return false;
        if (player.level().getGameTime() - refusedAt > REFUSAL_WINDOW_TICKS) {
            frameRefusals.remove(player.getUUID());
            return false;
        }
        return true;
    }

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
        // The whole line is one red literal, name included. Splicing the raw hover name in would
        // carry any legacy § codes it holds straight into the message, and those recolour
        // everything after them — which is why this used to render in the item's colour.
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        if (name == null || name.isBlank()) name = "That item";

        player.sendSystemMessage(
                Component.literal(name + " cannot be stored.").withStyle(ChatFormatting.RED),
                true
        );
    }
}

