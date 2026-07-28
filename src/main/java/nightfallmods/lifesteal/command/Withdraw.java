package nightfallmods.lifesteal.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.item.Items;
import nightfallmods.lifesteal.manager.CraftedHeartTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Withdraw {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("withdraw")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(Withdraw::execute))
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use /withdraw."));
            return 0;
        }

        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            source.sendFailure(Component.literal("Could not read health attribute."));
            return 0;
        }

        ServerConfig cfg = ServerConfig.getInstance();

        int requested = IntegerArgumentType.getInteger(ctx, "amount");
        int currentHearts = (int) Math.floor(attr.getBaseValue() / Constants.HEART_VALUE);

        // Withdrawing is the other way a player's heart count can fall, so it answers to the same
        // floor as death does — otherwise the minimum would only hold until someone typed /withdraw.
        int floorHearts = cfg.minHeartsEnabled ? Math.max(Constants.MIN_HEARTS, cfg.minHearts) : Constants.MIN_HEARTS;
        int maxWithdrawable = currentHearts - floorHearts;

        if (maxWithdrawable <= 0) {
            source.sendFailure(Component.literal("You do not have enough hearts to withdraw."));
            return 0;
        }

        int amount = Math.min(requested, maxWithdrawable);

        int craftedGiven = switch (cfg.craftedHeartWithdrawMode()) {
            case SPECIFIC -> Math.min(amount, Math.min(CraftedHeartTracker.getCount(player.getUUID()), currentHearts));
            case CAP -> amount - Math.min(amount, Math.max(0, currentHearts - cfg.craftedHeartWithdrawCap));
            case NONE -> 0;
        };
        int naturalGiven = amount - craftedGiven;
        if (cfg.isWithdrawActionPrevent() && !canFit(player, naturalGiven, craftedGiven)) {
            source.sendFailure(Component.literal("Your inventory is too full to withdraw."));
            return 0;
        }

        if (amount != requested)
            player.sendSystemMessage(Component.literal(
                    "You can only withdraw " + amount + " " + heartWord(amount) + "."
            ).withStyle(ChatFormatting.YELLOW));

        attr.setBaseValue(attr.getBaseValue() - (amount * Constants.HEART_VALUE));
        if (player.getHealth() > player.getMaxHealth())
            player.setHealth(player.getMaxHealth());
        CraftedHeartTracker.take(player.getUUID(), craftedGiven);

        give(player, Items.HEART, naturalGiven);
        give(player, Items.CRAFTED_HEART, craftedGiven);

        String summary = "Withdrew " + amount + " " + heartWord(amount)
                + (craftedGiven > 0 ? " (" + craftedGiven + " crafted)" : "") + ".";
        player.sendSystemMessage(Component.literal(summary).withStyle(ChatFormatting.GREEN));

        return amount;
    }

    private static String heartWord(int amount) {
        return amount == 1 ? "heart" : "hearts";
    }

    private static void give(ServerPlayer player, Item item, int count) {
        if (count <= 0) return;
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    private static boolean canFit(ServerPlayer player, int hearts, int craftedHearts) {
        var inv = player.getInventory();
        int heartSpace = 0, craftedSpace = 0, emptySlots = 0;

        // getContainerSize() is 41 here (36 main + armor + offhand), but Inventory#add only fills
        // the 36 main slots — counting the equipment slots would overstate the free space.
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty())                            emptySlots++;
            else if (stack.getItem() == Items.HEART)         heartSpace   += stack.getMaxStackSize() - stack.getCount();
            else if (stack.getItem() == Items.CRAFTED_HEART) craftedSpace += stack.getMaxStackSize() - stack.getCount();
        }

        int slotsNeeded = 0;
        if (hearts > heartSpace)          slotsNeeded += slotsFor(hearts - heartSpace);
        if (craftedHearts > craftedSpace) slotsNeeded += slotsFor(craftedHearts - craftedSpace);
        return slotsNeeded <= emptySlots;
    }

    private static int slotsFor(int overflow) {
        return (overflow + 63) / 64;
    }
}

