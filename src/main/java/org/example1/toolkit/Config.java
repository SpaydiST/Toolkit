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

    public static String MSG_CAULDRON = "Котёл";
    public static String MSG_WATER_FILLED = "Вода";
    public static String MSG_POTION_ADDED = "Зелье добавлено";
    public static String MSG_POTION_MIXED = "Зелья смешаны";
    public static String MSG_POTION_FULL = "Макс. 3 зелья";
    public static String MSG_POTION_TAKEN = "Зелье забрано";
    public static String MSG_ARROWS_TIPPED = "Стрелы пропитаны";
    public static String MSG_DYE_ADDED = "Краска добавлена";
    public static String MSG_ARMOR_DYED = "Броня окрашена";
    public static String MSG_ARMOR_POTIONED = "Броня пропитана зельем";
    public static String MSG_ARMOR_CLEANED = "Броня очищена";
    public static String MSG_BANNER_CLEANED = "Флаг очищен";
    public static String MSG_SHULKER_CLEANED = "Шалкер очищен";
    public static String MSG_MIXED = "Смешанное";
    public static String MSG_LEVEL = "Ур.";
    public static String MSG_PCS = "шт.";

    public static String MSG_DEBUGSTICK = "Дебагстик";
    public static String MSG_NO_EDITABLE = "Нечего менять";
    public static String MSG_SELECTED = "Свойство";
    public static String MSG_CHANGED = "Изменено";
    public static String MSG_USES_LEFT = "Осталось";
    public static String MSG_STICK_BROKEN = "Сломалась";
    public static String MSG_STICK_STATE_COPIED = "Скопировано";
    public static String MSG_STICK_STATE_APPLIED = "Применено";
    public static String MSG_STICK_STATE_FAIL = "Ошибка";

    public static String MSG_LIGHT_PREFIX = "Свет";
    public static String MSG_LIGHT_LEVEL = "Яркость";

    public static boolean BANNER_HAT_ENABLED = true;
    public static String MSG_BANNER_HAT_PREFIX = "Шлем-флаг";
    public static String MSG_BANNER_HAT_EQUIPPED = "Надет";

    public static boolean FIRE_ELYTRA_ENABLED = true;
    public static String MSG_FIRE_ELYTRA_PREFIX = "Элитра";
    public static String MSG_FIRE_ELYTRA_BLOCKED = "Запрещено в огне";
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

        MSG_CAULDRON = c.getString("messages.cauldron", MSG_CAULDRON);
        MSG_WATER_FILLED = c.getString("messages.water-filled", MSG_WATER_FILLED);
        MSG_POTION_ADDED = c.getString("messages.potion-added", MSG_POTION_ADDED);
        MSG_POTION_MIXED = c.getString("messages.potion-mixed", MSG_POTION_MIXED);
        MSG_POTION_FULL = c.getString("messages.potion-full", MSG_POTION_FULL);
        MSG_POTION_TAKEN = c.getString("messages.potion-taken", MSG_POTION_TAKEN);
        MSG_ARROWS_TIPPED = c.getString("messages.arrows-tipped", MSG_ARROWS_TIPPED);
        MSG_DYE_ADDED = c.getString("messages.dye-added", MSG_DYE_ADDED);
        MSG_ARMOR_DYED = c.getString("messages.armor-dyed", MSG_ARMOR_DYED);
        MSG_ARMOR_POTIONED = c.getString("messages.armor-potioned", MSG_ARMOR_POTIONED);
        MSG_ARMOR_CLEANED = c.getString("messages.armor-cleaned", MSG_ARMOR_CLEANED);
        MSG_BANNER_CLEANED = c.getString("messages.banner-cleaned", MSG_BANNER_CLEANED);
        MSG_SHULKER_CLEANED = c.getString("messages.shulker-cleaned", MSG_SHULKER_CLEANED);
        MSG_MIXED = c.getString("messages.mixed", MSG_MIXED);
        MSG_LEVEL = c.getString("messages.level", MSG_LEVEL);
        MSG_PCS = c.getString("messages.pcs", MSG_PCS);

        MSG_DEBUGSTICK = c.getString("messages.debugstick", MSG_DEBUGSTICK);
        MSG_NO_EDITABLE = c.getString("messages.no-editable", MSG_NO_EDITABLE);
        MSG_SELECTED = c.getString("messages.selected", MSG_SELECTED);
        MSG_CHANGED = c.getString("messages.changed", MSG_CHANGED);
        MSG_USES_LEFT = c.getString("messages.uses-left", MSG_USES_LEFT);
        MSG_STICK_BROKEN = c.getString("messages.stick-broken", MSG_STICK_BROKEN);
        MSG_STICK_STATE_COPIED = c.getString("messages.stick-state-copied", MSG_STICK_STATE_COPIED);
        MSG_STICK_STATE_APPLIED = c.getString("messages.stick-state-applied", MSG_STICK_STATE_APPLIED);
        MSG_STICK_STATE_FAIL = c.getString("messages.stick-state-fail", MSG_STICK_STATE_FAIL);

        MSG_LIGHT_PREFIX = c.getString("messages.light-prefix", MSG_LIGHT_PREFIX);
        MSG_LIGHT_LEVEL = c.getString("messages.light-level", MSG_LIGHT_LEVEL);

        BANNER_HAT_ENABLED = c.getBoolean("banner-hat.enabled", true);
        MSG_BANNER_HAT_PREFIX = c.getString("messages.banner-hat-prefix", MSG_BANNER_HAT_PREFIX);
        MSG_BANNER_HAT_EQUIPPED = c.getString("messages.banner-hat-equipped", MSG_BANNER_HAT_EQUIPPED);

        FIRE_ELYTRA_ENABLED = c.getBoolean("fire-elytra.enabled", true);
        MSG_FIRE_ELYTRA_PREFIX = c.getString("messages.fire-elytra-prefix", MSG_FIRE_ELYTRA_PREFIX);
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
