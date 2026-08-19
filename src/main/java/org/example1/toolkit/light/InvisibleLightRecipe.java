package org.example1.toolkit.light;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;

public final class InvisibleLightRecipe {

    private static final String MARKER = "invisible-light";
    private static final String LEVEL_KEY = "invisible-light-level";
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 15;
    public static final int DEFAULT_LEVEL = 15;

    private static NamespacedKey markerKey;
    private static NamespacedKey levelKey;
    private static NamespacedKey recipeKey;

    private InvisibleLightRecipe() {}

    public static void register(JavaPlugin plugin) {
        recipeKey = new NamespacedKey(plugin, "invisible_light");
        markerKey = new NamespacedKey(plugin, MARKER);
        levelKey = new NamespacedKey(plugin, LEVEL_KEY);

        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, createResult(Config.INVISIBLE_LIGHT_DEFAULT_LEVEL, 4));
        recipe.addIngredient(4, Material.GLOWSTONE_DUST);
        recipe.addIngredient(Material.GLASS);
        Bukkit.addRecipe(recipe);
    }

    public static void unregister() {
        if (recipeKey != null) Bukkit.removeRecipe(recipeKey);
    }

    public static Material getItemMaterial() {
        return Material.LIGHT;
    }

    public static boolean isInvisibleLight(ItemStack item) {
        if (item == null || item.getType() != getItemMaterial()) return false;
        if (item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }

    public static int getLevel(ItemStack item) {
        if (!isInvisibleLight(item)) return DEFAULT_LEVEL;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level != null ? level : DEFAULT_LEVEL;
    }

    public static void setLevel(ItemStack item, int level) {
        if (!isInvisibleLight(item)) return;
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
            meta.displayName(buildDisplayName(level));
        });
    }

    public static final float MODEL_DATA = 1001f;

    public static ItemStack createResult(int level, int count) {
        ItemStack result = new ItemStack(getItemMaterial(), count);
        result.editMeta(meta -> {
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, "1");
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
            meta.displayName(buildDisplayName(level));
        });
        result.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addFloat(MODEL_DATA).build());
        return result;
    }

    private static Component buildDisplayName(int level) {
        return                 Component.text("Невидимый свет", NamedTextColor.YELLOW)
                .append(Component.text(" [" + level + "]", NamedTextColor.GRAY));
    }
}
