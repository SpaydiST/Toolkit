package org.example1.toolkit.temple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.example1.toolkit.Config;

public final class JungleTempleListener implements Listener {

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (!Config.JUNGLE_TEMPLE_ARROWS_ENABLED) return;
        if (event.getItem().getType() != Material.ARROW) return;
        if (!isInJungleTemple(event.getBlock())) return;

        convertDispenser(event.getBlock());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Config.JUNGLE_TEMPLE_ARROWS_ENABLED) return;
        for (var state : event.getChunk().getTileEntities()) {
            if (state instanceof Dispenser && state instanceof Block block) {
                if (isInJungleTemple(block)) {
                    convertDispenser(block);
                }
            }
        }
    }

    private void convertDispenser(Block block) {
        if (!(block.getState() instanceof Dispenser dispenser)) return;
        Inventory inv = dispenser.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.ARROW) {
                ItemStack tipped = new ItemStack(Material.TIPPED_ARROW, item.getAmount());
                PotionMeta meta = (PotionMeta) tipped.getItemMeta();
                meta.setBasePotionType(PotionType.POISON);
                tipped.setItemMeta(meta);
                inv.setItem(i, tipped);
            }
        }
    }

    private boolean isInJungleTemple(Block block) {
        Location loc = block.getLocation();
        var world = loc.getWorld();
        if (world == null) return false;

        var result = world.locateNearestStructure(loc,
                org.bukkit.generator.structure.StructureType.JUNGLE_TEMPLE, 100, false);
        if (result != null) {
            Location center = result.getLocation();
            if (center.getWorld() == loc.getWorld() &&
                    Math.abs(center.getBlockX() - loc.getBlockX()) < 30 &&
                    Math.abs(center.getBlockZ() - loc.getBlockZ()) < 30) {
                return true;
            }
        }
        return false;
    }
}
