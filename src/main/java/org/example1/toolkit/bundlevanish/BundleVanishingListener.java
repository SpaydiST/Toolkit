package org.example1.toolkit.bundlevanish;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.example1.toolkit.Config;

import java.util.Map;

public final class BundleVanishingListener implements Listener {

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!Config.BUNDLE_VANISHING_ENABLED) return;
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) return;
        if (left.getType() != Material.BUNDLE) return;
        if (right.getType() != Material.ENCHANTED_BOOK) return;

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
        if (bookMeta == null) return;

        if (!bookMeta.hasStoredEnchant(Enchantment.VANISHING_CURSE)) return;

        if (left.getEnchantmentLevel(Enchantment.VANISHING_CURSE) > 0) return;

        int bookLevel = bookMeta.getStoredEnchantLevel(Enchantment.VANISHING_CURSE);
        ItemStack result = left.clone();
        result.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, bookLevel);

        event.setResult(result);
    }
}
