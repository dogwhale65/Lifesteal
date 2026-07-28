package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pins an open revive menu to the exact Beacon of Life stack that opened it.
 *
 * <p>The client never sees the revive menu classes — both are sent as plain chest menu types, so
 * client-side every board slot looks interactive and clicks are predicted locally. Mispredictions
 * on menu slots are repaired by {@code broadcastChanges()}, but a chest menu has no armour or
 * offhand slot, so hotswapping the beacon out with F mutates client state the server has no slot
 * to correct — the beacon shows up in two places at once. Rather than patching each such path,
 * the menu is torn down the moment the beacon stops sitting exactly where it was when the menu
 * opened, and the player's real inventory is re-sent behind it.
 */
public final class BeaconGuard {

    private final int slot;
    private final ItemStack tracked;
    private final int count;
    private final boolean operator;

    private BeaconGuard(Player player) {
        this.operator = ReviveScreenHandler.isOperator(player);
        this.slot     = ReviveScreenHandler.findBeaconSlot(player);
        this.tracked  = slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
        this.count    = tracked.getCount();
    }

    public static BeaconGuard of(Player player) {
        return new BeaconGuard(player);
    }

    /** Inventory index the beacon occupied when the menu opened, or -1 for an operator who had none. */
    public int slot() {
        return slot;
    }

    /**
     * True while the beacon is untouched. Identity is compared, not contents: moving a stack anywhere
     * — another slot, the offhand, the ground — leaves a different (or empty) stack behind, and the
     * count catches a stack being split rather than moved whole.
     */
    public boolean intact(Player player) {
        if (slot < 0) return operator;
        ItemStack current = player.getInventory().getItem(slot);
        return current == tracked
                && current.getItem() == Items.BEACON_OF_LIFE
                && current.getCount() == count;
    }

    /**
     * Re-sends the open menu and the player's own inventory. Called whenever a click is refused or a
     * revive menu closes, so a move the client predicted but the server never made cannot linger.
     * Unlike {@code broadcastChanges()}, the inventory menu covers the armour and offhand slots.
     */
    public static void resync(Player player) {
        if (!(player instanceof ServerPlayer sp) || sp.connection == null) return;
        sp.containerMenu.broadcastFullState();
        if (sp.containerMenu != sp.inventoryMenu) sp.inventoryMenu.broadcastFullState();
    }
}
