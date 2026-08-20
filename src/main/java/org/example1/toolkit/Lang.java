package org.example1.toolkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Переводы по локали игрока. Файлы: lang/&lt;locale&gt;.yml (resources).
 * Фолбэк: точная локаль -&gt; языковой префикс -&gt; ru_ru -&gt; Config.
 */
public final class Lang {

    private static final List<String> LOCALES = List.of(
            "ru_ru", "uk_ua", "be_by", "en_us", "en_gb", "de_de", "fr_fr",
            "es_es", "es_mx", "pt_br", "pt_pt", "it_it", "pl_pl", "cs_cz",
            "sk_sk", "hu_hu", "ro_ro", "bg_bg", "el_gr", "nl_nl", "sv_se",
            "da_dk", "no_no", "fi_fi", "et_ee", "lv_lv", "lt_lt", "tr_tr",
            "az_az", "kk_kz", "vi_vn", "th_th", "id_id", "ms_my", "ja_jp",
            "ko_kr", "zh_cn", "zh_tw", "ar_sa", "he_il", "fa_ir", "hi_in");

    private static final Map<String, Map<String, String>> TABLES = new HashMap<>();
    private static final Map<String, Supplier<String>> FALLBACKS = new HashMap<>();
    private static final Map<String, String> PREFIX_ALIAS = Map.of(
            "nb", "no", "nn", "no", "fil", "id");

    private Lang() {}

    public static void init(JavaPlugin plugin) {
        TABLES.clear();
        for (String locale : LOCALES) {
            InputStream in = plugin.getResource("lang/" + locale + ".yml");
            if (in == null) continue;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            Map<String, String> table = new LinkedHashMap<>();
            for (String key : cfg.getKeys(false)) {
                String v = cfg.getString(key);
                if (v != null) table.put(key, v);
            }
            TABLES.put(locale, table);
        }
    }

    /** Фолбэк-значение ключа из Config (можно переопределить в config.yml). */
    public static void fallback(String key, Supplier<String> def) {
        FALLBACKS.put(key, def);
    }

    public static void bindFallbacks() {
        FALLBACKS.clear();
        fallback("coords", () -> Config.MSG_COORDS);
        fallback("tracking", () -> Config.MSG_TRACKING);
        fallback("tracking-offline", () -> Config.MSG_TRACKING_OFFLINE);
        fallback("tracking-lost", () -> Config.MSG_TRACKING_LOST);
        fallback("distance", () -> Config.MSG_DISTANCE);
        fallback("cooldown", () -> Config.MSG_COOLDOWN);
        fallback("target-invisible", () -> Config.MSG_TARGET_INVISIBLE);
        fallback("need-magnet", () -> Config.MSG_NEED_MAGNET);
        fallback("magnet-broken", () -> Config.MSG_MAGNET_BROKEN);
        fallback("unbound", () -> Config.MSG_UNBOUND);
        fallback("time", () -> Config.MSG_TIME);
        fallback("to-night", () -> Config.MSG_TO_NIGHT);
        fallback("to-morning", () -> Config.MSG_TO_MORNING);
        fallback("to-morning-fast", () -> Config.MSG_TO_MORNING_FAST);
        fallback("cauldron", () -> Config.MSG_CAULDRON);
        fallback("water-filled", () -> Config.MSG_WATER_FILLED);
        fallback("potion-added", () -> Config.MSG_POTION_ADDED);
        fallback("potion-mixed", () -> Config.MSG_POTION_MIXED);
        fallback("potion-full", () -> Config.MSG_POTION_FULL);
        fallback("potion-taken", () -> Config.MSG_POTION_TAKEN);
        fallback("arrows-tipped", () -> Config.MSG_ARROWS_TIPPED);
        fallback("dye-added", () -> Config.MSG_DYE_ADDED);
        fallback("armor-dyed", () -> Config.MSG_ARMOR_DYED);
        fallback("armor-potioned", () -> Config.MSG_ARMOR_POTIONED);
        fallback("armor-cleaned", () -> Config.MSG_ARMOR_CLEANED);
        fallback("banner-cleaned", () -> Config.MSG_BANNER_CLEANED);
        fallback("shulker-cleaned", () -> Config.MSG_SHULKER_CLEANED);
        fallback("mixed", () -> Config.MSG_MIXED);
        fallback("level", () -> Config.MSG_LEVEL);
        fallback("pcs", () -> Config.MSG_PCS);
        fallback("debugstick", () -> Config.MSG_DEBUGSTICK);
        fallback("no-editable", () -> Config.MSG_NO_EDITABLE);
        fallback("selected", () -> Config.MSG_SELECTED);
        fallback("changed", () -> Config.MSG_CHANGED);
        fallback("uses-left", () -> Config.MSG_USES_LEFT);
        fallback("stick-broken", () -> Config.MSG_STICK_BROKEN);
        fallback("stick-state-copied", () -> Config.MSG_STICK_STATE_COPIED);
        fallback("stick-state-applied", () -> Config.MSG_STICK_STATE_APPLIED);
        fallback("stick-state-fail", () -> Config.MSG_STICK_STATE_FAIL);
        fallback("light-prefix", () -> Config.MSG_LIGHT_PREFIX);
        fallback("light-level", () -> Config.MSG_LIGHT_LEVEL);
        fallback("banner-hat-prefix", () -> Config.MSG_BANNER_HAT_PREFIX);
        fallback("banner-hat-equipped", () -> Config.MSG_BANNER_HAT_EQUIPPED);
        fallback("fire-elytra-prefix", () -> Config.MSG_FIRE_ELYTRA_PREFIX);
        fallback("fire-elytra-blocked", () -> Config.MSG_FIRE_ELYTRA_BLOCKED);
    }

    public static String get(Player player, String key) {
        Locale loc = player == null ? null : player.locale();
        String locale = loc == null ? "ru_ru" : loc.toString().toLowerCase(Locale.ROOT);
        return get(locale, key);
    }

    public static String get(String locale, String key) {
        Map<String, String> table = TABLES.get(locale);
        if (table != null) {
            String v = table.get(key);
            if (v != null) return v;
        }
        int us = locale.indexOf('_');
        if (us > 0) {
            String prefix = locale.substring(0, us);
            String base = PREFIX_ALIAS.getOrDefault(prefix, prefix);
            for (String l : LOCALES) {
                if (!l.startsWith(base + "_")) continue;
                Map<String, String> t = TABLES.get(l);
                if (t != null) {
                    String v = t.get(key);
                    if (v != null) return v;
                }
            }
        }
        Map<String, String> ru = TABLES.get("ru_ru");
        if (ru != null) {
            String v = ru.get(key);
            if (v != null) return v;
        }
        Supplier<String> def = FALLBACKS.get(key);
        return def != null ? def.get() : key;
    }
}
