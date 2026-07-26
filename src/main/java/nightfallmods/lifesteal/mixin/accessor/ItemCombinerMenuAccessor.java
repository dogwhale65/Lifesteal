package nightfallmods.lifesteal.mixin.accessor;

import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code resultSlots} is declared on ItemCombinerMenu, and @Shadow only resolves fields declared on
 * the mixin's own target class — so the SmithingMenu mixin reaches the inherited field through here.
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    @Accessor("resultSlots")
    ResultContainer getResultSlots();
}
