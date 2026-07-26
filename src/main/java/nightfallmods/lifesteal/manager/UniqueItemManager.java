package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class UniqueItemManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-legendaries.json";
    private static final String UNIQUE_TAG = "unique";
    private static final String EGG_ID = "minecraft:dragon_egg";

    private static Path dataPath;

    private static Map<String, Boolean> existing = new TreeMap<>();

    public static void onServerStarted(MinecraftServer server) {
        dataPath = FabricLoader.getInstance().getConfigDir().resolve(DATA_FILE);
        load();
        Lifesteal.LOGGER.info("[Legendaries] Unique items {} — state: {}",
                ServerConfig.getInstance().uniqueItems, existing);
    }

    public static boolean blocksCrafting(ItemStack result) {
        if (result.isEmpty()) return false;
        String id = idOf(result);
        if (ServerConfig.getInstance().bannedItems.contains(id)) return true;
        if (isUnique(result)) return false;
        return isConfiguredUnique(id) && exists(id);
    }

    public static void tagCrafted(ItemStack result) {
        if (result.isEmpty() || isUnique(result)) return;
        String id = idOf(result);
        if (EGG_ID.equals(id) || !isConfiguredUnique(id) || exists(id)) return;
        applyTag(result, id);
        setExists(id, true);
        Lifesteal.LOGGER.info("[Legendaries] Unique {} crafted — recipe locked.", id);
    }

    public static boolean noteStack(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() == Items.DRAGON_EGG) {
            if (ServerConfig.getInstance().uniqueDragonEgg && !exists(EGG_ID)) setExists(EGG_ID, true);
            return false;
        }

        if (isUnique(stack)) {
            String tagged = getUniqueId(stack);
            if (!tagged.isEmpty() && !exists(tagged)) setExists(tagged, true);
            return false;
        }

        String id = idOf(stack);
        if (!isConfiguredUnique(id)) return false;
        applyTag(stack, id);
        setExists(id, true);
        Lifesteal.LOGGER.info("[Legendaries] Adopted untagged {} as the unique one.", id);
        return true;
    }

    public static void onUniqueDestroyed(ItemStack stack) {
        if (stack.isEmpty()) return;

        String id = stack.getItem() == Items.DRAGON_EGG ? EGG_ID : getUniqueId(stack);
        if (id.isEmpty() || !exists(id)) return;

        setExists(id, false);
        if (EGG_ID.equals(id)) {
            Lifesteal.LOGGER.info("[Legendaries] Dragon Egg destroyed — defeat the Ender Dragon to earn a new one.");
        } else {
            Lifesteal.LOGGER.info("[Legendaries] Unique {} destroyed — recipe reset.", id);
        }
    }

    public static boolean isTrackedUnique(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == Items.DRAGON_EGG) return ServerConfig.getInstance().uniqueDragonEgg;
        return isUnique(stack);
    }

    public static void onDragonDefeated(MinecraftServer server) {
        if (!ServerConfig.getInstance().uniqueDragonEgg || exists(EGG_ID)) return;

        ServerLevel overworld = server.overworld();
        overworld.getChunk(0, 0);
        int y = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0);
        BlockPos pos = new BlockPos(0, y, 0);
        overworld.setBlockAndUpdate(pos, Blocks.DRAGON_EGG.defaultBlockState());

        setExists(EGG_ID, true);

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("The Dragon Egg has appeared at (0, " + y + ", 0) in the overworld!")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                false
        );
        Lifesteal.LOGGER.info("[Legendaries] Dragon defeated — placed unique Dragon Egg at {}.", pos);
    }

    private static String idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static boolean isConfiguredUnique(String id) {
        return ServerConfig.getInstance().uniqueItems.contains(id);
    }

    private static boolean exists(String id) {
        return existing.getOrDefault(id, false);
    }

    private static void setExists(String id, boolean value) {
        existing.put(id, value);
        save();
    }

    public static boolean isUnique(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        return tag != null && tag.contains(UNIQUE_TAG);
    }

    public static String getUniqueId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        CompoundTag tag = data.copyTag();
        if (tag == null || !tag.contains(UNIQUE_TAG)) return "";
        String value = tag.getString(UNIQUE_TAG).orElse("");
        return switch (value) {
            case "mace" -> "minecraft:mace";
            case "chestplate" -> "minecraft:netherite_chestplate";
            default -> value;
        };
    }

    private static ItemStack applyTag(ItemStack stack, String id) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putString(UNIQUE_TAG, id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<TreeMap<String, Boolean>>(){}.getType();
            Map<String, Boolean> loaded = GSON.fromJson(reader, type);
            if (loaded != null) existing = new TreeMap<>(loaded);
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[Legendaries] Failed to load state. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(existing, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Legendaries] Failed to save state. ({})", e.getMessage());
        }
    }
}

