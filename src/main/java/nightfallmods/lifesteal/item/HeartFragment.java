package nightfallmods.lifesteal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The fragment is a plain Item on versions with components; 1.20.1 needs a class of its own
 * purely so the lore can be attached through appendHoverText.
 */
public class HeartFragment extends Item {

    public HeartFragment(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Items.line()
                .append("A ")
                .append(Items.colored("fragment", ChatFormatting.RED))
                .append(" of a ")
                .append(Items.colored("Heart", ChatFormatting.DARK_RED))
                .append(", used to make ")
                .append(Items.colored("Crafted Hearts", ChatFormatting.GOLD))
                .append("."));
    }
}
