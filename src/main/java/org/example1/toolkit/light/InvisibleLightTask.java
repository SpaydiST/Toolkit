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
            double cx = player.getEyeLocation().getX();
            double cy = player.getEyeLocation().getY();
            double cz = player.getEyeLocation().getZ();

            // Только блоки в ближайших чанках, а не все блоки сервера.
            for (long packed : InvisibleLightListener.nearby(world, (int) Math.floor(cx), (int) Math.floor(cz), 1)) {
                int bx = InvisibleLightListener.unpackX(packed);
                int by = InvisibleLightListener.unpackY(packed);
                int bz = InvisibleLightListener.unpackZ(packed);
                double dx = bx + 0.5 - cx;
                double dy = by + 0.5 - cy;
                double dz = bz + 0.5 - cz;
                if (dx * dx + dy * dy + dz * dz > RANGE_SQ) continue;
                if (world.getBlockAt(bx, by, bz).getType() != Material.LIGHT) {
                    InvisibleLightListener.forget(world, bx, by, bz);
                    continue;
                }
                world.spawnParticle(Particle.END_ROD, bx + 0.5, by + 0.5, bz + 0.5,
                        3, 0.15, 0.15, 0.15, 0.01);
            }
        }
    }
}
