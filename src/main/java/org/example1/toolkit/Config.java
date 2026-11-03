package org.example1.toolkit;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Config {

    public static TextColor COLOR_ACCENT = Msg.parseColor("#55FF55", TextColor.fromHexString("#55FF55"));
    public static TextColor COLOR_ERROR = Msg.parseColor("#E74C3C", TextColor.fromHexString("#E74C3C"));
    public static TextColor COLOR_DIM = Msg.parseColor("#AAAAAA", TextColor.fromHexString("#AAAAAA"));
    public static TextColor COLOR_SEP = Msg.parseColor("#555555", TextColor.fromHexString("#555555"));

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
    public static boolean FRAME_OVERLAY_CUSTOM_MODEL = true;
    public static boolean INVISIBLE_LIGHT_ENABLED = true;
    public static int INVISIBLE_LIGHT_DEFAULT_LEVEL = 15;
    public static String INVISIBLE_LIGHT_NAME = "Невидимый свет";

    public static boolean DEATH_COORDS_ENABLED = false;
    public static boolean DEATH_COORDS_DEFAULT_ENABLED = true;
    public static String MSG_DEATH_COORDS = "☠ Координаты смерти: X: %d, Y: %d, Z: %d (%s)";
    public static String MSG_DEATH_NONE = "У вас ещё нет записи о смерти";
    public static String MSG_DEATH_ON = "Уведомления о координатах смерти включены";
    public static String MSG_DEATH_OFF = "Уведомления о координатах смерти выключены";
    public static String MSG_DEATH_USAGE = "Использование: /deathcoords <on|off|show>";

    public static String MSG_COMPASS = "Компас";
    public static String MSG_COORDS = "Координаты:";
    public static String MSG_TRACKING = "Цель: %s";
    public static String MSG_TRACKING_OFFLINE = "Цель оффлайн";
    public static String MSG_TRACKING_LOST = "Цель в другом мире";
    public static String MSG_DISTANCE = " · %s м";
    public static String MSG_COOLDOWN = " · %s с";
    public static String MSG_TARGET_INVISIBLE = "Нельзя отслеживать невидимого игрока";
    public static String MSG_TIME_LABEL = "Время";
    public static String MSG_TIME = "Время: %s";
    public static String MSG_TO_NIGHT = "До ночи: %s";
    public static String MSG_TO_MORNING = "До утра: %s";
    public static String MSG_TO_MORNING_FAST = "До утра: %s (x%.1f)";
    public static String MSG_NEED_MAGNET = "Сначала привяжи компас к магниту (ПКМ по магниту)";
    public static String MSG_MAGNET_BROKEN = "Магнит сломан — отслеживание сброшено";
    public static String MSG_UNBOUND = "Отслеживание и магнит сняты";
    public static String MSG_UNBOUND_MAGNET = "Магнит снят";

    public static String MSG_CAULDRON = "Котёл";
    public static String MSG_WATER_FILLED = "Вода";
    public static String MSG_POTION_ADDED = "Зелье добавлено";
    public static String MSG_POTION_MIXED = "Зелья смешаны";
    public static String MSG_POTION_FULL = "Макс. 3 зелья";
    public static String MSG_POTION_TAKEN = "Зелье забрано";
    public static String MSG_POTION_DISSOLVED = "Зелье размылось";
    public static String MSG_MURKY_WATER = "Мутная вода";
    public static String MSG_CAULDRON_EXPLODED = "Котёл взорвался!";
    public static String MSG_POTION_CHAOS = "Судьба подкинула рандомное зелье!";
    public static String MSG_POTION_SYNERGY = "Синергия отвара!";
    public static String MSG_ARROWS_TIPPED = "Стрелы пропитаны";
    public static String MSG_DYE_ADDED = "Краска добавлена";
    public static String MSG_ARMOR_DYED = "Броня окрашена";
    public static String MSG_ARMOR_POTIONED = "Броня пропитана зельем";
    public static String MSG_ARMOR_CLEANED = "Броня очищена";
    public static String MSG_BANNER_CLEANED = "Флаг очищен";
    public static String MSG_BANNER_LAYER = "Узор снят";
    public static String MSG_SHULKER_CLEANED = "Шалкер очищен";
    public static String MSG_MIXED = "Смешанное";
    public static String MSG_LEVEL = "Ур.";
    public static String MSG_PCS = "шт.";
    public static String MSG_BLOCK_WASHED = "Краска смыта";
    public static String MSG_HONEY = "Мёд";
    public static String MSG_HONEY_COLLECTED = "Мёд собран";
    public static String MSG_HONEY_POURED = "Мёд налит";
    public static String MSG_HONEY_FULL = "Котёл полон";

    public static String MSG_DEBUGSTICK = "Палочка отладки";
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
    public static String MSG_BANNER_HAT_PREFIX = "Флаг";
    public static String MSG_BANNER_HAT_EQUIPPED = "Надет";

    public static boolean TNT_HAT_ENABLED = true;
    public static String TNT_HAT_NAME = "ТНТ-шлем";
    public static String TNT_HAT_LORE = "Взрывается при получении урона";
    public static double TNT_HAT_YIELD = 4.0;
    public static double TNT_HAT_KNOCKBACK_MULTIPLIER = 2.0;
    public static boolean TNT_HAT_BREAK_BLOCKS = true;

    public static boolean FIRE_ELYTRA_ENABLED = true;
    public static String MSG_FIRE_ELYTRA_PREFIX = "Элитра";
    public static String MSG_FIRE_ELYTRA_BLOCKED = "Невозможно лететь в огне";
    public static boolean CAULDRON_BEDROCK_ENABLED = true;
    public static boolean CAULDRON_WASH_BLOCKS_ENABLED = true;
    public static boolean CAULDRON_HONEY_ENABLED = true;
    public static boolean POWDER_POTION_BREAKS_BLOCKS = false;
    public static int CAULDRON_HONEY_FILL_SECONDS = 60;
    public static int CAULDRON_HONEY_MAX_LEVELS = 3;
    public static int CAULDRON_POTION_MAX_LEVEL = 3;
    public static java.util.Set<String> CAULDRON_POTION_FOURTH_LEVEL_EFFECTS = java.util.Set.of("speed", "strength");
    public static double CAULDRON_REDSTONE_MAX_RISK_PERCENT = 0.1;

    public static boolean TIPPED_ARROW_CLOUDS_ENABLED = true;
    public static boolean MULTISHOT_CONSUMES_THREE = true;
    public static double ARROW_POWDER_DAMAGE = 5.0;
    public static double ARROW_POWDER_KNOCKBACK = 1.0;
    public static double ARROW_WIND_KNOCKBACK = 2.0;
    public static double ARROW_WIND_LIFT = 0.3;

    public static boolean PARROT_TRANSPLANT_ENABLED = true;
    public static boolean SHIELD_ENCHANT_ENABLED = true;
    public static boolean MACE_NO_MENDING_ENABLED = true;
    public static boolean JUNGLE_TEMPLE_ARROWS_ENABLED = true;
    public static boolean SNOW_GOLEM_FREEZE_ENABLED = true;
    public static boolean BUNDLE_VANISHING_ENABLED = true;
    public static boolean TURTLE_HELM_TRIDENT_ENABLED = true;
    public static boolean ANIMAL_SPAWNING_ENABLED = false;
    public static boolean FLECTONEPULSE_COMPAT = true;

    private Config() {}

    public static void reload(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();

        COLOR_ACCENT = Msg.parseColor(c.getString("colors.accent", "#55FF55"), COLOR_ACCENT);
        COLOR_ERROR = Msg.parseColor(c.getString("colors.error", "#E74C3C"), COLOR_ERROR);
        COLOR_DIM = Msg.parseColor(c.getString("colors.dim", "#AAAAAA"), COLOR_DIM);
        COLOR_SEP = Msg.parseColor(c.getString("colors.separator", "#555555"), COLOR_SEP);
        Msg.EMERALD = COLOR_ACCENT;

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
        FRAME_OVERLAY_CUSTOM_MODEL = c.getBoolean("frames.overlay-custom-model", true);

        INVISIBLE_LIGHT_ENABLED = c.getBoolean("light.enabled", true);
        INVISIBLE_LIGHT_DEFAULT_LEVEL = Math.max(1, Math.min(15, c.getInt("light.default-level", 15)));
        INVISIBLE_LIGHT_NAME = c.getString("light.name", INVISIBLE_LIGHT_NAME);

        DEATH_COORDS_ENABLED = c.getBoolean("death-coords.enabled", false);
        DEATH_COORDS_DEFAULT_ENABLED = c.getBoolean("death-coords.default-enabled", true);
        MSG_DEATH_COORDS = c.getString("death-coords.message", MSG_DEATH_COORDS);
        MSG_DEATH_NONE = c.getString("death-coords.no-death", MSG_DEATH_NONE);
        MSG_DEATH_ON = c.getString("death-coords.on", MSG_DEATH_ON);
        MSG_DEATH_OFF = c.getString("death-coords.off", MSG_DEATH_OFF);
        MSG_DEATH_USAGE = c.getString("death-coords.usage", MSG_DEATH_USAGE);

        MSG_COMPASS = c.getString("messages.compass", MSG_COMPASS);
        MSG_COORDS = c.getString("messages.coords", MSG_COORDS);
        MSG_TRACKING = c.getString("messages.tracking", MSG_TRACKING);
        MSG_TRACKING_OFFLINE = c.getString("messages.tracking-offline", MSG_TRACKING_OFFLINE);
        MSG_TRACKING_LOST = c.getString("messages.tracking-lost", MSG_TRACKING_LOST);
        MSG_DISTANCE = c.getString("messages.distance", MSG_DISTANCE);
        MSG_COOLDOWN = c.getString("messages.cooldown", MSG_COOLDOWN);
        MSG_TARGET_INVISIBLE = c.getString("messages.target-invisible", MSG_TARGET_INVISIBLE);
        MSG_TIME_LABEL = c.getString("messages.time-label", MSG_TIME_LABEL);
        MSG_TIME = c.getString("messages.time", MSG_TIME);
        MSG_TO_NIGHT = c.getString("messages.to-night", MSG_TO_NIGHT);
        MSG_TO_MORNING = c.getString("messages.to-morning", MSG_TO_MORNING);
        MSG_TO_MORNING_FAST = c.getString("messages.to-morning-fast", MSG_TO_MORNING_FAST);
        MSG_NEED_MAGNET = c.getString("messages.need-magnet", MSG_NEED_MAGNET);
        MSG_MAGNET_BROKEN = c.getString("messages.magnet-broken", MSG_MAGNET_BROKEN);
        MSG_UNBOUND = c.getString("messages.unbound", MSG_UNBOUND);
        MSG_UNBOUND_MAGNET = c.getString("messages.unbound-magnet", MSG_UNBOUND_MAGNET);

        MSG_CAULDRON = c.getString("messages.cauldron", MSG_CAULDRON);
        MSG_WATER_FILLED = c.getString("messages.water-filled", MSG_WATER_FILLED);
        MSG_POTION_ADDED = c.getString("messages.potion-added", MSG_POTION_ADDED);
        MSG_POTION_MIXED = c.getString("messages.potion-mixed", MSG_POTION_MIXED);
        MSG_POTION_FULL = c.getString("messages.potion-full", MSG_POTION_FULL);
        MSG_POTION_TAKEN = c.getString("messages.potion-taken", MSG_POTION_TAKEN);
        MSG_POTION_DISSOLVED = c.getString("messages.potion-dissolved", MSG_POTION_DISSOLVED);
        MSG_MURKY_WATER = c.getString("messages.murky-water", MSG_MURKY_WATER);
        MSG_CAULDRON_EXPLODED = c.getString("messages.cauldron-exploded", MSG_CAULDRON_EXPLODED);
        MSG_POTION_CHAOS = c.getString("messages.potion-chaos", MSG_POTION_CHAOS);
        MSG_POTION_SYNERGY = c.getString("messages.potion-synergy", MSG_POTION_SYNERGY);
        MSG_ARROWS_TIPPED = c.getString("messages.arrows-tipped", MSG_ARROWS_TIPPED);
        MSG_DYE_ADDED = c.getString("messages.dye-added", MSG_DYE_ADDED);
        MSG_ARMOR_DYED = c.getString("messages.armor-dyed", MSG_ARMOR_DYED);
        MSG_ARMOR_POTIONED = c.getString("messages.armor-potioned", MSG_ARMOR_POTIONED);
        MSG_ARMOR_CLEANED = c.getString("messages.armor-cleaned", MSG_ARMOR_CLEANED);
        MSG_BANNER_CLEANED = c.getString("messages.banner-cleaned", MSG_BANNER_CLEANED);
        MSG_BANNER_LAYER = c.getString("messages.banner-layer", MSG_BANNER_LAYER);
        MSG_SHULKER_CLEANED = c.getString("messages.shulker-cleaned", MSG_SHULKER_CLEANED);
        MSG_MIXED = c.getString("messages.mixed", MSG_MIXED);
        MSG_LEVEL = c.getString("messages.level", MSG_LEVEL);
        MSG_PCS = c.getString("messages.pcs", MSG_PCS);
        MSG_BLOCK_WASHED = c.getString("messages.block-washed", MSG_BLOCK_WASHED);
        MSG_HONEY = c.getString("messages.honey", MSG_HONEY);
        MSG_HONEY_COLLECTED = c.getString("messages.honey-collected", MSG_HONEY_COLLECTED);
        MSG_HONEY_POURED = c.getString("messages.honey-poured", MSG_HONEY_POURED);
        MSG_HONEY_FULL = c.getString("messages.honey-full", MSG_HONEY_FULL);

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

        TNT_HAT_ENABLED = c.getBoolean("tnt-hat.enabled", true);
        TNT_HAT_NAME = c.getString("tnt-hat.name", TNT_HAT_NAME);
        TNT_HAT_LORE = c.getString("tnt-hat.lore", TNT_HAT_LORE);
        TNT_HAT_YIELD = Math.max(0, Math.min(100, c.getDouble("tnt-hat.yield", 4.0)));
        TNT_HAT_KNOCKBACK_MULTIPLIER = Math.max(0, Math.min(20, c.getDouble("tnt-hat.knockback-multiplier", 2.0)));
        TNT_HAT_BREAK_BLOCKS = c.getBoolean("tnt-hat.break-blocks", true);

        FIRE_ELYTRA_ENABLED = c.getBoolean("fire-elytra.enabled", true);
        MSG_FIRE_ELYTRA_PREFIX = c.getString("messages.fire-elytra-prefix", MSG_FIRE_ELYTRA_PREFIX);
        MSG_FIRE_ELYTRA_BLOCKED = c.getString("messages.fire-elytra-blocked", MSG_FIRE_ELYTRA_BLOCKED);

        CAULDRON_BEDROCK_ENABLED = c.getBoolean("cauldron-bedrock.enabled", true);
        CAULDRON_WASH_BLOCKS_ENABLED = c.getBoolean("cauldron-bedrock.wash-blocks", true);
        CAULDRON_HONEY_ENABLED = c.getBoolean("cauldron-bedrock.honey.enabled", true);
        POWDER_POTION_BREAKS_BLOCKS = c.getBoolean("cauldron-bedrock.powder-potion-breaks-blocks", false);
        CAULDRON_HONEY_FILL_SECONDS = Math.max(1, c.getInt("cauldron-bedrock.honey.fill-seconds", 60));
        CAULDRON_HONEY_MAX_LEVELS = Math.max(1, Math.min(3, c.getInt("cauldron-bedrock.honey.max-levels", 3)));
        CAULDRON_POTION_MAX_LEVEL = Math.max(0, c.getInt("cauldron-bedrock.potion-max-level", 3));
        CAULDRON_POTION_FOURTH_LEVEL_EFFECTS = c.getStringList("cauldron-bedrock.fourth-level-effects")
                .stream().map(s -> s.toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        CAULDRON_REDSTONE_MAX_RISK_PERCENT = Math.max(0, Math.min(100,
                c.getDouble("cauldron-bedrock.redstone-max-risk-percent", 0.1)));

        TIPPED_ARROW_CLOUDS_ENABLED = c.getBoolean("tipped-arrows-cloud.enabled", true);
        MULTISHOT_CONSUMES_THREE = c.getBoolean("tipped-arrows-cloud.multishot-consumes-three", true);
        ARROW_POWDER_DAMAGE = Math.max(0, c.getDouble("tipped-arrows-cloud.powder-damage", 5.0));
        ARROW_POWDER_KNOCKBACK = Math.max(0, c.getDouble("tipped-arrows-cloud.powder-knockback", 1.0));
        ARROW_WIND_KNOCKBACK = Math.max(0, c.getDouble("tipped-arrows-cloud.wind-knockback", 2.0));
        ARROW_WIND_LIFT = Math.max(0, c.getDouble("tipped-arrows-cloud.wind-lift", 0.3));

        PARROT_TRANSPLANT_ENABLED = c.getBoolean("parrot-transplant.enabled", true);
        SHIELD_ENCHANT_ENABLED = c.getBoolean("shield-enchant.enabled", true);
        MACE_NO_MENDING_ENABLED = c.getBoolean("shield-enchant.mace-no-mending", true);
        JUNGLE_TEMPLE_ARROWS_ENABLED = c.getBoolean("jungle-temple-arrows.enabled", true);
        SNOW_GOLEM_FREEZE_ENABLED = c.getBoolean("snow-golem-freeze.enabled", true);
        BUNDLE_VANISHING_ENABLED = c.getBoolean("bundle-vanishing.enabled", true);
        TURTLE_HELM_TRIDENT_ENABLED = c.getBoolean("turtle-helm-trident.enabled", true);
        ANIMAL_SPAWNING_ENABLED = c.getBoolean("animal-spawning.enabled", false);
        FLECTONEPULSE_COMPAT = c.getBoolean("flectonepulse-compat", true);
    }
}
