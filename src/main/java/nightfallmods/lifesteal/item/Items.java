package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.Registry;
import net.minecraft.ChatFormatting;

import java.util.List;

public class Items {

    public static Item HEART;
    public static Item CRAFTED_HEART;
    public static Item HEART_FRAGMENT;
    public static Item BEACON_OF_LIFE;

    public static void registerModItems() {
        int cap = ServerConfig.getInstance().craftedHeartCap;

        HEART = register("heart", new Heart(settings()
                .component(DataComponents.ITEM_NAME, name("Heart", ChatFormatting.DARK_RED))
                .component(DataComponents.LORE, lore(line()
                        .append("Consume to gain a ")
                        .append(colored("Heart", ChatFormatting.DARK_RED))
                        .append(". Kill players to gain more.")))));

        CRAFTED_HEART = register("crafted_heart", new CraftedHeart(settings()
                .component(DataComponents.ITEM_NAME, name("Crafted Heart", ChatFormatting.GOLD))
                .component(DataComponents.LORE, lore(line()
                        .append("Consume to gain a ")
                        .append(colored("Heart", ChatFormatting.DARK_RED))
                        .append(". Cannot be applied above ")
                        .append(colored(cap + " hearts", ChatFormatting.GOLD))
                        .append(".")))));

        HEART_FRAGMENT = register("heart_fragment", new Item(settings()
                .component(DataComponents.ITEM_NAME, name("Heart Fragment", ChatFormatting.RED))
                .component(DataComponents.LORE, lore(line()
                        .append("A ")
                        .append(colored("fragment", ChatFormatting.RED))
                        .append(" of a ")
                        .append(colored("Heart", ChatFormatting.DARK_RED))
                        .append(", used to make ")
                        .append(colored("Crafted Hearts", ChatFormatting.GOLD))
                        .append(".")))));

        BEACON_OF_LIFE = register("beacon_of_life", new BeaconOfLife(settings()
                .component(DataComponents.ITEM_NAME, name("Beacon of Life", ChatFormatting.LIGHT_PURPLE))
                .component(DataComponents.LORE, lore(line()
                        .append(colored("Revives", ChatFormatting.LIGHT_PURPLE))
                        .append(" a banned player.")))));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(HEART);
            entries.accept(CRAFTED_HEART);
            entries.accept(HEART_FRAGMENT);
            entries.accept(BEACON_OF_LIFE);
        });

        Lifesteal.LOGGER.info("[Items] Registered 4 items.");
    }

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name), item);
    }

    private static Item.Properties settings() {
        return new Item.Properties();
    }

    private static Component name(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    private static MutableComponent line() {
        return Component.empty().withStyle(style ->
                style.withColor(ChatFormatting.GRAY).withItalic(false));
    }

    private static Component colored(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    private static ItemLore lore(Component lineComponent) {
        return new ItemLore(List.of(lineComponent));
    }
}
