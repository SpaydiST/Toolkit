package org.example1.toolkit.frames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт: 8 рамок + зелье невидимости = 8 невидимых рамок.
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
        recipe.addIngredient(8, Material.ITEM_FRAME);
        recipe.addIngredient(invisibilityPotionChoice());
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
        ItemStack result = new ItemStack(Material.ITEM_FRAME, 8);
        result.editMeta(meta -> {
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, "1");
            meta.displayName(Component.translatable("block.minecraft.item_frame")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(" "))
                    .append(Component.translatable("effect.minecraft.invisibility")
                            .color(NamedTextColor.GRAY)));
        });
        return result;
    }

    /** Любое зелье невидимости (обычное/длинное, обычное/брызги/туман) — чтобы рецепт срабатывал всегда. */
    private static RecipeChoice invisibilityPotionChoice() {
        List<ItemStack> choices = new ArrayList<>();
        for (PotionType type : new PotionType[]{PotionType.INVISIBILITY, PotionType.LONG_INVISIBILITY}) {
            addPotion(choices, Material.POTION, type);
            addPotion(choices, Material.SPLASH_POTION, type);
            addPotion(choices, Material.LINGERING_POTION, type);
        }
        return new RecipeChoice.ExactChoice(choices);
    }

    private static void addPotion(List<ItemStack> list, Material material, PotionType type) {
        ItemStack potion = new ItemStack(material);
        potion.editMeta(PotionMeta.class, meta -> meta.setBasePotionType(type));
        list.add(potion);
    }
}
