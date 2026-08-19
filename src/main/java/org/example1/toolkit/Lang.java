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
        fallback("no-editable", () -> Config.MSG_NO_EDITABLE);
        fallback("selected", () -> Config.MSG_SELECTED);
        fallback("changed", () -> Config.MSG_CHANGED);
        fallback("uses-left", () -> Config.MSG_USES_LEFT);
        fallback("stick-broken", () -> Config.MSG_STICK_BROKEN);
        fallback("invisible-light-level", () -> Config.MSG_LIGHT_LEVEL);
        fallback("banner-hat-equipped", () -> Config.MSG_BANNER_HAT_EQUIPPED);
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
