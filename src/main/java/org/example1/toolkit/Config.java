package org.example1.toolkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Config {

    public static int HUD_INTERVAL_TICKS = 2;
    public static boolean HIDE_F3_COORDINATES = true;
    public static boolean COMPASS_ENABLED = true;
    public static int COMPASS_COOLDOWN_TICKS = 1200;
    public static boolean CLOCK_ENABLED = true;
    public static boolean DEBUG_STICK_ENABLED = true;
    public static int DEBUG_STICK_USES = 250;
    public static String DEBUG_STICK_NAME = "Палочка отладки";
    public static boolean INVISIBLE_FRAMES_ENABLED = true;
    public static String INVISIBLE_FRAME_NAME = "Невидимая рамка";
    public static boolean INVISIBLE_LIGHT_ENABLED = true;
    public static int INVISIBLE_LIGHT_DEFAULT_LEVEL = 15;
    public static String INVISIBLE_LIGHT_NAME = "Невидимый свет";

    public static boolean COORD_OFFSET_ENABLED = true;
    public static int COORD_OFFSET_RANGE_CHUNKS = 128;
    public static boolean COORD_OFFSET_PERSISTENT = true;
    public static String COORD_OFFSET_BYPASS_PERMISSION = "toolkit.coordoffset.bypass";

    public static boolean DEATH_COORDS_ENABLED = true;
    public static boolean DEATH_COORDS_DEFAULT_ENABLED = true;
    public static String MSG_DEATH_COORDS = "☠ Координаты смерти: X: %d, Y: %d, Z: %d (%s)";
    public static String MSG_DEATH_NONE = "У вас ещё нет записи о смерти";
    public static String MSG_DEATH_ON = "Уведомления о координатах смерти включены";
    public static String MSG_DEATH_OFF = "Уведомления о координатах смерти выключены";
    public static String MSG_DEATH_USAGE = "Использование: /deathcoords <on|off|show>";

    public static String MSG_COORDS = "Координаты:";
    public static String MSG_TRACKING = "Цель: %s";
    public static String MSG_TRACKING_OFFLINE = "Цель оффлайн";
    public static String MSG_TRACKING_LOST = "Цель в другом мире";
    public static String MSG_DISTANCE = " · %s м";
    public static String MSG_COOLDOWN = " · %s с";
    public static String MSG_TARGET_INVISIBLE = "Нельзя отслеживать невидимого игрока";
    public static String MSG_TIME = "Время: %s";
    public static String MSG_TO_NIGHT = "До ночи: %s";
    public static String MSG_TO_MORNING = "До утра: %s";
    public static String MSG_TO_MORNING_FAST = "До утра: %s (x%.1f)";
    public static String MSG_NEED_MAGNET = "Сначала привяжи компас к магниту (ПКМ по магниту)";
    public static String MSG_MAGNET_BROKEN = "Магнит сломан — отслеживание сброшено";
    public static String MSG_UNBOUND = "Отслеживание и магнит сняты";
    public static String MSG_NO_EDITABLE = "Здесь нечего менять";
    public static String MSG_SELECTED = "Свойство: %s";
    public static String MSG_CHANGED = "%s.%s = %s";
    public static String MSG_USES_LEFT = "Использований: %s";
    public static String MSG_STICK_BROKEN = "Палочка отладки сломалась";
    public static String MSG_LIGHT_LEVEL = "☀ Яркость: %d";
    public static boolean BANNER_HAT_ENABLED = true;
    public static String MSG_BANNER_HAT_EQUIPPED = "Шлем-флаг надет";
    public static boolean FIRE_ELYTRA_ENABLED = true;
    public static String MSG_FIRE_ELYTRA_BLOCKED = "Нельзя лететь на элитрах в огне";
    public static boolean CAULDRON_BEDROCK_ENABLED = true;

    public static boolean PARROT_TRANSPLANT_ENABLED = true;
    public static boolean SHIELD_ENCHANT_ENABLED = true;
    public static boolean JUNGLE_TEMPLE_ARROWS_ENABLED = true;
    public static boolean SNOW_GOLEM_FREEZE_ENABLED = true;
    public static boolean BUNDLE_VANISHING_ENABLED = true;
    public static boolean TURTLE_HELM_TRIDENT_ENABLED = true;

    private Config() {}

    public static void reload(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();

        HUD_INTERVAL_TICKS = Math.max(1, c.getInt("hud.interval-ticks", 2));
        HIDE_F3_COORDINATES = c.getBoolean("hud.hide-f3-coordinates", true);

        COMPASS_ENABLED = c.getBoolean("compass.enabled", true);
        COMPASS_COOLDOWN_TICKS = Math.max(1, c.getInt("compass.cooldown-seconds", 60)) * 20;

        CLOCK_ENABLED = c.getBoolean("clock.enabled", true);
        DEBUG_STICK_ENABLED = c.getBoolean("debug-stick.enabled", true);
        DEBUG_STICK_USES = Math.max(1, c.getInt("debug-stick.uses", 250));
        DEBUG_STICK_NAME = c.getString("debug-stick.name", DEBUG_STICK_NAME);

        INVISIBLE_FRAMES_ENABLED = c.getBoolean("frames.invisible-enabled", true);
        INVISIBLE_FRAME_NAME = c.getString("frames.invisible-name", INVISIBLE_FRAME_NAME);

        INVISIBLE_LIGHT_ENABLED = c.getBoolean("light.enabled", true);
        INVISIBLE_LIGHT_DEFAULT_LEVEL = Math.max(1, Math.min(15, c.getInt("light.default-level", 15)));
        INVISIBLE_LIGHT_NAME = c.getString("light.name", INVISIBLE_LIGHT_NAME);

        COORD_OFFSET_ENABLED = c.getBoolean("coord-offset.enabled", true);
        COORD_OFFSET_RANGE_CHUNKS = Math.max(0, c.getInt("coord-offset.range-chunks", 128));
        COORD_OFFSET_PERSISTENT = c.getBoolean("coord-offset.persistent", true);
        COORD_OFFSET_BYPASS_PERMISSION = c.getString("coord-offset.bypass-permission", COORD_OFFSET_BYPASS_PERMISSION);

        DEATH_COORDS_ENABLED = c.getBoolean("death-coords.enabled", true);
        DEATH_COORDS_DEFAULT_ENABLED = c.getBoolean("death-coords.default-enabled", true);
        MSG_DEATH_COORDS = c.getString("death-coords.message", MSG_DEATH_COORDS);
        MSG_DEATH_NONE = c.getString("death-coords.no-death", MSG_DEATH_NONE);
        MSG_DEATH_ON = c.getString("death-coords.on", MSG_DEATH_ON);
        MSG_DEATH_OFF = c.getString("death-coords.off", MSG_DEATH_OFF);
        MSG_DEATH_USAGE = c.getString("death-coords.usage", MSG_DEATH_USAGE);

        MSG_COORDS = c.getString("messages.coords", MSG_COORDS);
        MSG_TRACKING = c.getString("messages.tracking", MSG_TRACKING);
        MSG_TRACKING_OFFLINE = c.getString("messages.tracking-offline", MSG_TRACKING_OFFLINE);
        MSG_TRACKING_LOST = c.getString("messages.tracking-lost", MSG_TRACKING_LOST);
        MSG_DISTANCE = c.getString("messages.distance", MSG_DISTANCE);
        MSG_COOLDOWN = c.getString("messages.cooldown", MSG_COOLDOWN);
        MSG_TARGET_INVISIBLE = c.getString("messages.target-invisible", MSG_TARGET_INVISIBLE);
        MSG_TIME = c.getString("messages.time", MSG_TIME);
        MSG_TO_NIGHT = c.getString("messages.to-night", MSG_TO_NIGHT);
        MSG_TO_MORNING = c.getString("messages.to-morning", MSG_TO_MORNING);
        MSG_TO_MORNING_FAST = c.getString("messages.to-morning-fast", MSG_TO_MORNING_FAST);
        MSG_NEED_MAGNET = c.getString("messages.need-magnet", MSG_NEED_MAGNET);
        MSG_MAGNET_BROKEN = c.getString("messages.magnet-broken", MSG_MAGNET_BROKEN);
        MSG_UNBOUND = c.getString("messages.unbound", MSG_UNBOUND);
        MSG_NO_EDITABLE = c.getString("messages.no-editable", MSG_NO_EDITABLE);
        MSG_SELECTED = c.getString("messages.selected", MSG_SELECTED);
        MSG_CHANGED = c.getString("messages.changed", MSG_CHANGED);
        MSG_USES_LEFT = c.getString("messages.uses-left", MSG_USES_LEFT);
        MSG_STICK_BROKEN = c.getString("messages.stick-broken", MSG_STICK_BROKEN);
        MSG_LIGHT_LEVEL = c.getString("messages.invisible-light-level", MSG_LIGHT_LEVEL);

        BANNER_HAT_ENABLED = c.getBoolean("banner-hat.enabled", true);
        MSG_BANNER_HAT_EQUIPPED = c.getString("messages.banner-hat-equipped", MSG_BANNER_HAT_EQUIPPED);

        FIRE_ELYTRA_ENABLED = c.getBoolean("fire-elytra.enabled", true);
        MSG_FIRE_ELYTRA_BLOCKED = c.getString("messages.fire-elytra-blocked", MSG_FIRE_ELYTRA_BLOCKED);

        CAULDRON_BEDROCK_ENABLED = c.getBoolean("cauldron-bedrock.enabled", true);

        PARROT_TRANSPLANT_ENABLED = c.getBoolean("parrot-transplant.enabled", true);
        SHIELD_ENCHANT_ENABLED = c.getBoolean("shield-enchant.enabled", true);
        JUNGLE_TEMPLE_ARROWS_ENABLED = c.getBoolean("jungle-temple-arrows.enabled", true);
        SNOW_GOLEM_FREEZE_ENABLED = c.getBoolean("snow-golem-freeze.enabled", true);
        BUNDLE_VANISHING_ENABLED = c.getBoolean("bundle-vanishing.enabled", true);
        TURTLE_HELM_TRIDENT_ENABLED = c.getBoolean("turtle-helm-trident.enabled", true);
    }
}
