package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.screen.ReviveScreenHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BeaconOfLife extends Item {

    public BeaconOfLife(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;

        ServerPlayer player = (ServerPlayer) user;

        MenuProvider factory = new SimpleMenuProvider(
                (syncId, inventory, p) -> new ReviveScreenHandler(syncId, inventory, ((ServerLevel) level).getServer()),
                Component.literal("Revive a Player")
        );
        player.openMenu(factory);
        return InteractionResult.SUCCESS;
    }
}

