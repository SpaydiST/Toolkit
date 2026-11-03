package org.example1.toolkit.fire;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;
import org.example1.toolkit.progression.AchievementProgression;

public final class FireElytraTask extends BukkitRunnable {

    private final JavaPlugin plugin;

    public FireElytraTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!Config.FIRE_ELYTRA_ENABLED) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getFireTicks() <= 0) continue;
            if (!player.isGliding()) continue;
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE)) continue;

            ItemStack chest = player.getInventory().getChestplate();
            if (chest == null || chest.getType() != Material.ELYTRA) continue;

            AchievementProgression.award(player, "toolkit_fire");
            player.setGliding(false);
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "fire-elytra-prefix"), Config.COLOR_ERROR),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "fire-elytra-blocked")));
        }
    }
}
