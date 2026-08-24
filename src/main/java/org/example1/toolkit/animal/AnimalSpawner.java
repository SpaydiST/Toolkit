package org.example1.toolkit.animal;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import io.papermc.paper.entity.Leashable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AnimalSpawner {

    public static final String MARKED_TAG = "_m";
    public static final String DOMESTIC_TAG = "_d";

    private static boolean enabled;
    private static float spawningChunkChance;
    private static int minSpawnTries;
    private static int maxSpawnTries;
    private static List<AnimalSpawnRule> spawnRules = new ArrayList<>();

    private AnimalSpawner() {}

    public static void reload(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        enabled = c.getBoolean("animal-spawning.enabled", false);
        spawningChunkChance = (float) c.getDouble("animal-spawning.spawning-chunk-chance", 0.65);
        minSpawnTries = c.getInt("animal-spawning.spawn-tries.min", 1);
        maxSpawnTries = Math.abs(c.getInt("animal-spawning.spawn-tries.max", 5) + 1);
        spawnRules = AnimalSpawnRule.fromConfig(c.getConfigurationSection("animal-spawning"));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getSpawnCount() {
        if (ThreadLocalRandom.current().nextFloat() > spawningChunkChance) return 0;
        return minSpawnTries + ThreadLocalRandom.current().nextInt(maxSpawnTries);
    }

    public static List<AnimalSpawnRule> getSpawnRules() {
        return spawnRules;
    }

    public static void markDomestic(Entity entity) {
        entity.addScoreboardTag(DOMESTIC_TAG);
    }

    public static boolean shouldDespawn(Entity entity) {
        if (!entity.getScoreboardTags().contains(MARKED_TAG)) return false;
        if (entity.getScoreboardTags().contains(DOMESTIC_TAG)) return false;
        if (entity.getCustomName() != null) return false;
        if (entity.getVehicle() != null || !entity.getPassengers().isEmpty()) return false;
        if (entity instanceof Tameable tame && tame.isTamed()) return false;
        if (entity instanceof Leashable leashable && leashable.isLeashed()) return false;
        if (entity instanceof LivingEntity living && living.getEquipment() != null) {
            if (!living.getEquipment().getItemInMainHand().isEmpty()) return false;
            if (!living.getEquipment().getItemInOffHand().isEmpty()) return false;
        }
        if (entity instanceof Ageable ageable && !ageable.isAdult()) return false;
        return true;
    }
}
