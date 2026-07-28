package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pins a revive menu to the exact inventory slot the Beacon of Life occupied when the menu opened.
 *
 * <p>The offhand and armour slots belong to {@link Inventory} but are <em>not</em> slots of the open
 * container menu, so shrinking a beacon there never reaches the client — which is why swapping the
 * beacon to the offhand mid-revive left a duplicate on the main screen and a ghost item on the
 * confirmation screen. Anchoring to one slot means any move at all — offhand swap, drag, drop,
 * hotbar swap — makes the anchor stale, and a stale anchor closes the menu.
 */
public final class BeaconAnchor {

    private final int slot;

    /** One anchor per menu, so this stays a per-menu one-shot. */
    private boolean warned = false;

    private BeaconAnchor(int slot) {
        this.slot = slot;
    }

    /** {@code slot < 0} anchors nothing — an operator opening the menu with {@code /revive}. */
    public static BeaconAnchor at(int slot) {
        return new BeaconAnchor(Math.max(slot, -1));
    }

    /** Locates {@code held} by identity so the anchor tracks the stack actually used, not just any beacon. */
    public static int slotOf(Player player, ItemStack held) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == held) return i;
        }
        return -1;
    }

    public int slot() { return slot; }

    public boolean present() { return slot >= 0; }

    /** True while the anchored slot still holds a beacon. */
    public boolean intact(Player player) {
        if (slot < 0) return false;
        Inventory inv = player.getInventory();
        return slot < inv.getContainerSize() && inv.getItem(slot).getItem() == Items.BEACON_OF_LIFE;
    }

    /**
     * Explains the closure once. The menu can close from two places — a click that invalidates the
     * anchor, or {@code stillValid()} on the next tick — and the player should hear about it exactly
     * once whichever gets there first.
     */
    public void warnClosed(Player player) {
        if (warned || !(player instanceof ServerPlayer sp)) return;
        warned = true;
        sp.sendSystemMessage(Component.literal(present()
                        ? "Your Beacon of Life moved — revive cancelled."
                        : "You can no longer use the revive menu.")
                .withStyle(ChatFormatting.RED));
    }

    /** Shrinks the anchored beacon by one; false if it is no longer there. */
    public boolean consume(Player player) {
        if (!intact(player)) return false;
        player.getInventory().getItem(slot).shrink(1);
        // A beacon used from the offhand lives in a slot the open menu does not own, so the shrink
        // would never reach the client. Push a full inventory sync so nothing is left ghosted.
        if (player instanceof ServerPlayer sp) sp.inventoryMenu.broadcastFullState();
        Lifesteal.LOGGER.info("[Revival] Beacon of Life consumed by {}.", player.getName().getString());
        return true;
    }
}
