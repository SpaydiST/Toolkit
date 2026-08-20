package org.example1.toolkit.cauldron;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.example1.toolkit.Config;

public final class CauldronBedrockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.WATER_CAULDRON && type != Material.CAULDRON) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        boolean isWaterCauldron = type == Material.WATER_CAULDRON;
        int level = isWaterCauldron ? CauldronBedrock.getWaterLevel(block) : 0;

        if (CauldronBedrock.isPotion(item)) {
            handlePotion(event, player, block, item, level);
        } else if (CauldronBedrock.isDye(item) && isWaterCauldron) {
            handleDye(event, player, block, item, level);
        } else if (CauldronBedrock.isLeatherArmor(item) && isWaterCauldron) {
            handleLeatherArmor(event, player, block, item, level);
        } else if (CauldronBedrock.isDyedBanner(item) && isWaterCauldron) {
            handleCleanBanner(event, player, block, item, level);
        } else if (CauldronBedrock.isDyedShulker(item) && isWaterCauldron) {
            handleCleanShulker(event, player, block, item, level);
        }
    }

    private void handlePotion(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        event.setCancelled(true);

        if (CauldronBedrock.isWaterPotion(item)) {
            int newLevel = Math.min(level + 1, 3);
            CauldronBedrock.setWaterLevel(block, newLevel);
            CauldronBedrock.removeAll(block);
            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setType(Material.GLASS_BOTTLE);
                item.editMeta(m -> {});
            }
            player.sendActionBar(Component.text("Котёл наполнен водой", NamedTextColor.AQUA));
            return;
        }

        CauldronBedrock.PotionData data = CauldronBedrock.potionFromItem(item);
        if (data == null) return;

        CauldronBedrock.setPotion(block, data);
        CauldronBedrock.setDyeColor(block, null);
        int newLevel = Math.min(level + 1, 3);
        CauldronBedrock.setWaterLevel(block, newLevel);

        Color color = CauldronBedrock.potionColor(data.type());
        CauldronBedrock.spawnColoredDisplay(block, color, newLevel);

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setType(Material.GLASS_BOTTLE);
            item.editMeta(m -> {});
        }

        player.sendActionBar(Component.text("Зелье добавлено в котёл", NamedTextColor.LIGHT_PURPLE));
        spawnDust(block, color);
    }

    private void handleDye(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        DyeColor dye = CauldronBedrock.dyeFromMaterial(item.getType());
        if (dye == null) return;

        event.setCancelled(true);

        Color existing = CauldronBedrock.getDyeColor(block);
        Color newColor = existing == null ? dye.getColor() : CauldronBedrock.mixColors(existing, dye.getColor());

        CauldronBedrock.setDyeColor(block, newColor);
        CauldronBedrock.setPotion(block, null);

        CauldronBedrock.spawnColoredDisplay(block, newColor, level);

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        player.sendActionBar(Component.text("Краска добавлена в котёл", NamedTextColor.AQUA));
        spawnDust(block, newColor);
    }

    private void handleLeatherArmor(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        Color dyeColor = CauldronBedrock.getDyeColor(block);
        CauldronBedrock.PotionData potionData = CauldronBedrock.getPotion(block);

        if (dyeColor != null) {
            event.setCancelled(true);
            CauldronBedrock.applyDyeToArmor(item, dyeColor);
            int newLevel = level - 1;
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (newLevel <= 0) {
                CauldronBedrock.removeAll(block);
            } else {
                CauldronBedrock.updateDisplayLevel(block, newLevel);
            }
            player.sendActionBar(Component.text("Броня окрашена!", NamedTextColor.GREEN));
        } else if (potionData != null) {
            event.setCancelled(true);
            CauldronBedrock.applyPotionToArmor(item, potionData);
            int newLevel = level - 1;
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (newLevel <= 0) {
                CauldronBedrock.removeAll(block);
            } else {
                CauldronBedrock.updateDisplayLevel(block, newLevel);
            }
            player.sendActionBar(Component.text("Броня пропитана зельем!", NamedTextColor.GREEN));
        } else {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            boolean isDefault = CauldronBedrock.isDefaultLeatherColor(meta);
            if (isDefault) return;

            event.setCancelled(true);
            CauldronBedrock.cleanArmor(item);
            int newLevel = level - 1;
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (newLevel <= 0) {
                CauldronBedrock.removeAll(block);
            } else {
                CauldronBedrock.updateDisplayLevel(block, newLevel);
            }
            player.sendActionBar(Component.text("Броня очищена", NamedTextColor.GREEN));
        }
    }

    private void handleCleanBanner(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        event.setCancelled(true);
        CauldronBedrock.cleanBanner(item);
        int newLevel = level - 1;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }
        player.sendActionBar(Component.text("Флаг очищен", NamedTextColor.GREEN));
    }

    private void handleCleanShulker(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        event.setCancelled(true);
        CauldronBedrock.cleanShulker(item);
        int newLevel = level - 1;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }
        player.sendActionBar(Component.text("Шалкер очищен", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Material m = event.getBlock().getType();
        if (m == Material.WATER_CAULDRON || m == Material.CAULDRON) {
            CauldronBedrock.removeAll(event.getBlock());
        }
    }

    private void spawnDust(Block block, Color color) {
        int level = CauldronBedrock.getWaterLevel(block);
        double y = level > 0 ? CauldronBedrock.waterLevelY(level) - 0.02 : 0.5;
        block.getWorld().spawnParticle(
                Particle.DUST,
                block.getLocation().add(0.5, y, 0.5),
                10, 0.15, 0.01, 0.15, 0,
                new Particle.DustOptions(color, 1.0f));
    }
}
