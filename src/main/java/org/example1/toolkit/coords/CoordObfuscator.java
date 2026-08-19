package org.example1.toolkit.coords;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.LodestoneTracker;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleItemStackData;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleTrailData;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleVibrationData;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.WorldBlockPosition;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoordObfuscator {

    private static final ConcurrentMap<UUID, PlayerOffsets> PLAYER_OFFSETS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static PacketListenerCommon packetListener;
    private static boolean available;
    private static boolean enabled;
    private static int rangeChunks;
    private static boolean persistent;
    private static String bypassPermission;

    private CoordObfuscator() {}

    public static boolean isAvailable() {
        return available;
    }

    public static boolean isEnabled() {
        return available && enabled;
    }

    public static void init(JavaPlugin plugin) {
        available = checkAvailable();
        if (!available) {
            plugin.getLogger().warning("Плагин PacketEvents не найден — обфускация координат отключена");
            return;
        }
        reload(plugin);
        // LOWEST: сдвигаем координаты раньше остальных слушателей (в т.ч. LibsDisguises).
        // LD (NORMAL) читает позицию из пакета и строит спавн дизгайза уже на сдвинутых
        // координатах — у наблюдателя в сдвинутом мире модель стоит на месте.
        packetListener = PacketEvents.getAPI().getEventManager()
                .registerListener(new CoordPacketListener(), PacketListenerPriority.LOWEST);
        plugin.getLogger().info("Обфускация координат активна (PacketEvents)");
    }

    public static void shutdown(JavaPlugin plugin) {
        if (packetListener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            } catch (Throwable ignored) {
            }
            packetListener = null;
        }
        PLAYER_OFFSETS.clear();
        available = false;
    }

    public static void reload(JavaPlugin plugin) {
        enabled = Config.COORD_OFFSET_ENABLED;
        rangeChunks = Config.COORD_OFFSET_RANGE_CHUNKS;
        persistent = Config.COORD_OFFSET_PERSISTENT;
        bypassPermission = Config.COORD_OFFSET_BYPASS_PERMISSION;
        if (available) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                PlayerOffsets offsets = PLAYER_OFFSETS.get(player.getUniqueId());
                if (offsets == null) {
                    handlePlayerJoin(player);
                } else {
                    offsets.update(player, bypassPermission);
                }
            }
        }
    }

    public static void handlePlayerJoin(Player player) {
        if (!enabled) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerOffsets offsets = PLAYER_OFFSETS.get(uuid);
        FixedOffset base = null;
        if (offsets == null) {
            base = persistent
                    ? generateOffset(rangeChunks, uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits())
                    : generateOffset(rangeChunks, RANDOM.nextLong());
            PlayerOffsets created = new PlayerOffsets(base);
            PlayerOffsets raced = PLAYER_OFFSETS.putIfAbsent(uuid, created);
            offsets = raced == null ? created : raced;
        }
        offsets.update(player, bypassPermission);
        if (base == null) {
            base = offsets.current();
        }
        org.bukkit.Bukkit.getLogger().info("[Toolkit] Обфускация координат для " + player.getName()
                + ": базовый оффсет X=" + base.x() + " Z=" + base.z()
                + ", текущий X=" + offsets.current().x() + " Z=" + offsets.current().z()
                + (offsets.bypass() ? " (bypass)" : ""));
    }

    public static void handleWorldChange(Player player) {
        PlayerOffsets offsets = PLAYER_OFFSETS.get(player.getUniqueId());
        if (offsets != null) {
            offsets.update(player, bypassPermission);
        }
    }

    /**
     * Переключить текущий оффсет игрока на оффсет нового измерения.
     * Вызывается на netty-потоке при обработке пакета RESPAWN, чтобы пакеты
     * перехода (чанки, свет, позиция) сразу использовали правильный оффсет.
     */
    public static FixedOffset switchDimension(User user, String worldName) {
        PlayerOffsets offsets = offsets(user);
        if (offsets == null) {
            return null;
        }
        if (worldName != null) {
            World.Environment env = switch (worldName) {
                case "minecraft:the_nether" -> World.Environment.NETHER;
                case "minecraft:the_end" -> World.Environment.THE_END;
                default -> World.Environment.NORMAL;
            };
            offsets.setEnvironment(env);
        }
        return offsets.current();
    }

    public static void handlePlayerQuit(Player player) {
        PLAYER_OFFSETS.remove(player.getUniqueId());
    }

    /**
     * Пометить игрока как Voxy/VSS-клиента. Обфускация для него НЕ отключается —
     * координаты остаются сдвинутыми, но LOD-колонки VSS (lss:* пакеты)
     * переводятся в базовое пространство (см. LssTranslator), чтобы LOD
     * совпадал с ванильными чанками. Вызывается при обнаружении lss-каналов
     * (minecraft:register или C2S/S2C lss:* payload).
     */
    public static void markVoxy(User user) {
        PlayerOffsets offsets = getOrCreateOffsets(user);
        if (offsets == null) {
            return;
        }
        if (!offsets.voxy()) {
            offsets.setVoxy(true);
            String name = user == null ? null : user.getName();
            if (name == null) {
                UUID uuid = resolveUuid(user);
                Player player = uuid == null ? null : org.bukkit.Bukkit.getPlayer(uuid);
                name = player == null ? "?" : player.getName();
            }
            org.bukkit.Bukkit.getLogger().info("[Toolkit] Обнаружен Voxy/VSS клиент: " + name
                    + " — LOD-координаты будут переводиться в базовое пространство");
        }
    }

    /**
     * Оффсет измерения для игрока (для перевода S2C lss:* пакетов). Возвращает
     * null, если перевод не нужен: игрок bypass'ит обфускацию или оффсет нулевой.
     */
    public static FixedOffset lssOffsetFor(User user) {
        PlayerOffsets offsets = offsets(user);
        if (offsets == null || !offsets.voxy() || offsets.bypass() || offsets.isCurrentZero()) {
            return null;
        }
        return offsets.current();
    }

    public static PlayerOffsets offsets(User user) {
        UUID uuid = resolveUuid(user);
        return uuid == null ? null : PLAYER_OFFSETS.get(uuid);
    }

    /**
     * Создать оффсет на netty-потоке, если игрок ещё не получил его
     * (гонка между пакетом JOIN_GAME и PlayerJoinEvent на входе).
     */
    public static PlayerOffsets getOrCreateOffsets(User user) {
        UUID uuid = resolveUuid(user);
        if (uuid == null || !enabled) {
            return null;
        }
        PlayerOffsets existing = PLAYER_OFFSETS.get(uuid);
        if (existing != null) {
            return existing;
        }
        FixedOffset base = persistent
                ? generateOffset(rangeChunks, uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits())
                : generateOffset(rangeChunks, RANDOM.nextLong());
        PlayerOffsets created = new PlayerOffsets(base);
        PlayerOffsets raced = PLAYER_OFFSETS.putIfAbsent(uuid, created);
        return raced == null ? created : raced;
    }

    private static UUID resolveUuid(User user) {
        if (user == null) {
            return null;
        }
        UUID uuid = user.getUUID();
        if (uuid == null) {
            com.github.retrooper.packetevents.protocol.player.UserProfile profile = user.getProfile();
            if (profile != null) {
                uuid = profile.getUUID();
            }
        }
        if (uuid == null) {
            String name = user.getName();
            Player player = name == null ? null : org.bukkit.Bukkit.getPlayerExact(name);
            if (player != null) {
                uuid = player.getUniqueId();
            }
        }
        return uuid;
    }

    public static PlayerOffsets offsets(UUID uuid) {
        return uuid == null ? null : PLAYER_OFFSETS.get(uuid);
    }

    /**
     * Оффсет (x, z), который обфускация приписывает конкретному игроку.
     * Veiled отнимает его от реальных координат сущности и ставит модель
     * дизгайза LibsDisguises в сдвинутый мир наблюдателя.
     * null — обфускации у игрока нет (bypass/оффсет не задан).
     */
    public static double[] getOffsetXZ(UUID playerId) {
        PlayerOffsets offsets = playerId == null ? null : PLAYER_OFFSETS.get(playerId);
        if (offsets == null || offsets.bypass() || offsets.isCurrentZero()) {
            return null;
        }
        FixedOffset off = offsets.current();
        return new double[]{off.x(), off.z()};
    }

    /**
     * Пересчитывает реальные координаты сущности в те, что увидит конкретный
     * наблюдатель в своём сдвинутом мире (fake = real - offset). Используется
     * Veiled для коррекции дизгайзов LibsDisguises: LD шлёт их на реальных
     * координатах через sendPacketSilently (в обход пайплайна), а наблюдатель
     * смотрит на сдвинутый мир. Для bypass-наблюдателей и при отсутствии
     * оффсета возвращаются исходные координаты.
     */
    public static double[] shiftFor(UUID observerId, double x, double y, double z) {
        PlayerOffsets offsets = observerId == null ? null : PLAYER_OFFSETS.get(observerId);
        if (offsets == null || offsets.bypass() || offsets.isCurrentZero()) {
            return new double[]{x, y, z};
        }
        FixedOffset off = offsets.current();
        return new double[]{x - off.x(), y, z - off.z()};
    }

    private static boolean checkAvailable() {
        try {
            PacketEvents.getAPI();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static FixedOffset generateOffset(int rangeChunks, long seed) {
        Random random = new Random(seed);
        int halfSteps = rangeChunks / 8;
        if (halfSteps == 0) {
            return FixedOffset.none();
        }
        int x = (random.nextInt(halfSteps * 2 + 1) - halfSteps) * 128;
        int z = (random.nextInt(halfSteps * 2 + 1) - halfSteps) * 128;
        return new FixedOffset(x, z);
    }

    public static Vector3i apply(Vector3i vec, FixedOffset offset) {
        return new Vector3i(vec.getX() - offset.x(), vec.getY(), vec.getZ() - offset.z());
    }

    public static Vector3d apply(Vector3d vec, FixedOffset offset) {
        return new Vector3d(vec.getX() - offset.x(), vec.getY(), vec.getZ() - offset.z());
    }

    public static Vector3i unapply(Vector3i vec, FixedOffset offset) {
        return new Vector3i(vec.getX() + offset.x(), vec.getY(), vec.getZ() + offset.z());
    }

    public static Vector3d unapply(Vector3d vec, FixedOffset offset) {
        return new Vector3d(vec.getX() + offset.x(), vec.getY(), vec.getZ() + offset.z());
    }

    public static Vector3i applyChunk(Vector3i vec, FixedOffset offset) {
        return new Vector3i(vec.getX() - offset.chunkX(), vec.getY(), vec.getZ() - offset.chunkZ());
    }

    public static Vector3i applyTimes8(Vector3i vec, FixedOffset offset) {
        return new Vector3i(vec.getX() - offset.x() * 8, vec.getY(), vec.getZ() - offset.z() * 8);
    }

    public static WorldBlockPosition apply(WorldBlockPosition pos, FixedOffset offset) {
        return new WorldBlockPosition(pos.getWorld(), apply(pos.getBlockPosition(), offset));
    }

    public static OffsettedColumn applyColumn(Column column, FixedOffset offset) {
        if (column instanceof OffsettedColumn) {
            return (OffsettedColumn) column;
        }
        return new OffsettedColumn(column, offset);
    }

    public static boolean applyItem(ItemStack item, FixedOffset offset) {
        if (item == null || item.isEmpty() || !item.getType().equals(ItemTypes.COMPASS)) {
            return false;
        }
        Optional<LodestoneTracker> component = item.getComponent(ComponentTypes.LODESTONE_TRACKER);
        if (component.isEmpty()) {
            return false;
        }
        LodestoneTracker lodestone = component.get();
        WorldBlockPosition target = lodestone.getTarget();
        if (target == null) {
            return false;
        }
        lodestone.setTarget(apply(target, offset));
        item.setComponent(ComponentTypes.LODESTONE_TRACKER, lodestone);
        return true;
    }

    public static boolean unapplyItem(ItemStack item, FixedOffset offset) {
        return applyItem(item, offset.negate());
    }

    public static void applyParticleData(com.github.retrooper.packetevents.protocol.particle.Particle<?> particle, FixedOffset offset) {
        if (particle == null) {
            return;
        }
        Object data = particle.getData();
        if (data instanceof ParticleVibrationData vibration) {
            Vector3i start = vibration.getStartingPosition();
            if (start != null && !start.equals(Vector3i.zero())) {
                vibration.setStartingPosition(apply(start, offset));
            }
            vibration.getBlockPosition().ifPresent(pos -> vibration.setBlockPosition(apply(pos, offset)));
        } else if (data instanceof ParticleItemStackData itemData) {
            applyItem(itemData.getItemStack(), offset);
        } else if (data instanceof ParticleTrailData trail) {
            Vector3d target = trail.getTarget();
            if (target != null) {
                trail.setTarget(apply(target, offset));
            }
        }
    }

    public static void applyEntityMetadata(WrapperPlayServerEntityMetadata wrapper, FixedOffset offset) {
        for (EntityData<?> entityData : wrapper.getEntityMetadata()) {
            Object value = entityData.getValue();
            if (value instanceof Vector3i v) {
                setEntityDataValue(entityData, apply(v, offset));
            } else if (value instanceof ItemStack item) {
                applyItem(item, offset);
            } else if (value instanceof Optional<?> opt && opt.isPresent()) {
                Object inner = opt.get();
                if (inner instanceof Vector3i v) {
                    setEntityDataValue(entityData, Optional.of(apply(v, offset)));
                } else if (inner instanceof ItemStack item) {
                    applyItem(item, offset);
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEntityDataValue(EntityData entityData, Object value) {
        entityData.setValue(value);
    }
}
