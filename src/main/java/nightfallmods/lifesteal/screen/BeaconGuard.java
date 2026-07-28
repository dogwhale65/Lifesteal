package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pins a revive menu to the exact Beacon of Life that opened it.
 *
 * <p>Checking "does this player still own a beacon somewhere" is not enough. The revive menus do not
 * own the offhand slot, so a beacon moved there is invisible to the menu's own client sync: the
 * server can spend it while the client keeps drawing it, which reads as a duplicated beacon on the
 * list screen and a ghost item on the confirmation screen. The same hole opens up whenever the stack
 * is dragged, dropped, or swapped mid-session.
 *
 * <p>So the stack's <em>identity</em> is what is tracked, not its existence. Any move breaks the
 * identity, {@code stillValid} then fails, and vanilla closes the menu on the player's next tick —
 * which also makes the packet handler reject any click that races the move, since it re-checks
 * {@code stillValid} before dispatching.
 */
public final class BeaconGuard {

    private static final int UNBOUND = -1;

    private final int slot;
    private final ItemStack stack;

    private BeaconGuard(int slot, ItemStack stack) {
        this.slot  = slot;
        this.stack = stack;
    }

    /** Binds to the player's first beacon, or to nothing when they have none (an operator). */
    public static BeaconGuard capture(Player player) {
        int slot = ReviveScreenHandler.findBeaconSlot(player);
        return slot < 0
                ? new BeaconGuard(UNBOUND, ItemStack.EMPTY)
                : new BeaconGuard(slot, player.getInventory().getItem(slot));
    }

    /** Whether a beacon was present when the menu opened. */
    public boolean isBound() {
        return slot != UNBOUND;
    }

    /** The inventory slot the beacon was in, or -1 when unbound. */
    public int slot() {
        return slot;
    }

    /** True once the tracked beacon is no longer the stack sitting in the slot it was captured from. */
    public boolean hasMoved(Player player) {
        if (!isBound()) return false;
        if (stack.isEmpty() || stack.getItem() != Items.BEACON_OF_LIFE) return true;
        return player.getInventory().getItem(slot) != stack;
    }
}
