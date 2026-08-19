package org.example1.toolkit.light;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;

public final class InvisibleLightListener implements Listener {

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
            event.getPlayer().sendActionBar(Component.text(
                    Lang.get(event.getPlayer(), "invisible-light-level").formatted(next),
                    NamedTextColor.YELLOW));

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

            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}
