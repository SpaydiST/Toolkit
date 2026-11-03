package org.example1.toolkit.animal;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public record AnimalSpawnRule(
        List<World.Environment> environments,
        List<Biome> biomes,
        List<Material> spawnsOnTopOf,
        List<EntityType> entityTypes
) {
    @SuppressWarnings("unchecked")
    public static List<AnimalSpawnRule> fromConfig(ConfigurationSection section) {
        List<AnimalSpawnRule> rules = new ArrayList<>();
        if (!section.isList("spawn-rules")) return rules;

        List<Map<?, ?>> mapList = section.getMapList("spawn-rules");

        for (Map<?, ?> map : mapList) {
            List<String> envStrings = (List<String>) map.get("environments");
            if (envStrings == null) envStrings = List.of("NORMAL");
            List<World.Environment> environments = envStrings.stream()
                    .map(s -> World.Environment.valueOf(s.toUpperCase()))
                    .toList();

            List<String> biomeStrings = (List<String>) map.get("biomes");
            if (biomeStrings == null) biomeStrings = List.of();
            List<Biome> biomes = biomeStrings.stream()
                    .map(name -> {
                        try {
                            return (Biome) Biome.class.getField(name.toUpperCase()).get(null);
                        } catch (ReflectiveOperationException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            List<String> typeStrings = (List<String>) map.get("types");
            if (typeStrings == null) typeStrings = List.of();
            List<EntityType> entityTypes = typeStrings.stream()
                    .map(s -> {
                        try { return EntityType.valueOf(s.toUpperCase()); }
                        catch (IllegalArgumentException e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            List<String> topStrings = (List<String>) map.get("on-top-of");
            if (topStrings == null) topStrings = List.of("#ANIMALS_SPAWNABLE_ON");
            List<Material> spawnsOnTopOf = new ArrayList<>();
            for (String entry : topStrings) {
                if (entry.startsWith("#")) {
                    String tagName = entry.substring(1).toUpperCase();
                    try {
                        var field = Tag.class.getField(tagName);
                        Object value = field.get(null);
                        if (value instanceof Tag<?> tag) {
                            spawnsOnTopOf.addAll(((Tag<Material>) tag).getValues());
                        }
                    } catch (ReflectiveOperationException ignored) {}
                } else {
                    try {
                        spawnsOnTopOf.add(Material.valueOf(entry.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            if (!biomes.isEmpty() && !entityTypes.isEmpty()) {
                rules.add(new AnimalSpawnRule(environments, biomes, spawnsOnTopOf, entityTypes));
            }
        }
        return rules;
    }

    public boolean trySpawn(Chunk chunk) {
        World world = chunk.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int x = (chunk.getX() << 4) + r.nextInt(16);
        int z = (chunk.getZ() << 4) + r.nextInt(16);
        Block ground = findSpawnGround(world, x, z);
        if (ground == null || !matches(world, ground)) return false;
        return spawnAt(world, ground);
    }

    public boolean matches(World world, Block ground) {
        return environments.contains(world.getEnvironment())
                && biomes.contains(ground.getBiome())
                && (spawnsOnTopOf.isEmpty() || spawnsOnTopOf.contains(ground.getType()));
    }

    public boolean spawnAt(World world, Block ground) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        EntityType type = entityTypes.get(r.nextInt(entityTypes.size()));
        Location loc = ground.getLocation().add(0.5, 1, 0.5);

        var entity = world.spawnEntity(loc, type, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
        entity.addScoreboardTag(AnimalSpawner.MARKED_TAG);
        // Keep custom-spawned animals saveable if players tame or equip them.
        entity.setPersistent(true);
        return true;
    }

    public static Block findSpawnGround(World world, int x, int z) {
        Block ground = resolveGround(world.getHighestBlockAt(x, z));
        if (ground == null) return null;
        Block above = ground.getRelative(0, 1, 0);
        if (above.getLightFromSky() < 9 || !Tag.REPLACEABLE.isTagged(above.getType())) return null;
        return ground;
    }

    private static Block resolveGround(Block start) {
        Block current = start;
        for (int i = 0; i < 32; i++) {
            Material type = current.getType();
            if (Tag.REPLACEABLE.isTagged(type) || Tag.LEAVES.isTagged(type)) {
                current = current.getRelative(0, -1, 0);
            } else {
                return current;
            }
        }
        return null;
    }
}
