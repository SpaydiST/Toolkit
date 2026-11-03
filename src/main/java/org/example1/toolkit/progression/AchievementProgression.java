package org.example1.toolkit.progression;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public final class AchievementProgression {

    private static final String NAMESPACE = "server_progression";
    private static final String CRITERION = "complete";

    private AchievementProgression() {
    }

    public static void award(Player player, String achievement) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (achievement == null || achievement.isBlank()) {
            return;
        }

        if (achievement.startsWith("toolkit_") && !"toolkit".equals(achievement)) {
            awardOne(player, "toolkit");
        }

        awardOne(player, achievement);
    }

    private static void awardOne(Player player, String achievement) {

        Advancement advancement = Bukkit.getAdvancement(new NamespacedKey(NAMESPACE, achievement));
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone() && progress.getRemainingCriteria().contains(CRITERION)) {
            progress.awardCriteria(CRITERION);
        }
    }
}
