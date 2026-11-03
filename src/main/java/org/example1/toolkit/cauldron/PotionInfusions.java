package org.example1.toolkit.cauldron;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.example1.toolkit.Config;

import java.util.ArrayList;
import java.util.List;

/** Powder and wind-charge traces carried by cauldron mixtures and arrows. */
public final class PotionInfusions {
    private static final NamespacedKey POWDER = new NamespacedKey("toolkit", "potion_powder_infused");
    private static final NamespacedKey WIND = new NamespacedKey("toolkit", "potion_wind_infused");

    private PotionInfusions() {}

    public static boolean hasGunpowder(ItemStack item) {
        return read(item, POWDER);
    }

    public static boolean hasWind(ItemStack item) {
        return read(item, WIND);
    }

    public static void write(ItemStack item, boolean gunpowder, boolean wind) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        write(meta, gunpowder, wind);
        item.setItemMeta(meta);
    }

    public static void write(ItemMeta meta, boolean gunpowder, boolean wind) {
        var pdc = meta.getPersistentDataContainer();
        if (gunpowder) pdc.set(POWDER, PersistentDataType.BYTE, (byte) 1);
        else pdc.remove(POWDER);
        if (wind) pdc.set(WIND, PersistentDataType.BYTE, (byte) 1);
        else pdc.remove(WIND);

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(line -> line.equals(powderLore()) || line.equals(windLore()));
        if (gunpowder) lore.add(powderLore());
        if (wind) lore.add(windLore());
        meta.lore(lore.isEmpty() ? null : lore);
    }

    public static void burst(Location location, boolean gunpowder, boolean wind, boolean drinking, Entity source) {
        burst(location, gunpowder, wind, drinking, source, null);
    }

    public static void burst(Location location, boolean gunpowder, boolean wind, boolean drinking,
                             Entity source, Entity directHit) {
        burst(location, gunpowder, wind, drinking, source, directHit, null);
    }

    /** direction — направление полёта стрелы (для толчка ветрового заряда вдоль выстрела), может быть null. */
    public static void burst(Location location, boolean gunpowder, boolean wind, boolean drinking,
                             Entity source, Entity directHit, Vector direction) {
        burst(location, gunpowder, wind, drinking, source, directHit, direction, null);
    }

    /** projectile — сама стрела: её саму взрыв не толкает. */
    public static void burst(Location location, boolean gunpowder, boolean wind, boolean drinking,
                             Entity source, Entity directHit, Vector direction, Entity projectile) {
        if (location == null || (!gunpowder && !wind)) return;
        World world = location.getWorld();
        if (world == null) return;

        if (drinking && gunpowder) {
            if (!world.createExplosion(location, 0.8f, false, Config.POWDER_POTION_BREAKS_BLOCKS)) return;
        } else if (gunpowder) {
            world.spawnParticle(Particle.EXPLOSION, location, 1);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1.4f);
        }
        if (wind) {
            world.spawnParticle(Particle.GUST, location, 1);
            world.playSound(location, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.75f, 1.3f);
        }

        if (!drinking) {
            arrowBlast(location, gunpowder, wind, source, directHit, direction, projectile);
            return;
        }

        double radius = gunpowder ? 2.5 : 3.0;
        double strength = (gunpowder ? 0.48 : 0.0) + (wind ? 0.42 : 0.0);
        if (gunpowder) strength *= 0.55; // the small vanilla explosion already pushes entities
        double damage = 0.0;

        for (Entity target : world.getNearbyEntities(location, radius, radius, radius)) {
            if (!(target instanceof LivingEntity living) || (!drinking && target.equals(source))) continue;
            if (target.getLocation().distanceSquared(location) > radius * radius) continue;
            Vector push = target.getLocation().toVector().subtract(location.toVector());
            if (push.lengthSquared() < 0.0001) push = new Vector(0, strength, 0);
            else {
                push.normalize();
                double vertical = Math.max(push.getY(), 0.25) * strength;
                push.multiply(strength).setY(vertical);
            }
            living.setVelocity(living.getVelocity().add(push));
        }
    }

    /**
     * Взрыв стрелы. Порох: урон как от взрыва (работает защита от взрыва), одинаковый для прямого попадания и
     * окружающих. Ветер: сильный толчок вдоль полёта стрелы с небольшим подъёмом.
     */
    private static void arrowBlast(Location location, boolean gunpowder, boolean wind,
                                   Entity source, Entity directHit, Vector direction, Entity projectile) {
        World world = location.getWorld();
        double radius = gunpowder ? 2.0 : 2.5;
        Vector flight = direction == null ? new Vector() : direction.clone().setY(0);
        boolean hasFlight = flight.lengthSquared() > 0.0025;
        if (hasFlight) flight.normalize();

        List<Runnable> pushes = new ArrayList<>();
        double objectRadius = radius * 1.5;
        for (Entity target : world.getNearbyEntities(location, objectRadius, objectRadius, objectRadius)) {
            if (target.equals(projectile) || target.equals(source)) continue;
            if (!(target instanceof LivingEntity living)) {
                Runnable push = objectPush(target, location, objectRadius, gunpowder, wind, flight, hasFlight);
                if (push != null) pushes.add(push);
                continue;
            }
            double dist = target.getLocation().distance(location);
            if (dist > radius) continue;
            boolean direct = target.equals(directHit);

            Vector away = target.getLocation().toVector().subtract(location.toVector()).setY(0);
            if (away.lengthSquared() > 0.0001) away.normalize();
            else away = hasFlight ? flight.clone() : new Vector();

            if (gunpowder) {
                // Урон-взрыв считается через броню и Взрывоустойчивость; максимум задан в конфиге.
                living.damage(Config.ARROW_POWDER_DAMAGE, explosionSource(source));
            }

            Vector push = new Vector();
            if (gunpowder) {
                push.add(away.clone().multiply(Config.ARROW_POWDER_KNOCKBACK * 0.6));
                push.setY(Math.max(0.25, Config.ARROW_POWDER_KNOCKBACK * 0.4));
            }
            if (wind) {
                // Прямое попадание летит строго по стреле, остальных отбрасывает смесью «от центра» и «по стреле».
                Vector dir = hasFlight
                        ? (direct ? flight.clone() : flight.clone().multiply(0.5).add(away.clone().multiply(0.5)))
                        : away.clone();
                if (dir.lengthSquared() > 0.0001) dir.normalize();
                double power = Config.ARROW_WIND_KNOCKBACK * (direct ? 1.0 : 0.65 * (1.0 - 0.4 * dist / radius));
                push.add(dir.multiply(power));
                push.setY(Math.max(push.getY(), Config.ARROW_WIND_LIFT));
            }
            if (push.lengthSquared() < 0.0001) continue;
            pushes.add(() -> {
                if (living.isValid()) living.setVelocity(living.getVelocity().add(push));
            });
        }
        if (pushes.isEmpty()) return;
        // Толчок — на следующем тике, после урона стрелы: собственное отбрасывание попадания иначе гасит наш импульс.
        Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(PotionInfusions.class),
                () -> pushes.forEach(Runnable::run));
    }

    /** Отбрасывание неживых сущностей как от TNT: тнт, предметы, стрелы, жемчуг, лодки, вагонетки, падающие блоки. */
    private static Runnable objectPush(Entity target, Location center, double radius,
                                       boolean gunpowder, boolean wind, Vector flight, boolean hasFlight) {
        if (target instanceof AbstractArrow || target instanceof Hanging || target instanceof Display || target instanceof Interaction
                || target instanceof AreaEffectCloud || target instanceof Marker || target.isInsideVehicle()) {
            return null;
        }
        Location mid = target.getLocation().add(0, target.getHeight() / 2, 0);
        double dist = mid.distance(center);
        if (dist > radius) return null;
        double falloff = 1.0 - dist / radius;

        Vector away = mid.toVector().subtract(center.toVector());
        if (away.lengthSquared() < 0.0001) away = new Vector(0, 1, 0);
        away.normalize();

        Vector push = new Vector();
        if (gunpowder) push.add(away.clone().multiply(Config.ARROW_POWDER_KNOCKBACK * 1.2 * falloff));
        if (wind) {
            Vector dir = hasFlight ? flight.clone().multiply(0.6).add(away.clone().multiply(0.4)) : away.clone();
            if (dir.lengthSquared() > 0.0001) dir.normalize();
            push.add(dir.multiply(Config.ARROW_WIND_KNOCKBACK * 0.5 * falloff));
            push.setY(push.getY() + Config.ARROW_WIND_LIFT * falloff);
        }
        if (push.lengthSquared() < 0.0001) return null;
        final Vector impulse = push;
        return () -> {
            if (target.isValid()) target.setVelocity(target.getVelocity().add(impulse));
        };
    }

    private static DamageSource explosionSource(Entity source) {
        DamageSource.Builder builder = DamageSource.builder(
                source instanceof Player ? DamageType.PLAYER_EXPLOSION : DamageType.EXPLOSION);
        if (source != null) builder.withCausingEntity(source).withDirectEntity(source);
        return builder.build();
    }

    private static boolean read(ItemStack item, NamespacedKey key) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != 0;
    }

    private static Component powderLore() {
        return Component.text("Содержит порох", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private static Component windLore() {
        return Component.text("Содержит ветровой заряд", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }
}
