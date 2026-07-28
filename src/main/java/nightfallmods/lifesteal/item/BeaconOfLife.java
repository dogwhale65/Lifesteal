package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.screen.BeaconAnchor;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        ServerPlayer player = (ServerPlayer) user;

        // Anchor the menu to the stack that was actually used, so any later move of it closes the menu.
        int beaconSlot = BeaconAnchor.slotOf(player, stack);

        MenuProvider factory = new SimpleMenuProvider(
                (syncId, inventory, p) -> new ReviveScreenHandler(syncId, inventory, ((ServerLevel) level).getServer(), beaconSlot),
                Component.literal("Revive a Player")
        );
        player.openMenu(factory);
        return InteractionResultHolder.success(stack);
    }
}
