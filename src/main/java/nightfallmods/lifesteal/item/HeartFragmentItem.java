package nightfallmods.lifesteal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class HeartFragmentItem extends LifestealItem {

    public HeartFragmentItem(Properties properties) {
        super(properties, ChatFormatting.RED);
    }

    @Override
    protected Component loreLine() {
        return line()
                .append("A ")
                .append(colored("fragment", ChatFormatting.RED))
                .append(" of a ")
                .append(colored("Heart", ChatFormatting.DARK_RED))
                .append(", used to make ")
                .append(colored("Crafted Hearts", ChatFormatting.GOLD))
                .append(".");
    }
}
