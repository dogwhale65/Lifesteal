package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.EnchantmentsConfig;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

public class InventoryEnforcer {

    private static final int SCAN_INTERVAL_TICKS = 20;

    private static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(InventoryEnforcer::onTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                scanPlayer(handler.getPlayer()));
        Lifesteal.LOGGER.info("[Enforcer] Inventory enforcer registered.");
    }

    private static void onTick(MinecraftServer server) {
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scanPlayer(player);
        }
    }

    private static void scanPlayer(ServerPlayer player) {
        EnchantmentsConfig ench = EnchantmentsConfig.getInstance();
        List<String> banned = ServerConfig.getInstance().bannedItems;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isBanned(stack, banned)) inv.setItem(i, ItemStack.EMPTY);
            else enforcePolicies(stack, ench);
        }

        for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isBanned(stack, banned)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            } else if (slot == EquipmentSlot.CHEST && stack.is(net.minecraft.world.item.Items.ELYTRA)
                    && ServerConfig.getInstance().disableElytras) {

                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            } else if (enforcePolicies(stack, ench)) {
                player.setItemSlot(slot, stack);
            }
        }

        var ender = player.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) {
            ItemStack stack = ender.getItem(i);
            if (isBanned(stack, banned)) ender.setItem(i, ItemStack.EMPTY);
            else enforcePolicies(stack, ench);
        }
    }

    private static boolean enforcePolicies(ItemStack stack, EnchantmentsConfig ench) {
        boolean changed = UniqueItemManager.noteStack(stack);
        if (ench.enforce) changed |= clampStack(stack, ench);
        return changed;
    }

    private static boolean isBanned(ItemStack stack, List<String> banned) {
        if (stack.isEmpty() || banned.isEmpty()) return false;
        return banned.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static boolean clampStack(ItemStack stack, EnchantmentsConfig cfg) {
        if (stack.isEmpty()) return false;
        boolean changed = clampTagList(stack, ItemStack.TAG_ENCH, cfg);
        changed |= clampTagList(stack, EnchantedBookItem.TAG_STORED_ENCHANTMENTS, cfg);
        return changed;
    }

    /**
     * Item components do not exist on 1.20.2, so enchantments live in the stack's NBT as a list
     * under either "Enchantments" or (for enchanted books) "StoredEnchantments". The list is edited
     * in place rather than round-tripped through {@code EnchantmentHelper.getEnchantments} /
     * {@code setEnchantments}, because that pair does not write back to the stored-enchantment list
     * of an enchanted book and would silently drop the clamp.
     */
    private static boolean clampTagList(ItemStack stack, String key, EnchantmentsConfig cfg) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key, Tag.TAG_LIST)) return false;

        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return false;

        boolean changed = false;

        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(entry);
            if (id == null) continue;

            String idString = id.toString();
            int vanillaMax = vanillaMaxLevel(id);
            int cap = cfg.maxLevelFor(idString, vanillaMax);

            if (cfg.isDisabled(idString) || cap <= 0) {
                list.remove(i);
                changed = true;
            } else if (EnchantmentHelper.getEnchantmentLevel(entry) > cap) {
                EnchantmentHelper.setEnchantmentLevel(entry, cap);
                changed = true;
            }
        }

        if (changed && list.isEmpty()) stack.removeTagKey(key);
        return changed;
    }

    private static int vanillaMaxLevel(ResourceLocation id) {
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        return enchantment == null ? Integer.MAX_VALUE : enchantment.getMaxLevel();
    }
}

