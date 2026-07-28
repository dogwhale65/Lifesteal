package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Watches the Beacon of Life stacks a player held when a revive menu was opened.
 *
 * <p>The menus are driven by the beacon in the opener's inventory, so any movement of it while a
 * menu is up desynchronises the two: the client predicts a move the server never applies to the
 * open container, leaving a duplicate or a ghost stack behind. Rather than intercepting every
 * {@link net.minecraft.world.inventory.ClickType} that could move it, the whole beacon layout is
 * snapshotted at open time and compared each tick from {@code stillValid} — offhanding, dragging,
 * dropping, picking it up onto the cursor and losing it to a command all read as a change.
 */
public final class BeaconMenuGuard {

    private final Player player;
    private final int[] snapshot;

    public BeaconMenuGuard(Player player) {
        this.player   = player;
        this.snapshot = beaconLayout(player);
    }

    /** False once the beacon has moved, changed count, or left the inventory in any way. */
    public boolean beaconUnmoved() {
        return Arrays.equals(snapshot, beaconLayout(player));
    }

    /**
     * Closes {@code menu} if the beacon has moved, then pushes the real inventory to the client.
     *
     * <p>Closing through {@code stillValid} alone is not enough to clear a mispredicted stack:
     * {@code doCloseContainer} hands the closing menu's remote-slot state to the inventory menu, so
     * the server believes the client is already in sync and the next diff sends nothing. The full
     * state has to be broadcast after the handover, which is why this runs from
     * {@code broadcastChanges} rather than from {@code stillValid}.
     */
    public boolean closeIfBeaconMoved(AbstractContainerMenu menu) {
        if (beaconUnmoved()) return false;
        if (player instanceof ServerPlayer sp && sp.containerMenu == menu) {
            sp.closeContainer();
            sp.inventoryMenu.broadcastFullState();
        }
        return true;
    }

    /** Beacon counts per inventory slot; zero everywhere else. Slot 40 is the offhand on 1.20.1. */
    private static int[] beaconLayout(Player player) {
        Inventory inv = player.getInventory();
        int[] counts = new int[inv.getContainerSize()];
        for (int i = 0; i < counts.length; i++) {
            ItemStack stack = inv.getItem(i);
            counts[i] = stack.getItem() == Items.BEACON_OF_LIFE ? stack.getCount() : 0;
        }
        return counts;
    }
}
