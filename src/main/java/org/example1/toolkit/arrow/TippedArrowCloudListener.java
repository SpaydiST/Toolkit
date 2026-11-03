package org.example1.toolkit.arrow;

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.ActionBarManager;
import org.example1.toolkit.Lang;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.example1.toolkit.Config;
import org.example1.toolkit.cauldron.PotionInfusions;
import org.example1.toolkit.progression.AchievementProgression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Стрела с зельем при попадании (в блок ИЛИ в существо) создаёт вокруг себя
 * облако эффектов — как туманное зелье.
 *
 * На новых версиях (1.21.5+) сущности TippedArrow больше нет: отравленные
 * стрелы спавнятся как обычные Arrow с данными зелья на самой стреле, поэтому
 * слушаем Arrow и читаем данные зелья напрямую с сущности.
 */
public final class TippedArrowCloudListener implements Listener {

    private static final int MULTISHOT_ARROWS = 3;
    private static final NamespacedKey COLOR_KEY = new NamespacedKey("toolkit", "arrow_cloud_color");
    private static final NamespacedKey POWDER_KEY = new NamespacedKey("toolkit", "arrow_infused_powder");
    private static final NamespacedKey WIND_KEY = new NamespacedKey("toolkit", "arrow_infused_wind");
    private static final NamespacedKey SPENT_KEY = new NamespacedKey("toolkit", "arrow_infusion_spent");

    /**
     * Арбалет с Тройным выстрелом, заряженный стрелой с зельем / порохом / ветровым зарядом,
     * тратит 3 такие стрелы (вместо одной на три выстрела).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
        if (!Config.MULTISHOT_CONSUMES_THREE) return;
        if (!(event.getEntity() instanceof Player player) || !event.shouldConsumeItem()) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack crossbow = event.getCrossbow();
        if (crossbow == null || crossbow.getEnchantmentLevel(Enchantment.MULTISHOT) <= 0) return;

        ItemStack ammo = findAmmo(player);
        if (ammo == null || !isSpecialArrow(ammo)) return;

        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(ammo)) total += stack.getAmount();
        }
        if (total < MULTISHOT_ARROWS) {
            event.setCancelled(true);
            ActionBarManager.show(player, "toolkit:feedback",
                    Component.text(Lang.get(player, "multishot-need-arrows"), Config.COLOR_ERROR), 40, 50);
            return;
        }
        // Ваниль спишет одну стрелу после события — остальные две снимаем сами.
        ItemStack extra = ammo.clone();
        extra.setAmount(MULTISHOT_ARROWS - 1);
        player.getInventory().removeItem(extra);
    }

    /** Стрела, которую ваниль возьмёт для заряда: сначала руки, затем инвентарь по порядку. */
    private static ItemStack findAmmo(Player player) {
        var inv = player.getInventory();
        for (ItemStack hand : new ItemStack[]{inv.getItemInOffHand(), inv.getItemInMainHand()}) {
            if (isArrow(hand)) return hand;
        }
        for (ItemStack stack : inv.getStorageContents()) {
            if (isArrow(stack)) return stack;
        }
        return null;
    }

    private static boolean isArrow(ItemStack item) {
        return item != null && (item.getType() == Material.ARROW || item.getType() == Material.TIPPED_ARROW
                || item.getType() == Material.SPECTRAL_ARROW);
    }

    private static boolean isSpecialArrow(ItemStack item) {
        return item.getType() == Material.TIPPED_ARROW
                || PotionInfusions.hasGunpowder(item) || PotionInfusions.hasWind(item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof Arrow arrow && event.getConsumable() != null) {
            copyInfusions(arrow, event.getConsumable());
        }
    }

