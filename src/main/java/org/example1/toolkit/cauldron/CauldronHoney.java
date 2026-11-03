package org.example1.toolkit.cauldron;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Мёд в котле: котёл, стоящий под полным ульем (honey_level 5), медленно наполняется
 * каплями (как котёл с лавой под капельником). Стеклянной бутылкой из котла
 * забирается 1 уровень = 1 бутылка мёда.
 */
public final class CauldronHoney {

    private static final String TRACK_PREFIX = "honey_track_";
    private static final String LEVEL_PREFIX = "honey_";
    private static final int HIVE_SCAN_UP = 8;
    private static final NamespacedKey HONEY_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_honey");

    private static final Map<String, Integer> honeyLevels = new HashMap<>();
    private static final Map<String, ItemDisplay> honeyDisplays = new HashMap<>();
    private static final Set<String> tracked = new HashSet<>();
    private static final Map<String, Integer> progressTicks = new HashMap<>();

    private static JavaPlugin plugin;

    private CauldronHoney() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Config.CAULDRON_HONEY_ENABLED) return;
                tick();
            }
        }.runTaskTimer(pl, 20L, 20L);
    }

    private static String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private static String coordsOf(Block block) {
        return block.getX() + "_" + block.getY() + "_" + block.getZ();
    }

    public static boolean isHoneyCauldron(Block block) {
        return block != null && getHoneyLevel(block) > 0;
    }

    public static int getHoneyLevel(Block block) {
        return honeyLevels.getOrDefault(key(block), 0);
    }

    /** Начинает отслеживать пустой котёл под ульем (наполнение каплями). */
    public static void startTracking(Block block) {
        if (!Config.CAULDRON_HONEY_ENABLED) return;
        if (block == null || block.getType() != Material.CAULDRON) return;
        // Начинаем отслеживать и пустые ульи: пчёлы наполняют их уже после
        // установки, а тик сам проверит готовность мёда перед переливом.
        if (findHiveAbove(block) == null) return;
        String k = key(block);
        if (tracked.add(k)) {
            saveTrackPDC(block);
            progressTicks.putIfAbsent(k, 0);
        }
    }

    /** Котёл поставлен под улей или улей поставлен над котлом. */
    public static void tryTrack(Block cauldron) {
        if (!Config.CAULDRON_HONEY_ENABLED) return;
        if (cauldron == null || cauldron.getType() != Material.CAULDRON) return;
        if (findHiveAbove(cauldron) != null) {
            startTracking(cauldron);
        }
    }

    /** Проверяет котёл под только что поставленным ульем, в том числе через воздух. */
    public static void tryTrackBelow(Block hive) {
        if (!Config.CAULDRON_HONEY_ENABLED || hive == null) return;
        for (int dy = 1; dy <= HIVE_SCAN_UP; dy++) {
            Block candidate = hive.getRelative(0, -dy, 0);
            if (candidate.getType() == Material.CAULDRON) {
                tryTrack(candidate);
                return;
            }
            if (!candidate.getType().isAir()) return;
        }
    }

    public static void removeAll(Block block) {
        String k = key(block);
        honeyLevels.remove(k);
        progressTicks.remove(k);
        tracked.remove(k);
        removeDisplay(block);
        clearTrackPDC(block);
        clearLevelPDC(block);
    }

    public static void setHoneyLevel(Block block, int level) {
        String k = key(block);
        if (level <= 0) {
            honeyLevels.remove(k);
            clearLevelPDC(block);
            removeDisplay(block);
            return;
        }
        level = Math.min(level, Math.max(1, Config.CAULDRON_HONEY_MAX_LEVELS));
        honeyLevels.put(k, level);
        saveLevelPDC(block, level);
        spawnDisplay(block, level);
    }

    /** Сбор мёда стеклянной бутылкой: 1 уровень = 1 бутылка мёда. */
    public static void handleTake(Player player, Block block) {
        int level = getHoneyLevel(block);
        if (level <= 0) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack bottle = player.getInventory().getItemInMainHand();
            bottle.setAmount(bottle.getAmount() - 1);
            ItemStack honey = new ItemStack(Material.HONEY_BOTTLE);
            if (player.getInventory().firstEmpty() == -1) {
                block.getWorld().dropItemNaturally(player.getLocation(), honey);
            } else {
                player.getInventory().addItem(honey);
            }
        }
        int newLevel = level - 1;
        setHoneyLevel(block, newLevel);
        Msg.of(player,
                Msg.prefix(Lang.get(player, "honey"), NamedTextColor.GOLD),
                Msg.sep(),
                Msg.action(Lang.get(player, "honey-collected")),
                Msg.sep(),
                Msg.level(Math.max(newLevel, 0), Config.CAULDRON_HONEY_MAX_LEVELS));
    }

    /** Наливание мёда из бутылки обратно в котёл: 1 бутылка = 1 уровень. */
    public static void handlePour(Player player, Block block) {
        int max = Math.max(1, Config.CAULDRON_HONEY_MAX_LEVELS);
        int level = getHoneyLevel(block);
        if (level >= max) {
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "honey"), NamedTextColor.GOLD),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "honey-full")));
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack bottle = player.getInventory().getItemInMainHand();
            bottle.setAmount(bottle.getAmount() - 1);
            ItemStack glass = new ItemStack(Material.GLASS_BOTTLE);
            if (player.getInventory().firstEmpty() == -1) {
                block.getWorld().dropItemNaturally(player.getLocation(), glass);
            } else {
                player.getInventory().addItem(glass);
            }
        }
        int newLevel = level + 1;
        setHoneyLevel(block, newLevel);
        Msg.of(player,
                Msg.prefix(Lang.get(player, "honey"), NamedTextColor.GOLD),
                Msg.sep(),
                Msg.action(Lang.get(player, "honey-poured")),
                Msg.sep(),
                Msg.level(newLevel, Config.CAULDRON_HONEY_MAX_LEVELS));
    }

    private static void tick() {
        Iterator<String> iter = tracked.iterator();
        while (iter.hasNext()) {
            String k = iter.next();
            String[] parts = k.split(":");
            if (parts.length != 4) {
                iter.remove();
                continue;
            }
            World w = CauldronBedrock.worldOf(parts[0]);
            if (w == null) {
                iter.remove();
                continue;
            }
            Block block;
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                block = w.getBlockAt(x, y, z);
            } catch (NumberFormatException e) {
                iter.remove();
                continue;
            }

            if (block.getType() != Material.CAULDRON || findHiveAbove(block) == null) {
                iter.remove();
                clearTrackPDC(block);
                progressTicks.remove(k);
                continue;
            }

            int fillSeconds = Math.max(1, Config.CAULDRON_HONEY_FILL_SECONDS);
            int p = progressTicks.getOrDefault(k, 0) + 20;
            if (p < fillSeconds * 20) {
                progressTicks.put(k, p);
                continue;
            }

            int max = Math.max(1, Config.CAULDRON_HONEY_MAX_LEVELS);
            if (findFullHiveAbove(block) == null) {
                progressTicks.put(k, 0);
                continue;
            }
            progressTicks.put(k, 0);
            int level = getHoneyLevel(block);
            if (level >= max) continue;
            setHoneyLevel(block, level + 1);

            Block hive = findFullHiveAbove(block);
            if (hive != null && hive.getBlockData() instanceof Beehive data) {
                data.setHoneyLevel(0);
                hive.setBlockData(data);
            }

            block.getWorld().spawnParticle(Particle.DRIPPING_HONEY,
                    block.getLocation().add(0.5, 1.0, 0.5), 2, 0.05, 0.0, 0.05, 0);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEEHIVE_DRIP, 0.5f, 1.0f);
        }
    }

    private static Block findHiveAbove(Block block) {
        // Механика капельника без капельника: мёд капает сквозь воздух из улья,
        // стоящего над котлом (не обязательно вплотную). Если на пути встречается
        // твёрдый блок — капли не пройдут, поиск прекращается.
        for (int dy = 1; dy <= HIVE_SCAN_UP; dy++) {
            Block b = block.getRelative(0, dy, 0);
            Material m = b.getType();
            if (m == Material.BEEHIVE || m == Material.BEE_NEST) return b;
            if (!m.isAir()) return null;
        }
        return null;
    }

    private static Block findFullHiveAbove(Block block) {
        Block hive = findHiveAbove(block);
        if (hive == null) return null;
        if (hive.getBlockData() instanceof Beehive data && data.getHoneyLevel() == 5) {
            return hive;
        }
        return null;
    }

    private static void spawnDisplay(Block block, int level) {
        String k = key(block);
        ItemDisplay existing = honeyDisplays.get(k);
        if (existing != null && existing.isValid()) {
            existing.remove();
        }

        double y = CauldronBedrock.waterLevelY(level) + 0.005;
        Location loc = block.getLocation().add(0.5, y, 0.5);

        ItemDisplay display = block.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ItemStack fluidItem = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) fluidItem.getItemMeta();
            meta.setColor(Color.fromRGB(0xF0, 0xB4, 0x30));
            meta.setItemModel(HONEY_MODEL_KEY);
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

        honeyDisplays.put(k, display);
    }

    private static void removeDisplay(Block block) {
        String k = key(block);
        ItemDisplay display = honeyDisplays.remove(k);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public static void removeAllDisplays() {
        for (ItemDisplay display : honeyDisplays.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        honeyDisplays.clear();
    }

    public static void unloadChunk(Chunk chunk) {
        unloadChunkMap(chunk, honeyLevels);
        progressTicks.keySet().removeIf(k -> inChunk(chunk, k));
        tracked.removeIf(k -> inChunk(chunk, k));
        unloadDisplays(chunk);
    }

    public static void restoreChunk(Chunk chunk) {
        if (plugin == null) return;
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        World w = chunk.getWorld();
        for (NamespacedKey key : pdc.getKeys()) {
            String name = key.getKey();
            if (name.startsWith(TRACK_PREFIX)) {
                int[] xyz = parseCoords(name.substring(TRACK_PREFIX.length()));
                if (xyz == null) continue;
                Block block = w.getBlockAt(xyz[0], xyz[1], xyz[2]);
                if (block.getType() != Material.CAULDRON) continue;
                String k = key(block);
                if (tracked.add(k)) progressTicks.putIfAbsent(k, 0);
            } else if (name.startsWith(LEVEL_PREFIX)) {
                int[] xyz = parseCoords(name.substring(LEVEL_PREFIX.length()));
                if (xyz == null) continue;
                Block block = w.getBlockAt(xyz[0], xyz[1], xyz[2]);
                if (block.getType() != Material.CAULDRON) continue;
                Integer level = pdc.get(key, PersistentDataType.INTEGER);
                if (level == null) continue;
                String k = key(block);
                int restoredLevel = Math.min(level, Math.max(1, Config.CAULDRON_HONEY_MAX_LEVELS));
                if (restoredLevel <= 0) continue;
                honeyLevels.put(k, restoredLevel);
                saveLevelPDC(block, restoredLevel);
                spawnDisplay(block, restoredLevel);
            }
        }

        // Reconnect hives placed before Toolkit was installed or while tracking
        // was disabled. Tile-entity scanning avoids a full block scan per chunk.
        for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
            Block hive = state.getBlock();
            if (hive.getBlockData() instanceof Beehive) {
                tryTrackBelow(hive);
            }
        }
    }

    public static void restoreAll() {
        if (plugin == null) return;
        for (var world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                restoreChunk(chunk);
            }
        }
    }

    private static int[] parseCoords(String s) {
        String[] p = s.split("_");
        if (p.length != 3) return null;
        try {
            return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean inChunk(Chunk chunk, String k) {
        String[] p = k.split(":");
        if (p.length != 4) return false;
        try {
            int cx = chunk.getX() << 4;
            int cz = chunk.getZ() << 4;
            return chunk.getWorld().getUID().toString().equals(p[0])
                    && Integer.parseInt(p[1]) >= cx && Integer.parseInt(p[1]) < cx + 16
                    && Integer.parseInt(p[3]) >= cz && Integer.parseInt(p[3]) < cz + 16;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void unloadChunkMap(Chunk chunk, Map<String, ?> map) {
        map.keySet().removeIf(k -> inChunk(chunk, k));
    }

    private static void unloadDisplays(Chunk chunk) {
        Iterator<Map.Entry<String, ItemDisplay>> iter = honeyDisplays.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ItemDisplay> entry = iter.next();
            ItemDisplay display = entry.getValue();
            if (display == null || !display.isValid()) {
                iter.remove();
                continue;
            }
            if (inChunk(chunk, entry.getKey())) {
                display.remove();
                iter.remove();
            }
        }
    }

    private static void saveTrackPDC(Block block) {
        if (plugin == null) return;
        block.getChunk().getPersistentDataContainer()
                .set(new NamespacedKey(plugin, TRACK_PREFIX + coordsOf(block)), PersistentDataType.INTEGER, 1);
    }

    private static void clearTrackPDC(Block block) {
        if (plugin == null) return;
        block.getChunk().getPersistentDataContainer()
                .remove(new NamespacedKey(plugin, TRACK_PREFIX + coordsOf(block)));
    }

    private static void saveLevelPDC(Block block, int level) {
        if (plugin == null) return;
        block.getChunk().getPersistentDataContainer()
                .set(new NamespacedKey(plugin, LEVEL_PREFIX + coordsOf(block)), PersistentDataType.INTEGER, level);
    }

    private static void clearLevelPDC(Block block) {
        if (plugin == null) return;
        block.getChunk().getPersistentDataContainer()
                .remove(new NamespacedKey(plugin, LEVEL_PREFIX + coordsOf(block)));
    }
}
