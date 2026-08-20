package org.example1.toolkit.cauldron;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Chunk;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;

public final class CauldronBedrock {

    private static final Map<String, Color> cauldronDyeColors = new HashMap<>();
    private static final Map<String, PotionData> cauldronPotions = new HashMap<>();
    private static final Map<String, ItemDisplay> cauldronDisplays = new HashMap<>();
    private static final int DEFAULT_LEATHER_R = 102;
    private static final int DEFAULT_LEATHER_G = 76;
    private static final int DEFAULT_LEATHER_B = 56;
    private static final NamespacedKey ITEM_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_water");
    private static final NamespacedKey POTION_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_potion");
    private static final int MAX_MIX = 3;
    private static final NamespacedKey BOTTLE_MIX_COUNT = new NamespacedKey("toolkit", "bottle_mix_count");
    private static final NamespacedKey BOTTLE_MIXED_TYPES = new NamespacedKey("toolkit", "bottle_mixed_types");

    private static JavaPlugin plugin;

    public record PotionData(PotionType type, List<PotionEffect> customEffects, int mixCount, List<PotionType> mixedTypes) {
        public PotionData(PotionType type, List<PotionEffect> customEffects) {
            this(type, customEffects, 1, List.of(type));
        }
    }

    private CauldronBedrock() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        startBoilingTask();
    }

    private static void startBoilingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Config.CAULDRON_BEDROCK_ENABLED) return;
                for (String key : cauldronPotions.keySet()) {
                    String[] parts = key.split(":");
                    if (parts.length != 4) continue;
                    try {
                        String worldName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        World w = plugin.getServer().getWorld(worldName);
                        if (w == null) continue;
                        Block cauldron = w.getBlockAt(x, y, z);
                        if (cauldron.getType() != Material.WATER_CAULDRON) continue;
                        Block below = cauldron.getRelative(0, -1, 0);
                        Material belowType = below.getType();
                        boolean overHeat = belowType == Material.CAMPFIRE || belowType == Material.SOUL_CAMPFIRE
                                || belowType == Material.FIRE || belowType == Material.SOUL_FIRE;
                        if (!overHeat) continue;
                        PotionData data = cauldronPotions.get(key);
                        if (data == null) continue;
                        Color color = data.mixedTypes().size() > 1
                                ? mixPotionColors(data.mixedTypes()) : potionColor(data.type());
                        double py = waterLevelY(getWaterLevel(cauldron)) + 0.3;
                        w.spawnParticle(Particle.BUBBLE_POP, cauldron.getLocation().add(0.5, py, 0.5),
                                3, 0.2, 0.05, 0.2, 0,
                                new Particle.DustOptions(color, 1.0f));
                        w.playSound(cauldron.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 1.5f);
                    } catch (NumberFormatException ignored) {}
                }
                for (String key : cauldronDyeColors.keySet()) {
                    String[] parts = key.split(":");
                    if (parts.length != 4) continue;
                    try {
                        String worldName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        World w = plugin.getServer().getWorld(worldName);
                        if (w == null) continue;
                        Block cauldron = w.getBlockAt(x, y, z);
                        if (cauldron.getType() != Material.WATER_CAULDRON) continue;
                        if (cauldronPotions.containsKey(key)) continue;
                        Block below = cauldron.getRelative(0, -1, 0);
                        Material belowType = below.getType();
                        boolean overHeat = belowType == Material.CAMPFIRE || belowType == Material.SOUL_CAMPFIRE
                                || belowType == Material.FIRE || belowType == Material.SOUL_FIRE;
                        if (!overHeat) continue;
                        Color color = cauldronDyeColors.get(key);
                        if (color == null) continue;
                        double py = waterLevelY(getWaterLevel(cauldron)) + 0.3;
                        w.spawnParticle(Particle.BUBBLE_POP, cauldron.getLocation().add(0.5, py, 0.5),
                                3, 0.2, 0.05, 0.2, 0,
                                new Particle.DustOptions(color, 1.0f));
                        w.playSound(cauldron.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 1.5f);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private static String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public static Color getDyeColor(Block block) {
        return cauldronDyeColors.get(key(block.getLocation()));
    }

    public static void setDyeColor(Block block, Color color) {
        String k = key(block.getLocation());
        if (color != null) {
            cauldronDyeColors.put(k, color);
            saveDyePDC(block, color.asRGB());
        } else {
            cauldronDyeColors.remove(k);
            clearDyePDC(block);
        }
    }

    public static PotionData getPotion(Block block) {
        return cauldronPotions.get(key(block.getLocation()));
    }

    public static void setPotion(Block block, PotionData data) {
        String k = key(block.getLocation());
        if (data != null) {
            cauldronPotions.put(k, data);
            savePotionPDC(block, data);
        } else {
            cauldronPotions.remove(k);
            clearPotionPDC(block);
        }
    }

    public static void removeAll(Block block) {
        String k = key(block.getLocation());
        cauldronDyeColors.remove(k);
        cauldronPotions.remove(k);
        clearDyePDC(block);
        clearPotionPDC(block);
        removeDisplay(block);
    }

    private static final Set<PotionType> NEGATIVE_TYPES = Set.of(
            PotionType.POISON, PotionType.LONG_POISON, PotionType.STRONG_POISON,
            PotionType.HARMING, PotionType.STRONG_HARMING,
            PotionType.WEAKNESS, PotionType.LONG_WEAKNESS,
            PotionType.SLOWNESS, PotionType.LONG_SLOWNESS, PotionType.STRONG_SLOWNESS,
            PotionType.INFESTED
    );

    private static final Set<PotionType> POSITIVE_TYPES = Set.of(
            PotionType.FIRE_RESISTANCE, PotionType.LONG_FIRE_RESISTANCE,
            PotionType.SWIFTNESS, PotionType.LONG_SWIFTNESS, PotionType.STRONG_SWIFTNESS,
            PotionType.HEALING, PotionType.STRONG_HEALING,
            PotionType.REGENERATION, PotionType.LONG_REGENERATION, PotionType.STRONG_REGENERATION,
            PotionType.STRENGTH, PotionType.LONG_STRENGTH, PotionType.STRONG_STRENGTH,
            PotionType.NIGHT_VISION, PotionType.LONG_NIGHT_VISION,
            PotionType.WATER_BREATHING, PotionType.LONG_WATER_BREATHING,
            PotionType.LEAPING, PotionType.LONG_LEAPING, PotionType.STRONG_LEAPING,
            PotionType.INVISIBILITY, PotionType.LONG_INVISIBILITY,
            PotionType.SLOW_FALLING, PotionType.LONG_SLOW_FALLING,
            PotionType.LUCK, PotionType.TURTLE_MASTER, PotionType.LONG_TURTLE_MASTER, PotionType.STRONG_TURTLE_MASTER,
            PotionType.OOZING, PotionType.WEAVING, PotionType.WIND_CHARGED
    );

    public static boolean canMix(PotionData existing, PotionData incoming, int level) {
        if (level >= MAX_MIX) return false;
        return true;
    }

    public static PotionData mixPotions(PotionData existing, PotionData incoming) {
        List<PotionEffect> allEffects = new ArrayList<>(getAllEffects(existing));
        allEffects.addAll(getAllEffects(incoming));

        List<PotionType> allTypes = new ArrayList<>(existing.mixedTypes());
        if (!allTypes.contains(incoming.type())) {
            allTypes.add(incoming.type());
        }

        int negCount = 0;
        int posCount = 0;
        for (PotionType t : allTypes) {
            if (NEGATIVE_TYPES.contains(t)) negCount++;
            else if (POSITIVE_TYPES.contains(t)) posCount++;
        }

        int newMixCount = Math.min(existing.mixCount() + 1, MAX_MIX);
        PotionEffect bonus = rollBonusEffect(negCount, posCount, newMixCount, allEffects.size());
        if (bonus != null) {
            allEffects.add(bonus);
        }

        return new PotionData(existing.type(), allEffects, newMixCount, allTypes);
    }

    public static List<PotionEffect> getAllEffects(PotionData data) {
        List<PotionEffect> effects = new ArrayList<>(potionEffects(data.type()));
        effects.addAll(data.customEffects());
        return effects;
    }

    private static PotionEffect rollBonusEffect(int negCount, int posCount, int mixCount, int totalEffects) {
        double chance = mixCount * 0.25;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() > chance) return null;

        if (totalEffects > 5) {
            PotionEffectType[] negatives = {
                    PotionEffectType.NAUSEA, PotionEffectType.SLOWNESS,
                    PotionEffectType.WEAKNESS, PotionEffectType.POISON
            };
            return new PotionEffect(negatives[rng.nextInt(negatives.length)], 200, 0);
        }

        if (negCount > posCount) {
            PotionEffectType[] positives = {
                    PotionEffectType.SPEED, PotionEffectType.STRENGTH,
                    PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE
            };
            return new PotionEffect(positives[rng.nextInt(positives.length)], 400, 0);
        } else if (posCount > negCount) {
            PotionEffectType[] negatives = {
                    PotionEffectType.NAUSEA, PotionEffectType.SLOWNESS,
                    PotionEffectType.WEAKNESS, PotionEffectType.POISON
            };
            return new PotionEffect(negatives[rng.nextInt(negatives.length)], 200, 0);
        } else {
            PotionEffectType[] all = {
                    PotionEffectType.SPEED, PotionEffectType.NAUSEA,
                    PotionEffectType.REGENERATION, PotionEffectType.WEAKNESS
            };
            return new PotionEffect(all[rng.nextInt(all.length)], 300, 0);
        }
    }

    public static Color mixPotionColors(List<PotionType> types) {
        if (types.isEmpty()) return potionColor(PotionType.WATER);
        int r = 0, g = 0, b = 0;
        for (PotionType t : types) {
            Color c = potionColor(t);
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        return Color.fromRGB(r / types.size(), g / types.size(), b / types.size());
    }

    public static Color mixColors(Color existing, Color add) {
        int r = (existing.getRed() + add.getRed()) / 2;
        int g = (existing.getGreen() + add.getGreen()) / 2;
        int b = (existing.getBlue() + add.getBlue()) / 2;
        return Color.fromRGB(r, g, b);
    }

    public static int getWaterLevel(Block block) {
        if (block.getBlockData() instanceof Levelled l) return l.getLevel();
        return 0;
    }

    public static void setWaterLevel(Block block, int level) {
        if (level <= 0) {
            block.setType(Material.CAULDRON);
        } else {
            if (block.getType() != Material.WATER_CAULDRON) {
                block.setType(Material.WATER_CAULDRON);
            }
            if (block.getBlockData() instanceof Levelled l) {
                l.setLevel(Math.max(1, Math.min(l.getMaximumLevel(), level)));
                block.setBlockData(l);
            }
        }
    }

    public static void spawnColoredDisplay(Block block, Color color, int level) {
        if (level <= 0 || color == null) {
            removeDisplay(block);
            return;
        }

        String k = key(block.getLocation());
        ItemDisplay existing = cauldronDisplays.get(k);
        if (existing != null && existing.isValid()) {
            existing.remove();
        }

        boolean isPotion = cauldronPotions.containsKey(k);
        NamespacedKey modelKey = isPotion ? POTION_MODEL_KEY : ITEM_MODEL_KEY;

        double waterY = waterLevelY(level);
        Location loc = block.getLocation().add(0.5, waterY + 0.005, 0.5);

        ItemDisplay display = block.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ItemStack fluidItem = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) fluidItem.getItemMeta();
            meta.setColor(color);
            meta.setItemModel(modelKey);
            fluidItem.setItemMeta(meta);

            ent.setItemStack(fluidItem);
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            ent.setPersistent(false);
            ent.setViewRange(1.0f);
            ent.setShadowRadius(0f);
            ent.setShadowStrength(0f);

            Matrix4f transform = new Matrix4f();
            transform.scale(0.75f, 1.0f, 0.75f);
            ent.setTransformationMatrix(transform);
        });

        cauldronDisplays.put(k, display);
    }

    public static void removeDisplay(Block block) {
        String k = key(block.getLocation());
        ItemDisplay display = cauldronDisplays.remove(k);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public static void updateDisplayLevel(Block block, int newLevel) {
        String k = key(block.getLocation());
        ItemDisplay display = cauldronDisplays.get(k);
        if (display == null || !display.isValid()) {
            cauldronDisplays.remove(k);
            if (newLevel > 0) {
                Color dye = cauldronDyeColors.get(k);
                PotionData pot = cauldronPotions.get(k);
                Color color = dye != null ? dye : (pot != null ? potionColor(pot.type()) : null);
                if (color != null) spawnColoredDisplay(block, color, newLevel);
            }
            return;
        }
        if (newLevel <= 0) {
            removeDisplay(block);
            return;
        }
        Location newLoc = block.getLocation().add(0.5, waterLevelY(newLevel), 0.5);
        display.teleport(newLoc);
    }

    public static void removeAllDisplays() {
        for (ItemDisplay display : cauldronDisplays.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        cauldronDisplays.clear();
    }

    public static void unloadChunk(Chunk chunk) {
        removeByChunk(cauldronDyeColors, chunk);
        removeByChunk(cauldronPotions, chunk);
        var iter = cauldronDisplays.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            ItemDisplay display = entry.getValue();
            if (display == null || !display.isValid()) {
                iter.remove();
                continue;
            }
            Location loc = display.getLocation();
            if (loc.getWorld() == chunk.getWorld()
                    && (loc.getBlockX() >> 4) == chunk.getX()
                    && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                display.remove();
                iter.remove();
            }
        }
    }

    private static void removeByChunk(Map<String, ?> map, Chunk chunk) {
        String prefix = chunk.getWorld().getName() + ":";
        int cx = chunk.getX() << 4;
        int cz = chunk.getZ() << 4;
        map.keySet().removeIf(k -> {
            if (!k.startsWith(prefix)) return false;
            String[] p = k.substring(prefix.length()).split(":");
            if (p.length != 3) return false;
            try {
                int x = Integer.parseInt(p[0]);
                int z = Integer.parseInt(p[2]);
                return x >= cx && x < cx + 16 && z >= cz && z < cz + 16;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    public static void restoreAllDisplays() {
        if (plugin == null) return;
        for (var world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                restoreChunk(chunk);
            }
        }
    }

    public static void restoreChunk(Chunk chunk) {
        if (plugin == null) return;
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        org.bukkit.World w = chunk.getWorld();
        for (NamespacedKey key : pdc.getKeys()) {
            String keyStr = key.getKey();
            if (keyStr.startsWith("dye_")) {
                String coords = keyStr.substring(4);
                String[] parts = coords.split("_");
                if (parts.length != 3) continue;
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Block block = w.getBlockAt(x, y, z);
                    if (block.getType() != Material.WATER_CAULDRON) continue;
                    String mapKey = key(block.getLocation());
                    if (cauldronDyeColors.containsKey(mapKey)) continue;
                    Integer rgb = pdc.get(key, PersistentDataType.INTEGER);
                    if (rgb == null) continue;
                    Color color = Color.fromRGB(rgb);
                    int level = getWaterLevel(block);
                    cauldronDyeColors.put(mapKey, color);
                    spawnColoredDisplay(block, color, level);
                } catch (NumberFormatException ignored) {}
            } else if (keyStr.startsWith("pot_type_")) {
                String coords = keyStr.substring(9);
                String[] parts = coords.split("_");
                if (parts.length != 3) continue;
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Block block = w.getBlockAt(x, y, z);
                    if (block.getType() != Material.WATER_CAULDRON) continue;
                    String mapKey = key(block.getLocation());
                    if (cauldronPotions.containsKey(mapKey)) continue;
                    String typeName = pdc.get(key, PersistentDataType.STRING);
                    if (typeName == null) continue;
                    PotionType type = PotionType.valueOf(typeName);
                    int mixCount = pdc.getOrDefault(new NamespacedKey(plugin, "pot_mix_" + coords), PersistentDataType.INTEGER, 1);
                    String typesStr = pdc.getOrDefault(new NamespacedKey(plugin, "pot_types_" + coords), PersistentDataType.STRING, typeName);
                    List<PotionType> mixedTypes = java.util.Arrays.stream(typesStr.split(","))
                            .map(s -> { try { return PotionType.valueOf(s.trim()); } catch (Exception e) { return null; } })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    String fxStr = pdc.getOrDefault(new NamespacedKey(plugin, "pot_fx_" + coords), PersistentDataType.STRING, "");
                    List<PotionEffect> customFx = new ArrayList<>();
                    if (!fxStr.isEmpty()) {
                        for (String fx : fxStr.split(",")) {
                            String[] p = fx.split(":");
                            if (p.length != 3) continue;
                            PotionEffectType et = PotionEffectType.getByName(p[0]);
                            if (et == null) continue;
                            customFx.add(new PotionEffect(et, Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                        }
                    }
                    PotionData data = new PotionData(type, customFx, Math.max(1, Math.min(mixCount, MAX_MIX)), mixedTypes);
                    cauldronPotions.put(mapKey, data);
                    int level = getWaterLevel(block);
                    Color color = mixedTypes.size() > 1 ? mixPotionColors(mixedTypes) : potionColor(type);
                    spawnColoredDisplay(block, color, level);
                } catch (Exception ignored) {}
            }
        }
    }

    public static double waterLevelY(int level) {
        return switch (level) {
            case 1 -> 0.5;
            case 2 -> 0.6875;
            case 3 -> 0.875;
            default -> 0.5;
        };
    }

    public static void startColorTask(JavaPlugin pl) {
        new BukkitRunnable() {
            @Override
            public void run() {
                var iter = cauldronDyeColors.entrySet().iterator();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    String[] p = entry.getKey().split(":");
                    org.bukkit.World w = pl.getServer().getWorld(p[0]);
                    if (w == null) continue;
                    Block block = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    if (block.getType() != Material.WATER_CAULDRON) {
                        iter.remove();
                        removeDisplay(block);
                        continue;
                    }
                    ensureDisplay(block, entry.getValue());
                }
                var piter = cauldronPotions.entrySet().iterator();
                while (piter.hasNext()) {
                    var entry = piter.next();
                    String[] p = entry.getKey().split(":");
                    org.bukkit.World w = pl.getServer().getWorld(p[0]);
                    if (w == null) continue;
                    Block block = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    if (block.getType() != Material.WATER_CAULDRON) {
                        piter.remove();
                        removeDisplay(block);
                        continue;
                    }
                    spawnColoredParticles(block, potionColor(entry.getValue().type()));
                    ensureDisplay(block, potionColor(entry.getValue().type()));
                }
            }
        }.runTaskTimer(pl, 10L, 10L);
    }

    private static void ensureDisplay(Block block, Color color) {
        String k = key(block.getLocation());
        ItemDisplay display = cauldronDisplays.get(k);
        if (display == null || !display.isValid()) {
            int level = getWaterLevel(block);
            spawnColoredDisplay(block, color, level);
        }
    }

    private static void spawnColoredParticles(Block block, Color c) {
        int level = getWaterLevel(block);
        if (level <= 0) return;
        double y = waterLevelY(level) - 0.02;
        Location loc = block.getLocation().add(0.5, y, 0.5);
        block.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.15, 0.01, 0.15, 0,
                new Particle.DustOptions(c, 1.0f));
    }

    public static Color potionColor(PotionType type) {
        return switch (type) {
            case WATER, MUNDANE, THICK, AWKWARD -> Color.fromRGB(0x38, 0x5D, 0xC6);
            case NIGHT_VISION, LONG_NIGHT_VISION -> Color.fromRGB(0xC2, 0xFF, 0x66);
            case INVISIBILITY, LONG_INVISIBILITY -> Color.fromRGB(0xF6, 0xF6, 0xF6);
            case LEAPING, LONG_LEAPING, STRONG_LEAPING -> Color.fromRGB(0xFD, 0xFF, 0x84);
            case FIRE_RESISTANCE, LONG_FIRE_RESISTANCE -> Color.fromRGB(0xFF, 0x99, 0x00);
            case SWIFTNESS, LONG_SWIFTNESS, STRONG_SWIFTNESS -> Color.fromRGB(0x33, 0xEB, 0xFF);
            case SLOWNESS, LONG_SLOWNESS, STRONG_SLOWNESS -> Color.fromRGB(0x8B, 0xAF, 0xE0);
            case TURTLE_MASTER, LONG_TURTLE_MASTER -> Color.fromRGB(0x8B, 0x80, 0xE3);
            case STRONG_TURTLE_MASTER -> Color.fromRGB(0x8B, 0x83, 0xE3);
            case WATER_BREATHING, LONG_WATER_BREATHING -> Color.fromRGB(0x98, 0xDA, 0xC0);
            case HEALING, STRONG_HEALING -> Color.fromRGB(0xF8, 0x24, 0x23);
            case HARMING, STRONG_HARMING -> Color.fromRGB(0xA9, 0x65, 0x6A);
            case POISON, LONG_POISON, STRONG_POISON -> Color.fromRGB(0x87, 0xA3, 0x63);
            case REGENERATION, LONG_REGENERATION, STRONG_REGENERATION -> Color.fromRGB(0xCD, 0x5C, 0xAB);
            case STRENGTH, LONG_STRENGTH, STRONG_STRENGTH -> Color.fromRGB(0xFF, 0xC7, 0x00);
            case WEAKNESS, LONG_WEAKNESS -> Color.fromRGB(0x48, 0x4D, 0x48);
            case LUCK -> Color.fromRGB(0x59, 0xC1, 0x06);
            case SLOW_FALLING, LONG_SLOW_FALLING -> Color.fromRGB(0xF3, 0xCF, 0xB9);
            case OOZING -> Color.fromRGB(0x99, 0xFF, 0xA3);
            case WIND_CHARGED -> Color.fromRGB(0xBD, 0xC9, 0xFF);
            case INFESTED -> Color.fromRGB(0x8C, 0x9B, 0x8C);
            case WEAVING -> Color.fromRGB(0x78, 0x69, 0x5A);
            default -> Color.fromRGB(0x38, 0x5D, 0xC6);
        };
    }

    public static List<PotionEffect> potionEffects(PotionType type) {
        return switch (type) {
            case FIRE_RESISTANCE -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 400, 0));
            case LONG_FIRE_RESISTANCE -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 960, 0));
            case SWIFTNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 360, 0));
            case LONG_SWIFTNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 960, 0));
            case STRONG_SWIFTNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 360, 2));
            case SLOWNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 360, 0));
            case LONG_SLOWNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 960, 0));
            case STRONG_SLOWNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 360, 3));
            case HEALING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_HEALTH, 1, 0));
            case STRONG_HEALING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_HEALTH, 1, 1));
            case HARMING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE, 1, 0));
            case STRONG_HARMING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE, 1, 1));
            case LEAPING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 360, 0));
            case LONG_LEAPING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 960, 0));
            case STRONG_LEAPING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 360, 2));
            case INVISIBILITY -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 360, 0));
            case LONG_INVISIBILITY -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 960, 0));
            case POISON -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 400, 0));
            case LONG_POISON -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 960, 0));
            case STRONG_POISON -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 200, 2));
            case REGENERATION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 400, 0));
            case LONG_REGENERATION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 960, 0));
            case STRONG_REGENERATION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 160, 2));
            case STRENGTH -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 360, 0));
            case LONG_STRENGTH -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 960, 0));
            case STRONG_STRENGTH -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 360, 2));
            case WEAKNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 360, 0));
            case LONG_WEAKNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 960, 0));
            case TURTLE_MASTER -> List.of(
                    new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 400, 3),
                    new PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 400, 2));
            case LONG_TURTLE_MASTER -> List.of(
                    new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 800, 3),
                    new PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 800, 2));
            case STRONG_TURTLE_MASTER -> List.of(
                    new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 400, 5),
                    new PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 400, 3));
            case WATER_BREATHING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 360, 0));
            case LONG_WATER_BREATHING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 960, 0));
            case SLOW_FALLING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 360, 0));
            case LONG_SLOW_FALLING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 960, 0));
            case NIGHT_VISION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 360, 0));
            case LONG_NIGHT_VISION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 960, 0));
            case LUCK -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 600, 0));
            case OOZING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.OOZING, 600, 0));
            case WEAVING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WEAVING, 600, 0));
            case WIND_CHARGED -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WIND_CHARGED, 600, 0));
            case INFESTED -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INFESTED, 600, 0));
            default -> List.of();
        };
    }

    public static boolean isPotion(ItemStack item) {
        Material m = item.getType();
        return m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION;
    }

    public static boolean isWaterPotion(ItemStack item) {
        if (item.getItemMeta() instanceof PotionMeta meta) {
            return meta.getBasePotionType() == PotionType.WATER;
        }
        return false;
    }

    public static PotionData potionFromItem(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return null;
        PotionType type = meta.getBasePotionType();
        if (type == null) {
            List<PotionEffect> effects = new ArrayList<>(meta.getCustomEffects());
            if (effects.isEmpty()) return null;
            type = PotionType.AWKWARD;
        }
        if (type == PotionType.WATER) return null;
        List<PotionEffect> effects = new ArrayList<>(meta.getCustomEffects());
        int mixCount = meta.getPersistentDataContainer().getOrDefault(BOTTLE_MIX_COUNT, PersistentDataType.INTEGER, 1);
        String typesStr = meta.getPersistentDataContainer().getOrDefault(BOTTLE_MIXED_TYPES, PersistentDataType.STRING, "");
        List<PotionType> mixedTypes;
        if (typesStr.isEmpty()) {
            mixedTypes = List.of(type);
        } else {
            mixedTypes = java.util.Arrays.stream(typesStr.split(","))
                    .map(s -> { try { return PotionType.valueOf(s.trim()); } catch (Exception e) { return null; } })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (mixedTypes.isEmpty()) mixedTypes = List.of(type);
        }
        return new PotionData(type, effects, mixCount, mixedTypes);
    }

    public static void saveMixToItem(ItemStack item, PotionData data) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;
        meta.getPersistentDataContainer().set(BOTTLE_MIX_COUNT, PersistentDataType.INTEGER, data.mixCount());
        String typesStr = data.mixedTypes().stream()
                .map(Enum::name)
                .reduce((a, b) -> a + "," + b)
                .orElse(data.type().name());
        meta.getPersistentDataContainer().set(BOTTLE_MIXED_TYPES, PersistentDataType.STRING, typesStr);
        item.setItemMeta(meta);
    }

    public static void applyPotionToArmor(ItemStack item, PotionData data) {
        if (!(item.getItemMeta() instanceof LeatherArmorMeta meta)) return;
        meta.setColor(potionColor(data.type()));
        item.setItemMeta(meta);
    }

    public static void cleanArmor(ItemStack item) {
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(DEFAULT_LEATHER_R, DEFAULT_LEATHER_G, DEFAULT_LEATHER_B));
            item.setItemMeta(meta);
        }
    }

    public static boolean isLeatherArmor(ItemStack item) {
        Material m = item.getType();
        return m == Material.LEATHER_BOOTS || m == Material.LEATHER_LEGGINGS
                || m == Material.LEATHER_CHESTPLATE || m == Material.LEATHER_HELMET;
    }

    public static boolean isDefaultLeatherColor(LeatherArmorMeta meta) {
        if (!meta.isDyed()) return true;
        Color c = meta.getColor();
        return c.getRed() == DEFAULT_LEATHER_R && c.getGreen() == DEFAULT_LEATHER_G && c.getBlue() == DEFAULT_LEATHER_B;
    }

    public static void applyDyeToArmor(ItemStack item, Color color) {
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
    }

    public static boolean isDye(ItemStack item) {
        return item.getType().name().endsWith("_DYE");
    }

    public static DyeColor dyeFromMaterial(Material material) {
        return switch (material) {
            case WHITE_DYE -> DyeColor.WHITE;
            case ORANGE_DYE -> DyeColor.ORANGE;
            case MAGENTA_DYE -> DyeColor.MAGENTA;
            case LIGHT_BLUE_DYE -> DyeColor.LIGHT_BLUE;
            case YELLOW_DYE -> DyeColor.YELLOW;
            case LIME_DYE -> DyeColor.LIME;
            case PINK_DYE -> DyeColor.PINK;
            case GRAY_DYE -> DyeColor.GRAY;
            case LIGHT_GRAY_DYE -> DyeColor.LIGHT_GRAY;
            case CYAN_DYE -> DyeColor.CYAN;
            case PURPLE_DYE -> DyeColor.PURPLE;
            case BLUE_DYE -> DyeColor.BLUE;
            case BROWN_DYE -> DyeColor.BROWN;
            case GREEN_DYE -> DyeColor.GREEN;
            case RED_DYE -> DyeColor.RED;
            case BLACK_DYE -> DyeColor.BLACK;
            default -> null;
        };
    }

    public static boolean isDyedBanner(ItemStack item) {
        if (!item.getType().name().endsWith("_BANNER")) return false;
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return false;
        return !meta.getPatterns().isEmpty();
    }

    /**
     * Убирает верхний слой узора флага (Bedrock-стиль: по одному слою за клик).
     * Возвращает true, если слой был снят.
     */
    public static boolean cleanBanner(ItemStack item) {
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return false;
        List<Pattern> patterns = new ArrayList<>(meta.getPatterns());
        if (patterns.isEmpty()) return false;
        patterns.removeLast();
        meta.setPatterns(patterns);
        item.setItemMeta(meta);
        return true;
    }

    /** Сколько слоёв узора осталось на флаге. */
    public static int bannerLayers(ItemStack item) {
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return 0;
        return meta.getPatterns().size();
    }

    public static boolean isDyedShulker(ItemStack item) {
        return item.getType().name().endsWith("_SHULKER_BOX") && item.getType() != Material.SHULKER_BOX;
    }

    public static void cleanShulker(ItemStack item) {
        item.setType(Material.SHULKER_BOX);
    }

    private static void saveDyePDC(Block block, int rgb) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String coords = block.getX() + "_" + block.getY() + "_" + block.getZ();
        NamespacedKey key = new NamespacedKey(plugin, "dye_" + coords);
        pdc.set(key, PersistentDataType.INTEGER, rgb);
    }

    private static void clearDyePDC(Block block) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String coords = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.remove(new NamespacedKey(plugin, "dye_" + coords));
    }

    private static void savePotionPDC(Block block, PotionData data) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String c = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.set(new NamespacedKey(plugin, "pot_type_" + c), PersistentDataType.STRING, data.type().name());
        pdc.set(new NamespacedKey(plugin, "pot_mix_" + c), PersistentDataType.INTEGER, data.mixCount());
        pdc.set(new NamespacedKey(plugin, "pot_types_" + c), PersistentDataType.STRING,
                String.join(",", data.mixedTypes().stream().map(Enum::name).toList()));
        StringBuilder fx = new StringBuilder();
        for (PotionEffect effect : data.customEffects()) {
            if (!fx.isEmpty()) fx.append(",");
            fx.append(effect.getType().getName()).append(":").append(effect.getDuration()).append(":").append(effect.getAmplifier());
        }
        pdc.set(new NamespacedKey(plugin, "pot_fx_" + c), PersistentDataType.STRING, fx.toString());
    }

    private static void clearPotionPDC(Block block) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String c = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.remove(new NamespacedKey(plugin, "pot_type_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_mix_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_types_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_fx_" + c));
    }
}