    private void copyInfusions(Arrow arrow, org.bukkit.inventory.ItemStack item) {
        if (PotionInfusions.hasGunpowder(item)) arrow.getPersistentDataContainer().set(POWDER_KEY, PersistentDataType.BYTE, (byte) 1);
        if (PotionInfusions.hasWind(item)) arrow.getPersistentDataContainer().set(WIND_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        copyInfusions(arrow, arrow.getItemStack());
    }
    // Ванильная стрела отравляет на 1/8 длительности зелья.
    private static final double ARROW_EFFECT_FACTOR = 0.125;

    private static final List<String> NO_EFFECT_TYPES = List.of(
            PotionType.WATER.name(), PotionType.AWKWARD.name()
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        copyInfusions(arrow, arrow.getItemStack());
        if (!Config.TIPPED_ARROW_CLOUDS_ENABLED) return;
        // Нет данных зелья — не стрела с эффектом.
        if (!hasPotionData(arrow)) return;

        Color color = arrow.getColor();
        if (color != null) {
            // Для надёжности (выстрел из игрока/диспенсера) сохраняем цвет.
            arrow.getPersistentDataContainer().set(COLOR_KEY, PersistentDataType.INTEGER, color.asRGB());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        copyInfusions(arrow, arrow.getItemStack());
        boolean powder = arrow.getPersistentDataContainer().has(POWDER_KEY);
        boolean wind = arrow.getPersistentDataContainer().has(WIND_KEY);
        if (powder || wind) {
            // Каждое попадание (в т.ч. пробивающей стрелы) взрывается; облако — только в первый раз.
            boolean first = !arrow.getPersistentDataContainer().has(SPENT_KEY);
            arrow.getPersistentDataContainer().set(SPENT_KEY, PersistentDataType.BYTE, (byte) 1);
            PotionInfusions.burst(arrow.getLocation(), powder, wind, false,
                    arrow.getShooter() instanceof org.bukkit.entity.Entity entity ? entity : arrow, event.getHitEntity(),
                    arrow.getVelocity(), arrow);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            if (!first) return;
        }
        if (!Config.TIPPED_ARROW_CLOUDS_ENABLED) return;
        if (!hasPotionData(arrow)) return;

        boolean hitGround = event.getHitBlock() != null && event.getHitEntity() == null;
        if (!hitGround && !powder) return;

        List<PotionEffect> sourceEffects = gatherEffects(arrow);
        if (sourceEffects.isEmpty()) return;

        List<PotionEffect> cloudEffects = new ArrayList<>();
        boolean hasInstant = false;
        for (PotionEffect e : sourceEffects) {
            if (e.getType().isInstant()) {
                hasInstant = true;
                // В облаке от промаха стрелой моментальный эффект ослаблен до I уровня, чтобы промах в пол не шотал сильнее прямого попадания
                cloudEffects.add(new PotionEffect(e.getType(), 1, 0,
                        e.isAmbient(), e.hasParticles(), e.hasIcon()));
            } else {
                int cloudDuration = Math.max(120, (int) Math.round(e.getDuration() * 0.75));
                cloudEffects.add(new PotionEffect(e.getType(), cloudDuration, e.getAmplifier(),
                        e.isAmbient(), e.hasParticles(), e.hasIcon()));
            }
        }

        Color color = colorFromArrow(arrow);
        org.bukkit.Location spawnLoc = arrow.getLocation().add(0, 0.2, 0);
        boolean instantCloud = hasInstant;

        arrow.getWorld().spawn(spawnLoc, AreaEffectCloud.class, c -> {
            c.setWaitTime(0);
            c.setRadius(powder ? 2.2f : 1.6f);
            // При срабатывании облако сразу схлопывается (особенно на моментальном уроне — строго 1 срабатывание!)
            c.setRadiusOnUse(instantCloud ? (powder ? -2.2f : -1.6f) : -0.85f);
            c.setRadiusPerTick(-0.025f);
            c.setDuration(45);
            c.setReapplicationDelay(25);
            c.setColor(color);
            c.setParticle(Particle.ENTITY_EFFECT, color);
            for (PotionEffect e : cloudEffects) {
                c.addCustomEffect(e, false);
            }
            if (arrow.getShooter() instanceof LivingEntity le) {
                c.setSource(le);
            }
        });
        if (arrow.getShooter() instanceof Player player) {
            AchievementProgression.award(player, "toolkit_cloud");
        }

        // Не пересоздаём стрелу: снимаем эффекты на месте и запрещаем подбор.
        if (hitGround) {
            arrow.setBasePotionType(PotionType.WATER);
            arrow.clearCustomEffects();
            arrow.setColor(null);
        }
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }

    private static boolean hasPotionData(Arrow arrow) {
        if (arrow.getColor() != null) return true;
        if (arrow.hasCustomEffects() && !arrow.getCustomEffects().isEmpty()) return true;
        PotionType base = arrow.getBasePotionType();
        return base != null && !NO_EFFECT_TYPES.contains(base.name());
    }

    private static List<PotionEffect> gatherEffects(Arrow arrow) {
        Map<PotionEffectType, PotionEffect> merged = new LinkedHashMap<>();

        // При попадании ваниль применяет 1/8 длительности эффекта стрелы.
        // Облако получает ту же фактическую длительность, что и прямое попадание.
        mergeEffects(merged, scaleEffects(new ArrayList<>(arrow.getCustomEffects()), ARROW_EFFECT_FACTOR));

        // 2. Только если своих эффектов нет (обычная ванильная стрела с базовым
        // зельем): добавляем эффекты базового типа. Важно: у наших стрел base =
        // AWKWARD + все эффекты в customEffects, поэтому длительности берутся
        // ровно как на стреле, без пересчёта.
        if (merged.isEmpty()) {
            PotionType base = arrow.getBasePotionType();
            if (base != null && !NO_EFFECT_TYPES.contains(base.name())) {
                try {
                    // У ванильной стрелы эффект длится 1/8 от длительности зелья
                    // (PotionType.getPotionEffects() отдаёт длительность ПОЛНОГО
                    // зелья, а не стрелы). Приводим к длительности стрелы, иначе
                    // облако получалось в 8 раз жирнее и после грунта в 0.2 давало
                    // больше исходного зелья.
                    Collection<PotionEffect> baseFx = base.getPotionEffects();
                    if (baseFx != null) mergeEffects(merged, scaleEffects(new ArrayList<>(baseFx), ARROW_EFFECT_FACTOR));
                } catch (Exception ignored) {}
            }
        }
        if (merged.isEmpty()) return List.of();
        return new ArrayList<>(merged.values());
    }

    private static List<PotionEffect> scaleEffects(List<PotionEffect> effects, double factor) {
        if (effects.isEmpty()) return effects;
        List<PotionEffect> scaled = new ArrayList<>(effects.size());
        for (PotionEffect e : effects) {
            int duration = Math.max(1, (int) Math.round(e.getDuration() * factor));
            scaled.add(new PotionEffect(e.getType(), duration, e.getAmplifier(),
                    e.isAmbient(), e.hasParticles(), e.hasIcon()));
        }
        return scaled;
    }

    private static Color colorFromArrow(Arrow arrow) {
        Integer rgb = arrow.getPersistentDataContainer().get(COLOR_KEY, PersistentDataType.INTEGER);
        if (rgb != null) return Color.fromRGB(rgb);
        Color c = arrow.getColor();
        return c != null ? c : Color.fromRGB(0x38, 0x5D, 0xC6);
    }

    private static void mergeEffects(Map<PotionEffectType, PotionEffect> out, Collection<PotionEffect> effects) {
        for (PotionEffect e : effects) {
            PotionEffect cur = out.get(e.getType());
            if (cur == null || e.getAmplifier() > cur.getAmplifier()
                    || (e.getAmplifier() == cur.getAmplifier() && e.getDuration() > cur.getDuration())) {
                out.put(e.getType(), e);
            }
        }
    }
}
