package org.example1.toolkit.bannerhat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public final class BannerHatRecipe {

    public static final float MODEL_DATA = 1002f;
    private static NamespacedKey markerKey;
    private static final Set<Material> BANNERS = EnumSet.of(
            Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER, Material.LIGHT_BLUE_BANNER,
            Material.YELLOW_BANNER, Material.LIME_BANNER, Material.PINK_BANNER, Material.GRAY_BANNER,
            Material.LIGHT_GRAY_BANNER, Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
            Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, Material.BLACK_BANNER
    );

    private BannerHatRecipe() {}

    public static void register(JavaPlugin plugin) {
        markerKey = new NamespacedKey(plugin, "banner-hat");
    }

    public static void unregister() {
        markerKey = null;
    }

    public static boolean isBannerHat(ItemStack item) {
        if (item == null || markerKey == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING);
    }

    public static boolean isBanner(Material material) {
        return BANNERS.contains(material);
    }

    public static ItemStack createBannerHat(ItemStack banner) {
        ItemStack item = banner.clone();
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, "1");
            meta.displayName(Component.text("Шлем-флаг", NamedTextColor.GOLD));
        });
        item.setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HEAD)
                .swappable(false)
                .build());
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addFloat(MODEL_DATA).build());
        return item;
    }
}
