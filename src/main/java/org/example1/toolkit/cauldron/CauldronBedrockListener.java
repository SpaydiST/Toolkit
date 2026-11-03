package org.example1.toolkit.cauldron;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;
import org.example1.toolkit.progression.AchievementProgression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CauldronBedrockListener implements Listener {

    private static final int BATH_OVERLOAD_SECONDS = 5;
    private final Map<UUID, BathStay> bathStays = new HashMap<>();

    private record BathStay(Block block, int seconds) {}

    private static final PotionEffectType[] CHAOS_POSITIVE_EFFECTS = {
            PotionEffectType.SPEED,
            PotionEffectType.HASTE,
            PotionEffectType.STRENGTH,
            PotionEffectType.JUMP_BOOST,
            PotionEffectType.REGENERATION,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.WATER_BREATHING,
            PotionEffectType.INVISIBILITY,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.ABSORPTION,
            PotionEffectType.LUCK,
            PotionEffectType.SLOW_FALLING
    };

    private static final PotionEffectType[] CHAOS_NEGATIVE_EFFECTS = {
            PotionEffectType.SLOWNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.WEAKNESS,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.BLINDNESS,
            PotionEffectType.DARKNESS,
            PotionEffectType.NAUSEA,
            PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION,
            PotionEffectType.UNLUCK,
            PotionEffectType.OOZING,
            PotionEffectType.WEAVING,
            PotionEffectType.INFESTED,
            PotionEffectType.WIND_CHARGED
    };

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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

        if (CauldronHoney.isHoneyCauldron(block)) {
            if (item.getType() == Material.GLASS_BOTTLE) {
                event.setCancelled(true);
                int before = CauldronHoney.getHoneyLevel(block);
                CauldronHoney.handleTake(player, block);
                if (CauldronHoney.getHoneyLevel(block) < before) {
                    AchievementProgression.award(player, "toolkit_honey");
                }
            } else if (item.getType() == Material.HONEY_BOTTLE) {
                event.setCancelled(true);
                int before = CauldronHoney.getHoneyLevel(block);
                CauldronHoney.handlePour(player, block);
                if (CauldronHoney.getHoneyLevel(block) > before) {
                    AchievementProgression.award(player, "toolkit_honey");
                }
            }
            return;
        }

        if (item.getType() == Material.HONEY_BOTTLE && type == Material.CAULDRON) {
            event.setCancelled(true);
            int before = CauldronHoney.getHoneyLevel(block);
            CauldronHoney.handlePour(player, block);
            if (CauldronHoney.getHoneyLevel(block) > before) {
                AchievementProgression.award(player, "toolkit_honey");
            }
            return;
        }

        if (CauldronBedrock.isPotion(item)) {
            handlePotion(event, player, block, item, level);
        } else if (item.getType() == Material.GLASS_BOTTLE && isWaterCauldron) {
            handleBottleTake(event, player, block, item, level);
        } else if (item.getType() == Material.ARROW && isWaterCauldron) {
            handleArrow(event, player, block, item, level);
        } else if (item.getType() == Material.TIPPED_ARROW && isWaterCauldron) {
            handleTippedArrow(event, player, block, item, level);
        } else if ((item.getType() == Material.GUNPOWDER || item.getType() == Material.BREEZE_ROD)
                && isWaterCauldron && CauldronBedrock.getPotion(block) != null) {
            handlePotionInfusion(event, player, block, item);
        } else if ((item.getType() == Material.REDSTONE || item.getType() == Material.GLOWSTONE_DUST)
                && isWaterCauldron && CauldronBedrock.getPotion(block) != null) {
            handleIngredient(event, player, block, item);
        } else if (CauldronBedrock.isDye(item) && isWaterCauldron) {
            handleDye(event, player, block, item, level);
        } else if (CauldronBedrock.isLeatherArmor(item) && isWaterCauldron) {
            handleLeatherArmor(event, player, block, item, level);
        } else if (CauldronBedrock.isDyedBanner(item) && isWaterCauldron) {
            handleCleanBanner(event, player, block, item, level);
        } else if (CauldronBedrock.isDyedShulker(item) && isWaterCauldron) {
            handleCleanShulker(event, player, block, item, level);
        } else if (CauldronBedrock.isWashableBlock(item) && isWaterCauldron) {
            handleWashBlock(event, player, block, item, level);
        }
    }

    private void handlePotion(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        event.setCancelled(true);
        pourPotion(player, block, item, level, true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplashPotion(PotionSplashEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        Block block = event.getHitBlock();
        if (block != null && pourPotion(null, block, event.getEntity().getItem(),
                CauldronBedrock.getWaterLevel(block), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLingeringPotion(LingeringPotionSplashEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        Block block = event.getHitBlock();
        if (block != null && pourPotion(null, block, event.getEntity().getItem(),
                CauldronBedrock.getWaterLevel(block), false)) {
            event.setCancelled(true);
        }
    }

    private boolean pourPotion(Player player, Block block, ItemStack item, int level, boolean byHand) {
        if (block.getType() != Material.WATER_CAULDRON && block.getType() != Material.CAULDRON) return false;
        if (CauldronHoney.isHoneyCauldron(block)) return false;

        if (CauldronBedrock.isWaterPotion(item)) {
            CauldronBedrock.PotionData existing = CauldronBedrock.getPotion(block);
            if (existing != null) return false;
            if (level >= CauldronBedrock.MAX_WATER_LEVEL) return false;
            int newLevel = Math.min(level + 1, CauldronBedrock.MAX_WATER_LEVEL);
            CauldronBedrock.setWaterLevel(block, newLevel);
            Color dyeColor = CauldronBedrock.getDyeColor(block);
            if (dyeColor != null) {
                CauldronBedrock.spawnColoredDisplay(block, dyeColor, newLevel);
            }
            if (byHand) consumePotionToBottle(player, item);
            return true;
        }

        CauldronBedrock.PotionData data = CauldronBedrock.potionFromItem(item);
        if (data == null) return false;
        if (CauldronBedrock.distinctEffectCount(CauldronBedrock.getAllEffects(data)) > CauldronBedrock.MAX_EFFECTS) return false;

        if (level >= CauldronBedrock.MAX_WATER_LEVEL) return false;

        CauldronBedrock.PotionData existing = CauldronBedrock.getPotion(block);
        // Запрещаем наливать зелья в котёл, где уже налита обычная или окрашенная вода
        if (existing == null && level > 0) return false;
        // Частичная оплата остаётся привязана к тому же указателю: при доливе
        // с общим эффектом порядок старых эффектов не меняется.
        if (existing != null && existing.redstoneCredit() > 0
                && !CauldronBedrock.hasOverlappingEffectTypes(existing, data)) return false;

        CauldronBedrock.PotionData result;
        if (existing != null && CauldronBedrock.hasOverlappingEffectTypes(existing, data)) {
            List<PotionEffect> combined = new ArrayList<>(CauldronBedrock.getAllEffects(existing));
            combined.addAll(CauldronBedrock.getAllEffects(data));
            if (CauldronBedrock.distinctEffectCount(combined) > CauldronBedrock.MAX_EFFECTS) return false;
            result = CauldronBedrock.blendOverlappingEffects(existing, data, level);
        } else if (existing != null) {
            if (!CauldronBedrock.canMix(existing, data, level)) {
                return false;
            }

            List<PotionEffect> combined = new ArrayList<>(CauldronBedrock.getAllEffects(existing));
            combined.addAll(CauldronBedrock.getAllEffects(data));
            if (CauldronBedrock.distinctEffectCount(combined) > CauldronBedrock.MAX_EFFECTS) return false;

            CauldronBedrock.MixReaction reaction = CauldronBedrock.evaluateReaction(existing, data);
            boolean incompatible = reaction != CauldronBedrock.MixReaction.NORMAL
                    && reaction != CauldronBedrock.MixReaction.SYNERGY;
            // Несовместимые составы: 50% смесь, 25% дополнительный эффект,
            // 25% нестабильное зелье вместо исходных эффектов.
            int roll = incompatible ? ThreadLocalRandom.current().nextInt(4) : -1;
            if (roll == 0) {
                handleChaosPotion(byHand ? player : null, block, item,
                        existing.powderInfused() || data.powderInfused(),
                        existing.windInfused() || data.windInfused());
                return true;
            }
            result = CauldronBedrock.mixPotions(existing, data,
                    reaction == CauldronBedrock.MixReaction.SYNERGY);
            if (result == null) return false;
            if (roll == 1) {
                List<PotionEffect> effects = new ArrayList<>(result.customEffects());
                PotionEffect bonus = randomBonusEffect(effects);
                if (bonus != null) {
                    // В полном зелье случайный эффект вытесняет один из трёх.
                    if (effects.size() == CauldronBedrock.MAX_EFFECTS) {
                        effects.remove(ThreadLocalRandom.current().nextInt(effects.size()));
                    }
                    effects.add(bonus);
                    result = new CauldronBedrock.PotionData(result.type(), effects,
                            result.mixCount(), result.mixedTypes(), result.currentPointer(), result.redstoneCredit(),
                            result.matchingLayers(), result.durationTotals(), result.amplifierTotals(),
                            result.powderInfused(), result.windInfused());
                }
            }
        } else {
            result = data;
        }

        CauldronBedrock.setPotion(block, result);
        CauldronBedrock.setDyeColor(block, null);
        int newLevel = Math.min(level + 1, CauldronBedrock.MAX_WATER_LEVEL);
        CauldronBedrock.setWaterLevel(block, newLevel);

        Color color = CauldronBedrock.getPotionDisplayColor(result);
        CauldronBedrock.spawnColoredDisplay(block, color, newLevel);

        if (byHand) consumePotionToBottle(player, item);
        if (player != null) AchievementProgression.award(player, "toolkit_cauldron");

        spawnDust(block, color);
        return true;
    }

    private void handleSpoil(PlayerInteractEvent event, Player player, Block block, ItemStack item) {
        int level = CauldronBedrock.getWaterLevel(block);
        int newLevel = Math.min(level + 1, CauldronBedrock.MAX_WATER_LEVEL);

        CauldronBedrock.setPotion(block, null);
        CauldronBedrock.setDyeColor(block, null);
        CauldronBedrock.setWaterLevel(block, newLevel);

        Color color = CauldronBedrock.potionColor(PotionType.WATER);
        CauldronBedrock.spawnColoredDisplay(block, color, newLevel);

        if (player != null) consumePotionToBottle(player, item);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);

        spawnDust(block, color);
    }

    private void handleMixExplode(Player player, Block block, ItemStack item) {
        CauldronBedrock.removeAll(block);
        block.setType(Material.AIR);

        player.getWorld().createExplosion(block.getLocation().add(0.5, 0.5, 0.5), 1.0f, false, false);

        consumePotionToBottle(player, item);

    }

    private void handleChaosPotion(Player player, Block block, ItemStack item,
                                   boolean powderInfused, boolean windInfused) {
        int level = CauldronBedrock.getWaterLevel(block);
        int newLevel = Math.min(level + 1, CauldronBedrock.MAX_WATER_LEVEL);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        PotionEffectType[] pool = rng.nextBoolean() ? CHAOS_POSITIVE_EFFECTS : CHAOS_NEGATIVE_EFFECTS;
        PotionEffectType type = pool[rng.nextInt(pool.length)];
        List<PotionEffect> effects = List.of(new PotionEffect(type, 160 + rng.nextInt(241), rng.nextInt(3)));

        // mixCount = MAX_MIX (3), чтобы мутировавшее зелье было финальным и не абузилось повторным смешиванием
        CauldronBedrock.PotionData randomData = new CauldronBedrock.PotionData(
                PotionType.AWKWARD, effects, CauldronBedrock.MAX_MIX, List.of(PotionType.AWKWARD),
                powderInfused, windInfused
        );

        CauldronBedrock.setPotion(block, randomData);
        CauldronBedrock.setDyeColor(block, null);
        CauldronBedrock.setWaterLevel(block, newLevel);

        Color color = CauldronBedrock.getPotionDisplayColor(randomData);
        CauldronBedrock.spawnColoredDisplay(block, color, newLevel);

        if (player != null) consumePotionToBottle(player, item);

        spawnDust(block, color);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);

    }

    private PotionEffect randomBonusEffect(List<PotionEffect> existing) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<PotionEffectType> available = new ArrayList<>();
        for (PotionEffectType type : CHAOS_POSITIVE_EFFECTS) {
            if (existing.stream().noneMatch(e -> e.getType().equals(type))) available.add(type);
        }
        for (PotionEffectType type : CHAOS_NEGATIVE_EFFECTS) {
            if (existing.stream().noneMatch(e -> e.getType().equals(type))) available.add(type);
        }
        if (available.isEmpty()) return null;
        return new PotionEffect(available.get(rng.nextInt(available.size())), 160 + rng.nextInt(241), rng.nextInt(3));
    }

    private void handleBottleTake(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        if (level <= 0) return;

        event.setCancelled(true);

        CauldronBedrock.PotionData data = CauldronBedrock.getPotion(block);

        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack potion = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            if (data != null) {
                boolean plainSinglePotion = CauldronBedrock.isPlainSinglePotion(data);
                if (!plainSinglePotion) {
                    meta.setBasePotionType(PotionType.AWKWARD);
                    for (PotionEffect effect : CauldronBedrock.getAllEffects(data)) {
                        meta.addCustomEffect(effect, true);
                    }
                    meta.setColor(CauldronBedrock.getPotionDisplayColor(data));
                    if (data.type() == PotionType.AWKWARD && !data.customEffects().isEmpty()) {
                        meta.displayName(Component.text("Нестабильное зелье", NamedTextColor.LIGHT_PURPLE));
                    }
                } else {
                    meta.setBasePotionType(data.type());
                }
            } else {
                meta.setBasePotionType(PotionType.WATER);
            }
            potion.setItemMeta(meta);
            if (data != null) PotionInfusions.write(potion, data.powderInfused(), data.windInfused());
            if (data != null && !CauldronBedrock.isPlainSinglePotion(data)
                    && (data.mixCount() > 1 || !data.customEffects().isEmpty()
                    || data.powderInfused() || data.windInfused())) {
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
        AchievementProgression.award(player, "toolkit_cauldron");
    }

    private void handleArrow(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        CauldronBedrock.PotionData data = CauldronBedrock.getPotion(block);
        if (data == null || level <= 0) return;

        event.setCancelled(true);

        int perLevel = 8;
        if (item.getAmount() < perLevel && player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        int maxBatches = Math.min(item.getAmount() / perLevel, level);
        int levelsUsed = player.getGameMode() == GameMode.CREATIVE ? 1 : Math.max(1, maxBatches);
        int arrowsToTip = player.getGameMode() == GameMode.CREATIVE
                ? Math.min(item.getAmount(), perLevel)
                : levelsUsed * perLevel;

        ItemStack tipped = new ItemStack(Material.TIPPED_ARROW, arrowsToTip);
        PotionMeta arrowMeta = (PotionMeta) tipped.getItemMeta();
        arrowMeta.setBasePotionType(PotionType.AWKWARD);
        for (PotionEffect effect : CauldronBedrock.limitArrowDurations(CauldronBedrock.getAllEffects(data))) {
            int duration = effect.getType().isInstant() ? 1 : effect.getDuration() * 8;
            arrowMeta.addCustomEffect(new PotionEffect(effect.getType(), duration, effect.getAmplifier(),
                    effect.isAmbient(), effect.hasParticles(), effect.hasIcon()), true);
        }
        arrowMeta.setColor(CauldronBedrock.getPotionDisplayColor(data));
        tipped.setItemMeta(arrowMeta);
        PotionInfusions.write(tipped, data.powderInfused(), data.windInfused());
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - arrowsToTip);
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), tipped);
        } else {
            player.getInventory().addItem(tipped);
        }

        int newLevel = level - levelsUsed;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }

        AchievementProgression.award(player, "toolkit_arrows");
    }

    private void handleIngredient(PlayerInteractEvent event, Player player, Block block, ItemStack item) {
        event.setCancelled(true);
        CauldronBedrock.PotionData data = CauldronBedrock.getPotion(block);
        List<PotionEffect> effects = CauldronBedrock.getAllEffects(data);
        if (effects.isEmpty()) return;
        boolean glowstone = item.getType() == Material.GLOWSTONE_DUST;
        if (glowstone) {
            if (data.redstoneCredit() > 0) return;
            if (item.getAmount() != 64) return;
            CauldronBedrock.PotionData improved = CauldronBedrock.improve(data, true);
            if (improved == null) return;
            CauldronBedrock.setPotion(block, improved);
            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(0);
            return;
        }

        int pointer = Math.floorMod(data.currentPointer(), effects.size());
        PotionEffect current = effects.get(pointer);
        if (!CauldronCycle.canExtend(current.getDuration())) return;
        long price = CauldronBedrock.redstonePrice(current.getAmplifier());
        long needed = Math.max(1, price - Math.min(price, data.redstoneCredit()));
        int consumed = (int) Math.min(item.getAmount(), needed);
        if (consumed <= 0) return;
        long credit = data.redstoneCredit() + consumed;
        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - consumed);
        if (credit < price) {
            CauldronBedrock.setPotion(block, new CauldronBedrock.PotionData(data.type(), data.customEffects(),
                    data.mixCount(), data.mixedTypes(), pointer, credit, data.matchingLayers(),
                    data.durationTotals(), data.amplifierTotals(), data.powderInfused(), data.windInfused()));
        } else {
            double risk = CauldronCycle.redstoneFailureChance(current.getDuration() + 20,
                    Config.CAULDRON_REDSTONE_MAX_RISK_PERCENT);
            if (ThreadLocalRandom.current().nextDouble() < risk) {
                CauldronBedrock.setPotion(block, null);
                CauldronBedrock.setDyeColor(block, null);
                Color waterColor = CauldronBedrock.potionColor(PotionType.WATER);
                CauldronBedrock.spawnColoredDisplay(block, waterColor, CauldronBedrock.getWaterLevel(block));
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
                spawnDust(block, waterColor);
                return;
            }
            CauldronBedrock.PotionData improved = CauldronBedrock.improve(data, false);
            if (improved != null) CauldronBedrock.setPotion(block, improved);
        }
    }

    private void handlePotionInfusion(PlayerInteractEvent event, Player player, Block block, ItemStack item) {
        CauldronBedrock.PotionData data = CauldronBedrock.getPotion(block);
        if (data == null) return;
        boolean powder = item.getType() == Material.GUNPOWDER;
        if ((powder && data.powderInfused()) || (!powder && data.windInfused())) return;
        event.setCancelled(true);
        int cost = powder ? 32 : 8;
        if (player.getGameMode() != GameMode.CREATIVE && item.getAmount() < cost) return;
        CauldronBedrock.setPotion(block, new CauldronBedrock.PotionData(data.type(), data.customEffects(),
                data.mixCount(), data.mixedTypes(), data.currentPointer(), data.redstoneCredit(),
                data.matchingLayers(), data.durationTotals(), data.amplifierTotals(),
                data.powderInfused() || powder, data.windInfused() || !powder));
        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - cost);
        if (powder) {
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.7f, 1.5f);
            block.getWorld().spawnParticle(Particle.SMOKE, block.getLocation().add(0.5, 0.7, 0.5), 5,
                    0.2, 0.1, 0.2, 0.01);
        } else {
            block.getWorld().playSound(block.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.7f, 1.4f);
            block.getWorld().spawnParticle(Particle.GUST, block.getLocation().add(0.5, 0.7, 0.5), 1);
        }
    }

    private void handleTippedArrow(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;
        event.setCancelled(true);
        int requiredArrows = 8 * level;
        if (level <= 0 || item.getAmount() < requiredArrows) return;
        List<PotionEffect> incoming = new ArrayList<>();
        if (meta.getBasePotionType() != null) {
            for (PotionEffect effect : CauldronBedrock.realPotionEffects(meta.getBasePotionType())) {
                int duration = effect.getType().isInstant() ? 1 : Math.max(1, effect.getDuration() / 8);
                incoming.add(new PotionEffect(effect.getType(), duration, effect.getAmplifier()));
            }
        }
        for (PotionEffect custom : meta.getCustomEffects()) {
            incoming.removeIf(effect -> effect.getType().equals(custom.getType()));
            int duration = custom.getType().isInstant() ? 1
                    : custom.getDuration() < 0 ? -1 : Math.max(1, custom.getDuration() / 8);
            incoming.add(new PotionEffect(custom.getType(), duration, custom.getAmplifier(),
                    custom.isAmbient(), custom.hasParticles(), custom.hasIcon()));
        }
        boolean powderInfused = PotionInfusions.hasGunpowder(item);
        boolean windInfused = PotionInfusions.hasWind(item);
        if (incoming.isEmpty()) return;
        CauldronBedrock.PotionData existing = CauldronBedrock.getPotion(block);
        if (existing != null && existing.redstoneCredit() > 0) return;
        List<PotionEffect> merged = existing == null ? new ArrayList<>() : CauldronBedrock.getAllEffects(existing);
        for (PotionEffect arrowEffect : incoming) {
            int index = -1;
            for (int i = 0; i < merged.size(); i++) {
                if (merged.get(i).getType().equals(arrowEffect.getType())) { index = i; break; }
            }
            if (index < 0) {
                if (merged.size() >= CauldronBedrock.MAX_EFFECTS) return;
                merged.add(arrowEffect);
            } else {
                PotionEffect old = merged.get(index);
                int duration = old.getType().isInstant() ? 1
                        : old.getDuration() < 0 || arrowEffect.getDuration() < 0 ? -1
                        : (int) (((long) old.getDuration() + arrowEffect.getDuration()) / 2);
                int amplifier = (int) (((long) old.getAmplifier() + arrowEffect.getAmplifier()) / 2);
                merged.set(index, new PotionEffect(old.getType(), duration, amplifier));
            }
        }
        CauldronBedrock.PotionData result = existing == null
                ? new CauldronBedrock.PotionData(PotionType.AWKWARD, merged, 1, List.of(PotionType.AWKWARD),
                        powderInfused, windInfused)
                : new CauldronBedrock.PotionData(existing.type(), merged, existing.mixCount(),
                        existing.mixedTypes(), existing.currentPointer() % merged.size(), 0,
                        0, List.of(), List.of(), existing.powderInfused() || powderInfused,
                        existing.windInfused() || windInfused);
        CauldronBedrock.setPotion(block, result);
        CauldronBedrock.setDyeColor(block, null);
        CauldronBedrock.spawnColoredDisplay(block, CauldronBedrock.getPotionDisplayColor(result), level);
        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - requiredArrows);
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
                Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                Msg.sep(),
                Msg.action(Lang.get(player, "dye-added")),
                Msg.sep(),
                Msg.level(level, CauldronBedrock.MAX_WATER_LEVEL));
        AchievementProgression.award(player, "toolkit_wash");
    }

    private void handleLeatherArmor(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        Color dyeColor = CauldronBedrock.getDyeColor(block);
        CauldronBedrock.PotionData potionData = CauldronBedrock.getPotion(block);

        if (dyeColor != null) {
            event.setCancelled(true);
            CauldronBedrock.applyDyeToArmor(item, dyeColor);
            AchievementProgression.award(player, "toolkit_wash");
            int newLevel = level - 1;
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (newLevel <= 0) {
                CauldronBedrock.removeAll(block);
            } else {
                CauldronBedrock.updateDisplayLevel(block, newLevel);
            }
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "armor-dyed")));
        } else if (potionData != null) {
            event.setCancelled(true);
            CauldronBedrock.applyPotionToArmor(item, potionData);
            AchievementProgression.award(player, "toolkit_wash");
            int newLevel = level - 1;
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (newLevel <= 0) {
                CauldronBedrock.removeAll(block);
            } else {
                CauldronBedrock.updateDisplayLevel(block, newLevel);
            }
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "armor-potioned")));
        } else {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            boolean isDefault = CauldronBedrock.isDefaultLeatherColor(meta);
            if (isDefault) return;

            event.setCancelled(true);
            CauldronBedrock.cleanArmor(item);
            AchievementProgression.award(player, "toolkit_wash");
            int newLevel = level - 1;
            CauldronBedrock.setWaterLevel(block, newLevel);
            if (newLevel <= 0) {
                CauldronBedrock.removeAll(block);
            } else {
                CauldronBedrock.updateDisplayLevel(block, newLevel);
            }
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "armor-cleaned")));
        }
    }

    private void handleCleanBanner(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        event.setCancelled(true);
        CauldronBedrock.cleanBanner(item);
        AchievementProgression.award(player, "toolkit_wash");
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
                    Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "banner-layer")),
                    Msg.sep(),
                    Msg.val(String.valueOf(remaining)));
        } else {
            Msg.of(player,
                    Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                    Msg.sep(),
                    Msg.action(Lang.get(player, "banner-cleaned")));
        }
    }

    private void handleCleanShulker(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        event.setCancelled(true);
        CauldronBedrock.cleanShulker(item);
        AchievementProgression.award(player, "toolkit_wash");
        int newLevel = level - 1;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }
        Msg.of(player,
                Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                Msg.sep(),
                Msg.action(Lang.get(player, "shulker-cleaned")));
    }

    private void handleWashBlock(PlayerInteractEvent event, Player player, Block block, ItemStack item, int level) {
        if (!Config.CAULDRON_WASH_BLOCKS_ENABLED) return;
        DyeColor dye = CauldronBedrock.washDyeOf(item.getType());
        Material plain = CauldronBedrock.washPlainOf(item.getType());
        if (dye == null || plain == null || level <= 0) return;

        event.setCancelled(true);

        int toWash = Math.min(item.getAmount(), 16);
        AchievementProgression.award(player, "toolkit_wash");

        Color existing = CauldronBedrock.getDyeColor(block);
        Color newColor = existing == null ? dye.getColor() : CauldronBedrock.mixColors(existing, dye.getColor());
        CauldronBedrock.setDyeColor(block, newColor);
        CauldronBedrock.setPotion(block, null);
        CauldronBedrock.spawnColoredDisplay(block, newColor, level);

        int newLevel = level - 1;
        CauldronBedrock.setWaterLevel(block, newLevel);
        if (newLevel <= 0) {
            CauldronBedrock.removeAll(block);
        } else {
            CauldronBedrock.updateDisplayLevel(block, newLevel);
        }

        if (item.getAmount() <= toWash) {
            item.setType(plain);
        } else {
            item.setAmount(item.getAmount() - toWash);
            ItemStack washed = item.withType(plain);
            washed.setAmount(toWash);
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), washed);
            } else {
                player.getInventory().addItem(washed);
            }
        }

        Msg.of(player,
                Msg.prefix(Lang.get(player, "cauldron"), Msg.EMERALD),
                Msg.sep(),
                Msg.action(Lang.get(player, "block-washed")),
                Msg.sep(),
                Msg.count(toWash, Lang.get(player, "pcs")),
                Msg.sep(),
                Msg.level(Math.max(newLevel, 0), CauldronBedrock.MAX_WATER_LEVEL));
    }

    public void tickBaths() {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            Block block = player.getLocation().getBlock();
            CauldronBedrock.PotionData data = block.getType() == Material.WATER_CAULDRON
                    ? CauldronBedrock.getPotion(block) : null;
            if (data == null || CauldronBedrock.getWaterLevel(block) <= 0) {
                bathStays.remove(player.getUniqueId());
                continue;
            }
            BathStay previous = bathStays.get(player.getUniqueId());
            int seconds = previous != null && previous.block().equals(block) ? previous.seconds() + 1 : 1;
            if (seconds >= BATH_OVERLOAD_SECONDS) {
                burstPotionBath(player, block, data);
                bathStays.remove(player.getUniqueId());
                continue;
            }
            bathStays.put(player.getUniqueId(), new BathStay(block, seconds));
            for (PotionEffect effect : CauldronBedrock.getAllEffects(data)) {
                int duration = effect.getType().isInstant() ? 1 : 20;
                PotionEffect active = player.getPotionEffect(effect.getType());
                if (active != null && (active.getAmplifier() > effect.getAmplifier()
                        || (active.getAmplifier() == effect.getAmplifier() && active.getDuration() > duration))) {
                    continue;
                }
                player.addPotionEffect(new PotionEffect(effect.getType(), duration,
                        effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
            }
        }
    }

    private void burstPotionBath(Player player, Block block, CauldronBedrock.PotionData data) {
        Color color = CauldronBedrock.getPotionDisplayColor(data);
        List<PotionEffect> effects = CauldronBedrock.getAllEffects(data);
        block.getWorld().spawn(block.getLocation().add(0.5, 0.6, 0.5), AreaEffectCloud.class, cloud -> {
            cloud.setWaitTime(0);
            cloud.setDuration(80);
            cloud.setRadius(2.5f);
            cloud.setRadiusPerTick(-0.03f);
            cloud.setReapplicationDelay(20);
            cloud.setColor(color);
            for (PotionEffect effect : effects) {
                cloud.addCustomEffect(new PotionEffect(effect.getType(), effect.getType().isInstant() ? 1 : 40,
                        effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()), true);
            }
        });
        CauldronBedrock.removeAll(block);
        CauldronBedrock.setWaterLevel(block, 0);
        player.setVelocity(player.getLocation().getDirection().multiply(-1.8).setY(1.1));
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        block.getWorld().spawnParticle(Particle.EXPLOSION, block.getLocation().add(0.5, 0.7, 0.5), 1);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bathStays.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED && !Config.CAULDRON_HONEY_ENABLED) return;
        Block block = event.getBlock();
        // Запрещаем дождю/капельнику и ванильной механике менять уровень котла с зельем или мёдом
        if (CauldronBedrock.getPotion(block) != null || CauldronHoney.isHoneyCauldron(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material m = event.getBlock().getType();
        if (m == Material.WATER_CAULDRON || m == Material.CAULDRON) {
            CauldronBedrock.removeAll(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        dropDestroyedCauldrons(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        dropDestroyedCauldrons(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        dropDestroyedCauldrons(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        dropDestroyedCauldrons(event.getBlocks());
    }

    /** Котёл уничтожен или сдвинут не игроком — снимаем данные зелья/краски/мёда, иначе они воскреснут на этом месте. */
    private void dropDestroyedCauldrons(List<Block> blocks) {
        for (Block b : blocks) {
            Material m = b.getType();
            if (m == Material.WATER_CAULDRON || m == Material.CAULDRON) {
                CauldronBedrock.removeAll(b);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked();
        if (block == null) return;
        Material type = block.getType();
        if (type == Material.WATER_CAULDRON || type == Material.CAULDRON
                || type == Material.LAVA_CAULDRON || type == Material.POWDER_SNOW_CAULDRON) {
            CauldronBedrock.removeAll(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlockClicked();
        if (block == null) return;
        if (block.getType() == Material.WATER_CAULDRON) {
            if (CauldronBedrock.getPotion(block) != null) {
                event.setCancelled(true);
                Msg.of(event.getPlayer(),
                        Msg.prefix(Lang.get(event.getPlayer(), "cauldron"), NamedTextColor.RED),
                        Msg.sep(),
                        Msg.dim(Lang.get(event.getPlayer(), "cauldron-bucket-denied")));
                return;
            }
            CauldronBedrock.removeAll(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED && !Config.CAULDRON_HONEY_ENABLED) return;
        Material m = event.getBlockPlaced().getType();
        if (m == Material.CAULDRON) {
            CauldronHoney.tryTrack(event.getBlockPlaced());
        } else if (m == Material.BEEHIVE || m == Material.BEE_NEST) {
            CauldronHoney.tryTrackBelow(event.getBlockPlaced());
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) return;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;
        var pdc = meta.getPersistentDataContainer();
        if (!pdc.has(CauldronBedrock.CHAOS_EFFECT, PersistentDataType.STRING)) return;
        String name = pdc.get(CauldronBedrock.CHAOS_EFFECT, PersistentDataType.STRING);
        int duration = pdc.getOrDefault(CauldronBedrock.CHAOS_DURATION, PersistentDataType.INTEGER, 0);
        int amplifier = pdc.getOrDefault(CauldronBedrock.CHAOS_AMPLIFIER, PersistentDataType.INTEGER, 0);
        PotionEffectType et = PotionEffectType.getByKey(NamespacedKey.minecraft(name));
        if (et != null) event.getPlayer().addPotionEffect(new PotionEffect(et, duration, amplifier));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInfusionConsume(PlayerItemConsumeEvent event) {
        if (!Config.CAULDRON_BEDROCK_ENABLED) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) return;
        boolean powder = PotionInfusions.hasGunpowder(item);
        boolean wind = PotionInfusions.hasWind(item);
        if (!powder && !wind) return;
        Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(CauldronBedrockListener.class),
                () -> PotionInfusions.burst(event.getPlayer().getLocation(), powder, wind, true, event.getPlayer()));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (Config.CAULDRON_BEDROCK_ENABLED) CauldronBedrock.restoreChunk(event.getChunk());
        if (Config.CAULDRON_HONEY_ENABLED) CauldronHoney.restoreChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (Config.CAULDRON_BEDROCK_ENABLED) CauldronBedrock.unloadChunk(event.getChunk());
        if (Config.CAULDRON_HONEY_ENABLED) CauldronHoney.unloadChunk(event.getChunk());
    }

    private void consumePotionToBottle(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack cleanBottle = new ItemStack(Material.GLASS_BOTTLE);
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(cleanBottle);
        } else {
            item.setAmount(item.getAmount() - 1);
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), cleanBottle);
            } else {
                player.getInventory().addItem(cleanBottle);
            }
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
