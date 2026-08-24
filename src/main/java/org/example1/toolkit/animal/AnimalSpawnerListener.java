package org.example1.toolkit.animal;

import org.bukkit.GameRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

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

        for (int i = 0; i < count; i++) {
            var rule = rules.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(rules.size()));
            rule.trySpawn(chunk);
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
