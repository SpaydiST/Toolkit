package org.example1.toolkit.status;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatusManager {
    public enum PlayerStatus {
        NONE, STREAMING, RECORDING, DND
    }

    private static final Map<UUID, PlayerStatus> STATUSES = new ConcurrentHashMap<>();

    private StatusManager() {}

    public static void onJoin(Player player) {
        STATUSES.putIfAbsent(player.getUniqueId(), PlayerStatus.NONE);
        updateListName(player);
    }

    public static void onQuit(Player player) {
        STATUSES.remove(player.getUniqueId());
    }

    public static PlayerStatus get(UUID uuid) {
        return STATUSES.getOrDefault(uuid, PlayerStatus.NONE);
    }

    public static void set(Player player, PlayerStatus status) {
        STATUSES.put(player.getUniqueId(), status);
        updateListName(player);
    }

    private static void updateListName(Player player) {
        String prefix = switch (get(player.getUniqueId())) {
            case STREAMING, RECORDING -> "● ";
            case DND -> "■ ";
            default -> "";
        };
        NamedTextColor color = switch (get(player.getUniqueId())) {
            case STREAMING -> NamedTextColor.DARK_PURPLE;
            case RECORDING -> NamedTextColor.RED;
            case DND -> NamedTextColor.DARK_RED;
            default -> NamedTextColor.WHITE;
        };
        player.playerListName(Component.text(prefix + player.getName(), color));
    }
}
