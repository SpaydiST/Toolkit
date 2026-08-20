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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CauldronBedrockListener implements Listener {

    private final Set<UUID> inPotionCauldron = new HashSet<>();

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
        } else if (item.getType() == Material.GLASS_BOTTLE && isWaterCauldron) {
            handleBottleTake(event, player, block, item, level);
        } else if (item.getType() == Material.ARROW && isWaterCauldron) {
            handleArrow(event, player, block, item, level);
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
            CauldronBedrock.PotionData existing = CauldronBedrock.getPotion(block);
            if (existing != null) return;
            if (level >= 3) return;
            int newLevel = Math.min(level + 1, 3);
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setType(Material.GLASS_BOTTLE);
                item.editMeta(m -> {});
            }
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.AQUA),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "water-filled")),
                    Msg.sep(),
                    Msg.dim(Lang.get(player, "level")),
                    Msg.level(newLevel, 3));
            return;
        }

        CauldronBedrock.PotionData data = CauldronBedrock.potionFromItem(item);
        if (data == null) return;

        if (level >= 3) return;

        CauldronBedrock.PotionData existing = CauldronBedrock.getPotion(block);

        CauldronBedrock.PotionData result;
        if (existing != null) {
            if (!CauldronBedrock.canMix(existing, data)) {
                Msg.of(player,
                        Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.RED),
                        Msg.sep(),
                        Msg.action(Lang.get(player, "potion-full")));
                return;
            }
            if (existing.type() == data.type() && existing.mixCount() == 1) {
                result = new CauldronBedrock.PotionData(data.type(), List.of(), 1, List.of(data.type()));
            } else {
                result = CauldronBedrock.mixPotions(existing, data);
                Msg.of(player,
                        Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.LIGHT_PURPLE),
                        Msg.sep(),
                        Msg.action(Lang.get(player, "potion-mixed")),
                        Msg.sep(),
                        Msg.level(result.mixCount(), 3));
            }
        } else {
            result = data;
        }

        CauldronBedrock.setPotion(block, result);
        CauldronBedrock.setDyeColor(block, null);
        int newLevel = Math.min(level + 1, 3);
        CauldronBedrock.setWaterLevel(block, newLevel);

        Color color = result.mixedTypes().size() > 1
                ? CauldronBedrock.mixPotionColors(result.mixedTypes())
                : CauldronBedrock.potionColor(result.type());
        CauldronBedrock.spawnColoredDisplay(block, color, newLevel);

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setType(Material.GLASS_BOTTLE);
            item.editMeta(m -> {});
        }

        if (existing == null || existing.type() == data.type()) {
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.LIGHT_PURPLE),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "potion-added")),
                    Msg.sep(),
                    Msg.level(newLevel, 3));
        }
        spawnDust(block, color);
    }

    private void handleBottleTake(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        if (level <= 0) return;

        event.setCancelled(true);

        CauldronBedrock.PotionData data = CauldronBedrock.getPotion(block);

        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack potion = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            if (data != null) {
                if (data.mixCount() > 1) {
                    meta.setBasePotionType(PotionType.AWKWARD);
                    for (PotionEffect effect : CauldronBedrock.getAllEffects(data)) {
                        meta.addCustomEffect(effect, true);
                    }
                } else {
                    meta.setBasePotionType(data.type());
                }
            }
            potion.setItemMeta(meta);
            if (data != null && data.mixCount() > 1) {
                CauldronBedrock.saveMixToItem(potion, data);
            }
            item.setAmount(item.getAmount() - 1);
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), potion);
            } else {
                player.getInventory().addItem(potion);
            }
        }

        int newLevel = level - 1;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }
        Msg.of(player,
                Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.LIGHT_PURPLE),
                Msg.sep(),
                Msg.action(Lang.get(player, "potion-taken")),
                Msg.sep(),
                Msg.level(Math.max(newLevel, 0), 3));
    }

    private void handleArrow(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        CauldronBedrock.PotionData data = CauldronBedrock.getPotion(block);
        if (data == null) return;
        if (level <= 0) return;

        event.setCancelled(true);

        boolean isMixed = data.mixCount() > 1;
        int perLevel = isMixed ? 16 : 21;
        int maxTippable = level * perLevel;
        int arrowsToTip = Math.min(item.getAmount(), maxTippable);
        if (arrowsToTip <= 0) return;

        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack tipped = new ItemStack(Material.TIPPED_ARROW, arrowsToTip);
            PotionMeta arrowMeta = (PotionMeta) tipped.getItemMeta();
            if (isMixed) {
                arrowMeta.setBasePotionType(PotionType.AWKWARD);
                for (PotionEffect effect : CauldronBedrock.getAllEffects(data)) {
                    arrowMeta.addCustomEffect(effect, true);
                }
            } else {
                arrowMeta.setBasePotionType(data.type());
            }
            tipped.setItemMeta(arrowMeta);
            item.setAmount(item.getAmount() - arrowsToTip);
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), tipped);
            } else {
                player.getInventory().addItem(tipped);
            }
        }

        int levelsUsed = (int) Math.ceil((double) arrowsToTip / perLevel);
        int newLevel = level - levelsUsed;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }
        Component arrowMsg = Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.LIGHT_PURPLE)
                .append(Msg.sep())
                .append(Msg.action(Lang.get(player, "arrows-tipped")))
                .append(Msg.sep())
                .append(Msg.count(arrowsToTip, Lang.get(player, "pcs")))
                .append(Msg.sep())
                .append(Msg.level(Math.max(newLevel, 0), 3));
        if (isMixed) {
            arrowMsg = arrowMsg.append(Msg.sep()).append(Msg.accent(Lang.get(player, "mixed"), NamedTextColor.LIGHT_PURPLE));
        }
        player.sendActionBar(arrowMsg);
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

        Msg.of(player,
                Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.AQUA),
                Msg.sep(),
                Msg.action(Lang.get(player, "dye-added")),
                Msg.sep(),
                Msg.level(level, 3));
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
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.GREEN),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "armor-dyed")));
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
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.GREEN),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "armor-potioned")));
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
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.GREEN),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "armor-cleaned")));
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
        int remaining = CauldronBedrock.bannerLayers(item);
        if (remaining > 0) {
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.GREEN),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "banner-layer")),
                    Msg.sep(),
                    Msg.val(String.valueOf(remaining)));
        } else {
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.GREEN),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "banner-cleaned")));
        }
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
        Msg.of(player,
                Msg.prefix(Lang.get(player, "cauldron"), NamedTextColor.GREEN),
                Msg.sep(),
                Msg.action(Lang.get(player, "shulker-cleaned")));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        Player player = event.getPlayer();
        Block toBlock = event.getTo() != null ? event.getTo().getBlock() : null;
        if (toBlock == null) return;

        boolean wasInside = inPotionCauldron.contains(player.getUniqueId());
        boolean nowInside = false;

        if (toBlock.getType() == Material.WATER_CAULDRON) {
            CauldronBedrock.PotionData data = CauldronBedrock.getPotion(toBlock);
            if (data != null) {
                nowInside = true;
                if (!wasInside) {
                    for (PotionEffect effect : CauldronBedrock.getAllEffects(data)) {
                        player.addPotionEffect(effect);
                    }
                    inPotionCauldron.add(player.getUniqueId());
                }
            }
        }

        if (!nowInside && wasInside) {
            inPotionCauldron.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Material m = event.getBlock().getType();
        if (m == Material.WATER_CAULDRON || m == Material.CAULDRON) {
            CauldronBedrock.removeAll(event.getBlock());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        CauldronBedrock.restoreChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        CauldronBedrock.unloadChunk(event.getChunk());
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
