package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ConfirmationScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final Player player;
    private final MinecraftServer server;
    private final String targetName;
    private final ReviveSort returnSort;
    private final ReviveLogic logic;

    public ConfirmationScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server,
                                     String targetName, boolean targetBanned, ReviveSort returnSort) {
        super(MenuType.GENERIC_9x3, syncId);
        this.player     = playerInventory.player;
        this.server     = server;
        this.targetName = targetName;
        this.returnSort = returnSort;
        this.inventory  = new SimpleContainer(Constants.CHEST_3X9_SIZE);
        this.logic      = new ReviveLogic(server, player);

        addSlots(playerInventory);

        ReviveItemFactory factory = new ReviveItemFactory();
        for (int i = 0; i < Constants.CHEST_3X9_SIZE; i++) {
            inventory.setItem(i, factory.createFiller());
        }
        inventory.setItem(Constants.SLOT_YES_BUTTON, factory.createYesButton(targetName));
        inventory.setItem(Constants.SLOT_CONFIRM_HEAD, factory.createConfirmationHead(targetName));
        inventory.setItem(Constants.SLOT_NO_BUTTON, factory.createNoButton());
    }

    private void addSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new ReadOnlySlot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    @Override
    public boolean stillValid(Player player) {
        return ReviveScreenHandler.isOperator(player) || ReviveScreenHandler.hasBeaconInInventory(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (slotIndex >= 0 && slotIndex < Constants.CHEST_3X9_SIZE && clickType == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotIndex);
            if (slot != null && slot.hasItem()) {
                handleClick(slot.getItem());
                return;
            }
        }
        super.clicked(slotIndex, button, clickType, player);
    }

    private void handleClick(ItemStack stack) {
        if (!stack.hasCustomHoverName()) return;
        String name = stack.getHoverName().getString();

        if (name.equals("Revive " + targetName)) {
            confirmRevive();
        } else if (name.equals("Cancel")) {
            returnToList();
        }
    }

    /**
     * The beacon is re-checked here rather than when the menu was opened. An arbitrary amount of
     * wall-clock time passes between opening the menu and clicking confirm, during which the player
     * can drop, stash, or destroy the beacon — so a reference (or a "had a beacon" flag) captured at
     * open time is not evidence that the beacon is still owned. Locating the slot, reviving, and
     * shrinking the stack all happen within this one call, so nothing can run in between.
     */
    private void confirmRevive() {
        boolean operator = ReviveScreenHandler.isOperator(player);
        int beaconSlot = ReviveScreenHandler.findBeaconSlot(player);

        if (beaconSlot < 0 && !operator) {
            player.sendSystemMessage(
                    Component.literal("You no longer have a Beacon of Life.").withStyle(ChatFormatting.RED));
            closeMenu();
            return;
        }

        if (!logic.revivePlayer(targetName)) {
            player.sendSystemMessage(
                    Component.literal("Failed to revive " + targetName + ".").withStyle(ChatFormatting.RED));
            returnToList();
            return;
        }

        // Operators may revive without a beacon, but a beacon in hand is still spent.
        if (beaconSlot >= 0) consumeBeaconAt(beaconSlot);

        player.sendSystemMessage(
                Component.literal("Revived " + targetName + ".").withStyle(ChatFormatting.GREEN));
        closeMenu();
    }

    private void consumeBeaconAt(int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.getItem() != Items.BEACON_OF_LIFE) {
            Lifesteal.LOGGER.warn("[Revival] Beacon of Life vanished from slot {} during {}'s revive.",
                    slot, player.getName().getString());
            return;
        }
        stack.shrink(1);
        Lifesteal.LOGGER.info("[Revival] Beacon of Life consumed by {}.", player.getName().getString());
    }

    private void closeMenu() {
        if (player instanceof ServerPlayer sp) sp.closeContainer();
    }

    private void returnToList() {
        if (!(player instanceof ServerPlayer sp)) return;
        ReviveSort sortToRestore = returnSort;
        MinecraftServer srv = server;
        sp.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReviveScreenHandler(syncId, inv, srv, sortToRestore),
                Component.literal("Revive a Player")
        ));
    }
}
