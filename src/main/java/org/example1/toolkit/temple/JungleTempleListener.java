package org.example1.toolkit.temple;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import org.example1.toolkit.Config;
import org.example1.toolkit.progression.AchievementProgression;

public final class JungleTempleListener implements Listener {

    private static final NamespacedKey CHUNK_CHECKED_KEY = new NamespacedKey("toolkit", "jt_checked");

    private static NamespacedKey dispenserKey(Block block) {
        return new NamespacedKey("toolkit", "jt_disp_" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.DISPENSER) {
            // Раздатчик, поставленный игроком, помечаем как уже обработанный, чтобы не превращать его стрелы в ядовитые
            event.getBlockPlaced().getChunk().getPersistentDataContainer()
                    .set(dispenserKey(event.getBlockPlaced()), PersistentDataType.BYTE, (byte) 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (!Config.JUNGLE_TEMPLE_ARROWS_ENABLED) return;
        if (event.getItem().getType() != Material.ARROW && event.getItem().getType() != Material.TIPPED_ARROW) return;
        Block block = event.getBlock();
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = dispenserKey(block);
        Byte marker = pdc.get(key, PersistentDataType.BYTE);
        if (marker != null && marker == 2) return;
        if (marker == null) {
            if (!isInJungleTemple(block)) {
                pdc.set(key, PersistentDataType.BYTE, (byte) 1);
                return;
            }
            pdc.set(key, PersistentDataType.BYTE, (byte) 3);
        } else if (marker == 1) {
            return;
        }
        if (event.getItem().getType() == Material.ARROW) {
            event.setItem(createPoisonArrow(event.getItem().getAmount()));
            convertDispenser(block);
        }
        awardNearestPlayer(block);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Config.JUNGLE_TEMPLE_ARROWS_ENABLED) return;
        Chunk chunk = event.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (pdc.has(CHUNK_CHECKED_KEY, PersistentDataType.BYTE)) return;
        pdc.set(CHUNK_CHECKED_KEY, PersistentDataType.BYTE, (byte) 1);

        for (var state : chunk.getTileEntities()) {
            if (state instanceof Dispenser dispenser) {
                Block block = dispenser.getBlock();
                NamespacedKey key = dispenserKey(block);
                Byte marker = pdc.get(key, PersistentDataType.BYTE);
                if (marker != null) continue;
                if (isInJungleTemple(block)) {
                    pdc.set(key, PersistentDataType.BYTE, (byte) 3);
                    convertDispenser(block);
                } else {
                    pdc.set(key, PersistentDataType.BYTE, (byte) 1);
                }
            }
        }
    }

    private ItemStack createPoisonArrow(int amount) {
        ItemStack tipped = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta meta = (PotionMeta) tipped.getItemMeta();
        meta.setBasePotionType(PotionType.POISON);
        tipped.setItemMeta(meta);
        return tipped;
    }

    private void convertDispenser(Block block) {
        if (!(block.getState() instanceof Dispenser dispenser)) return;
        Inventory inv = dispenser.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.ARROW) {
                inv.setItem(i, createPoisonArrow(item.getAmount()));
            }
        }
    }

    private void awardNearestPlayer(Block dispenser) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Location center = dispenser.getLocation().add(0.5, 0.5, 0.5);
        for (Player player : dispenser.getWorld().getNearbyPlayers(center, 8.0)) {
            double distance = player.getLocation().distanceSquared(center);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            AchievementProgression.award(nearest, "toolkit_temple");
        }
    }

    private boolean isInJungleTemple(Block block) {
        Location loc = block.getLocation();
        var world = loc.getWorld();
        if (world == null) return false;

        var result = world.locateNearestStructure(loc,
                org.bukkit.generator.structure.StructureType.JUNGLE_TEMPLE, 3, false);
        if (result != null) {
            Location center = result.getLocation();
            if (center.getWorld() == loc.getWorld() &&
                    Math.abs(center.getBlockX() - loc.getBlockX()) < 32 &&
                    Math.abs(center.getBlockZ() - loc.getBlockZ()) < 32) {
                return true;
            }
        }
        return false;
    }
}
