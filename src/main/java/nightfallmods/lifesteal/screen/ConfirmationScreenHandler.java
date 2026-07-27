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
    private final BeaconAnchor anchor;

    public ConfirmationScreenHandler(int syncId, Inventory playerInventory, MinecraftServer server,
                                     String targetName, boolean targetBanned, ReviveSort returnSort,
                                     BeaconAnchor anchor) {
        super(MenuType.GENERIC_9x3, syncId);
        this.player     = playerInventory.player;
        this.server     = server;
        this.targetName = targetName;
        this.returnSort = returnSort;
        this.anchor     = anchor;
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
        return anchor.isIntact(player)
                && (anchor.isAnchored() || ReviveScreenHandler.isOperator(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (!anchor.isIntact(player)) {
            ReviveScreenHandler.closeMenu(player);
            return;
        }
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
        if (logic.revivePlayer(targetName)) {
            sendTo(Component.literal("Revived " + targetName + ".").withStyle(ChatFormatting.GREEN));
            if (anchor.consume(player)) {
                Lifesteal.LOGGER.info("[Revival] Beacon of Life consumed by {}.", player.getName().getString());
            }
            ReviveScreenHandler.closeMenu(player);
        } else {
            sendTo(Component.literal("Failed to revive " + targetName + ".").withStyle(ChatFormatting.RED));
            returnToList();
        }
    }

    private void sendTo(Component message) {
        if (player instanceof ServerPlayer sp) sp.sendSystemMessage(message);
    }

    private void returnToList() {
        if (!(player instanceof ServerPlayer sp)) return;
        ReviveSort sortToRestore = returnSort;
        MinecraftServer srv = server;
        BeaconAnchor currentAnchor = anchor;
        sp.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReviveScreenHandler(syncId, inv, srv, sortToRestore, currentAnchor),
                Component.literal("Revive a Player")
        ));
    }
}

