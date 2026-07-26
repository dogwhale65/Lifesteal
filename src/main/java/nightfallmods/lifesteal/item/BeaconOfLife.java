package nightfallmods.lifesteal.item;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BeaconOfLife extends LifestealItem {

    public BeaconOfLife(Properties properties) {
        super(properties, ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected Component loreLine() {
        return line()
                .append(colored("Revives", ChatFormatting.LIGHT_PURPLE))
                .append(" a banned player.");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(itemStack);

        ServerPlayer player = (ServerPlayer) user;

        MenuProvider factory = new SimpleMenuProvider(
                (syncId, inventory, p) -> new ReviveScreenHandler(syncId, inventory, ((ServerLevel) level).getServer()),
                Component.literal("Revive a Player")
        );
        player.openMenu(factory);
        return InteractionResultHolder.success(itemStack);
    }
}
