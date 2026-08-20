package org.example1.toolkit.light;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InvisibleLightListener implements Listener {

    static final Set<String> LIGHT_BLOCKS = ConcurrentHashMap.newKeySet();

    static String locKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    public static void loadAll(JavaPlugin plugin) {
        LIGHT_BLOCKS.clear();
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
                    LIGHT_BLOCKS.add(w.getName() + ":" + x + ":" + y + ":" + z);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void unloadChunk(Chunk chunk) {
        String prefix = chunk.getWorld().getName() + ":";
        int cx = chunk.getX() << 4;
        int cz = chunk.getZ() << 4;
        LIGHT_BLOCKS.removeIf(k -> {
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

    @EventHandler(priority = EventPriority.HIGH)
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
                    Msg.prefix(Lang.get(event.getPlayer(), "light-prefix"), NamedTextColor.YELLOW),
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
            target.setType(Material.LIGHT, false);
            BlockData data = target.getBlockData();
            if (data instanceof org.bukkit.block.data.type.Light lightData) {
                lightData.setLevel(level);
                target.setBlockData(lightData, false);
            }

            LIGHT_BLOCKS.add(locKey(target));
            saveLightPDC(target, level);

            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.LIGHT) return;
        String k = locKey(event.getBlock());
        if (LIGHT_BLOCKS.remove(k)) {
            clearLightPDC(event.getBlock());
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
