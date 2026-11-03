package org.example1.toolkit.frames;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Config;
import org.example1.toolkit.progression.AchievementProgression;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Размещение невидимой рамки: невидимые рамки не ставятся как обычные,
 * а спавнятся с setVisible(false).
 */
public final class InvisibleFrameListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
        InvisibleFrameOverlay.mark(frame);
        InvisibleFrameOverlay.track(frame);
        AchievementProgression.award(event.getPlayer(), "toolkit_frame");

        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvisibleFrameBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)
                || !InvisibleFrameOverlay.isMarked(frame)) return;

        event.setCancelled(true);
        Location dropLocation = frame.getLocation();
        ItemStack displayedItem = frame.getItem().clone();
        float itemDropChance = frame.getItemDropChance();
        boolean creativeBreak = event instanceof HangingBreakByEntityEvent byEntity
                && byEntity.getRemover() instanceof Player player
                && player.getGameMode() == GameMode.CREATIVE;
        frame.remove();

        if (creativeBreak) return;

        frame.getWorld().dropItemNaturally(dropLocation, InvisibleFrameRecipe.createResult());
        if (!displayedItem.getType().isAir()
                && ThreadLocalRandom.current().nextFloat() < itemDropChance) {
            frame.getWorld().dropItemNaturally(dropLocation, displayedItem);
        }
    }
}
