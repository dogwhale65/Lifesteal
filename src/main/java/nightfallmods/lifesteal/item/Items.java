package nightfallmods.lifesteal.item;

import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class Items {

    public static Item HEART;
    public static Item CRAFTED_HEART;
    public static Item HEART_FRAGMENT;
    public static Item BEACON_OF_LIFE;

    public static void registerModItems() {
        HEART          = register("heart", new Heart(new Item.Properties()));
        CRAFTED_HEART  = register("crafted_heart", new CraftedHeart(new Item.Properties()));
        HEART_FRAGMENT = register("heart_fragment", new HeartFragmentItem(new Item.Properties()));
        BEACON_OF_LIFE = register("beacon_of_life", new BeaconOfLife(new Item.Properties()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
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
}
