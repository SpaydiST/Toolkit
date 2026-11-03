package org.example1.toolkit.frames;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.example1.toolkit.Msg;

/**
 * Рецепт: 1 мембрана фантома + 1 рамка = 1 невидимая (полупрозрачная) рамка.
 */
public final class InvisibleFrameRecipe {

    private static final String MARKER = "invisible-frame";
    private static NamespacedKey markerKey;
    private static NamespacedKey recipeKey;

    private InvisibleFrameRecipe() {}

    public static void register(JavaPlugin plugin) {
        recipeKey = new NamespacedKey(plugin, "invisible_item_frame");
        markerKey = new NamespacedKey(plugin, MARKER);

        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, createResult());
        recipe.addIngredient(Material.PHANTOM_MEMBRANE);
        recipe.addIngredient(Material.ITEM_FRAME);
        Bukkit.addRecipe(recipe);
    }

    public static void unregister() {
        if (recipeKey != null) Bukkit.removeRecipe(recipeKey);
    }

    public static boolean isInvisibleFrame(ItemStack item) {
        if (item == null || item.getType() != Material.ITEM_FRAME) return false;
        if (item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }

    public static ItemStack createResult() {
        ItemStack result = new ItemStack(Material.ITEM_FRAME, 1);
        result.editMeta(meta -> {
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, "1");
            meta.displayName(Component.text(Config.INVISIBLE_FRAME_NAME).color(Msg.EMERALD));
        });
        return result;
    }
}