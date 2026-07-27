package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Shared refusal messages for the heart items, so both caps read the same way to a player. */
final class HeartMessages {
    private HeartMessages() {}

    static Component maxHearts(ServerConfig cfg) {
        return Component.literal("You cannot apply more than " + cfg.maxHearts + " hearts.")
                .withStyle(ChatFormatting.RED);
    }

    static Component craftedHeartCap(ServerConfig cfg) {
        return Component.literal("Crafted Hearts cannot raise your health beyond "
                        + cfg.craftedHeartCap + " hearts.")
                .withStyle(ChatFormatting.RED);
    }
}
