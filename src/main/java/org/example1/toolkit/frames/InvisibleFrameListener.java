package org.example1.toolkit.frames;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Config;

/**
 * Размещение невидимой рамки: невидимые рамки не ставятся как обычные,
 * а спавнятся с setVisible(false).
 */
public final class InvisibleFrameListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(PlayerInteractEvent event) {
        if (!Config.INVISIBLE_FRAMES_ENABLED) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !InvisibleFrameRecipe.isInvisibleFrame(item)) return;

        Block block = event.getClickedBlock();
        BlockFace face = event.getBlockFace();
        if (block == null || block.getType().isAir() || face == BlockFace.SELF) return;

        event.setCancelled(true);

        Block target = block.getRelative(face);
        if (!target.getType().isAir()) return;
        Location center = target.getLocation().add(0.5, 0.5, 0.5);
        for (Entity e : block.getWorld().getNearbyEntities(center, 0.5, 0.5, 0.5)) {
            if (e instanceof Hanging) return;
        }

        ItemFrame frame = block.getWorld().spawn(center, ItemFrame.class);
        frame.setFacingDirection(face, false);
        frame.setVisible(false);

        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }
}
