package nightfallmods.lifesteal.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ReviveItemFactory {

    public ItemStack createPlayerHead(String playerName, boolean isBanned) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.setHoverName(text("Revive " + playerName, isBanned ? ChatFormatting.RED : ChatFormatting.YELLOW));
        applySkin(head, playerName);
        return head;
    }

    public ItemStack createPreviousPageButton() {
        ItemStack item = new ItemStack(Items.ARROW);
        item.setHoverName(text("Previous Page", ChatFormatting.GRAY));
        return item;
    }

    public ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Items.ARROW);
        item.setHoverName(text("Next Page", ChatFormatting.GRAY));
        return item;
    }

    public ItemStack createPageInfo(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Items.PAPER);
        item.setHoverName(text("Page " + currentPage + "/" + totalPages, ChatFormatting.WHITE));
        return item;
    }

    public ItemStack createSortButton(ReviveSort sort) {
        ItemStack item = new ItemStack(Items.COMPARATOR);
        item.setHoverName(text("Sort: " + sort.label(), ChatFormatting.AQUA));
        return item;
    }

    public ItemStack createYesButton(String playerName) {
        ItemStack item = new ItemStack(Blocks.LIME_CONCRETE.asItem());
        item.setHoverName(text("Revive " + playerName, ChatFormatting.GREEN));
        return item;
    }

    public ItemStack createNoButton() {
        ItemStack item = new ItemStack(Blocks.RED_CONCRETE.asItem());
        item.setHoverName(text("Cancel", ChatFormatting.RED));
        return item;
    }

    public ItemStack createConfirmationHead(String playerName) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.setHoverName(text("Are you sure you want to revive " + playerName + "?", ChatFormatting.GRAY));
        applySkin(head, playerName);
        return head;
    }

    public ItemStack createFiller() {
        ItemStack item = new ItemStack(Blocks.GRAY_STAINED_GLASS_PANE.asItem());
        item.setHoverName(Component.literal(" "));
        return item;
    }

    private static Component text(String label, ChatFormatting color) {
        return Component.literal(label).withStyle(style ->
                style.withColor(color).withItalic(false));
    }

    // 1.20.1 predates the PROFILE component: a bare SkullOwner name is resolved by the client.
    private static void applySkin(ItemStack head, String playerName) {
        head.getOrCreateTag().putString("SkullOwner", playerName);
    }
}
