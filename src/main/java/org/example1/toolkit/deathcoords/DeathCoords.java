package org.example1.toolkit.deathcoords;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Координаты смерти: при смерти игроку отправляется сообщение с координатами
 * (реальными — обфускация на это сообщение не действует).
 * Каждый игрок включает/выключает уведомления отдельно командой /deathcoords
 * (настройка хранится в PDC игрока и сохраняется между заходами);
 * /deathcoords без аргументов повторно показывает координаты последней смерти.
 */
public final class DeathCoords {

    private static final Map<UUID, DeathRecord> LAST_DEATHS = new ConcurrentHashMap<>();

    private static NamespacedKey enabledKey;
    private static boolean globalEnabled;

    private DeathCoords() {
    }

    public static void init(JavaPlugin plugin) {
        enabledKey = new NamespacedKey(plugin, "death-coords-enabled");
    }

    public static void reload() {
        globalEnabled = Config.DEATH_COORDS_ENABLED;
    }

    public static boolean isEnabled(Player player) {
        if (!globalEnabled || player == null) {
            return false;
        }
        Byte b = player.getPersistentDataContainer().get(enabledKey, PersistentDataType.BYTE);
        return b == null ? Config.DEATH_COORDS_DEFAULT_ENABLED : b != 0;
    }

    public static void setEnabled(Player player, boolean enabled) {
        player.getPersistentDataContainer().set(enabledKey, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
    }

    /** Запомнить место последней смерти игрока. */
    public static void recordDeath(Player player) {
        Location loc = player.getLocation();
        LAST_DEATHS.put(player.getUniqueId(), new DeathRecord(
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dimensionPath(loc.getWorld())));
    }

    /** Повторно показать игроку сообщение с координатами его последней смерти. */
    public static void showLast(Player player) {
        DeathRecord r = LAST_DEATHS.get(player.getUniqueId());
        if (r == null) {
            player.sendMessage(Component.text(Config.MSG_DEATH_NONE, NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(deathComponent(r.x(), r.y(), r.z(), r.dimension()));
    }

    public static Component deathComponent(Player player) {
        Location loc = player.getLocation();
        return deathComponent(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dimensionPath(loc.getWorld()));
    }

    private static Component deathComponent(int x, int y, int z, String dimension) {
        String msg = String.format(Config.MSG_DEATH_COORDS, x, y, z, dimension);
        String coords = x + " " + y + " " + z;
        return Component.text(msg, NamedTextColor.RED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.copyToClipboard(coords))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Нажми, чтобы скопировать координаты", NamedTextColor.GRAY)));
    }

    private static String dimensionPath(World world) {
        if (world == null) {
            return "overworld";
        }
        return switch (world.getEnvironment()) {
            case NETHER -> "the_nether";
            case THE_END -> "the_end";
            default -> "overworld";
        };
    }

    private record DeathRecord(int x, int y, int z, String dimension) {
    }
}
