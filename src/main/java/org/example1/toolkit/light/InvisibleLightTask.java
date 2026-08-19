package org.example1.toolkit.light;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example1.toolkit.Config;

public final class InvisibleLightTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private static final int RANGE = 16;

    public InvisibleLightTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!Config.INVISIBLE_LIGHT_ENABLED) continue;

            ItemStack item = player.getInventory().getItemInMainHand();
            if (!InvisibleLightRecipe.isInvisibleLight(item)) {
                item = player.getInventory().getItemInOffHand();
                if (!InvisibleLightRecipe.isInvisibleLight(item)) continue;
            }

            World world = player.getWorld();
            Location center = player.getEyeLocation();
            int cx = center.getBlockX();
            int cy = center.getBlockY();
            int cz = center.getBlockZ();

            for (int x = -RANGE; x <= RANGE; x++) {
                for (int y = -RANGE; y <= RANGE; y++) {
                    for (int z = -RANGE; z <= RANGE; z++) {
                        Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                        if (block.getType() == Material.LIGHT) {
                            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                            world.spawnParticle(Particle.END_ROD, loc, 3, 0.15, 0.15, 0.15, 0.01);
                        }
                    }
                }
            }
        }
    }
}
