package org.example1.toolkit.snowgolem;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.example1.toolkit.Config;
import org.example1.toolkit.progression.AchievementProgression;

public final class SnowGolemFreezeListener implements Listener {

    private static final int FREEZE_TICKS = 30;

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!Config.SNOW_GOLEM_FREEZE_ENABLED) return;
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Snowman)) return;
        if (event.getHitEntity() == null) return;

        Entity hit = event.getHitEntity();
        if (hit instanceof LivingEntity living) {
            living.setFreezeTicks(living.getFreezeTicks() + FREEZE_TICKS);
            if (living instanceof Player player) {
                AchievementProgression.award(player, "toolkit_snowgolem");
            }
        }
    }
}
