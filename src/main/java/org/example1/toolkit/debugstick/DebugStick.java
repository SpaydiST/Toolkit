package org.example1.toolkit.debugstick;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;

public final class DebugStick {

    private static NamespacedKey markerKey;
    private static NamespacedKey usesKey;
    private static NamespacedKey maxUsesKey;
    private static NamespacedKey copiedTypeKey;
    private static NamespacedKey copiedStateKey;

    private DebugStick() {}

    public static void init(JavaPlugin plugin) {
        markerKey = new NamespacedKey(plugin, "debug-stick");
        usesKey = new NamespacedKey(plugin, "debug-stick-uses");
        maxUsesKey = new NamespacedKey(plugin, "debug-stick-max-uses");
        copiedTypeKey = new NamespacedKey(plugin, "debug-stick-copied-type");
        copiedStateKey = new NamespacedKey(plugin, "debug-stick-copied-state");
    }

    public static boolean isDebugStick(ItemStack item) {
        if (item == null || item.getType() != Material.DEBUG_STICK) return false;
        if (item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }

    public static int usesLeft(ItemStack item) {
        if (!isDebugStick(item)) return 0;
        Integer u = item.getItemMeta().getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
        return u == null ? Config.DEBUG_STICK_USES : u;
    }

    private static int maxUses(ItemStack item) {
        if (item.getItemMeta() == null) return Config.DEBUG_STICK_USES;
        Integer u = item.getItemMeta().getPersistentDataContainer().get(maxUsesKey, PersistentDataType.INTEGER);
        return u == null ? Config.DEBUG_STICK_USES : u;
    }

    public static ItemStack createCrafted(int uses) {
        ItemStack item = new ItemStack(Material.DEBUG_STICK);
        mark(item, uses);
        return item;
    }

    public static void mark(ItemStack item, int uses) {
        int u = Math.max(1, uses);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, "1");
            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, u);
            meta.getPersistentDataContainer().set(maxUsesKey, PersistentDataType.INTEGER, u);
            meta.displayName(null);
            meta.lore(java.util.List.of(
                    Component.translatable("item.durability", Component.text(u), Component.text(u))));
        });
    }

    public static void consumeUse(ItemStack item) {
        int left = usesLeft(item) - 1;
        if (left <= 0) {
            item.editMeta(meta -> {
                meta.getPersistentDataContainer().remove(markerKey);
                meta.getPersistentDataContainer().remove(usesKey);
                meta.getPersistentDataContainer().remove(maxUsesKey);
                meta.getPersistentDataContainer().remove(copiedTypeKey);
                meta.getPersistentDataContainer().remove(copiedStateKey);
                meta.displayName(null);
                meta.lore(null);
            });
            item.setType(Material.STICK);
            return;
        }
        int finalUses = left;
        int max = maxUses(item);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, finalUses);
            meta.lore(java.util.List.of(
                    Component.translatable("item.durability", Component.text(finalUses), Component.text(max))));
        });
    }

    public static void saveState(ItemStack item, Block block) {
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(copiedTypeKey, PersistentDataType.STRING, block.getType().name());
            meta.getPersistentDataContainer().set(copiedStateKey, PersistentDataType.STRING, block.getBlockData().getAsString());
        });
    }

    public static boolean hasCopiedState(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(copiedTypeKey, PersistentDataType.STRING);
    }

    public static String getCopiedType(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(copiedTypeKey, PersistentDataType.STRING);
    }

    public static BlockData getCopiedState(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        String str = item.getItemMeta().getPersistentDataContainer().get(copiedStateKey, PersistentDataType.STRING);
        if (str == null) return null;
        try {
            return Bukkit.createBlockData(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearCopiedState(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return;
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(copiedTypeKey);
            meta.getPersistentDataContainer().remove(copiedStateKey);
        });
    }
}
