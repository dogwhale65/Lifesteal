package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.manager.StorageRestrictionHandler;
import nightfallmods.lifesteal.screen.ReviveScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BeaconOfLife extends Item {

    public BeaconOfLife(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        ServerPlayer player = (ServerPlayer) user;

        // A refused item frame placement must not also pop the revive menu open.
        if (StorageRestrictionHandler.wasRejectedByItemFrame(player)) return InteractionResultHolder.fail(stack);

        MenuProvider factory = new SimpleMenuProvider(
                (syncId, inventory, p) -> new ReviveScreenHandler(syncId, inventory, ((ServerLevel) level).getServer()),
                Component.literal("Revive a Player")
        );
        player.openMenu(factory);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Items.line()
                .append(Items.colored("Revives", ChatFormatting.LIGHT_PURPLE))
                .append(" a banned player."));
    }
}
