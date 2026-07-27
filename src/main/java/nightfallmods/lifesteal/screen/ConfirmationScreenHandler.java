package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import net.minecraft.ChatFormatting;
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

public class ConfirmationScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final Player player;
    private final MinecraftServer server;
    private final String targetName;
    private final ReviveSort returnSort;
    private final ReviveLogic logic;
    private final BeaconBinding beacon;

    public ConfirmationScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server,
                                     String targetName, boolean targetBanned, ReviveSort returnSort,
                                     BeaconBinding beacon) {
        super(MenuType.GENERIC_9x3, syncId);
        this.player     = playerInventory.player;
        this.server     = server;
        this.targetName = targetName;
        this.returnSort = returnSort;
        this.beacon     = beacon;
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
        return ReviveScreenHandler.stillValid(player, beacon);
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
        if (!stack.has(DataComponents.CUSTOM_NAME)) return;
        String name = stack.get(DataComponents.CUSTOM_NAME).getString();

        if (name.equals("Revive " + targetName)) {
            confirmRevive();
        } else if (name.equals("Cancel")) {
            returnToList();
        }
    }

    private void confirmRevive() {
        if (!(player instanceof ServerPlayer sp)) return;

        // Re-validate the bound beacon at confirm time, not at open time. The menu can stay open
        // across any number of ticks, so the player may have dropped, stashed or otherwise
        // lost the beacon since it was checked — revive must fail closed in that case.
        if (beacon != null && !beacon.stillHeld(player)) {
            sp.sendSystemMessage(Component.literal(
                    "You no longer have a Beacon of Life.").withStyle(ChatFormatting.RED));
            sp.closeContainer();
            return;
        }

        if (logic.revivePlayer(targetName)) {
            // Consume in the same tick as the revive so the two can never be separated.
            if (beacon != null) {
                beacon.consume(player);
                Lifesteal.LOGGER.info("[Revival] Beacon of Life consumed by {}.", player.getName().getString());
            }
            sp.sendSystemMessage(Component.literal("Revived " + targetName + ".").withStyle(ChatFormatting.GREEN));
            sp.closeContainer();
        } else {
            sp.sendSystemMessage(Component.literal("Failed to revive " + targetName + ".").withStyle(ChatFormatting.RED));
            returnToList();
        }
    }

    private void returnToList() {
        if (!(player instanceof ServerPlayer sp)) return;
        ReviveSort sortToRestore = returnSort;
        MinecraftServer srv = server;
        BeaconBinding beaconToRestore = beacon;
        sp.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReviveScreenHandler(syncId, inv, srv, sortToRestore, beaconToRestore),
                Component.literal("Revive a Player")
        ));
    }
}

