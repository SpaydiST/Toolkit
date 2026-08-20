package org.example1.toolkit.light;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example1.toolkit.Config;

public final class InvisibleLightTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private static final int RANGE = 16;
    private static final double RANGE_SQ = RANGE * (double) RANGE;

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
            String worldName = world.getName();
            double cx = player.getEyeLocation().getX();
            double cy = player.getEyeLocation().getY();
            double cz = player.getEyeLocation().getZ();

            for (String key : InvisibleLightListener.LIGHT_BLOCKS) {
                String[] p = key.split(":");
                if (p.length != 4 || !p[0].equals(worldName)) continue;
                try {
                    int bx = Integer.parseInt(p[1]);
                    int by = Integer.parseInt(p[2]);
                    int bz = Integer.parseInt(p[3]);
                    double dx = bx + 0.5 - cx;
                    double dy = by + 0.5 - cy;
                    double dz = bz + 0.5 - cz;
                    if (dx * dx + dy * dy + dz * dz > RANGE_SQ) continue;
                    world.spawnParticle(Particle.END_ROD, bx + 0.5, by + 0.5, bz + 0.5,
                            3, 0.15, 0.15, 0.15, 0.01);
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}
