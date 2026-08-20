package org.example1.toolkit.cauldron;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.Chunk;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CauldronBedrock {

    private static final Map<String, Color> cauldronDyeColors = new HashMap<>();
    private static final Map<String, PotionData> cauldronPotions = new HashMap<>();
    private static final Map<String, ItemDisplay> cauldronDisplays = new HashMap<>();
    private static final int DEFAULT_LEATHER_R = 102;
    private static final int DEFAULT_LEATHER_G = 76;
    private static final int DEFAULT_LEATHER_B = 56;
    private static final NamespacedKey ITEM_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_water");
    private static final NamespacedKey POTION_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_potion");

    private static JavaPlugin plugin;

    public record PotionData(PotionType type, List<PotionEffect> customEffects) {}

    private CauldronBedrock() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
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
            savePotionPDC(block, data.type().name());
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
            } else if (keyStr.startsWith("pot_")) {
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
                    if (cauldronPotions.containsKey(mapKey)) continue;
                    String typeName = pdc.get(key, PersistentDataType.STRING);
                    if (typeName == null) continue;
                    PotionType type = PotionType.valueOf(typeName);
                    PotionData data = new PotionData(type, List.of());
                    cauldronPotions.put(mapKey, data);
                    int level = getWaterLevel(block);
                    spawnColoredDisplay(block, potionColor(type), level);
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
        if (type == PotionType.WATER) return null;
        List<PotionEffect> effects = new ArrayList<>(meta.getCustomEffects());
        return new PotionData(type, effects);
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

    public static void cleanBanner(ItemStack item) {
        if (item.getItemMeta() instanceof BannerMeta meta) {
            meta.setPatterns(new ArrayList<>());
            item.setItemMeta(meta);
        }
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

    private static void savePotionPDC(Block block, String typeName) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String coords = block.getX() + "_" + block.getY() + "_" + block.getZ();
        NamespacedKey key = new NamespacedKey(plugin, "pot_" + coords);
        pdc.set(key, PersistentDataType.STRING, typeName);
    }

    private static void clearPotionPDC(Block block) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String coords = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.remove(new NamespacedKey(plugin, "pot_" + coords));
    }
}
