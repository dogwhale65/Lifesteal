package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pins a revive menu to the exact Beacon of Life stack that opened it.
 *
 * <p>The beacon is identified by slot <em>and</em> stack identity, so anything that moves it —
 * offhand swap, drag, drop, hotbar swap, a hopper pulling it away — leaves a different object
 * (or nothing) in that slot and breaks the anchor. The menu then refuses further clicks and
 * closes, which is what keeps the beacon from being spent twice or going out of sync.
 */
public final class BeaconAnchor {

    private static final BeaconAnchor NONE = new BeaconAnchor(-1, ItemStack.EMPTY);

    private final int slot;
    private final ItemStack stack;

    private BeaconAnchor(int slot, ItemStack stack) {
        this.slot  = slot;
        this.stack = stack;
    }

    /** For menus opened without a beacon, such as an operator running {@code /revive}. */
    public static BeaconAnchor none() {
        return NONE;
    }

    /** Anchors to the beacon the player is holding in the given hand. */
    public static BeaconAnchor held(Player player, InteractionHand hand) {
        int slot = hand == InteractionHand.OFF_HAND
                ? Inventory.SLOT_OFFHAND
                : player.getInventory().getSelectedSlot();
        return new BeaconAnchor(slot, player.getItemInHand(hand));
    }

    public boolean isAnchored() {
        return slot >= 0;
    }

    /** True while the anchored beacon is still sitting untouched in the slot it was opened from. */
    public boolean isIntact(Player player) {
        if (!isAnchored()) return true;
        ItemStack current = player.getInventory().getItem(slot);
        return current == stack && current.getItem() == Items.BEACON_OF_LIFE;
    }

    /** Spends one beacon from the anchored slot. Returns false if it is no longer there. */
    public boolean consume(Player player) {
        if (!isAnchored()) return true;
        if (!isIntact(player)) return false;
        stack.shrink(1);
        return true;
    }
}
