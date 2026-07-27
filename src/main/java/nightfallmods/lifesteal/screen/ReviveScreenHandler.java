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

    /**
     * The inventory slot the Beacon of Life occupied when this menu was opened, or -1 when an
     * operator opened it without one. The beacon must stay put: moving it in any way — dragging,
     * dropping, swapping it to the offhand — closes the menu, because the offhand and armour slots
     * are not part of this menu and edits to them never reach the client as slot updates.
     */
    private final int watchedBeaconSlot;

    private ReviveSort sort;

    public ReviveScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server, int watchedBeaconSlot) {
        this(syncId, playerInventory, server, ReviveSort.EARLIEST_BANNED, watchedBeaconSlot);
    }

    public ReviveScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server,
                               ReviveSort sort, int watchedBeaconSlot) {
        super(MenuType.GENERIC_9x6, syncId);
        this.player    = playerInventory.player;
        this.server    = server;
        this.sort      = sort;
        this.watchedBeaconSlot = watchedBeaconSlot;
        this.inventory = new SimpleContainer(Constants.CHEST_6X9_SIZE);
        this.collector = new PlayerCollector(server);
        this.pages     = new PageManager();
        this.factory   = new ReviveItemFactory();

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
        return menuStillAuthorised(player, watchedBeaconSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        // stillValid() only runs once per player tick, which is too late: the click that moved the
        // beacon is fully applied before the next check. Bracketing every click catches it.
        if (closeIfBeaconMoved(player, watchedBeaconSlot)) return;

        if (slotIndex >= 0 && slotIndex < Constants.CHEST_6X9_SIZE && clickType == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotIndex);
            if (slot != null && slot.hasItem()) {
                handleClick(slot.getItem());
                return;
            }
        }
        super.clicked(slotIndex, button, clickType, player);
        closeIfBeaconMoved(player, watchedBeaconSlot);
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
            int beaconSlot = watchedBeaconSlot;
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new ConfirmationScreenHandler(
                            syncId, inv, server, target, isBanned, currentSort, beaconSlot),
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
     * Inventory slot holding the stack the player is using, or -1 if it cannot be located.
     * Compared by identity, so it finds the offhand and hotbar without depending on the layout
     * of {@link Inventory}'s compartments.
     */
    public static int heldSlot(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return -1;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == held) return i;
        }
        // Identity always matches in practice; the fallback exists so a miss degrades to watching
        // the wrong beacon rather than locking a non-operator out of the menu entirely.
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == held.getItem()) return i;
        }
        return -1;
    }

    /** True while the beacon the menu was opened with is still untouched in the slot it came from. */
    public static boolean beaconStillAt(Player player, int slot) {
        if (slot < 0) return false;
        var inv = player.getInventory();
        if (slot >= inv.getContainerSize()) return false;
        return inv.getItem(slot).getItem() == Items.BEACON_OF_LIFE;
    }

    /**
     * A beacon-opened menu stays open only while that beacon sits still; an operator who opened the
     * menu without one (slot -1) keeps it open on permission alone.
     */
    public static boolean menuStillAuthorised(Player player, int watchedBeaconSlot) {
        return watchedBeaconSlot < 0 ? isOperator(player) : beaconStillAt(player, watchedBeaconSlot);
    }

    /** Closes the menu when the watched beacon has moved. Returns true if it closed. */
    public static boolean closeIfBeaconMoved(Player player, int watchedBeaconSlot) {
        if (menuStillAuthorised(player, watchedBeaconSlot)) return false;
        if (player instanceof ServerPlayer sp) sp.closeContainer();
        return true;
    }
}

