package org.example1.toolkit.temple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.example1.toolkit.Config;

import java.util.HashMap;
import java.util.Map;

public final class JungleTempleListener implements Listener {

    private static final int TRAP_RANGE = 16;
    private final Map<Location, Long> recentDispenserFires = new HashMap<>();

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (!Config.JUNGLE_TEMPLE_ARROWS_ENABLED) return;
        if (!isArrow(event.getItem())) return;
        if (!(event.getBlock().getState() instanceof Dispenser dispenser)) return;
        if (!(dispenser.getBlock().getBlockData().getAsString().contains("dispenser"))) return;

        if (!isInJungleTemple(event.getBlock())) return;

        recentDispenserFires.put(event.getBlock().getLocation(), System.currentTimeMillis());
    }

    @EventHandler
    public void onArrowSpawn(EntitySpawnEvent event) {
        if (!Config.JUNGLE_TEMPLE_ARROWS_ENABLED) return;
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();
        Location arrowLoc = arrow.getLocation();

        long now = System.currentTimeMillis();
        recentDispenserFires.entrySet().removeIf(e -> now - e.getValue() > 1000);

        for (Map.Entry<Location, Long> entry : recentDispenserFires.entrySet()) {
            if (entry.getKey().getWorld() != arrowLoc.getWorld()) continue;
            if (entry.getKey().distance(arrowLoc) < TRAP_RANGE) {
                arrow.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 0), true);
                return;
            }
        }
    }

    private boolean isArrow(ItemStack item) {
        Material m = item.getType();
        return m == Material.ARROW || m == Material.TIPPED_ARROW || m == Material.SPECTRAL_ARROW;
    }

    private boolean isInJungleTemple(Block block) {
        Location loc = block.getLocation();
        var world = loc.getWorld();
        if (world == null) return false;

        var result = world.locateNearestStructure(loc, org.bukkit.generator.structure.StructureType.JUNGLE_TEMPLE, 100, false);
        if (result != null) {
            Location center = result.getLocation();
            if (center.getWorld() == loc.getWorld() &&
                    Math.abs(center.getBlockX() - loc.getBlockX()) < 30 &&
                    Math.abs(center.getBlockZ() - loc.getBlockZ()) < 30) {
                return true;
            }
        }
        return false;
    }
}
