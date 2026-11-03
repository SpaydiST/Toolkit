package org.example1.toolkit.animal;

import org.bukkit.GameRules;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AnimalSpawnerListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!AnimalSpawner.isEnabled()) return;

        int count = AnimalSpawner.getSpawnCount();
        if (count < 1) return;

        var chunk = event.getChunk();
        if (Boolean.FALSE.equals(chunk.getWorld().getGameRuleValue(GameRules.SPAWN_MOBS))) return;

        var rules = AnimalSpawner.getSpawnRules();
        if (rules.isEmpty()) return;

        var world = chunk.getWorld();
        int animalsInChunk = 0;
        for (var entity : chunk.getEntities()) {
            if (entity instanceof Animals) animalsInChunk++;
        }
        int remainingSlots = AnimalSpawner.getMaxAnimalsPerChunk() - animalsInChunk;
        if (remainingSlots <= 0) return;

        count = Math.min(count, remainingSlots);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            Block ground = AnimalSpawnRule.findSpawnGround(world, x, z);
            if (ground == null) continue;

            List<AnimalSpawnRule> applicable = new ArrayList<>();
            for (AnimalSpawnRule rule : rules) {
                if (rule.matches(world, ground)) applicable.add(rule);
            }
            if (applicable.isEmpty()) continue;

            AnimalSpawnRule rule = applicable.get(random.nextInt(applicable.size()));
            rule.spawnAt(world, ground);
        }
    }

    @EventHandler
    public void onEntityEnterLoveMode(EntityEnterLoveModeEvent event) {
        AnimalSpawner.markDomestic(event.getEntity());
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        AnimalSpawner.markDomestic(event.getEntity());
        if (event.getMother() != null) AnimalSpawner.markDomestic(event.getMother());
        if (event.getFather() != null) AnimalSpawner.markDomestic(event.getFather());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        if (!AnimalSpawner.isEnabled()) return;
        for (var entity : event.getEntities()) {
            if (AnimalSpawner.shouldDespawn(entity)) {
                entity.remove();
            }
        }
    }
}
