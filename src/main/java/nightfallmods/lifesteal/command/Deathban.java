package nightfallmods.lifesteal.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import nightfallmods.lifesteal.manager.DeathEventHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

import java.util.Optional;
import java.util.UUID;

public class Deathban {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("deathban")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(Deathban::execute))
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        String input = StringArgumentType.getString(ctx, "target");

        GameProfile profile = resolve(server, input);
        if (profile == null) {
            source.sendFailure(Component.literal("Could not find a player matching '" + input + "'."));
            return 0;
        }

        if (server.getPlayerList().getBans().isBanned(profile)) {
            source.sendFailure(Component.literal(profile.getName() + " is already deathbanned."));
            return 0;
        }

        DeathEventHandler.deathban(server, profile);
        source.sendSuccess(
                () -> Component.literal("Deathbanned " + profile.getName() + ".").withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static GameProfile resolve(MinecraftServer server, String input) {
        GameProfileCache cache = server.getProfileCache();

        try {
            UUID uuid = UUID.fromString(input);
            if (cache != null) {
                Optional<GameProfile> byId = cache.get(uuid);
                if (byId.isPresent()) return byId.get();
            }
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online != null) return online.getGameProfile();

            return new GameProfile(uuid, uuid.toString());
        } catch (IllegalArgumentException notAUuid) {

        }

        if (cache != null) {
            Optional<GameProfile> byName = cache.get(input);
            if (byName.isPresent()) return byName.get();
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(input);
        if (online != null) return online.getGameProfile();
        return null;
    }
}

