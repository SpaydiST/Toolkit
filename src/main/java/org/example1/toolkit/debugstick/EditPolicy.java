package org.example1.toolkit.debugstick;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Политика разрешённых/запрещённых блоков и свойств для дебагстика
 * (файл debugstick.yml).
 */
public final class EditPolicy {

    public static final String ALL = "all";
    private static final String REGISTRY = Tag.REGISTRY_BLOCKS;

    public static final class Entry {
        public final String id;
        public final Map<String, List<String>> properties;

        Entry(String id, Map<String, List<String>> properties) {
            this.id = id;
            this.properties = properties;
        }
    }

    private final boolean whitelist;
    private final List<Entry> allowedBlocks = new ArrayList<>();
    private final List<Entry> allowedTags = new ArrayList<>();
    private final Map<String, List<String>> allowedProperties = new LinkedHashMap<>();
    private final List<Entry> forbiddenBlocks = new ArrayList<>();
    private final List<Entry> forbiddenTags = new ArrayList<>();
    private final Map<String, List<String>> forbiddenProperties = new LinkedHashMap<>();

    private EditPolicy(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public static void ensureResource(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "debugstick.yml");
        if (!file.exists()) plugin.saveResource("debugstick.yml", false);
    }

    public static EditPolicy load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "debugstick.yml");
        if (!file.exists()) plugin.saveResource("debugstick.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        EditPolicy policy = new EditPolicy(cfg.getBoolean("whitelist", true));
        policy.parse(cfg);
        return policy;
    }

    private void parse(YamlConfiguration cfg) {
        for (Map<?, ?> m : cfg.getMapList("allowed.blocks")) addEntry(allowedBlocks, m);
        for (Map<?, ?> m : cfg.getMapList("allowed.tags")) addEntry(allowedTags, m);
        parseProperties(cfg, "allowed.properties", allowedProperties);
        for (Map<?, ?> m : cfg.getMapList("forbidden.blocks")) addEntry(forbiddenBlocks, m);
        for (Map<?, ?> m : cfg.getMapList("forbidden.tags")) addEntry(forbiddenTags, m);
        parseProperties(cfg, "forbidden.properties", forbiddenProperties);
    }

    private static void addEntry(List<Entry> out, Map<?, ?> m) {
        Object idObj = m.get("id");
        if (idObj == null) return;
        Map<String, List<String>> props = new LinkedHashMap<>();
        Object propsObj = m.get("properties");
        if (propsObj instanceof Map<?, ?> pm) {
            for (Map.Entry<?, ?> e : pm.entrySet()) {
                List<String> values = new ArrayList<>();
                Object v = e.getValue();
                if (v instanceof List<?> list) {
                    for (Object o : list) values.add(String.valueOf(o));
                } else if (v != null) {
                    values.add(String.valueOf(v));
                }
                props.put(String.valueOf(e.getKey()), values);
            }
        }
        out.add(new Entry(String.valueOf(idObj), props));
    }

    private static void parseProperties(YamlConfiguration cfg, String path, Map<String, List<String>> out) {
        ConfigurationSection sec = cfg.getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            out.put(key, new ArrayList<>(sec.getStringList(key)));
        }
    }

    /**
     * Возвращает редактируемые свойства блока (имя -> допустимые значения).
     * Пустой/All-список значений означает «все возможные значения».
     */
    public Map<String, List<String>> editableProperties(Material material, List<String> present, Map<String, Object> currentValues) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (whitelist) {
            addAllowed(material, present, allowedBlocks, result);
            addAllowed(material, present, allowedTags, result);
            addGlobal(present, allowedProperties, result);
        } else {
            for (String name : present) result.put(name, List.of(ALL));
        }

        if (matchesAny(material, forbiddenBlocks) || matchesAny(material, forbiddenTags)) {
            result.clear();
            return result;
        }
        applyForbidden(present, currentValues, forbiddenProperties, result);
        return result;
    }

    private static void addAllowed(Material material, List<String> present, List<Entry> entries, Map<String, List<String>> result) {
        for (Entry e : entries) {
            if (!matches(material, e)) continue;
            if (e.properties.isEmpty()) {
                for (String name : present) merge(result, name, List.of(ALL));
            } else {
                for (Map.Entry<String, List<String>> pe : e.properties.entrySet()) {
                    if (present.contains(pe.getKey())) merge(result, pe.getKey(), pe.getValue());
                }
            }
        }
    }

    private static void addGlobal(List<String> present, Map<String, List<String>> global, Map<String, List<String>> result) {
        for (Map.Entry<String, List<String>> e : global.entrySet()) {
            if (present.contains(e.getKey())) merge(result, e.getKey(), e.getValue());
        }
    }

    private static void applyForbidden(List<String> present, Map<String, Object> currentValues,
                                       Map<String, List<String>> forbidden, Map<String, List<String>> result) {
        for (Map.Entry<String, List<String>> e : forbidden.entrySet()) {
            String name = e.getKey();
            if (!result.containsKey(name)) continue;
            List<String> values = e.getValue();
            if (values.contains(ALL)) {
                result.remove(name);
                continue;
            }
            Object cur = currentValues.get(name);
            if (cur != null && containsIgnoreCase(values, String.valueOf(cur))) {
                result.remove(name);
            }
        }
    }

    private static boolean containsIgnoreCase(List<String> values, String v) {
        for (String s : values) {
            if (s.equalsIgnoreCase(v)) return true;
        }
        return false;
    }

    private static void merge(Map<String, List<String>> result, String name, List<String> values) {
        List<String> existing = result.get(name);
        if (existing == null) {
            result.put(name, new ArrayList<>(values));
            return;
        }
        if (existing.contains(ALL) || values.contains(ALL)) {
            result.put(name, List.of(ALL));
            return;
        }
        LinkedHashSet<String> set = new LinkedHashSet<>(existing);
        set.addAll(values);
        result.put(name, new ArrayList<>(set));
    }

    private static boolean matchesAny(Material material, List<Entry> entries) {
        for (Entry e : entries) {
            if (matches(material, e)) return true;
        }
        return false;
    }

    private static boolean matches(Material material, Entry e) {
        String id = e.id;
        if (material.getKey().toString().equals(id) || material.getKey().getKey().equals(id)) return true;
        return isTagged(material, id);
    }

    private static boolean isTagged(Material material, String tagId) {
        NamespacedKey key;
        if (tagId.contains(":")) {
            key = NamespacedKey.fromString(tagId);
        } else {
            key = NamespacedKey.minecraft(tagId);
        }
        if (key == null) return false;
        Tag<Material> tag = Bukkit.getTag(REGISTRY, key, Material.class);
        return tag != null && tag.isTagged(material);
    }
}
