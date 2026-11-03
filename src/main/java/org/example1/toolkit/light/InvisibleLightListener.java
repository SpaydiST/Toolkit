package org.example1.toolkit.light;

import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;
import org.example1.toolkit.progression.AchievementProgression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InvisibleLightListener implements Listener {

    /** Мир -> чанк -> упакованные координаты невидимых источников света. */
    private static final Map<UUID, Map<Long, Set<Long>>> LIGHTS = new ConcurrentHashMap<>();

    private static long chunkKey(int x, int z) {
        return ((long) (x >> 4) & 0xFFFFFFFFL) | ((long) (z >> 4) << 32);
    }

    static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF);
    }

    static int unpackX(long p) {
        return (int) (p >> 38);
    }

    static int unpackZ(long p) {
        return (int) ((p << 26) >> 38);
    }

    static int unpackY(long p) {
        return (int) (p & 0xFFF) - 2048;
    }

    private static void track(World world, int x, int y, int z) {
        LIGHTS.computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey(x, z), k -> ConcurrentHashMap.newKeySet())
                .add(pack(x, y, z));
    }

    static boolean untrack(World world, int x, int y, int z) {
        Map<Long, Set<Long>> chunks = LIGHTS.get(world.getUID());
        if (chunks == null) return false;
        Set<Long> set = chunks.get(chunkKey(x, z));
        return set != null && set.remove(pack(x, y, z));
    }

    /** Источники света в чанках вокруг точки (по горизонтали на radiusChunks в каждую сторону). */
    static List<Long> nearby(World world, int x, int z, int radiusChunks) {
        Map<Long, Set<Long>> chunks = LIGHTS.get(world.getUID());
        if (chunks == null || chunks.isEmpty()) return List.of();
        List<Long> out = new ArrayList<>();
        int cx = x >> 4;
        int cz = z >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                Set<Long> set = chunks.get(chunkKey((cx + dx) << 4, (cz + dz) << 4));
                if (set != null) out.addAll(set);
            }
        }
        return out;
    }

    /** Убирает запись о блоке, который больше не LIGHT (взрыв, поршень, вода и т.п.), и метку в чанке. */
    static void forget(World world, int x, int y, int z) {
        untrack(world, x, y, z);
        world.getChunkAt(x >> 4, z >> 4).getPersistentDataContainer()
                .remove(new NamespacedKey("toolkit", "il_" + x + "_" + y + "_" + z));
    }

    public static void loadAll(JavaPlugin plugin) {
        LIGHTS.clear();
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadChunk(chunk);
            }
        }
    }

    public static void loadChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        World w = chunk.getWorld();
        for (NamespacedKey key : pdc.getKeys()) {
            String keyStr = key.getKey();
            if (!keyStr.startsWith("il_")) continue;
            String[] parts = keyStr.substring(3).split("_");
            if (parts.length != 3) continue;
            try {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                if (w.getBlockAt(x, y, z).getType() == Material.LIGHT) {
                    track(w, x, y, z);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void unloadChunk(Chunk chunk) {
        Map<Long, Set<Long>> chunks = LIGHTS.get(chunk.getWorld().getUID());
        if (chunks != null) chunks.remove(chunkKey(chunk.getX() << 4, chunk.getZ() << 4));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!Config.INVISIBLE_LIGHT_ENABLED) return;

        ItemStack item = event.getItem();
        if (item == null || !InvisibleLightRecipe.isInvisibleLight(item)) return;

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            int current = InvisibleLightRecipe.getLevel(item);
            boolean backward = event.getPlayer().isSneaking();
            int next;

            if (backward) {
                next = current <= InvisibleLightRecipe.MIN_LEVEL
                        ? InvisibleLightRecipe.MAX_LEVEL
                        : current - 1;
            } else {
                next = current >= InvisibleLightRecipe.MAX_LEVEL
                        ? InvisibleLightRecipe.MIN_LEVEL
                        : current + 1;
            }

            InvisibleLightRecipe.setLevel(item, next);
            Msg.of(event.getPlayer(),
                    Msg.prefix(Lang.get(event.getPlayer(), "light-prefix"), Msg.EMERALD),
                    Msg.sep(),
                    Msg.action(Lang.get(event.getPlayer(), "light-level")),
                    Msg.sep(),
                    Msg.val(String.valueOf(next)));

        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Block target = block.getRelative(event.getBlockFace());
            if (!target.getType().isAir() && target.getType() != Material.LIGHT) return;

            event.setCancelled(true);

            int level = InvisibleLightRecipe.getLevel(item);
            BlockState replaced = target.getState();
            target.setType(Material.LIGHT, false);
            BlockData data = target.getBlockData();
            if (data instanceof org.bukkit.block.data.type.Light lightData) {
                lightData.setLevel(level);
                target.setBlockData(lightData, false);
            }

            // Даём плагинам защиты и логирования увидеть установку как обычную постановку блока.
            BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, replaced, block, item,
                    event.getPlayer(), true, event.getHand() == null ? EquipmentSlot.HAND : event.getHand());
            if (!placeEvent.callEvent() || !placeEvent.canBuild()) {
                replaced.update(true, false);
                return;
            }

            track(target.getWorld(), target.getX(), target.getY(), target.getZ());
            saveLightPDC(target, level);
            AchievementProgression.award(event.getPlayer(), "toolkit_light");

            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.LIGHT) return;
        Block b = event.getBlock();
        if (untrack(b.getWorld(), b.getX(), b.getY(), b.getZ())) {
            clearLightPDC(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() == Material.LIGHT) return;
        if (untrack(placed.getWorld(), placed.getX(), placed.getY(), placed.getZ())) {
            clearLightPDC(placed);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Config.INVISIBLE_LIGHT_ENABLED) return;
        loadChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!Config.INVISIBLE_LIGHT_ENABLED) return;
        unloadChunk(event.getChunk());
    }

    private static void saveLightPDC(Block block, int level) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey("toolkit", "il_" + block.getX() + "_" + block.getY() + "_" + block.getZ());
        pdc.set(key, PersistentDataType.INTEGER, level);
    }

    private static void clearLightPDC(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        pdc.remove(new NamespacedKey("toolkit", "il_" + block.getX() + "_" + block.getY() + "_" + block.getZ()));
    }
}
