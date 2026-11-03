package org.example1.toolkit.tnthat;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.example1.toolkit.Config;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public final class TntHat implements Listener {
    /** Public marker (PDC, BYTE=1) on the hat item: {@code toolkit:tnthead}. Other plugins may read it. */
    public static final String HAT_KEY_NAME = "tnthead";

    private static NamespacedKey hatKey;
    private static NamespacedKey legacyHatKey;
    private static NamespacedKey primedKey;
    private static NamespacedKey recipeKey;
    private final JavaPlugin plugin;
    private final Set<UUID> pendingPlayerKnockback = new HashSet<>();

    public TntHat(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void register(JavaPlugin plugin) {
        TntHat.hatKey = new NamespacedKey(plugin, HAT_KEY_NAME);
        TntHat.legacyHatKey = new NamespacedKey(plugin, "tnt-hat");
        TntHat.primedKey = new NamespacedKey(plugin, "tnt-hat-explosion");
        TntHat.recipeKey = new NamespacedKey(plugin, "tnt-hat");
        Bukkit.removeRecipe(recipeKey);
        if (!Config.TNT_HAT_ENABLED) return;
        ItemStack result = createHat();
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, result)
                .addIngredient(new RecipeChoice.MaterialChoice(Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .filter(material -> material.name().endsWith("_HELMET"))
                        .toList()))
                .addIngredient(Material.TNT);
        Bukkit.addRecipe(recipe);
    }

    public static void unregister() {
        if (recipeKey != null) Bukkit.removeRecipe(recipeKey);
        hatKey = null;
        legacyHatKey = null;
        primedKey = null;
        recipeKey = null;
    }

    /** True for the TNT hat; also accepts items created before the key was renamed to {@code tnthead}. */
    public static boolean isHat(ItemStack item) {
        if (item == null || item.getType() != Material.TNT || hatKey == null) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        return pdc.has(hatKey, PersistentDataType.BYTE) || pdc.has(legacyHatKey, PersistentDataType.BYTE);
    }

    private static ItemStack createHat() {
        ItemStack hat = new ItemStack(Material.TNT);
        hat.editMeta(meta -> {
            meta.getPersistentDataContainer().set(hatKey, PersistentDataType.BYTE, (byte) 1);
            meta.setMaxStackSize(1);
            meta.displayName(Component.text(Config.TNT_HAT_NAME, NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.List.of(Component.text(Config.TNT_HAT_LORE, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
        });
        hat.setData(DataComponentTypes.EQUIPPABLE,
                Equippable.equippable(EquipmentSlot.HEAD).swappable(true).build());
        return hat;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!Config.TNT_HAT_ENABLED
                || !(event.getEntity() instanceof Player player) || event.getFinalDamage() <= 0) return;
        ItemStack helmet = player.getInventory().getHelmet();
        if (!isHat(helmet)) return;

        player.getInventory().setHelmet(null);
        var location = player.getLocation().clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!location.isWorldLoaded()) return;
            location.getWorld().spawn(location, TNTPrimed.class, primed -> {
                primed.setFuseTicks(0);
                primed.setYield((float) Config.TNT_HAT_YIELD);
                primed.setIsIncendiary(false);
                primed.setSource(player);
                primed.getPersistentDataContainer().set(primedKey, PersistentDataType.BYTE, (byte) 1);
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrimedTntDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getDamager() instanceof TNTPrimed tnt)
                || !tnt.getPersistentDataContainer().has(primedKey, PersistentDataType.BYTE)) return;
        UUID playerId = player.getUniqueId();
        pendingPlayerKnockback.add(playerId);
        plugin.getServer().getScheduler().runTask(plugin, () -> pendingPlayerKnockback.remove(playerId));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTntKnockback(EntityKnockbackEvent event) {
        if (event.getCause() != EntityKnockbackEvent.Cause.EXPLOSION
                || !(event.getEntity() instanceof Player player)) return;
        // Источник толчка известен напрямую — не зависим от порядка событий урона и отбрасывания.
        boolean fromHat = primedKey != null
                && event instanceof EntityPushedByEntityAttackEvent pushed
                && pushed.getPushedBy().getPersistentDataContainer().has(primedKey, PersistentDataType.BYTE);
        boolean pending = pendingPlayerKnockback.remove(player.getUniqueId());
        if (!fromHat && !pending) return;
        if (event.isCancelled()) return;
        Vector boosted = event.getKnockback().multiply(Config.TNT_HAT_KNOCKBACK_MULTIPLIER);
        event.setKnockback(boosted);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHatExplode(EntityExplodeEvent event) {
        if (Config.TNT_HAT_BREAK_BLOCKS || primedKey == null
                || !event.getEntity().getPersistentDataContainer().has(primedKey, PersistentDataType.BYTE)) return;
        event.blockList().clear();
    }
}
