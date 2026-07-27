package nightfallmods.lifesteal.command;

import com.mojang.brigadier.context.CommandContext;
import nightfallmods.lifesteal.screen.BeaconAnchor;
import nightfallmods.lifesteal.screen.ReviveScreenHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;

public class Revive {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("revive")
                                .requires(source -> source.hasPermission(3))
                                .executes(Revive::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can open the revive menu."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        MenuProvider factory = new SimpleMenuProvider(
                (syncId, inventory, p) -> new ReviveScreenHandler(syncId, inventory, server, BeaconAnchor.none()),
                Component.literal("Revive a Player")
        );
        player.openMenu(factory);
        return 1;
    }
}

