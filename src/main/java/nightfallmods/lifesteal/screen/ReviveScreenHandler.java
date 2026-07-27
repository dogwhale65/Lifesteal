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
import net.minecraft.world.inventory.ContainerInput;
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

    /** Inventory slot the opening Beacon of Life sits in, or {@link #NO_BEACON} when opened by command. */
    private final int beaconSlot;

    private ReviveSort sort;

    public static final int NO_BEACON = -1;

    public ReviveScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server) {
        this(syncId, playerInventory, server, ReviveSort.EARLIEST_BANNED, NO_BEACON);
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
        return beaconStillBound(player, beaconSlot) || (beaconSlot == NO_BEACON && isOperator(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < Constants.CHEST_6X9_SIZE && containerInput == ContainerInput.PICKUP) {
            Slot slot = this.slots.get(slotIndex);
            if (slot != null && slot.hasItem()) {
                handleClick(slot.getItem());
                return;
            }
        }
        super.clicked(slotIndex, button, containerInput, player);
        closeIfBeaconMoved(this, player, beaconSlot);
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
            int slot = beaconSlot;
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new ConfirmationScreenHandler(syncId, inv, server, target, isBanned, currentSort, slot),
                    Component.literal("Revive " + target + "?")
            ));
        }
    }

    public static boolean isOperator(Player player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        MinecraftServer server = ((ServerLevel) sp.level()).getServer();
        if (server == null) return false;
        return server.getPlayerList().isOp(sp.nameAndId());
    }

    /**
     * The revive menus stay bound to the exact slot the Beacon of Life was used from. Offhand swaps,
     * drags, drops and hotbar swaps all move the stack out of that slot, so a single check covers
     * every way the beacon can leave — and the menu closes instead of acting on a beacon that is
     * no longer there.
     */
    public static boolean beaconStillBound(Player player, int beaconSlot) {
        if (beaconSlot == NO_BEACON) return false;
        var inv = player.getInventory();
        if (beaconSlot < 0 || beaconSlot >= inv.getContainerSize()) return false;
        return inv.getItem(beaconSlot).getItem() == Items.BEACON_OF_LIFE;
    }

    /** Closes the menu immediately rather than waiting for the next {@code stillValid} tick. */
    public static void closeIfBeaconMoved(AbstractContainerMenu menu, Player player, int beaconSlot) {
        if (beaconSlot == NO_BEACON) return;
        if (beaconStillBound(player, beaconSlot)) return;
        if (player instanceof ServerPlayer sp && sp.containerMenu == menu) sp.closeContainer();
    }
}

