package nightfallmods.lifesteal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared presentation for the mod's items. Item components do not exist on 1.20.2, so the
 * coloured name and the lore line cannot be attached at registration — they are supplied by
 * these overrides instead. The plain name text lives in the language file; the colour is
 * applied here so a resource pack can still translate the item.
 */
public abstract class LifestealItem extends Item {

    private final ChatFormatting nameColor;

    protected LifestealItem(Properties properties, ChatFormatting nameColor) {
        super(properties);
        this.nameColor = nameColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(this.nameColor);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(loreLine());
    }

    /** The single lore line shown under the item name. */
    protected abstract Component loreLine();

    /** A grey, non-italic base to append coloured segments onto. */
    protected static MutableComponent line() {
        return Component.empty().withStyle(style ->
                style.withColor(ChatFormatting.GRAY).withItalic(false));
    }

    protected static Component colored(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }
}
