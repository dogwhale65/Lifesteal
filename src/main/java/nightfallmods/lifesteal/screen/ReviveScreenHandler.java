package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.item.Items;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.server.level.ServerLevel;

public class ReviveScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final Player player;
    private final MinecraftServer server;
    private final PlayerCollector collector;
    private final PageManager pages;
    private final ReviveItemFactory factory;

    /** Inventory slot the authorising beacon occupied when the menu opened; -1 for an operator without one. */
    private final int beaconSlot;

    private ReviveSort sort;

    public ReviveScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server) {
        this(syncId, playerInventory, server, ReviveSort.EARLIEST_BANNED);
    }

    public ReviveScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server, ReviveSort sort) {
        this(syncId, playerInventory, server, sort, findBeaconSlot(playerInventory.player));
    }

    public ReviveScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server,
                               ReviveSort sort, int beaconSlot) {
        super(MenuType.GENERIC_9x6, syncId);
        this.player     = playerInventory.player;
        this.server     = server;
        this.sort       = sort;
        this.beaconSlot = beaconSlot;
        this.inventory  = new SimpleContainer(Constants.CHEST_6X9_SIZE);
        this.collector  = new PlayerCollector(server);
        this.pages      = new PageManager();
        this.factory    = new ReviveItemFactory();

        addSlots(playerInventory);
        renderPage();
    }

    private void addSlots(Inventory playerInventory) {
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new ReadOnlySlot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
    }

    private void renderPage() {
        collector.collectPlayers();
        pages.populateCurrentPage(inventory, collector.getRevivables(), factory, sort);
    }

    @Override
    public boolean stillValid(Player player) {
        return beaconIntact(player, beaconSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        // stillValid() only runs once per tick, so a click that moved the beacon is processed before
        // the menu is torn down. Re-check on both sides of the click instead of trusting that.
        if (!beaconIntact(player, beaconSlot)) {
            closeMenu(player);
            return;
        }

        if (slotIndex >= 0 && slotIndex < Constants.CHEST_6X9_SIZE && clickType == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotIndex);
            if (slot != null && slot.hasItem()) {
                handleClick(slot.getItem());
                return;
            }
        }
        super.clicked(slotIndex, button, clickType, player);

        if (!beaconIntact(player, beaconSlot)) closeMenu(player);
    }

    private void handleClick(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) return;
        String name = stack.get(DataComponents.CUSTOM_NAME).getString();

        if (handleNavigation(name)) return;
        handleReviveClick(name);
    }

    private boolean handleNavigation(String name) {
        switch (name) {
            case "Previous Page" -> { pages.navigateToPreviousPage(); renderPage(); return true; }
            case "Next Page"     -> { pages.navigateToNextPage(collector.getTotalRevivableCount()); renderPage(); return true; }
        }
        if (name.startsWith("Sort: ")) {
            sort = sort.next();
            renderPage();
            return true;
        }
        return name.startsWith("Page ");
    }

    private void handleReviveClick(String name) {
        if (!name.startsWith("Revive ")) return;
        String target = name.substring(7);
        boolean isBanned = collector.isBanned(target);

        if (player instanceof ServerPlayer sp) {
            ReviveSort currentSort = sort;
            int pinnedBeacon = beaconSlot;
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new ConfirmationScreenHandler(
                            syncId, inv, server, target, isBanned, currentSort, pinnedBeacon),
                    Component.literal("Revive " + target + "?")
            ));
        }
    }

    public static boolean isOperator(Player player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        MinecraftServer server = ((ServerLevel) sp.level()).getServer();
        if (server == null) return false;
        return server.getPlayerList().isOp(sp.getGameProfile());
    }

    /**
     * Index of the first Beacon of Life in the player's inventory, or -1 if there is none.
     * Locating and consuming the beacon must scan identically, so both go through here.
     */
    public static int findBeaconSlot(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == Items.BEACON_OF_LIFE) return i;
        }
        return -1;
    }

    /**
     * The beacon is pinned to the slot it occupied when the menu opened. Anything that takes it out
     * of that slot — offhanding, dragging, dropping, a hotbar swap — invalidates the menu, so there
     * is never a window in which a moved beacon can still authorise (and then dodge) a revive.
     */
    static boolean beaconIntact(Player player, int beaconSlot) {
        if (beaconSlot < 0) return isOperator(player);
        return player.getInventory().getItem(beaconSlot).getItem() == Items.BEACON_OF_LIFE;
    }

    /** Closes the menu and resyncs the inventory so a half-moved stack cannot linger as a ghost item. */
    static void closeMenu(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        sp.closeContainer();
        sp.inventoryMenu.sendAllDataToRemote();
    }
}
