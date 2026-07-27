package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Binds an open revive menu to the exact Beacon of Life stack that opened it.
 *
 * <p>Scanning the inventory for "a beacon" is not enough: the stack that passes the check at
 * confirm time can be a different one than the stack the menu was opened with, which is how a
 * beacon could end up duplicated by off-handing it mid-revive. Holding the stack instance and the
 * hand it lives in means any movement at all — off-hand swap, drag, drop, shift-click — breaks the
 * binding on the very next tick and the menu closes.
 */
public final class BeaconBinding {

    private final InteractionHand hand;
    private final ItemStack stack;

    private BeaconBinding(InteractionHand hand, ItemStack stack) {
        this.hand  = hand;
        this.stack = stack;
    }

    /** Binds to the beacon currently held in {@code hand}, or returns null if there isn't one. */
    public static BeaconBinding of(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || held.getItem() != Items.BEACON_OF_LIFE) return null;
        return new BeaconBinding(hand, held);
    }

    /** True only while the very same stack is still sitting in the very same hand. */
    public boolean stillHeld(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return held == stack && !held.isEmpty() && held.getItem() == Items.BEACON_OF_LIFE;
    }

    public void consume(Player player) {
        if (!stillHeld(player)) return;
        stack.shrink(1);
    }
}
