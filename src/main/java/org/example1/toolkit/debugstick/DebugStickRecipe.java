package org.example1.toolkit.debugstick;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;

/**
 * Рецепт стола кузнеца: палка + звезда Незера = дебагстик (1000 использований).
 * Шаблон не требуется (RecipeChoice.empty()).
 */
public final class DebugStickRecipe {

    private static NamespacedKey recipeKey;

    private DebugStickRecipe() {}

    public static void register(JavaPlugin plugin) {
        recipeKey = new NamespacedKey(plugin, "debug_stick");

        ItemStack result = DebugStick.createCrafted(Config.DEBUG_STICK_USES);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                recipeKey,
                result,
                RecipeChoice.empty(),
                new RecipeChoice.MaterialChoice(Material.STICK),
                new RecipeChoice.MaterialChoice(Material.NETHER_STAR));
        Bukkit.addRecipe(recipe);
    }

    public static void unregister() {
        if (recipeKey != null) Bukkit.removeRecipe(recipeKey);
    }
}
