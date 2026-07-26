package nightfallmods.lifesteal.screen;

import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public class ReviveItemFactory {

    public ItemStack createPlayerHead(String playerName, boolean isBanned) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.CUSTOM_NAME,
                text("Revive " + playerName, isBanned ? ChatFormatting.RED : ChatFormatting.YELLOW));
        applySkin(head, playerName);
        return head;
    }

    public ItemStack createPreviousPageButton() {
        ItemStack item = new ItemStack(Items.ARROW);
        item.set(DataComponents.CUSTOM_NAME, text("Previous Page", ChatFormatting.GRAY));
        return item;
    }

    public ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Items.ARROW);
        item.set(DataComponents.CUSTOM_NAME, text("Next Page", ChatFormatting.GRAY));
        return item;
    }

    public ItemStack createPageInfo(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Items.PAPER);
        item.set(DataComponents.CUSTOM_NAME,
                text("Page " + currentPage + "/" + totalPages, ChatFormatting.WHITE));
        return item;
    }

    public ItemStack createSortButton(ReviveSort sort) {
        ItemStack item = new ItemStack(Items.COMPARATOR);
        item.set(DataComponents.CUSTOM_NAME, text("Sort: " + sort.label(), ChatFormatting.AQUA));
        return item;
    }

    public ItemStack createYesButton(String playerName) {
        ItemStack item = new ItemStack(Blocks.LIME_CONCRETE.asItem());
        item.set(DataComponents.CUSTOM_NAME, text("Revive " + playerName, ChatFormatting.GREEN));
        return item;
    }

    public ItemStack createNoButton() {
        ItemStack item = new ItemStack(Blocks.RED_CONCRETE.asItem());
        item.set(DataComponents.CUSTOM_NAME, text("Cancel", ChatFormatting.RED));
        return item;
    }

    public ItemStack createConfirmationHead(String playerName) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.CUSTOM_NAME,
                text("Are you sure you want to revive " + playerName + "?", ChatFormatting.GRAY));
        applySkin(head, playerName);
        return head;
    }

    public ItemStack createFiller() {
        ItemStack item = new ItemStack(Blocks.GRAY_STAINED_GLASS_PANE.asItem());
        item.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return item;
    }

    private static Component text(String label, ChatFormatting color) {
        return Component.literal(label).withStyle(style ->
                style.withColor(color).withItalic(false));
    }

    private static void applySkin(ItemStack head, String playerName) {
        // ResolvableProfile.createUnresolved does not exist on this line; the three-arg record
        // constructor is the equivalent — the profile resolves client-side from the name alone.
        head.set(DataComponents.PROFILE,
                new ResolvableProfile(Optional.of(playerName), Optional.empty(), new PropertyMap()));
        head.set(DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.PROFILE, true));
    }
}

