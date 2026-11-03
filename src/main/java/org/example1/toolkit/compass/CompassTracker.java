package org.example1.toolkit.compass;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.example1.toolkit.Msg;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CompassTracker {

    private static final String KEY_TRACKED = "tracked-player";
    private static final String KEY_ANCHOR = "magnet";
    private static final String KEY_LOST = "lost";
    private static final String KEY_COOLDOWN = "cooldown-until";
    private static final String KEY_LAST_KNOWN = "last-known";
    private static NamespacedKey trackedKey;
    private static NamespacedKey anchorKey;
    private static NamespacedKey lostKey;
    private static NamespacedKey cooldownKey;
    private static NamespacedKey lastKnownKey;
    static final Set<UUID> knownHolders = ConcurrentHashMap.newKeySet();

    /** Группа перезарядки только для компасов, привязанных к игроку. */
    public static final Key COOLDOWN_GROUP = Key.key("toolkit", "compass");

    private CompassTracker() {}

    public static void init(JavaPlugin plugin) {
        trackedKey = new NamespacedKey(plugin, KEY_TRACKED);
        anchorKey = new NamespacedKey(plugin, KEY_ANCHOR);
        lostKey = new NamespacedKey(plugin, KEY_LOST);
        cooldownKey = new NamespacedKey(plugin, KEY_COOLDOWN);
        lastKnownKey = new NamespacedKey(plugin, KEY_LAST_KNOWN);
    }

    public static boolean isPlayerBound(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(trackedKey, PersistentDataType.STRING);
    }

    /** Компас потерял цель (цель/владелец в другом мире) — стрелка не указывает. */
    public static boolean isLost(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(lostKey, PersistentDataType.STRING);
    }

    public static UUID trackedPlayer(ItemStack item) {
        if (item.getItemMeta() == null) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(trackedKey, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Компас привязан к магниту (имеет lodestone-компонент или якорь). */
    public static boolean isLodestoneBound(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!(item.getItemMeta() instanceof CompassMeta meta)) return false;
        return meta.hasLodestone() || meta.isLodestoneCompass()
                || meta.getPersistentDataContainer().has(anchorKey, PersistentDataType.STRING);
    }

    /** Привязать компас к игроку. Компас уже должен быть привязан к магниту. */
    public static void bind(ItemStack item, Player holder, Player target) {
        knownHolders.add(holder.getUniqueId());
        Location anchor = lodestone(item);
        item.editMeta(CompassMeta.class, meta -> {
            if (anchor != null && anchor.getWorld() != null
                    && !meta.getPersistentDataContainer().has(anchorKey, PersistentDataType.STRING)) {
                meta.getPersistentDataContainer().set(anchorKey, PersistentDataType.STRING,
                        encode(anchor.getWorld().getUID(), anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ()));
            }
            meta.getPersistentDataContainer().set(trackedKey, PersistentDataType.STRING, target.getUniqueId().toString());
            meta.displayName(Component.translatable("item.minecraft.compass")
                    .color(Msg.EMERALD)
                    .append(Component.text(": " + target.getName(), Msg.EMERALD)));
            meta.lore(null);
        });
        refresh(item, holder);
    }

    /**
     * Периодическая проверка: не меняет метку на позицию цели (это делает только
     * рефреш по ПКМ). Здесь — только переходы состояния: компас снова видит цель
     * (восстановить наводку) или потерял её (стрелка крутится).
     */
    public static void update(ItemStack item, Player holder) {
        UUID id = trackedPlayer(item);
        if (id == null) return;
        Player target = Bukkit.getPlayer(id);

        if (!anchorAlive(item)) {
            unbind(item);
            return;
        }

        World anchor = anchorWorld(item);
        boolean working = isWorking(holder, target, anchor);

        if (working) {
            if (isLost(item)) {
                Location loc = target.getLocation();
                item.editMeta(CompassMeta.class, meta -> {
                    meta.getPersistentDataContainer().remove(lostKey);
                    meta.setLodestone(loc);
                    meta.setLodestoneTracked(false);
                });
            }
            clearLastKnown(item);
            return;
        }

        if (target != null && target.isOnline() && !isLost(item)) {
            saveLastKnown(item, target);
        }

        Location desired = spinLodestone(target);
        if (desired == null) desired = getLastKnown(item);
        // Уже в нужном состоянии «потерян» — предмет не трогаем, иначе клиент получает пакет инвентаря каждую секунду.
        if (isLost(item) && item.getItemMeta() instanceof CompassMeta current
                && current.isLodestoneTracked() && current.hasLodestone() == (desired != null)) {
            return;
        }
        final Location lodestone = desired;
        item.editMeta(CompassMeta.class, meta -> {
            meta.getPersistentDataContainer().set(lostKey, PersistentDataType.STRING, "1");
            meta.setLodestoneTracked(true);
            if (lodestone != null) meta.setLodestone(lodestone);
            else meta.clearLodestone();
        });
    }

    /** Полный сброс: компас снова обычный (отслеживание и магнит сняты). */
    public static void unbind(ItemStack item) {
        item.editMeta(CompassMeta.class, meta -> {
            meta.getPersistentDataContainer().remove(trackedKey);
            meta.getPersistentDataContainer().remove(anchorKey);
            meta.getPersistentDataContainer().remove(lostKey);
            meta.getPersistentDataContainer().remove(lastKnownKey);
            meta.clearLodestone();
            meta.displayName(null);
            meta.lore(null);
        });
        removeCooldownMarker(item);
    }

    /** Обновить компас по действию игрока (ПКМ): навести стрелку на цель и запустить перезарядку. */
    public static void refresh(ItemStack item, Player holder) {
        UUID id = trackedPlayer(item);
        if (id == null) return;
        Player target = Bukkit.getPlayer(id);

        if (!anchorAlive(item)) {
            unbind(item);
            return;
        }

        World anchor = anchorWorld(item);
        boolean working = isWorking(holder, target, anchor);

        if (working) {
            Location loc = target.getLocation();
            item.editMeta(CompassMeta.class, meta -> {
                meta.getPersistentDataContainer().remove(lostKey);
                meta.setLodestone(loc);
                meta.setLodestoneTracked(false);
            });
            clearLastKnown(item);
        } else {
            if (target != null && target.isOnline() && !isLost(item)) {
                saveLastKnown(item, target);
            }
            update(item, holder);
        }
        applyCooldownMarker(item);
    }

    /** Компас работает: игрок, цель и якорь в одном мире. */
    private static boolean isWorking(Player holder, Player target, World anchor) {
        return anchor != null
                && holder.getWorld().getUID().equals(anchor.getUID())
                && target != null && target.isOnline()
                && target.getWorld().getUID().equals(anchor.getUID());
    }

    /**
     * Потерять цель: метку ставим на позицию цели с включённым tracking, чтобы клиент
     * проверял блок (это не магнетит или другой мир) и крутил стрелку, как у компаса,
     * потерявшего магнит. Сам магнит целью не используется. Офлайн-цель — метку сбрасываем.
     */
    private static Location spinLodestone(Player target) {
        if (target == null || !target.isOnline()) return null;
        return target.getLocation();
    }

    /** Мир магнита (якоря). Если неизвестен — null. */
    public static World anchorWorld(ItemStack item) {
        if (item.getItemMeta() == null) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(anchorKey, PersistentDataType.STRING);
        if (s == null) return null;
        String[] parts = s.split(":");
        if (parts.length != 4) return null;
        try {
            return Bukkit.getWorld(UUID.fromString(parts[0]));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Перезарядка у этого конкретного компаса (не у всех у игрока). */
    public static boolean isOnCooldown(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        Long until = item.getItemMeta().getPersistentDataContainer().get(cooldownKey, PersistentDataType.LONG);
        return until != null && System.currentTimeMillis() < until;
    }

    /** Остаток перезарядки у этого компаса в секундах. */
    public static int cooldownSeconds(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return 0;
        Long until = item.getItemMeta().getPersistentDataContainer().get(cooldownKey, PersistentDataType.LONG);
        if (until == null) return 0;
        long remain = until - System.currentTimeMillis();
        if (remain <= 0) return 0;
        return (int) Math.ceil(remain / 1000.0);
    }

    /** Сохранить последнюю известную локацию цели в PDC компаса. */
    public static void saveLastKnown(ItemStack item, Player target) {
        if (item == null || item.getItemMeta() == null || target == null) return;
        Location loc = target.getLocation();
        item.editMeta(m -> m.getPersistentDataContainer().set(lastKnownKey, PersistentDataType.STRING,
                encode(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
    }

    /** Получить последнюю известную локацию из PDC компаса. */
    public static Location getLastKnown(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(lastKnownKey, PersistentDataType.STRING);
        if (s == null) return null;
        String[] parts = s.split(":");
        if (parts.length != 4) return null;
        try {
            World w = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (w == null) return null;
            return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }

    /** Очистить последнюю известную локацию. */
    public static void clearLastKnown(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return;
        item.editMeta(m -> m.getPersistentDataContainer().remove(lastKnownKey));
    }

    /** Жив ли магнит (якорь). Непроверяемое (чанк выгружен) считается живым. */
    static boolean anchorAlive(ItemStack item) {
        if (item.getItemMeta() == null) return true;
        String s = item.getItemMeta().getPersistentDataContainer().get(anchorKey, PersistentDataType.STRING);
        if (s == null) return true;
        String[] parts = s.split(":");
        if (parts.length != 4) return true;
        try {
            World w = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (w == null) return true;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) return true;
            return w.getBlockAt(x, y, z).getType() == Material.LODESTONE;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static Location lodestone(ItemStack item) {
        if (!(item.getItemMeta() instanceof CompassMeta meta)) return null;
        return meta.hasLodestone() ? meta.getLodestone() : null;
    }

    private static String encode(UUID world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    /** Периодическая проверка целостности компасов в инвентарях игроков. */
    public static void startVerifyTask(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            knownHolders.removeIf(id -> Bukkit.getPlayer(id) == null);
            for (UUID id : knownHolders) {
                Player holder = Bukkit.getPlayer(id);
                if (holder == null) continue;
                boolean anyBound = false;
                for (int slot = 0; slot < 36; slot++) {
                    ItemStack s = holder.getInventory().getItem(slot);
                    if (isPlayerBound(s)) {
                        update(s, holder);
                        anyBound = true;
                    }
                }
                ItemStack off = holder.getInventory().getItemInOffHand();
                if (isPlayerBound(off)) {
                    update(off, holder);
                    anyBound = true;
                }
                if (!anyBound) knownHolders.remove(id);
            }
        }, 20L, 20L);
    }

    /** Компонент cooldown: клиент рисует полоску перезарядки только на компасах этой группы. */
    private static void applyCooldownMarker(ItemStack item) {
        float seconds = Config.COMPASS_COOLDOWN_TICKS / 20f;
        item.setData(DataComponentTypes.USE_COOLDOWN,
                UseCooldown.useCooldown(seconds).cooldownGroup(COOLDOWN_GROUP).build());
        item.editMeta(meta -> meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG,
                System.currentTimeMillis() + Config.COMPASS_COOLDOWN_TICKS * 50L));
    }

    private static void removeCooldownMarker(ItemStack item) {
        item.unsetData(DataComponentTypes.USE_COOLDOWN);
        item.editMeta(meta -> meta.getPersistentDataContainer().remove(cooldownKey));
    }
}
