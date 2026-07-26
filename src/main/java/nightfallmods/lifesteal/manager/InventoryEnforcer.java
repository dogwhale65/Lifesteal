package nightfallmods.lifesteal.manager;

import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.EnchantmentsConfig;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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
        boolean changed = clampComponent(stack, DataComponents.ENCHANTMENTS, cfg);
        changed |= clampComponent(stack, DataComponents.STORED_ENCHANTMENTS, cfg);
        return changed;
    }

    private static boolean clampComponent(ItemStack stack, DataComponentType<ItemEnchantments> type, EnchantmentsConfig cfg) {
        ItemEnchantments enchants = stack.get(type);
        if (enchants == null || enchants.isEmpty()) return false;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchants);
        boolean changed = false;

        for (Holder<Enchantment> holder : enchants.keySet()) {
            String id = holder.getRegisteredName();
            if (isRemoved(cfg, holder, id)) continue;
            int cap = cfg.maxLevelFor(id, holder.value().getMaxLevel());
            // keySet() is keyed by Holder, but getLevel/set still take the Enchantment itself on
            // this version — enchantments only become fully holder-based in 1.21.
            if (enchants.getLevel(holder.value()) > cap) {
                mutable.set(holder.value(), cap);
                changed = true;
            }
        }

        int sizeBefore = mutable.keySet().size();
        mutable.removeIf(holder -> isRemoved(cfg, holder, holder.getRegisteredName()));
        if (mutable.keySet().size() != sizeBefore) changed = true;

        if (changed) {
            ItemEnchantments result = mutable.toImmutable();
            if (result.isEmpty()) stack.remove(type);
            else stack.set(type, result);
        }
        return changed;
    }

    private static boolean isRemoved(EnchantmentsConfig cfg, Holder<Enchantment> holder, String id) {
        return cfg.isDisabled(id) || cfg.maxLevelFor(id, holder.value().getMaxLevel()) <= 0;
    }
}

