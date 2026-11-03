package org.example1.toolkit.status;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.example1.toolkit.FlectonePulseCompat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatusManager {

    public enum PlayerStatus {
        NONE(null, "", "", "", "", "none", "Нет статуса", "gray", "#AAAAAA", ""),
        STREAMING(NamedTextColor.DARK_PURPLE, "\u25CF ", "§5\u25CF ", "&5\u25CF ", "<dark_purple>\u25CF </dark_purple>",
                "streaming", "Стримит", "dark_purple", "#AA00AA", "\u25CF"),
        RECORDING(NamedTextColor.RED, "\u25CF ", "§c\u25CF ", "&c\u25CF ", "<red>\u25CF </red>",
                "recording", "Запись", "red", "#FF5555", "\u25CF"),
        DND(NamedTextColor.DARK_RED, "\u25A0 ", "§4\u25A0 ", "&4\u25A0 ", "<dark_red>\u25A0 </dark_red>",
                "dnd", "Не беспокоить", "dark_red", "#AA0000", "\u25A0");

        private final NamedTextColor color;
        private final String prefix;
        private final String legacyPrefix;
        private final String ampersandPrefix;
        private final String miniMessagePrefix;
        private final String id;
        private final String displayName;
        private final String colorName;
        private final String hexColor;
        private final String symbol;

        PlayerStatus(NamedTextColor color, String prefix, String legacyPrefix, String ampersandPrefix,
                     String miniMessagePrefix, String id, String displayName, String colorName,
                     String hexColor, String symbol) {
            this.color = color;
            this.prefix = prefix;
            this.legacyPrefix = legacyPrefix;
            this.ampersandPrefix = ampersandPrefix;
            this.miniMessagePrefix = miniMessagePrefix;
            this.id = id;
            this.displayName = displayName;
            this.colorName = colorName;
            this.hexColor = hexColor;
            this.symbol = symbol;
        }

        public NamedTextColor color() { return color; }
        public String id() { return id; }
        public String colorName() { return colorName; }
        public String hexColor() { return hexColor; }
        public String symbol() { return symbol; }

        /** Префикс для TAB (символ + цвет). Пустой для NONE. */
        public Component prefix() {
            if (color == null) return Component.empty();
            return Component.text(prefix, color);
        }

        /** Символьный префикс (без цвета). */
        public String prefixString() {
            return prefix;
        }

        /** Цветной префикс со знаком §. */
        public String legacyPrefix() {
            return legacyPrefix;
        }

        /** Цветной префикс со знаком &. */
        public String ampersandPrefix() {
            return ampersandPrefix;
        }

        /** Цветной префикс для MiniMessage формата. */
        public String miniMessagePrefix() {
            return miniMessagePrefix;
        }

        /** Человекочитаемое название статуса. */
        public String displayName() {
            return displayName;
        }

        /** Цветная текстовая метка со знаком § (например, §5● Стримит). */
        public String displayLegacy() {
            return this == NONE ? "" : (legacyPrefix + displayName);
        }

        /** Цветная текстовая метка для MiniMessage. */
        public String displayMiniMessage() {
            return this == NONE ? "" : ("<" + colorName + ">" + symbol + " " + displayName + "</" + colorName + ">");
        }

        /** Цветная метка статуса в виде Component. */
        public Component displayComponent() {
            return Component.text((symbol.isEmpty() ? "" : symbol + " ") + displayName,
                    color != null ? color : NamedTextColor.GRAY);
        }
    }

    private static final Map<UUID, PlayerStatus> STATUSES = new ConcurrentHashMap<>();
    private static final Map<UUID, Component> BASE_NAMES = new ConcurrentHashMap<>();
    /** Последнее имя в списке, выставленное нами: если оно изменилось, значит имя поменял другой плагин. */
    private static final Map<UUID, Component> APPLIED = new ConcurrentHashMap<>();
    private static final org.bukkit.NamespacedKey STATUS_KEY = new org.bukkit.NamespacedKey("toolkit", "status");

    private StatusManager() {}

    public static void onJoin(Player player) {
        BASE_NAMES.put(player.getUniqueId(), player.playerListName());
        APPLIED.remove(player.getUniqueId());
        STATUSES.put(player.getUniqueId(), loadSaved(player));
        applyPrefix(player, STATUSES.get(player.getUniqueId()));
        FlectonePulseCompat.refreshPlayerTab(player);
    }

    public static void onQuit(Player player) {
        STATUSES.remove(player.getUniqueId());
        BASE_NAMES.remove(player.getUniqueId());
        APPLIED.remove(player.getUniqueId());
    }

    /** Статус хранится в данных игрока и переживает перезаход и рестарт. */
    private static PlayerStatus loadSaved(Player player) {
        String id = player.getPersistentDataContainer().get(STATUS_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (id == null) return PlayerStatus.NONE;
        for (PlayerStatus status : PlayerStatus.values()) {
            if (status.id.equals(id)) return status;
        }
        return PlayerStatus.NONE;
    }

    private static void saveStatus(Player player, PlayerStatus status) {
        var pdc = player.getPersistentDataContainer();
        if (status == PlayerStatus.NONE) pdc.remove(STATUS_KEY);
        else pdc.set(STATUS_KEY, org.bukkit.persistence.PersistentDataType.STRING, status.id);
    }

    public static PlayerStatus get(UUID uuid) {
        return STATUSES.getOrDefault(uuid, PlayerStatus.NONE);
    }

    public static void set(Player player, PlayerStatus status) {
        STATUSES.put(player.getUniqueId(), status);
        saveStatus(player, status);
        applyPrefix(player, status);
        FlectonePulseCompat.refreshPlayerTab(player);
    }

    private static void applyPrefix(Player player, PlayerStatus status) {
        UUID id = player.getUniqueId();
        Component current = player.playerListName();
        Component last = APPLIED.get(id);
        // Если имя в списке сменил другой плагин, берём его вариант за основу, а не затираем старым.
        Component base = last != null && last.equals(current) && BASE_NAMES.get(id) != null
                ? BASE_NAMES.get(id) : current;
        BASE_NAMES.put(id, base);
        if (status == PlayerStatus.NONE) {
            player.playerListName(base);
            APPLIED.remove(id);
            return;
        }
        Component prefixed = status.prefix().append(base);
        player.playerListName(prefixed);
        APPLIED.put(id, prefixed);
    }
}
