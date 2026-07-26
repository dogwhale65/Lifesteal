package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.ChatFormatting;

public class Items {

    // CreativeModeTabs' keys are private on 1.20.1, so the vanilla "ingredients" key is rebuilt here.
    private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("ingredients"));

    public static Item HEART;
    public static Item CRAFTED_HEART;
    public static Item HEART_FRAGMENT;
    public static Item BEACON_OF_LIFE;

    public static void registerModItems() {
        // 1.20.1 has no item components: display names come from the lang file (with legacy
        // colour codes) and lore is supplied by each item class' appendHoverText override.
        HEART          = register("heart",          new Heart(new Item.Properties()));
        CRAFTED_HEART  = register("crafted_heart",  new CraftedHeart(new Item.Properties()));
        HEART_FRAGMENT = register("heart_fragment", new HeartFragment(new Item.Properties()));
        BEACON_OF_LIFE = register("beacon_of_life", new BeaconOfLife(new Item.Properties()));

        ItemGroupEvents.modifyEntriesEvent(INGREDIENTS_TAB).register(entries -> {
            entries.accept(HEART);
            entries.accept(CRAFTED_HEART);
            entries.accept(HEART_FRAGMENT);
            entries.accept(BEACON_OF_LIFE);
        });

        Lifesteal.LOGGER.info("[Items] Registered 4 items.");
    }

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Constants.MOD_ID, name), item);
    }

    static MutableComponent line() {
        return Component.empty().withStyle(style ->
                style.withColor(ChatFormatting.GRAY).withItalic(false));
    }

    static Component colored(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }
}
