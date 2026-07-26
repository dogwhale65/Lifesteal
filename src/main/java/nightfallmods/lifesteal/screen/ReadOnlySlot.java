package nightfallmods.lifesteal.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReadOnlySlot extends Slot {

    public ReadOnlySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) { return false; }

    @Override
    public boolean mayPickup(Player player) { return false; }
}
