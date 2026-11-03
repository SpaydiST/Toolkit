package org.example1.toolkit.parrot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.example1.toolkit.Config;
import org.example1.toolkit.nms.Nms;

public final class ParrotTransplantListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!Config.PARROT_TRANSPLANT_ENABLED) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        boolean hasLeft = Nms.hasShoulderParrot(player, true);
        boolean hasRight = Nms.hasShoulderParrot(player, false);

        if (!hasLeft && !hasRight) return;

        event.setCancelled(true);

        int variant = Nms.getShoulderParrotVariant(player, hasLeft);
        Nms.clearShoulderEntity(player, hasLeft);

        Location spawnLoc = block.getLocation().add(0.5, 1.0, 0.5);
        if (!block.getRelative(BlockFace.UP).getType().isAir()) {
            spawnLoc = block.getLocation().add(0.5, 0.0, 0.5);
        }

        Parrot parrot = (Parrot) block.getWorld().spawnEntity(spawnLoc, EntityType.PARROT);
        if (variant >= 0 && variant < Parrot.Variant.values().length) {
            parrot.setVariant(Parrot.Variant.values()[variant]);
        } else {
            parrot.setVariant(Parrot.Variant.RED);
        }
        parrot.setPersistent(true);
        parrot.setTamed(true);
        parrot.setOwner(player);
        parrot.setSitting(true);
    }
}
