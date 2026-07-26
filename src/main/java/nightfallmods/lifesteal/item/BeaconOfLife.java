package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.screen.ReviveScreenHandler;
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
import net.minecraft.world.level.Level;

public class BeaconOfLife extends Item {

    public BeaconOfLife(Properties properties) {
        super(properties);
    }

    // Item#use returns InteractionResultHolder here; the unified InteractionResult return arrives in 1.21.2.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        ServerPlayer player = (ServerPlayer) user;

        MenuProvider factory = new SimpleMenuProvider(
                (syncId, inventory, p) -> new ReviveScreenHandler(syncId, inventory, ((ServerLevel) level).getServer()),
                Component.literal("Revive a Player")
        );
        player.openMenu(factory);
        return InteractionResultHolder.success(stack);
    }
}

