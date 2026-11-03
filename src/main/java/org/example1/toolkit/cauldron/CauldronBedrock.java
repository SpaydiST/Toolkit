package org.example1.toolkit.cauldron;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Chunk;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.Config;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;

public final class CauldronBedrock {

    private static final Map<String, Color> cauldronDyeColors = new HashMap<>();
    private static final Map<String, PotionData> cauldronPotions = new HashMap<>();
    private static final Map<String, ItemDisplay> cauldronDisplays = new HashMap<>();
    private static final int DEFAULT_LEATHER_R = 102;
    private static final int DEFAULT_LEATHER_G = 76;
    private static final int DEFAULT_LEATHER_B = 56;
    private static final NamespacedKey ITEM_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_water");
    private static final NamespacedKey POTION_MODEL_KEY = new NamespacedKey("toolkit", "cauldron_potion");
    public static final int MAX_MIX = 3;
    public static final int MAX_WATER_LEVEL = 3;
    public static final int MAX_EFFECTS = 3;
    public static final int MAX_ARROW_DURATION = CauldronCycle.MAX_ARROW_DURATION;
    private static final NamespacedKey BOTTLE_MIX_COUNT = new NamespacedKey("toolkit", "bottle_mix_count");
    private static final NamespacedKey BOTTLE_MIXED_TYPES = new NamespacedKey("toolkit", "bottle_mixed_types");

    public static final NamespacedKey CHAOS_EFFECT = new NamespacedKey("toolkit", "chaos_effect");
    public static final NamespacedKey CHAOS_DURATION = new NamespacedKey("toolkit", "chaos_duration");
    public static final NamespacedKey CHAOS_AMPLIFIER = new NamespacedKey("toolkit", "chaos_amplifier");

    private static JavaPlugin plugin;

    public record PotionData(PotionType type, List<PotionEffect> customEffects, int mixCount,
                             List<PotionType> mixedTypes, int currentPointer, long redstoneCredit,
                             int matchingLayers, List<Long> durationTotals, List<Long> amplifierTotals,
                             boolean powderInfused, boolean windInfused) {
        public PotionData {
            durationTotals = List.copyOf(durationTotals);
            amplifierTotals = List.copyOf(amplifierTotals);
        }

        public PotionData(PotionType type, List<PotionEffect> customEffects, int mixCount,
                          List<PotionType> mixedTypes, int currentPointer, long redstoneCredit,
                          int matchingLayers, List<Long> durationTotals, List<Long> amplifierTotals) {
            this(type, customEffects, mixCount, mixedTypes, currentPointer, redstoneCredit,
                    matchingLayers, durationTotals, amplifierTotals, false, false);
        }

        public PotionData(PotionType type, List<PotionEffect> customEffects, int mixCount,
                          List<PotionType> mixedTypes, int currentPointer, long redstoneCredit) {
            this(type, customEffects, mixCount, mixedTypes, currentPointer, redstoneCredit, 0, List.of(), List.of());
        }

        public PotionData(PotionType type, List<PotionEffect> customEffects, int mixCount,
                          List<PotionType> mixedTypes, boolean powderInfused, boolean windInfused) {
            this(type, customEffects, mixCount, mixedTypes, 0, 0, 0, List.of(), List.of(),
                    powderInfused, windInfused);
        }

        public PotionData(PotionType type, List<PotionEffect> customEffects, int mixCount, List<PotionType> mixedTypes) {
            this(type, customEffects, mixCount, mixedTypes, 0, 0);
        }

        public PotionData(PotionType type, List<PotionEffect> customEffects) {
            this(type, customEffects, 1, List.of(type));
        }
    }

    private CauldronBedrock() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        startBoilingTask();
    }

    private static void startBoilingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Config.CAULDRON_BEDROCK_ENABLED) return;
                for (String key : cauldronPotions.keySet()) {
                    String[] parts = key.split(":");
                    if (parts.length != 4) continue;
                    try {
                        String worldName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        World w = worldOf(worldName);
                        if (w == null) continue;
                        if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                        Block cauldron = w.getBlockAt(x, y, z);
                        if (cauldron.getType() != Material.WATER_CAULDRON) continue;
                        Block below = cauldron.getRelative(0, -1, 0);
                        Material belowType = below.getType();
                        boolean isCampfire = belowType == Material.CAMPFIRE || belowType == Material.SOUL_CAMPFIRE;
                        boolean overHeat = (isCampfire && below.getBlockData() instanceof org.bukkit.block.data.type.Campfire cf && cf.isLit())
                                || belowType == Material.FIRE || belowType == Material.SOUL_FIRE;
                        if (!overHeat) continue;
                        PotionData data = cauldronPotions.get(key);
                        if (data == null) continue;
                        Color color = getPotionDisplayColor(data);
                        double py = waterLevelY(getWaterLevel(cauldron)) + 0.3;
                        w.spawnParticle(Particle.DUST, cauldron.getLocation().add(0.5, py, 0.5),
                                3, 0.2, 0.05, 0.2, 0,
                                new Particle.DustOptions(color, 1.0f));
                        w.playSound(cauldron.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 1.5f);
                    } catch (NumberFormatException ignored) {}
                }
                for (String key : cauldronDyeColors.keySet()) {
                    String[] parts = key.split(":");
                    if (parts.length != 4) continue;
                    try {
                        String worldName = parts[0];
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        World w = worldOf(worldName);
                        if (w == null) continue;
                        if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                        Block cauldron = w.getBlockAt(x, y, z);
                        if (cauldron.getType() != Material.WATER_CAULDRON) continue;
                        if (cauldronPotions.containsKey(key)) continue;
                        Block below = cauldron.getRelative(0, -1, 0);
                        Material belowType = below.getType();
                        boolean isCampfire = belowType == Material.CAMPFIRE || belowType == Material.SOUL_CAMPFIRE;
                        boolean overHeat = (isCampfire && below.getBlockData() instanceof org.bukkit.block.data.type.Campfire cf && cf.isLit())
                                || belowType == Material.FIRE || belowType == Material.SOUL_FIRE;
                        if (!overHeat) continue;
                        Color color = cauldronDyeColors.get(key);
                        if (color == null) continue;
                        double py = waterLevelY(getWaterLevel(cauldron)) + 0.3;
                        w.spawnParticle(Particle.DUST, cauldron.getLocation().add(0.5, py, 0.5),
                                3, 0.2, 0.05, 0.2, 0,
                                new Particle.DustOptions(color, 1.0f));
                        w.playSound(cauldron.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 1.5f);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** Мир по UID из ключа котла (ключи не зависят от имени мира). */
    static World worldOf(String id) {
        try {
            return plugin.getServer().getWorld(java.util.UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String key(Location loc) {
        return loc.getWorld().getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public static Color getDyeColor(Block block) {
        return cauldronDyeColors.get(key(block.getLocation()));
    }

    public static void setDyeColor(Block block, Color color) {
        String k = key(block.getLocation());
        if (color != null) {
            cauldronDyeColors.put(k, color);
            saveDyePDC(block, color.asRGB());
        } else {
            cauldronDyeColors.remove(k);
            clearDyePDC(block);
        }
    }

    public static PotionData getPotion(Block block) {
        return cauldronPotions.get(key(block.getLocation()));
    }

    public static void setPotion(Block block, PotionData data) {
        String k = key(block.getLocation());
        if (data != null) {
            cauldronPotions.put(k, data);
            savePotionPDC(block, data);
        } else {
            cauldronPotions.remove(k);
            clearPotionPDC(block);
        }
    }

    public static void removeAll(Block block) {
        String k = key(block.getLocation());
        cauldronDyeColors.remove(k);
        cauldronPotions.remove(k);
        clearDyePDC(block);
        clearPotionPDC(block);
        removeDisplay(block);
        CauldronHoney.removeAll(block);
    }

    private static final Set<PotionType> NEGATIVE_TYPES = Set.of(
            PotionType.POISON, PotionType.LONG_POISON, PotionType.STRONG_POISON,
            PotionType.HARMING, PotionType.STRONG_HARMING,
            PotionType.WEAKNESS, PotionType.LONG_WEAKNESS,
            PotionType.SLOWNESS, PotionType.LONG_SLOWNESS, PotionType.STRONG_SLOWNESS,
            PotionType.INFESTED, PotionType.OOZING, PotionType.WEAVING
    );

    private static final Set<PotionType> POSITIVE_TYPES = Set.of(
            PotionType.FIRE_RESISTANCE, PotionType.LONG_FIRE_RESISTANCE,
            PotionType.SWIFTNESS, PotionType.LONG_SWIFTNESS, PotionType.STRONG_SWIFTNESS,
            PotionType.HEALING, PotionType.STRONG_HEALING,
            PotionType.REGENERATION, PotionType.LONG_REGENERATION, PotionType.STRONG_REGENERATION,
            PotionType.STRENGTH, PotionType.LONG_STRENGTH, PotionType.STRONG_STRENGTH,
            PotionType.NIGHT_VISION, PotionType.LONG_NIGHT_VISION,
            PotionType.WATER_BREATHING, PotionType.LONG_WATER_BREATHING,
            PotionType.LEAPING, PotionType.LONG_LEAPING, PotionType.STRONG_LEAPING,
            PotionType.INVISIBILITY, PotionType.LONG_INVISIBILITY,
            PotionType.SLOW_FALLING, PotionType.LONG_SLOW_FALLING,
            PotionType.LUCK, PotionType.TURTLE_MASTER, PotionType.LONG_TURTLE_MASTER, PotionType.STRONG_TURTLE_MASTER,
            PotionType.WIND_CHARGED
    );

    public enum MixReaction {
        SYNERGY,   // Синергия (бонусный эффект + повышенная длительность, бывает у позитивных, негативных и смешанных)
        NORMAL,    // Стандартное успешное смешивание
        DISSOLVE,  // Размытие зелья в мутную воду
        CHAOS,     // Мутация: зелье в котле заменяется на рандомное (позитивное, негативное или гибридное)
        EXPLODE    // Детонация: котёл взрывается
    }

    public static String family(PotionType type) {
        if (type == null) return "UNKNOWN";
        String name = type.name();
        if (name.startsWith("LONG_")) return name.substring(5);
        if (name.startsWith("STRONG_")) return name.substring(7);
        return name;
    }

    private static int variantTier(PotionType type) {
        if (type == null) return 0;
        String name = type.name();
        if (name.startsWith("STRONG_")) return 2;
        if (name.startsWith("LONG_")) return 1;
        return 0;
    }

    public static boolean isNegativeType(PotionType type) {
        return type != null && NEGATIVE_TYPES.contains(type);
    }

    public static boolean canMix(PotionData existing, PotionData incoming, int level) {
        if (level >= MAX_MIX) return false;
        if (existing != null && incoming != null && existing.mixCount() + incoming.mixCount() > MAX_MIX) return false;
        return true;
    }

    public static Color getPotionDisplayColor(PotionData data) {
        if (data == null) return potionColor(PotionType.WATER);
        if (!data.customEffects().isEmpty() && data.type() == PotionType.AWKWARD) {
            int r = 0, g = 0, b = 0;
            for (PotionEffect fx : data.customEffects()) {
                Color c = fx.getType().getColor();
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
            int n = data.customEffects().size();
            return Color.fromRGB(r / n, g / n, b / n);
        }
        if (data.mixedTypes().size() > 1) {
            return mixPotionColors(data.mixedTypes());
        }
        if (!data.customEffects().isEmpty()) {
            return data.customEffects().get(0).getType().getColor();
        }
        return potionColor(data.type());
    }

    /**
     * Определяет исход смешивания в зависимости от точного порядка наливания зелий:
     * учитывает как позитивные, так и негативные и смешанные последовательности (2-шаговые и 3-шаговые).
     */
    public static MixReaction evaluateReaction(PotionData existing, PotionData incoming) {
        List<PotionType> history = existing.mixedTypes();
        PotionType lastType = history.isEmpty() ? existing.type() : history.get(history.size() - 1);
        PotionType nextType = incoming.type();

        String prev = family(lastType);
        String next = family(nextType);

        // 1. Взаимодействие внутри одного семейства зелий (например, LONG_SWIFTNESS и STRONG_SWIFTNESS)
        if (prev.equals(next)) {
            int prevTier = variantTier(lastType);
            int nextTier = variantTier(nextType);
            if (prevTier == 1 && nextTier == 2) {
                // LONG -> STRONG: идеальное слияние длительности и силы!
                return MixReaction.SYNERGY;
            }
            if (prevTier == 2 && nextTier == 1) {
                // STRONG -> LONG: разбавление концентрата размывает зелье в воду
                return MixReaction.DISSOLVE;
            }
            if (prevTier > nextTier) {
                // Обычное поверх усиленного/долгого — разбавление и размытие
                return MixReaction.DISSOLVE;
            }
            if (prevTier < nextTier) {
                // Усиленное/долгое поверх обычного — успешное укрепление
                return MixReaction.NORMAL;
            }
            // Повтор того же самого зелья поверх уже смешанного отвара вызывает мутацию
            return history.size() > 1 ? MixReaction.CHAOS : MixReaction.NORMAL;
        }

        // 2. Трёхшаговые триады (когда в котле уже 2+ зелья и наливается 3-е)
        if (history.size() >= 2) {
            String first = family(history.get(0));
            if (first.equals(next)) {
                // Замыкание кольца (A -> B -> A): негативная основа взрывается, позитивная мутирует в рандом
                return isNegativeType(history.get(0)) ? MixReaction.EXPLODE : MixReaction.CHAOS;
            }
            MixReaction triad = evaluateTriadReaction(first, prev, next);
            if (triad != null) {
                return triad;
            }
        }

        // 3. Двухшаговый порядок (предыдущее зелье -> новое зелье)
        MixReaction pairReaction = evaluatePairReaction(prev, next);
        if (pairReaction != null) {
            return pairReaction;
        }

        // 4. Универсальная формула порядка для всех остальных комбинаций:
        // Порядок (prev -> next) и (next -> prev) всегда даёт разную реакцию!
        return evaluateFallbackOrderReaction(lastType, nextType, prev, next, history.size());
    }

    private static MixReaction evaluateTriadReaction(String first, String second, String third) {
        String triad = first + "->" + second + "->" + third;
        return switch (triad) {
            // Позитивные синергии триад
            case "SWIFTNESS->STRENGTH->FIRE_RESISTANCE",
                 "NIGHT_VISION->INVISIBILITY->SWIFTNESS",
                 "HEALING->REGENERATION->FIRE_RESISTANCE",
                 "WATER_BREATHING->SWIFTNESS->NIGHT_VISION",
                 "LEAPING->SLOW_FALLING->SWIFTNESS" -> MixReaction.SYNERGY;

            // Негативные синергии триад (проклятия и боевые яды)
            case "POISON->SLOWNESS->WEAKNESS",
                 "POISON->HARMING->SLOWNESS",
                 "SLOWNESS->WEAKNESS->HARMING",
                 "OOZING->WEAVING->INFESTED",
                 "POISON->WEAKNESS->SLOWNESS" -> MixReaction.SYNERGY;

            // Смешанные синергии триад
            case "POISON->INVISIBILITY->SWIFTNESS",
                 "STRENGTH->POISON->FIRE_RESISTANCE",
                 "TURTLE_MASTER->REGENERATION->FIRE_RESISTANCE" -> MixReaction.SYNERGY;

            // Взрывные триады (позитивные, негативные и смешанные)
            case "STRENGTH->REGENERATION->HEALING",
                 "FIRE_RESISTANCE->SWIFTNESS->LEAPING",
                 "HARMING->POISON->WEAKNESS",
                 "WEAKNESS->SLOWNESS->HARMING",
                 "FIRE_RESISTANCE->SWIFTNESS->WIND_CHARGED",
                 "STRENGTH->LEAPING->WIND_CHARGED",
                 "POISON->STRENGTH->HARMING" -> MixReaction.EXPLODE;

            // Размывающие триады
            case "REGENERATION->WATER_BREATHING->NIGHT_VISION",
                 "INVISIBILITY->SLOW_FALLING->WATER_BREATHING",
                 "WEAKNESS->SLOWNESS->POISON",
                 "SWIFTNESS->STRENGTH->WEAKNESS",
                 "POISON->SLOWNESS->HEALING" -> MixReaction.DISSOLVE;

            // Мутирующие в рандом триады
            case "HEALING->SWIFTNESS->STRENGTH",
                 "SLOWNESS->POISON->HARMING",
                 "INFESTED->OOZING->POISON",
                 "NIGHT_VISION->POISON->REGENERATION",
                 "LEAPING->WEAVING->WIND_CHARGED" -> MixReaction.CHAOS;

            default -> null;
        };
    }

    private static MixReaction evaluatePairReaction(String prev, String next) {
        // Особое правило для Удачи (LUCK):
        // В качестве основы даёт синергию с мобильностью/регеном, а добавленная поверх (X -> LUCK) испытывает судьбу (CHAOS)!
        if ("LUCK".equals(prev)) {
            if ("SWIFTNESS".equals(next) || "LEAPING".equals(next)
                    || "WATER_BREATHING".equals(next) || "REGENERATION".equals(next)) {
                return MixReaction.SYNERGY;
            }
        }
        if ("LUCK".equals(next)) return MixReaction.CHAOS;

        String pair = prev + "->" + next;
        return switch (pair) {
            // ===== 1. ПОЗИТИВНЫЕ + ПОЗИТИВНЫЕ (порядок решает!) =====
            // Скорость и Сила
            case "SWIFTNESS->STRENGTH" -> MixReaction.SYNERGY;       // Разгон в удар -> Синергия
            case "STRENGTH->SWIFTNESS" -> MixReaction.CHAOS;         // Тяжесть ломает темп -> Рандомное зелье

            // Исцеление и Регенерация
            case "HEALING->REGENERATION" -> MixReaction.SYNERGY;     // Исцеление запускает реген -> Синергия
            case "REGENERATION->HEALING" -> MixReaction.DISSOLVE;    // Перенасыщение жизни -> Размытие

            // Ночное зрение и Невидимость
            case "NIGHT_VISION->INVISIBILITY" -> MixReaction.SYNERGY;// Призрачный взор -> Синергия
            case "INVISIBILITY->NIGHT_VISION" -> MixReaction.DISSOLVE;// Свет гасит иллюзию -> Размытие

            // Огнестойкость и Черепашья мощь
            case "FIRE_RESISTANCE->TURTLE_MASTER" -> MixReaction.SYNERGY; // Закалка панциря -> Синергия
            case "TURTLE_MASTER->FIRE_RESISTANCE" -> MixReaction.EXPLODE; // Перегрев внутри панциря -> Взрыв!

            // Водное дыхание и Скорость
            case "WATER_BREATHING->SWIFTNESS" -> MixReaction.SYNERGY;// Поток дельфина -> Синергия
            case "SWIFTNESS->WATER_BREATHING" -> MixReaction.DISSOLVE;// Выветривание -> Размытие

            // Прыгучесть и Плавное падение
            case "LEAPING->SLOW_FALLING" -> MixReaction.SYNERGY;     // Прыжок с мягкой посадкой -> Синергия
            case "SLOW_FALLING->LEAPING" -> MixReaction.CHAOS;       // Гравитационный сбой -> Рандомное зелье

            // Огнестойкость и Водное дыхание
            case "FIRE_RESISTANCE->WATER_BREATHING" -> MixReaction.DISSOLVE; // Вода гасит лавовую основу -> Размытие
            case "WATER_BREATHING->FIRE_RESISTANCE" -> MixReaction.CHAOS;    // Вскипание воды -> Рандомное зелье

            // Сила и Исцеление
            case "STRENGTH->HEALING" -> MixReaction.DISSOLVE;        // Грубая сила разрушает лечение -> Размытие
            case "HEALING->STRENGTH" -> MixReaction.NORMAL;          // Здоровая основа принимает силу -> Смесь

            // Скорость и Черепашья мощь
            case "SWIFTNESS->TURTLE_MASTER" -> MixReaction.EXPLODE;  // Таран на скорости в панцирь -> Взрыв!
            case "TURTLE_MASTER->SWIFTNESS" -> MixReaction.CHAOS;    // Трещины в панцире -> Рандомное зелье

            // Сила и Огнестойкость
            case "STRENGTH->FIRE_RESISTANCE" -> MixReaction.SYNERGY; // Пламенная мощь -> Синергия
            case "FIRE_RESISTANCE->STRENGTH" -> MixReaction.NORMAL;  // Стойкая сила -> Смесь

            // Регенерация и Огнестойкость
            case "REGENERATION->FIRE_RESISTANCE" -> MixReaction.DISSOLVE; // Сворачивание крови от жара -> Размытие
            case "FIRE_RESISTANCE->REGENERATION" -> MixReaction.NORMAL;   // Тёплая регенерация -> Смесь

            // Невидимость и Скорость
            case "INVISIBILITY->SWIFTNESS" -> MixReaction.SYNERGY;   // Бесшумный бег -> Синергия
            case "SWIFTNESS->INVISIBILITY" -> MixReaction.DISSOLVE;  // Вихрь срывает покров -> Размытие

            // Прыгучесть и Сила
            case "LEAPING->STRENGTH" -> MixReaction.EXPLODE;         // Сокрушительный удар с воздуха -> Взрыв!
            case "STRENGTH->LEAPING" -> MixReaction.NORMAL;          // Сильные ноги -> Смесь

            // Ночное зрение и Водное дыхание
            case "NIGHT_VISION->WATER_BREATHING" -> MixReaction.SYNERGY; // Зрение глубин -> Синергия
            case "WATER_BREATHING->NIGHT_VISION" -> MixReaction.DISSOLVE;// Мутные блики -> Размытие

            // Огнестойкость и Прыгучесть
            case "FIRE_RESISTANCE->LEAPING" -> MixReaction.EXPLODE;  // Реактивный взрыв -> Взрыв!
            case "LEAPING->FIRE_RESISTANCE" -> MixReaction.NORMAL;   // Прыжок сквозь огонь -> Смесь

            // Исцеление / Регенерация и Невидимость
            case "HEALING->INVISIBILITY",
                 "REGENERATION->INVISIBILITY" -> MixReaction.CHAOS;  // Фантомная плоть -> Рандомное зелье
            case "INVISIBILITY->HEALING",
                 "INVISIBILITY->REGENERATION" -> MixReaction.DISSOLVE;// Проявление тела -> Размытие

            // Плавное падение и Водное дыхание
            case "SLOW_FALLING->WATER_BREATHING" -> MixReaction.DISSOLVE; // Всплытие на поверхность -> Размытие
            case "WATER_BREATHING->SLOW_FALLING" -> MixReaction.NORMAL;   // Донное парение -> Смесь

            // ===== 2. НЕГАТИВНЫЕ + НЕГАТИВНЫЕ (алхимия ядов и проклятий!) =====
            // Яд и Замедление
            case "POISON->SLOWNESS" -> MixReaction.SYNERGY;          // Парализующий яд -> Тёмная синергия
            case "SLOWNESS->POISON" -> MixReaction.DISSOLVE;         // В вязкой среде яд выпадает в осадок -> Размытие

            // Яд и Моментальный урон
            case "POISON->HARMING" -> MixReaction.SYNERGY;           // Некротоксин -> Тёмная синергия (+Иссушение)
            case "HARMING->POISON" -> MixReaction.EXPLODE;           // Реакция распада -> Взрыв котла!

            // Замедление и Слабость
            case "SLOWNESS->WEAKNESS" -> MixReaction.SYNERGY;        // Полное бессилие -> Тёмная синергия (+Утомление)
            case "WEAKNESS->SLOWNESS" -> MixReaction.DISSOLVE;       // Два осадка гасят друг друга -> Размытие

            // Яд и Слабость
            case "POISON->WEAKNESS" -> MixReaction.SYNERGY;          // Болезненная лихорадка -> Тёмная синергия (+Тошнота)
            case "WEAKNESS->POISON" -> MixReaction.CHAOS;            // Брожение токсина -> Рандомное зелье

            // Слабость и Моментальный урон
            case "WEAKNESS->HARMING" -> MixReaction.EXPLODE;         // Пробой защитной оболочки -> Взрыв котла!
            case "HARMING->WEAKNESS" -> MixReaction.DISSOLVE;        // Урон гаснет в слабости -> Размытие

            // Замедление и Моментальный урон
            case "SLOWNESS->HARMING" -> MixReaction.SYNERGY;         // Каменный удар -> Тёмная синергия (+Слепота)
            case "HARMING->SLOWNESS" -> MixReaction.CHAOS;           // Сжатие тёмной материи -> Рандомное зелье

            // Слизистость (OOZING) и Яд
            case "OOZING->POISON" -> MixReaction.SYNERGY;            // Ядовитая слизь -> Тёмная синергия (+Голод)
            case "POISON->OOZING" -> MixReaction.DISSOLVE;           // Кислота растворяет слизь -> Размытие

            // Паутина (WEAVING) и Замедление
            case "WEAVING->SLOWNESS" -> MixReaction.SYNERGY;         // Паучий кокон -> Тёмная синергия (+Тьма)
            case "SLOWNESS->WEAVING" -> MixReaction.CHAOS;           // Спутанные чары -> Рандомное зелье

            // Заражение (INFESTED) и Яд / Урон / Паутина / Слизь
            case "INFESTED->POISON" -> MixReaction.CHAOS;            // Мутация паразитов -> Рандомное зелье
            case "POISON->INFESTED" -> MixReaction.DISSOLVE;         // Яд травит личинок -> Размытие
            case "INFESTED->HARMING",
                 "HARMING->INFESTED" -> MixReaction.EXPLODE;         // Детонация роя -> Взрыв котла!
            case "INFESTED->WEAVING" -> MixReaction.SYNERGY;         // Гнездо паразитов -> Тёмная синергия
            case "WEAVING->INFESTED" -> MixReaction.CHAOS;           // Бешенство роя -> Рандомное зелье
            case "OOZING->INFESTED" -> MixReaction.CHAOS;            // Био-мутант -> Рандомное зелье
            case "INFESTED->OOZING" -> MixReaction.DISSOLVE;         // Паразиты тонут в слизи -> Размытие
            case "OOZING->HARMING" -> MixReaction.EXPLODE;           // Разрыв слизи -> Взрыв котла!
            case "HARMING->OOZING" -> MixReaction.NORMAL;            // Едкая слизь -> Смесь
            case "WEAVING->OOZING" -> MixReaction.DISSOLVE;          // Слипшийся комок -> Размытие
            case "OOZING->WEAVING" -> MixReaction.NORMAL;            // Вязкая сеть -> Смесь

            // ===== 3. СМЕШАННЫЕ: ПОЗИТИВНЫЕ + НЕГАТИВНЫЕ И НАОБОРОТ =====
            // Исцеление и Урон
            case "HEALING->HARMING" -> MixReaction.EXPLODE;          // Столкновение жизни и смерти -> Взрыв котла!
            case "HARMING->HEALING" -> MixReaction.CHAOS;            // Искажённое исцеление -> Рандомное зелье

            // Регенерация и Яд
            case "REGENERATION->POISON" -> MixReaction.DISSOLVE;     // Яд сворачивает кровь -> Размытие
            case "POISON->REGENERATION" -> MixReaction.CHAOS;        // Бешеная мутация клеток -> Рандомное зелье

            // Регенерация и Урон
            case "REGENERATION->HARMING" -> MixReaction.EXPLODE;     // Разрыв жизненного потока -> Взрыв котла!
            case "HARMING->REGENERATION" -> MixReaction.DISSOLVE;    // Некроз гасит регенерацию -> Размытие

            // Сила и Слабость
            case "STRENGTH->WEAKNESS" -> MixReaction.DISSOLVE;       // Слабость гасит силу -> Размытие
            case "WEAKNESS->STRENGTH" -> MixReaction.EXPLODE;        // Рывок ярости рвёт котёл -> Взрыв котла!

            // Скорость и Замедление
            case "SWIFTNESS->SLOWNESS" -> MixReaction.DISSOLVE;      // Полная остановка -> Размытие
            case "SLOWNESS->SWIFTNESS" -> MixReaction.CHAOS;         // Рывок сквозь оковы -> Рандомное зелье

            // Невидимость и Яд / Урон
            case "INVISIBILITY->POISON" -> MixReaction.EXPLODE;      // Взрыв ядовитых паров -> Взрыв котла!
            case "POISON->INVISIBILITY" -> MixReaction.SYNERGY;      // Скрытый токсин ассасина -> Синергия
            case "INVISIBILITY->HARMING" -> MixReaction.EXPLODE;     // Теневая детонация -> Взрыв котла!
            case "HARMING->INVISIBILITY" -> MixReaction.CHAOS;       // Фантомный урон -> Рандомное зелье

            // Сила и Яд / Урон
            case "STRENGTH->POISON" -> MixReaction.SYNERGY;          // Ядовитая ярость берсерка -> Синергия
            case "POISON->STRENGTH" -> MixReaction.EXPLODE;          // Каталитический взрыв яда -> Взрыв котла!
            case "STRENGTH->HARMING" -> MixReaction.EXPLODE;         // Перегрузка разрушения -> Взрыв котла!
            case "HARMING->STRENGTH" -> MixReaction.CHAOS;           // Кровавое безумие -> Рандомное зелье

            // Огнестойкость и Яд / Урон / Замедление
            case "FIRE_RESISTANCE->POISON" -> MixReaction.DISSOLVE;  // Выжигание токсина -> Размытие
            case "POISON->FIRE_RESISTANCE" -> MixReaction.CHAOS;     // Кипящий мутаген -> Рандомное зелье
            case "FIRE_RESISTANCE->HARMING" -> MixReaction.EXPLODE;  // Термобарический взрыв -> Взрыв котла!
            case "HARMING->FIRE_RESISTANCE" -> MixReaction.DISSOLVE; // Поглощение удара теплом -> Размытие

            // Ночное зрение и Слабость / Яд
            case "NIGHT_VISION->WEAKNESS" -> MixReaction.DISSOLVE;   // Помутнение взгляда -> Размытие
            case "WEAKNESS->NIGHT_VISION" -> MixReaction.CHAOS;      // Галлюцинации -> Рандомное зелье

            // Прыгучесть и Замедление
            case "LEAPING->SLOWNESS" -> MixReaction.DISSOLVE;        // Притяжение к земле -> Размытие
            case "SLOWNESS->LEAPING" -> MixReaction.EXPLODE;         // Срыв сжатой пружины -> Взрыв котла!

            // Заряд ветра (WIND_CHARGED) с другими зельями
            case "WIND_CHARGED->SWIFTNESS" -> MixReaction.SYNERGY;   // Попутный ураган -> Синергия
            case "SWIFTNESS->WIND_CHARGED" -> MixReaction.EXPLODE;   // Сверхзвуковой взрыв -> Взрыв котла!
            case "STRENGTH->WIND_CHARGED" -> MixReaction.EXPLODE;    // Ударная волна -> Взрыв котла!
            case "WIND_CHARGED->STRENGTH" -> MixReaction.CHAOS;      // Буря силы -> Рандомное зелье
            case "WIND_CHARGED->SLOW_FALLING" -> MixReaction.DISSOLVE;// Ветер развеивает перо -> Размытие
            case "SLOW_FALLING->WIND_CHARGED" -> MixReaction.SYNERGY;// Восходящий поток -> Синергия
            case "WIND_CHARGED->HARMING" -> MixReaction.EXPLODE;     // Фугасный снаряд -> Взрыв котла!
            case "HARMING->WIND_CHARGED" -> MixReaction.CHAOS;       // Разброс осколков -> Рандомное зелье

            // Черепашья мощь с негативными
            case "TURTLE_MASTER->SLOWNESS" -> MixReaction.SYNERGY;   // Каменная крепость -> Синергия
            case "SLOWNESS->TURTLE_MASTER" -> MixReaction.DISSOLVE;  // Окаменение в воду -> Размытие
            case "TURTLE_MASTER->POISON" -> MixReaction.DISSOLVE;    // Панцирь не пропускает яд -> Размытие
            case "POISON->TURTLE_MASTER" -> MixReaction.CHAOS;       // Протравленный панцирь -> Рандомное зелье

            // Слизь и Заражение с Регенерацией / Исцелением
            case "OOZING->REGENERATION",
                 "INFESTED->REGENERATION" -> MixReaction.CHAOS;      // Мутация биомассы -> Рандомное зелье
            case "REGENERATION->OOZING",
                 "REGENERATION->INFESTED",
                 "HEALING->OOZING",
                 "HEALING->INFESTED",
                 "HEALING->WEAKNESS" -> MixReaction.DISSOLVE;        // Очищение раствора -> Размытие

            default -> null;
        };
    }

    /**
     * Алгоритм реакции для остальных пар, гарантирующий, что прямой (A -> B) и обратный (B -> A)
     * порядок ведут себя по-разному и могут приводить к смешиванию, размытию, рандому или взрыву.
     */
    private static MixReaction evaluateFallbackOrderReaction(PotionType lastType, PotionType nextType,
                                                             String prev, String next, int stepCount) {
        int h1 = Math.abs(prev.hashCode() * 31 + next.hashCode());
        int h2 = Math.abs(next.hashCode() * 31 + prev.hashCode());
        boolean forwardOrder = (prev.compareTo(next) < 0) ^ ((h1 & 1) == 0);
        boolean samePolarity = (isNegativeType(lastType) == isNegativeType(nextType));

        int roll = ThreadLocalRandom.current().nextInt(100);
        int bucket = (h1 ^ (h2 >>> 3)) % 4;

        if (samePolarity) {
            // Однополярные зелья (оба позитивные или оба негативные):
            // В правильном направлении — стабильная смесь (или синергия), в обратном — риск размытия, хаоса или взрыва!
            if (forwardOrder) {
                if (stepCount >= 2 && roll < 25) {
                    return bucket == 0 ? MixReaction.DISSOLVE : (bucket == 1 ? MixReaction.CHAOS : MixReaction.EXPLODE);
                }
                return roll < 15 ? MixReaction.SYNERGY : MixReaction.NORMAL;
            } else {
                return switch (bucket) {
                    case 0 -> roll < 65 ? MixReaction.DISSOLVE : MixReaction.NORMAL;
                    case 1 -> roll < 65 ? MixReaction.CHAOS : MixReaction.NORMAL;
                    case 2 -> roll < 45 ? MixReaction.EXPLODE : MixReaction.CHAOS;
                    default -> roll < 50 ? MixReaction.DISSOLVE : MixReaction.CHAOS;
                };
            }
        } else {
            // Разнополярные зелья (позитивное + негативное или негативное + позитивное):
            // Сильно зависят от того, что налито первым (основа), а что вторым (катализатор)
            if (forwardOrder) {
                return switch (bucket) {
                    case 0 -> roll < 70 ? MixReaction.DISSOLVE : MixReaction.NORMAL;
                    case 1 -> roll < 70 ? MixReaction.CHAOS : MixReaction.NORMAL;
                    case 2 -> MixReaction.NORMAL;
                    default -> roll < 60 ? MixReaction.EXPLODE : MixReaction.DISSOLVE;
                };
            } else {
                return switch (bucket) {
                    case 0 -> roll < 70 ? MixReaction.EXPLODE : MixReaction.CHAOS;
                    case 1 -> roll < 70 ? MixReaction.DISSOLVE : MixReaction.CHAOS;
                    case 2 -> roll < 65 ? MixReaction.CHAOS : MixReaction.EXPLODE;
                    default -> roll < 50 ? MixReaction.NORMAL : MixReaction.DISSOLVE;
                };
            }
        }
    }

    public static PotionData mixPotions(PotionData existing, PotionData incoming) {
        return mixPotions(existing, incoming, false);
    }

    public static PotionData mixPotions(PotionData existing, PotionData incoming, boolean synergy) {
        List<PotionEffect> existingEffects = getAllEffects(existing);
        List<PotionEffect> incomingEffects = getAllEffects(incoming);

        List<PotionType> history = existing.mixedTypes();
        PotionType lastType = history.isEmpty() ? existing.type() : history.get(history.size() - 1);

        List<PotionType> allTypes = new ArrayList<>(history);
        allTypes.add(incoming.type());

        int negCount = 0;
        int posCount = 0;
        for (PotionType t : allTypes) {
            if (NEGATIVE_TYPES.contains(t)) negCount++;
            else if (POSITIVE_TYPES.contains(t)) posCount++;
        }

        int oldMixCount = Math.max(1, existing.mixCount());
        int inMixCount = Math.max(1, incoming.mixCount());
        int totalPotions = oldMixCount + inMixCount;
        int newMixCount = Math.min(totalPotions, MAX_MIX);

        // При смешивании N зелий длительность каждого составляет 1/N от исходной:
        // 2 зелья -> 50% (1/2), 3 зелья -> 33.3% (1/3) и т.д. всегда становится короче!
        double existingFactor = (double) oldMixCount / totalPotions;
        double incomingFactor = (double) inMixCount / totalPotions;
        double bonusFactor = 1.0 / totalPotions;

        List<PotionEffect> pool = new ArrayList<>();
        for (PotionEffect e : existingEffects) {
            boolean instant = e.getType().isInstant();
            int d = e.getDuration() < 0 ? -1
                    : instant ? 1 : Math.max(1, (int) Math.round(e.getDuration() * existingFactor));
            int amp = (instant && !synergy && e.getAmplifier() > 0) ? e.getAmplifier() - 1 : e.getAmplifier();
            pool.add(new PotionEffect(e.getType(), d, amp));
        }
        for (PotionEffect e : incomingEffects) {
            boolean instant = e.getType().isInstant();
            int d = e.getDuration() < 0 ? -1
                    : instant ? 1 : Math.max(1, (int) Math.round(e.getDuration() * incomingFactor));
            int amp = (instant && !synergy && e.getAmplifier() > 0) ? e.getAmplifier() - 1 : e.getAmplifier();
            pool.add(new PotionEffect(e.getType(), d, amp));
        }

        // Объединяем одинаковые эффекты:
        // При синергии одного семейства (LONG -> STRONG) берём максимальную (уже урезанную до 1/N) длительность И максимальную силу!
        boolean sameFamilySynergy = synergy && family(lastType).equals(family(incoming.type()));
        Map<PotionEffectType, PotionEffect> merged = new LinkedHashMap<>();
        for (PotionEffect e : pool) {
            PotionEffectType t = e.getType();
            PotionEffect cur = merged.get(t);
            if (cur == null) {
                merged.put(t, e);
            } else if (sameFamilySynergy) {
                int maxDur = cur.getDuration() < 0 || e.getDuration() < 0 ? -1
                        : Math.max(cur.getDuration(), e.getDuration());
                int maxAmp = Math.max(cur.getAmplifier(), e.getAmplifier());
                merged.put(t, new PotionEffect(t, maxDur, maxAmp));
            } else {
                int duration = cur.getDuration() < 0 || e.getDuration() < 0 ? -1
                        : Math.max(cur.getDuration(), e.getDuration());
                int amplifier = (int) (((long) cur.getAmplifier() + e.getAmplifier()) / 2);
                merged.put(t, new PotionEffect(t, duration, amplifier));
            }
        }

        if (synergy) applySynergyBonuses(lastType, incoming.type(), allTypes, merged);

        List<PotionEffect> resultEffects = new ArrayList<>(merged.values());

        // Если уникальных эффектов больше MAX_EFFECTS, оставляем MAX_EFFECTS самых сильных при синергии, иначе фейл
        if (distinctEffectCount(resultEffects) > MAX_EFFECTS) {
            if (synergy) {
                resultEffects.sort((a, b) -> better(b, a) ? 1 : -1);
                resultEffects = new ArrayList<>(resultEffects.subList(0, MAX_EFFECTS));
            } else {
                return null;
            }
        }

        return new PotionData(existing.type(), resultEffects, newMixCount, allTypes,
                existing.powderInfused() || incoming.powderInfused(),
                existing.windInfused() || incoming.windInfused());
    }

    private static void putBonusEffect(Map<PotionEffectType, PotionEffect> merged, PotionEffect bonus) {
        if (merged.containsKey(bonus.getType()) || merged.size() < MAX_EFFECTS) {
            PotionEffect cur = merged.get(bonus.getType());
            if (cur == null || better(bonus, cur)) {
                merged.put(bonus.getType(), bonus);
            }
        }
    }

    private static void applySynergyBonuses(PotionType prevType, PotionType nextType,
                                            List<PotionType> allTypes, Map<PotionEffectType, PotionEffect> merged) {
        String f = family(prevType);
        String s = family(nextType);

        // Позитивные синергии (25-35 секунд)
        if (f.equals("SWIFTNESS") && s.equals("STRENGTH")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.JUMP_BOOST, 600, 0));
        } else if (f.equals("HEALING") && s.equals("REGENERATION")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.ABSORPTION, 600, 0));
        } else if (f.equals("NIGHT_VISION") && s.equals("INVISIBILITY")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.SPEED, 600, 0));
        } else if (f.equals("FIRE_RESISTANCE") && s.equals("TURTLE_MASTER")) {
            // Ослабляем Замедление черепашьей мощи вдвое (например, с IV до II), а не убираем полностью
            PotionEffect slow = merged.get(PotionEffectType.SLOWNESS);
            if (slow != null) {
                merged.put(PotionEffectType.SLOWNESS,
                        new PotionEffect(PotionEffectType.SLOWNESS, slow.getDuration(), Math.max(0, slow.getAmplifier() / 2)));
            }
        } else if (f.equals("WATER_BREATHING") && s.equals("SWIFTNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 600, 0));
        } else if (f.equals("LEAPING") && s.equals("SLOW_FALLING")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.SPEED, 600, 0));
        } else if (f.equals("STRENGTH") && s.equals("FIRE_RESISTANCE")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.HASTE, 600, 0));
        } else if (f.equals("INVISIBILITY") && s.equals("SWIFTNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.SLOW_FALLING, 500, 0));
        } else if (f.equals("NIGHT_VISION") && s.equals("WATER_BREATHING")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.CONDUIT_POWER, 600, 0));
        } else if (f.equals("WIND_CHARGED") && s.equals("SWIFTNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.JUMP_BOOST, 600, 0));
        } else if (f.equals("LUCK")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.ABSORPTION, 600, 0));
        }
        // Негативные синергии (15-25 секунд)
        else if (f.equals("POISON") && s.equals("SLOWNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.WEAKNESS, 500, 0));
        } else if (f.equals("POISON") && s.equals("HARMING")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.WITHER, 300, 0));
        } else if (f.equals("SLOWNESS") && s.equals("WEAKNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.MINING_FATIGUE, 500, 0));
        } else if (f.equals("POISON") && s.equals("WEAKNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.NAUSEA, 360, 0));
        } else if (f.equals("SLOWNESS") && s.equals("HARMING")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.BLINDNESS, 300, 0));
        } else if (f.equals("OOZING") && s.equals("POISON")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.HUNGER, 500, 1));
        } else if (f.equals("WEAVING") && s.equals("SLOWNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.DARKNESS, 360, 0));
        } else if (f.equals("INFESTED") && s.equals("WEAVING")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.SLOWNESS, 500, 1));
        }
        // Смешанные синергии (25 секунд)
        else if (f.equals("POISON") && s.equals("INVISIBILITY")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.NIGHT_VISION, 500, 0));
        } else if (f.equals("STRENGTH") && s.equals("POISON")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.HASTE, 500, 0));
        } else if (f.equals("SLOW_FALLING") && s.equals("WIND_CHARGED")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.SPEED, 500, 0));
        } else if (f.equals("TURTLE_MASTER") && s.equals("SLOWNESS")) {
            putBonusEffect(merged, new PotionEffect(PotionEffectType.ABSORPTION, 500, 0));
        }

        // Бонусы для 3-шаговых триад (20-40 секунд)
        if (allTypes.size() >= 3) {
            String triad = family(allTypes.get(0)) + "->" + f + "->" + s;
            switch (triad) {
                case "SWIFTNESS->STRENGTH->FIRE_RESISTANCE" ->
                        putBonusEffect(merged, new PotionEffect(PotionEffectType.RESISTANCE, 600, 0));
                case "HEALING->REGENERATION->FIRE_RESISTANCE" ->
                        putBonusEffect(merged, new PotionEffect(PotionEffectType.ABSORPTION, 800, 1));
                case "POISON->SLOWNESS->WEAKNESS", "POISON->WEAKNESS->SLOWNESS" ->
                        putBonusEffect(merged, new PotionEffect(PotionEffectType.WITHER, 400, 0));
                case "POISON->HARMING->SLOWNESS", "SLOWNESS->WEAKNESS->HARMING" ->
                        putBonusEffect(merged, new PotionEffect(PotionEffectType.DARKNESS, 400, 0));
                case "OOZING->WEAVING->INFESTED" ->
                        putBonusEffect(merged, new PotionEffect(PotionEffectType.POISON, 500, 1));
            }
        }
    }

    /** true, если a сильнее b (больше амплитуда, при равной амплитуде — больше длительность). */
    private static boolean better(PotionEffect a, PotionEffect b) {
        if (a.getAmplifier() != b.getAmplifier()) return a.getAmplifier() > b.getAmplifier();
        return a.getDuration() > b.getDuration();
    }

    public static List<PotionEffect> getAllEffects(PotionData data) {
        if (!data.customEffects().isEmpty()) {
            return new ArrayList<>(data.customEffects());
        }
        return new ArrayList<>(realPotionEffects(data.type()));
    }

    public static boolean hasSameEffectTypes(PotionData first, PotionData second) {
        List<PotionEffect> firstEffects = getAllEffects(first);
        List<PotionEffect> secondEffects = getAllEffects(second);
        return PotionLayerAverages.sameDistinctTypes(
                firstEffects.stream().map(PotionEffect::getType).toList(),
                secondEffects.stream().map(PotionEffect::getType).toList());
    }

    public static boolean hasOverlappingEffectTypes(PotionData first, PotionData second) {
        return PotionLayerAverages.overlaps(
                getAllEffects(first).stream().map(PotionEffect::getType).toList(),
                getAllEffects(second).stream().map(PotionEffect::getType).toList());
    }

    public static boolean hasSameEffectValues(PotionData first, PotionData second) {
        if (!hasSameEffectTypes(first, second)) return false;
        return getAllEffects(first).stream().allMatch(old -> getAllEffects(second).stream()
                .anyMatch(added -> added.getType().equals(old.getType())
                        && added.getDuration() == old.getDuration()
                        && added.getAmplifier() == old.getAmplifier()));
    }

    public static PotionData blendOverlappingEffects(PotionData existing, PotionData incoming, int existingLevel) {
        List<PotionEffect> oldEffects = getAllEffects(existing);
        List<PotionEffect> addedEffects = getAllEffects(incoming);
        int layers = existingLevel + 1;
        boolean stored = existing.matchingLayers() == existingLevel
                && existing.durationTotals().size() == oldEffects.size()
                && existing.amplifierTotals().size() == oldEffects.size();
        List<PotionEffect> averaged = new ArrayList<>(oldEffects.size() + addedEffects.size());
        List<Long> durationTotals = new ArrayList<>(oldEffects.size() + addedEffects.size());
        List<Long> amplifierTotals = new ArrayList<>(oldEffects.size() + addedEffects.size());
        for (int i = 0; i < oldEffects.size(); i++) {
            PotionEffect old = oldEffects.get(i);
            PotionEffect added = addedEffects.stream().filter(e -> e.getType().equals(old.getType()))
                    .findFirst().orElse(old);
            long oldDurationTotal = stored ? existing.durationTotals().get(i)
                    : PotionLayerAverages.totalFor(old.getDuration(), existingLevel);
            long oldAmplifierTotal = stored ? existing.amplifierTotals().get(i)
                    : PotionLayerAverages.totalFor(old.getAmplifier(), existingLevel);
            long durationTotal = oldDurationTotal < 0 || added.getDuration() < 0
                    ? -1 : oldDurationTotal + added.getDuration();
            long amplifierTotal = oldAmplifierTotal + added.getAmplifier();
            int duration = durationTotal < 0 ? -1 : PotionLayerAverages.average(durationTotal, layers);
            int amplifier = PotionLayerAverages.average(amplifierTotal, layers);
            averaged.add(new PotionEffect(old.getType(), duration, amplifier,
                    old.isAmbient(), old.hasParticles(), old.hasIcon()));
            durationTotals.add(durationTotal);
            amplifierTotals.add(amplifierTotal);
        }
        for (PotionEffect added : addedEffects) {
            if (oldEffects.stream().anyMatch(old -> old.getType().equals(added.getType()))) continue;
            averaged.add(added);
            durationTotals.add(PotionLayerAverages.totalFor(added.getDuration(), layers));
            amplifierTotals.add(PotionLayerAverages.totalFor(added.getAmplifier(), layers));
        }
        boolean expanded = averaged.size() > oldEffects.size();
        List<PotionType> history = new ArrayList<>(existing.mixedTypes());
        if (expanded) {
            history.add(incoming.type());
            while (history.size() > MAX_MIX) history.remove(0);
        }
        int mixCount = expanded ? Math.min(MAX_MIX, existing.mixCount() + incoming.mixCount()) : existing.mixCount();
        return new PotionData(existing.type(), averaged, mixCount, history,
                existing.currentPointer(), existing.redstoneCredit(), layers, durationTotals, amplifierTotals,
                existing.powderInfused() || incoming.powderInfused(),
                existing.windInfused() || incoming.windInfused());
    }

    public static PotionData homogenizeLayers(PotionData data, int remainingLevel) {
        if (data.matchingLayers() == 0 || remainingLevel <= 0) return data;
        List<PotionEffect> effects = getAllEffects(data);
        return new PotionData(data.type(), data.customEffects(), data.mixCount(), data.mixedTypes(),
                data.currentPointer(), data.redstoneCredit(), remainingLevel,
                effects.stream().map(e -> PotionLayerAverages.totalFor(e.getDuration(), remainingLevel)).toList(),
                effects.stream().map(e -> PotionLayerAverages.totalFor(e.getAmplifier(), remainingLevel)).toList(),
                data.powderInfused(), data.windInfused());
    }

    /** One successful ingredient application changes one effect, then advances the cycle. */
    public static PotionData improve(PotionData data, boolean glowstone) {
        List<PotionEffect> effects = getAllEffects(data);
        if (effects.isEmpty()) return null;
        int pointer = Math.floorMod(data.currentPointer(), effects.size());
        PotionEffect current = effects.get(pointer);
        if (glowstone) {
            int limit = Config.CAULDRON_POTION_MAX_LEVEL;
            String name = current.getType().getKey().getKey();
            if (limit > 0 && limit == 3 && Config.CAULDRON_POTION_FOURTH_LEVEL_EFFECTS.contains(name)) limit = 4;
            if (current.getAmplifier() == Integer.MAX_VALUE
                    || (limit > 0 && current.getAmplifier() >= limit - 1)) return null;
        } else if (!CauldronCycle.canExtend(current.getDuration())) {
            return null;
        }
        int duration = CauldronCycle.durationAfter(current.getDuration(), glowstone);
        int amplifier = glowstone ? current.getAmplifier() + 1 : current.getAmplifier();
        effects.set(pointer, new PotionEffect(current.getType(), duration, amplifier,
                current.isAmbient(), current.hasParticles(), current.hasIcon()));
        int layers = data.matchingLayers();
        List<Long> durationTotals = new ArrayList<>(data.durationTotals());
        List<Long> amplifierTotals = new ArrayList<>(data.amplifierTotals());
        if (layers > 0 && durationTotals.size() == effects.size() && amplifierTotals.size() == effects.size()) {
            durationTotals.set(pointer, glowstone
                    ? PotionLayerAverages.totalFor(duration, layers)
                    : PotionLayerAverages.addToEachLayer(durationTotals.get(pointer), layers, 20));
            if (glowstone) {
                amplifierTotals.set(pointer, PotionLayerAverages.addToEachLayer(amplifierTotals.get(pointer), layers, 1));
            }
        } else {
            layers = 0;
            durationTotals = List.of();
            amplifierTotals = List.of();
        }
        return new PotionData(data.type(), effects, data.mixCount(), data.mixedTypes(),
                CauldronCycle.next(pointer, effects.size()), 0, layers, durationTotals, amplifierTotals,
                data.powderInfused(), data.windInfused());
    }

    public static long redstonePrice(int amplifier) {
        return CauldronCycle.redstonePrice(amplifier);
    }

    /** Масштабирует длительность эффектов для стрел (мин. 6 сек, макс. 12 сек). */
    public static List<PotionEffect> limitArrowDurations(List<PotionEffect> effects) {
        List<PotionEffect> out = new ArrayList<>(effects.size());
        for (PotionEffect e : effects) {
            out.add(new PotionEffect(e.getType(), CauldronCycle.arrowDuration(e.getDuration(), e.getType().isInstant()),
                    e.getAmplifier(), e.isAmbient(), e.hasParticles(), e.hasIcon()));
        }
        return out;
    }

    /** Количество уникальных типов эффектов в списке. */
    public static int distinctEffectCount(List<PotionEffect> effects) {
        Set<PotionEffectType> types = new HashSet<>();
        for (PotionEffect e : effects) types.add(e.getType());
        return types.size();
    }

    /** Очищает метки смешивания бутылочки (возвращает её к обычной воде). */
    public static void clearMixFromItem(ItemStack item) {
        if (item.getItemMeta() instanceof PotionMeta meta) {
            meta.getPersistentDataContainer().remove(BOTTLE_MIX_COUNT);
            meta.getPersistentDataContainer().remove(BOTTLE_MIXED_TYPES);
            item.setItemMeta(meta);
        }
    }

    private static int computeAvgAmplifier(List<PotionEffect> effects) {
        if (effects.isEmpty()) return 0;
        long sum = 0;
        for (PotionEffect e : effects) sum += e.getAmplifier();
        return (int) (sum / effects.size());
    }

    private static PotionEffect rollBonusEffect(int negCount, int posCount, int mixCount, int totalEffects,
                                                int avgAmplifier) {
        double chance = mixCount * 0.25;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() > chance) return null;

        if (totalEffects > 5) {
            PotionEffectType[] negatives = {
                    PotionEffectType.NAUSEA, PotionEffectType.SLOWNESS,
                    PotionEffectType.WEAKNESS, PotionEffectType.POISON,
                    PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS
            };
            PotionEffectType type = negatives[rng.nextInt(negatives.length)];
            int amplifier = Math.min(avgAmplifier, 1);
            int duration = (int) Math.min(Integer.MAX_VALUE, 300L + Math.max(0, (long) avgAmplifier) * 100);
            return new PotionEffect(type, duration, amplifier);
        }

        if (negCount > posCount) {
            PotionEffectType[] positives = {
                    PotionEffectType.SPEED, PotionEffectType.STRENGTH,
                    PotionEffectType.REGENERATION, PotionEffectType.FIRE_RESISTANCE
            };
            return new PotionEffect(positives[rng.nextInt(positives.length)], 500, 0); // 25 секунд
        } else if (posCount > negCount) {
            PotionEffectType[] negatives = {
                    PotionEffectType.NAUSEA, PotionEffectType.SLOWNESS,
                    PotionEffectType.WEAKNESS, PotionEffectType.POISON
            };
            return new PotionEffect(negatives[rng.nextInt(negatives.length)], 320, 0); // 16 секунд
        } else {
            PotionEffectType[] all = {
                    PotionEffectType.SPEED, PotionEffectType.NAUSEA,
                    PotionEffectType.REGENERATION, PotionEffectType.WEAKNESS
            };
            return new PotionEffect(all[rng.nextInt(all.length)], 400, 0); // 20 секунд
        }
    }

    public static Color mixPotionColors(List<PotionType> types) {
        if (types.isEmpty()) return potionColor(PotionType.WATER);
        int r = 0, g = 0, b = 0;
        for (PotionType t : types) {
            Color c = potionColor(t);
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        return Color.fromRGB(r / types.size(), g / types.size(), b / types.size());
    }

    public static Color mixColors(Color existing, Color add) {
        int r = (existing.getRed() + add.getRed()) / 2;
        int g = (existing.getGreen() + add.getGreen()) / 2;
        int b = (existing.getBlue() + add.getBlue()) / 2;
        return Color.fromRGB(r, g, b);
    }

    public static int getWaterLevel(Block block) {
        if (block.getBlockData() instanceof Levelled l) return l.getLevel();
        return 0;
    }

    public static void setWaterLevel(Block block, int level) {
        if (level > 0 && level < getWaterLevel(block)) {
            PotionData potion = getPotion(block);
            if (potion != null && potion.matchingLayers() > 0) {
                setPotion(block, homogenizeLayers(potion, level));
            }
        }
        if (level <= 0) {
            block.setType(Material.CAULDRON);
        } else {
            if (block.getType() != Material.WATER_CAULDRON) {
                block.setType(Material.WATER_CAULDRON);
            }
            if (block.getBlockData() instanceof Levelled l) {
                l.setLevel(Math.max(1, Math.min(l.getMaximumLevel(), level)));
                block.setBlockData(l);
            }
        }
    }

    public static void spawnColoredDisplay(Block block, Color color, int level) {
        if (level <= 0 || color == null) {
            removeDisplay(block);
            return;
        }

        String k = key(block.getLocation());
        ItemDisplay existing = cauldronDisplays.get(k);
        if (existing != null && existing.isValid()) {
            existing.remove();
        }

        boolean isPotion = cauldronPotions.containsKey(k);
        NamespacedKey modelKey = isPotion ? POTION_MODEL_KEY : ITEM_MODEL_KEY;

        double waterY = waterLevelY(level);
        Location loc = block.getLocation().add(0.5, waterY + 0.005, 0.5);

        ItemDisplay display = block.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ItemStack fluidItem = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) fluidItem.getItemMeta();
            meta.setColor(color);
            meta.setItemModel(modelKey);
            fluidItem.setItemMeta(meta);

            ent.setItemStack(fluidItem);
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            ent.setPersistent(false);
            ent.setViewRange(1.0f);
            ent.setShadowRadius(0f);
            ent.setShadowStrength(0f);

            Matrix4f transform = new Matrix4f();
            transform.scale(0.75f, 1.0f, 0.75f);
            ent.setTransformationMatrix(transform);
        });

        cauldronDisplays.put(k, display);
    }

    public static void removeDisplay(Block block) {
        String k = key(block.getLocation());
        ItemDisplay display = cauldronDisplays.remove(k);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public static void updateDisplayLevel(Block block, int newLevel) {
        String k = key(block.getLocation());
        ItemDisplay display = cauldronDisplays.get(k);
        if (display == null || !display.isValid()) {
            cauldronDisplays.remove(k);
            if (newLevel > 0) {
                Color dye = cauldronDyeColors.get(k);
                PotionData pot = cauldronPotions.get(k);
                Color color = dye != null ? dye : (pot != null ? getPotionDisplayColor(pot) : null);
                if (color != null) spawnColoredDisplay(block, color, newLevel);
            }
            return;
        }
        if (newLevel <= 0) {
            removeDisplay(block);
            return;
        }
        Location newLoc = block.getLocation().add(0.5, waterLevelY(newLevel), 0.5);
        display.teleport(newLoc);
    }

    public static void removeAllDisplays() {
        for (ItemDisplay display : cauldronDisplays.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        cauldronDisplays.clear();
    }

    public static void unloadChunk(Chunk chunk) {
        removeByChunk(cauldronDyeColors, chunk);
        removeByChunk(cauldronPotions, chunk);
        var iter = cauldronDisplays.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            ItemDisplay display = entry.getValue();
            if (display == null || !display.isValid()) {
                iter.remove();
                continue;
            }
            Location loc = display.getLocation();
            if (loc.getWorld() == chunk.getWorld()
                    && (loc.getBlockX() >> 4) == chunk.getX()
                    && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                display.remove();
                iter.remove();
            }
        }
    }

    private static void removeByChunk(Map<String, ?> map, Chunk chunk) {
        String prefix = chunk.getWorld().getUID() + ":";
        int cx = chunk.getX() << 4;
        int cz = chunk.getZ() << 4;
        map.keySet().removeIf(k -> {
            if (!k.startsWith(prefix)) return false;
            String[] p = k.substring(prefix.length()).split(":");
            if (p.length != 3) return false;
            try {
                int x = Integer.parseInt(p[0]);
                int z = Integer.parseInt(p[2]);
                return x >= cx && x < cx + 16 && z >= cz && z < cz + 16;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    public static void restoreAllDisplays() {
        if (plugin == null) return;
        for (var world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                restoreChunk(chunk);
            }
        }
    }

    public static void restoreChunk(Chunk chunk) {
        if (plugin == null) return;
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        org.bukkit.World w = chunk.getWorld();
        for (NamespacedKey key : pdc.getKeys()) {
            String keyStr = key.getKey();
            if (keyStr.startsWith("dye_")) {
                String coords = keyStr.substring(4);
                String[] parts = coords.split("_");
                if (parts.length != 3) continue;
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Block block = w.getBlockAt(x, y, z);
                    if (block.getType() != Material.WATER_CAULDRON) continue;
                    String mapKey = key(block.getLocation());
                    if (cauldronDyeColors.containsKey(mapKey)) continue;
                    Integer rgb = pdc.get(key, PersistentDataType.INTEGER);
                    if (rgb == null) continue;
                    Color color = Color.fromRGB(rgb);
                    int level = getWaterLevel(block);
                    cauldronDyeColors.put(mapKey, color);
                    spawnColoredDisplay(block, color, level);
                } catch (NumberFormatException ignored) {}
            } else if (keyStr.startsWith("pot_type_")) {
                String coords = keyStr.substring(9);
                String[] parts = coords.split("_");
                if (parts.length != 3) continue;
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Block block = w.getBlockAt(x, y, z);
                    if (block.getType() != Material.WATER_CAULDRON) continue;
                    String mapKey = key(block.getLocation());
                    if (cauldronPotions.containsKey(mapKey)) continue;
                    String typeName = pdc.get(key, PersistentDataType.STRING);
                    if (typeName == null) continue;
                    PotionType type = PotionType.valueOf(typeName);
                    int mixCount = pdc.getOrDefault(new NamespacedKey(plugin, "pot_mix_" + coords), PersistentDataType.INTEGER, 1);
                    String typesStr = pdc.getOrDefault(new NamespacedKey(plugin, "pot_types_" + coords), PersistentDataType.STRING, typeName);
                    List<PotionType> mixedTypes = java.util.Arrays.stream(typesStr.split(","))
                            .map(s -> { try { return PotionType.valueOf(s.trim()); } catch (Exception e) { return null; } })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    String fxStr = pdc.getOrDefault(new NamespacedKey(plugin, "pot_fx_" + coords), PersistentDataType.STRING, "");
                    List<PotionEffect> customFx = new ArrayList<>();
                    if (!fxStr.isEmpty()) {
                        for (String fx : fxStr.split(",")) {
                            String[] p = fx.split(":");
                            if (p.length < 3) continue;
                            String effectName = String.join(":", java.util.Arrays.copyOf(p, p.length - 2));
                            NamespacedKey effectKey = NamespacedKey.fromString(effectName);
                            PotionEffectType et = effectKey == null ? null : PotionEffectType.getByKey(effectKey);
                            if (et == null) et = PotionEffectType.getByName(effectName);
                            if (et == null) continue;
                            customFx.add(new PotionEffect(et, Integer.parseInt(p[p.length - 2]), Integer.parseInt(p[p.length - 1])));
                        }
                    }
                    int pointer = pdc.getOrDefault(new NamespacedKey(plugin, "pot_pointer_" + coords), PersistentDataType.INTEGER, 0);
                    long credit = pdc.getOrDefault(new NamespacedKey(plugin, "pot_redstone_" + coords), PersistentDataType.LONG, 0L);
                    int savedLayers = pdc.getOrDefault(new NamespacedKey(plugin, "pot_layers_" + coords), PersistentDataType.INTEGER, 0);
                    int currentLevel = getWaterLevel(block);
                    int effectCount = customFx.isEmpty() ? realPotionEffects(type).size() : customFx.size();
                    List<Long> durationTotals = parseTotals(pdc.get(new NamespacedKey(plugin,
                            "pot_duration_totals_" + coords), PersistentDataType.STRING));
                    List<Long> amplifierTotals = parseTotals(pdc.get(new NamespacedKey(plugin,
                            "pot_amplifier_totals_" + coords), PersistentDataType.STRING));
                    if (effectCount == 1 && durationTotals.isEmpty() && amplifierTotals.isEmpty()) {
                        Long oldDuration = pdc.get(new NamespacedKey(plugin, "pot_duration_total_" + coords), PersistentDataType.LONG);
                        Long oldAmplifier = pdc.get(new NamespacedKey(plugin, "pot_amplifier_total_" + coords), PersistentDataType.LONG);
                        if (oldDuration != null && oldAmplifier != null) {
                            durationTotals = List.of(oldDuration);
                            amplifierTotals = List.of(oldAmplifier);
                        }
                    }
                    int layers = savedLayers == currentLevel && savedLayers > 0
                            && durationTotals.size() == effectCount && amplifierTotals.size() == effectCount
                            ? savedLayers : 0;
                    if (layers == 0) {
                        durationTotals = List.of();
                        amplifierTotals = List.of();
                    }
                    boolean powderInfused = pdc.getOrDefault(new NamespacedKey(plugin, "pot_powder_" + coords),
                            PersistentDataType.BYTE, (byte) 0) != 0;
                    boolean windInfused = pdc.getOrDefault(new NamespacedKey(plugin, "pot_wind_" + coords),
                            PersistentDataType.BYTE, (byte) 0) != 0;
                    PotionData data = new PotionData(type, customFx, Math.max(1, Math.min(mixCount, MAX_MIX)), mixedTypes,
                            Math.floorMod(pointer, Math.max(1, customFx.isEmpty() ? realPotionEffects(type).size() : customFx.size())),
                            Math.max(0, credit), layers, durationTotals, amplifierTotals, powderInfused, windInfused);
                    cauldronPotions.put(mapKey, data);
                    int level = getWaterLevel(block);
                    Color color = getPotionDisplayColor(data);
                    spawnColoredDisplay(block, color, level);
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось восстановить зелье котла " + coords
                            + " в чанке " + chunk.getX() + "," + chunk.getZ() + ": " + e);
                }
            }
        }
    }

    public static double waterLevelY(int level) {
        return switch (level) {
            case 1 -> 0.5;
            case 2 -> 0.6875;
            case 3 -> 0.875;
            default -> 0.5;
        };
    }

    public static void startColorTask(JavaPlugin pl) {
        new BukkitRunnable() {
            @Override
            public void run() {
                var iter = cauldronDyeColors.entrySet().iterator();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    String[] p = entry.getKey().split(":");
                    org.bukkit.World w = worldOf(p[0]);
                    if (w == null) continue;
                    if (!w.isChunkLoaded(Integer.parseInt(p[1]) >> 4, Integer.parseInt(p[3]) >> 4)) continue;
                    Block block = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    if (block.getType() != Material.WATER_CAULDRON) {
                        iter.remove();
                        removeDisplay(block);
                        clearDyePDC(block);
                        continue;
                    }
                    ensureDisplay(block, entry.getValue());
                }
                var piter = cauldronPotions.entrySet().iterator();
                while (piter.hasNext()) {
                    var entry = piter.next();
                    String[] p = entry.getKey().split(":");
                    org.bukkit.World w = worldOf(p[0]);
                    if (w == null) continue;
                    if (!w.isChunkLoaded(Integer.parseInt(p[1]) >> 4, Integer.parseInt(p[3]) >> 4)) continue;
                    Block block = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    if (block.getType() != Material.WATER_CAULDRON) {
                        piter.remove();
                        removeDisplay(block);
                        clearPotionPDC(block);
                        continue;
                    }
                    spawnColoredParticles(block, getPotionDisplayColor(entry.getValue()));
                    ensureDisplay(block, getPotionDisplayColor(entry.getValue()));
                }
                // Убираем осиротевшие дисплеи (цвет "летает в воздухе", если котёл сломан/вынесен).
                var diter = cauldronDisplays.entrySet().iterator();
                while (diter.hasNext()) {
                    var entry = diter.next();
                    String[] p = entry.getKey().split(":");
                    if (p.length != 4) continue;
                    org.bukkit.World w = worldOf(p[0]);
                    if (w == null) continue;
                    try {
                        if (!w.isChunkLoaded(Integer.parseInt(p[1]) >> 4, Integer.parseInt(p[3]) >> 4)) continue;
                        Block block = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                        if (block.getType() != Material.WATER_CAULDRON) {
                            ItemDisplay disp = entry.getValue();
                            if (disp != null && disp.isValid()) disp.remove();
                            diter.remove();
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }.runTaskTimer(pl, 10L, 10L);
    }

    private static void ensureDisplay(Block block, Color color) {
        String k = key(block.getLocation());
        ItemDisplay display = cauldronDisplays.get(k);
        if (display == null || !display.isValid()) {
            int level = getWaterLevel(block);
            spawnColoredDisplay(block, color, level);
        }
    }

    private static void spawnColoredParticles(Block block, Color c) {
        int level = getWaterLevel(block);
        if (level <= 0) return;
        double y = waterLevelY(level) - 0.02;
        Location loc = block.getLocation().add(0.5, y, 0.5);
        block.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.15, 0.01, 0.15, 0,
                new Particle.DustOptions(c, 1.0f));
    }

    public static Color potionColor(PotionType type) {
        return switch (type) {
            case WATER, MUNDANE, THICK, AWKWARD -> Color.fromRGB(0x38, 0x5D, 0xC6);
            case NIGHT_VISION, LONG_NIGHT_VISION -> Color.fromRGB(0xC2, 0xFF, 0x66);
            case INVISIBILITY, LONG_INVISIBILITY -> Color.fromRGB(0xF6, 0xF6, 0xF6);
            case LEAPING, LONG_LEAPING, STRONG_LEAPING -> Color.fromRGB(0xFD, 0xFF, 0x84);
            case FIRE_RESISTANCE, LONG_FIRE_RESISTANCE -> Color.fromRGB(0xFF, 0x99, 0x00);
            case SWIFTNESS, LONG_SWIFTNESS, STRONG_SWIFTNESS -> Color.fromRGB(0x33, 0xEB, 0xFF);
            case SLOWNESS, LONG_SLOWNESS, STRONG_SLOWNESS -> Color.fromRGB(0x8B, 0xAF, 0xE0);
            case TURTLE_MASTER, LONG_TURTLE_MASTER -> Color.fromRGB(0x8B, 0x80, 0xE3);
            case STRONG_TURTLE_MASTER -> Color.fromRGB(0x8B, 0x83, 0xE3);
            case WATER_BREATHING, LONG_WATER_BREATHING -> Color.fromRGB(0x98, 0xDA, 0xC0);
            case HEALING, STRONG_HEALING -> Color.fromRGB(0xF8, 0x24, 0x23);
            case HARMING, STRONG_HARMING -> Color.fromRGB(0xA9, 0x65, 0x6A);
            case POISON, LONG_POISON, STRONG_POISON -> Color.fromRGB(0x87, 0xA3, 0x63);
            case REGENERATION, LONG_REGENERATION, STRONG_REGENERATION -> Color.fromRGB(0xCD, 0x5C, 0xAB);
            case STRENGTH, LONG_STRENGTH, STRONG_STRENGTH -> Color.fromRGB(0xFF, 0xC7, 0x00);
            case WEAKNESS, LONG_WEAKNESS -> Color.fromRGB(0x48, 0x4D, 0x48);
            case LUCK -> Color.fromRGB(0x59, 0xC1, 0x06);
            case SLOW_FALLING, LONG_SLOW_FALLING -> Color.fromRGB(0xF3, 0xCF, 0xB9);
            case OOZING -> Color.fromRGB(0x99, 0xFF, 0xA3);
            case WIND_CHARGED -> Color.fromRGB(0xBD, 0xC9, 0xFF);
            case INFESTED -> Color.fromRGB(0x8C, 0x9B, 0x8C);
            case WEAVING -> Color.fromRGB(0x78, 0x69, 0x5A);
            default -> Color.fromRGB(0x38, 0x5D, 0xC6);
        };
    }

    public static List<PotionEffect> potionEffects(PotionType type) {
        return switch (type) {
            case FIRE_RESISTANCE -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 400, 0));
            case LONG_FIRE_RESISTANCE -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 960, 0));
            case SWIFTNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 360, 0));
            case LONG_SWIFTNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 960, 0));
            case STRONG_SWIFTNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 360, 2));
            case SLOWNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 360, 0));
            case LONG_SLOWNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 960, 0));
            case STRONG_SLOWNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 360, 3));
            case HEALING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_HEALTH, 1, 0));
            case STRONG_HEALING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_HEALTH, 1, 1));
            case HARMING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE, 1, 0));
            case STRONG_HARMING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE, 1, 1));
            case LEAPING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 360, 0));
            case LONG_LEAPING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 960, 0));
            case STRONG_LEAPING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 360, 2));
            case INVISIBILITY -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 360, 0));
            case LONG_INVISIBILITY -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 960, 0));
            case POISON -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 400, 0));
            case LONG_POISON -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 960, 0));
            case STRONG_POISON -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 200, 2));
            case REGENERATION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 400, 0));
            case LONG_REGENERATION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 960, 0));
            case STRONG_REGENERATION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 160, 2));
            case STRENGTH -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 360, 0));
            case LONG_STRENGTH -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 960, 0));
            case STRONG_STRENGTH -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 360, 2));
            case WEAKNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 360, 0));
            case LONG_WEAKNESS -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 960, 0));
            case TURTLE_MASTER -> List.of(
                    new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 400, 3),
                    new PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 400, 2));
            case LONG_TURTLE_MASTER -> List.of(
                    new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 800, 3),
                    new PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 800, 2));
            case STRONG_TURTLE_MASTER -> List.of(
                    new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 400, 5),
                    new PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 400, 3));
            case WATER_BREATHING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 360, 0));
            case LONG_WATER_BREATHING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 960, 0));
            case SLOW_FALLING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 360, 0));
            case LONG_SLOW_FALLING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 960, 0));
            case NIGHT_VISION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 360, 0));
            case LONG_NIGHT_VISION -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 960, 0));
            case LUCK -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 600, 0));
            case OOZING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.OOZING, 600, 0));
            case WEAVING -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WEAVING, 600, 0));
            case WIND_CHARGED -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.WIND_CHARGED, 600, 0));
            case INFESTED -> List.of(new PotionEffect(org.bukkit.potion.PotionEffectType.INFESTED, 600, 0));
            default -> List.of();
        };
    }

    /** Реальные эффекты ванильного base-зелья (с настоящей длительностью из PotionType). */
    public static List<PotionEffect> realPotionEffects(PotionType type) {
        try {
            java.util.Collection<org.bukkit.potion.PotionEffect> fx = type.getPotionEffects();
            if (fx == null || fx.isEmpty()) return List.of();
            return new ArrayList<>(fx);
        } catch (Exception e) {
            return new ArrayList<>(potionEffects(type));
        }
    }

    public static boolean isPotion(ItemStack item) {
        Material m = item.getType();
        return m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION;
    }

    public static boolean isWaterPotion(ItemStack item) {
        if (item.getItemMeta() instanceof PotionMeta meta) {
            return PotionLayerAverages.isPlainWater(meta.getBasePotionType() == PotionType.WATER,
                    meta.getCustomEffects().size());
        }
        return false;
    }

    public static PotionData potionFromItem(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return null;
        PotionType type = meta.getBasePotionType();
        if (type == null) {
            List<PotionEffect> effects = new ArrayList<>(meta.getCustomEffects());
            if (effects.isEmpty()) return null;
            type = PotionType.AWKWARD;
        }
        List<PotionEffect> effects = new ArrayList<>();
        if (type != PotionType.AWKWARD && type != PotionType.WATER) {
            effects.addAll(realPotionEffects(type));
        }
        for (PotionEffect customEffect : meta.getCustomEffects()) {
            addOrReplaceEffect(effects, customEffect);
        }
        if (effects.isEmpty()) return null;
        int mixCount = meta.getPersistentDataContainer().getOrDefault(BOTTLE_MIX_COUNT, PersistentDataType.INTEGER, 1);
        String typesStr = meta.getPersistentDataContainer().getOrDefault(BOTTLE_MIXED_TYPES, PersistentDataType.STRING, "");
        List<PotionType> mixedTypes;
        if (typesStr.isEmpty()) {
            mixedTypes = List.of(type);
        } else {
            mixedTypes = java.util.Arrays.stream(typesStr.split(","))
                    .map(s -> { try { return PotionType.valueOf(s.trim()); } catch (Exception e) { return null; } })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (mixedTypes.isEmpty()) mixedTypes = List.of(type);
        }
        return new PotionData(type, effects, mixCount, mixedTypes,
                PotionInfusions.hasGunpowder(item), PotionInfusions.hasWind(item));
    }

    private static void addOrReplaceEffect(List<PotionEffect> effects, PotionEffect incoming) {
        for (int i = 0; i < effects.size(); i++) {
            PotionEffect current = effects.get(i);
            if (!current.getType().equals(incoming.getType())) continue;
            if (better(incoming, current)) effects.set(i, incoming);
            return;
        }
        effects.add(incoming);
    }

    /**
     * A one-step potion with only its base potion effects can be bottled as its
     * normal base type. Legacy cauldrons may have an empty custom-effect list;
     * that also represents the base potion and should keep round-tripping.
     */
    public static boolean isPlainSinglePotion(PotionData data) {
        if (data == null || data.powderInfused() || data.windInfused()
                || data.type() == PotionType.AWKWARD || data.mixCount() != 1
                || data.mixedTypes().size() != 1 || data.mixedTypes().get(0) != data.type()) {
            return false;
        }
        if (data.customEffects().isEmpty()) return true;
        return haveSameEffects(data.customEffects(), realPotionEffects(data.type()));
    }

    private static boolean haveSameEffects(List<PotionEffect> first, List<PotionEffect> second) {
        if (first.size() != second.size()) return false;
        boolean[] matched = new boolean[second.size()];
        for (PotionEffect a : first) {
            boolean found = false;
            for (int i = 0; i < second.size(); i++) {
                if (matched[i]) continue;
                PotionEffect b = second.get(i);
                if (a.getType().equals(b.getType())
                        && a.getDuration() == b.getDuration()
                        && a.getAmplifier() == b.getAmplifier()
                        && a.isAmbient() == b.isAmbient()
                        && a.hasParticles() == b.hasParticles()
                        && a.hasIcon() == b.hasIcon()) {
                    matched[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static void saveMixToItem(ItemStack item, PotionData data) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;
        meta.getPersistentDataContainer().set(BOTTLE_MIX_COUNT, PersistentDataType.INTEGER, data.mixCount());
        String typesStr = data.mixedTypes().stream()
                .map(Enum::name)
                .reduce((a, b) -> a + "," + b)
                .orElse(data.type().name());
        meta.getPersistentDataContainer().set(BOTTLE_MIXED_TYPES, PersistentDataType.STRING, typesStr);
        PotionInfusions.write(meta, data.powderInfused(), data.windInfused());
        item.setItemMeta(meta);
    }

    public static void cleanArmor(ItemStack item) {
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(DEFAULT_LEATHER_R, DEFAULT_LEATHER_G, DEFAULT_LEATHER_B));
            item.setItemMeta(meta);
        }
    }

    public static boolean isLeatherArmor(ItemStack item) {
        Material m = item.getType();
        return m == Material.LEATHER_BOOTS || m == Material.LEATHER_LEGGINGS
                || m == Material.LEATHER_CHESTPLATE || m == Material.LEATHER_HELMET;
    }

    public static boolean isDefaultLeatherColor(LeatherArmorMeta meta) {
        if (!meta.isDyed()) return true;
        Color c = meta.getColor();
        return c.getRed() == DEFAULT_LEATHER_R && c.getGreen() == DEFAULT_LEATHER_G && c.getBlue() == DEFAULT_LEATHER_B;
    }

    public static void applyDyeToArmor(ItemStack item, Color color) {
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
    }

    public static void applyPotionToArmor(ItemStack item, PotionData data) {
        if (data == null) return;
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(getPotionDisplayColor(data));
            item.setItemMeta(meta);
        }
    }

    public static boolean isDye(ItemStack item) {
        return item.getType().name().endsWith("_DYE");
    }

    public static DyeColor dyeFromMaterial(Material material) {
        return switch (material) {
            case WHITE_DYE -> DyeColor.WHITE;
            case ORANGE_DYE -> DyeColor.ORANGE;
            case MAGENTA_DYE -> DyeColor.MAGENTA;
            case LIGHT_BLUE_DYE -> DyeColor.LIGHT_BLUE;
            case YELLOW_DYE -> DyeColor.YELLOW;
            case LIME_DYE -> DyeColor.LIME;
            case PINK_DYE -> DyeColor.PINK;
            case GRAY_DYE -> DyeColor.GRAY;
            case LIGHT_GRAY_DYE -> DyeColor.LIGHT_GRAY;
            case CYAN_DYE -> DyeColor.CYAN;
            case PURPLE_DYE -> DyeColor.PURPLE;
            case BLUE_DYE -> DyeColor.BLUE;
            case BROWN_DYE -> DyeColor.BROWN;
            case GREEN_DYE -> DyeColor.GREEN;
            case RED_DYE -> DyeColor.RED;
            case BLACK_DYE -> DyeColor.BLACK;
            default -> null;
        };
    }

    public static boolean isDyedBanner(ItemStack item) {
        if (!item.getType().name().endsWith("_BANNER")) return false;
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return false;
        return !meta.getPatterns().isEmpty();
    }

    /**
     * Убирает верхний слой узора флага (Bedrock-стиль: по одному слою за клик).
     * Возвращает true, если слой был снят.
     */
    public static boolean cleanBanner(ItemStack item) {
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return false;
        List<Pattern> patterns = new ArrayList<>(meta.getPatterns());
        if (patterns.isEmpty()) return false;
        patterns.removeLast();
        meta.setPatterns(patterns);
        item.setItemMeta(meta);
        return true;
    }

    /** Сколько слоёв узора осталось на флаге. */
    public static int bannerLayers(ItemStack item) {
        if (!(item.getItemMeta() instanceof BannerMeta meta)) return 0;
        return meta.getPatterns().size();
    }

    public static boolean isDyedShulker(ItemStack item) {
        return item.getType().name().endsWith("_SHULKER_BOX") && item.getType() != Material.SHULKER_BOX;
    }

    public static void cleanShulker(ItemStack item) {
        item.setType(Material.SHULKER_BOX);
    }

    private static final String[] WASH_SUFFIXES = {
            "_CONCRETE_POWDER", "_CONCRETE_SLAB", "_CONCRETE_STAIRS",
            "_WOOL_SLAB", "_WOOL_STAIRS",
            "_GLAZED_TERRACOTTA", "_STAINED_GLASS_PANE", "_STAINED_GLASS",
            "_TERRACOTTA", "_CONCRETE", "_WOOL", "_CANDLE", "_BED", "_CARPET",
            "_CUSHION", "_BUNDLE", "_HARNESS"
    };

    /** Возвращает "чистый" (обычный/белый) материал, если блок/предмет можно отмыть от краски, иначе null. */
    public static Material washPlainOf(Material material) {
        String n = material.name();
        Material plain = null;
        if (n.endsWith("_WOOL_SLAB")) plain = Material.WHITE_WOOL_SLAB;
        else if (n.endsWith("_WOOL_STAIRS")) plain = Material.WHITE_WOOL_STAIRS;
        else if (n.endsWith("_WOOL")) plain = Material.WHITE_WOOL;
        else if (n.endsWith("_GLAZED_TERRACOTTA")) plain = Material.TERRACOTTA;
        else if (n.endsWith("_TERRACOTTA")) plain = Material.TERRACOTTA;
        else if (n.endsWith("_CONCRETE_POWDER")) plain = Material.matchMaterial(n.substring(0, n.length() - "_POWDER".length()));
        else if (n.endsWith("_CONCRETE_SLAB")) plain = Material.WHITE_CONCRETE_SLAB;
        else if (n.endsWith("_CONCRETE_STAIRS")) plain = Material.WHITE_CONCRETE_STAIRS;
        else if (n.endsWith("_CONCRETE")) plain = Material.WHITE_CONCRETE;
        else if (n.endsWith("_STAINED_GLASS_PANE")) plain = Material.GLASS_PANE;
        else if (n.endsWith("_STAINED_GLASS")) plain = Material.GLASS;
        else if (n.endsWith("_CANDLE")) plain = Material.CANDLE;
        else if (n.endsWith("_BED")) plain = Material.WHITE_BED;
        else if (n.endsWith("_CARPET")) plain = Material.WHITE_CARPET;
        else if (n.endsWith("_CUSHION")) plain = Material.WHITE_CUSHION;
        else if (n.endsWith("_BUNDLE")) plain = Material.BUNDLE;
        else if (n.endsWith("_HARNESS")) plain = Material.WHITE_HARNESS;
        if (plain == null || plain == material) return null;
        return washDyeOf(material) == null ? null : plain;
    }

    /** Цвет краски отмываемого блока, или null, если это не цветной блок. */
    public static DyeColor washDyeOf(Material material) {
        String n = material.name();
        for (String suf : WASH_SUFFIXES) {
            if (n.endsWith(suf)) {
                String colorPart = n.substring(0, n.length() - suf.length());
                try {
                    return DyeColor.valueOf(colorPart);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isWashableBlock(ItemStack item) {
        return item != null && washPlainOf(item.getType()) != null;
    }

    private static void saveDyePDC(Block block, int rgb) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String coords = block.getX() + "_" + block.getY() + "_" + block.getZ();
        NamespacedKey key = new NamespacedKey(plugin, "dye_" + coords);
        pdc.set(key, PersistentDataType.INTEGER, rgb);
    }

    private static void clearDyePDC(Block block) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String coords = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.remove(new NamespacedKey(plugin, "dye_" + coords));
    }

    private static List<Long> parseTotals(String encoded) {
        if (encoded == null || encoded.isEmpty()) return List.of();
        try {
            return java.util.Arrays.stream(encoded.split(",")).map(Long::parseLong).toList();
        } catch (NumberFormatException ignored) {
            return List.of();
        }
    }

    private static void savePotionPDC(Block block, PotionData data) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String c = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.set(new NamespacedKey(plugin, "pot_type_" + c), PersistentDataType.STRING, data.type().name());
        pdc.set(new NamespacedKey(plugin, "pot_mix_" + c), PersistentDataType.INTEGER, data.mixCount());
        pdc.set(new NamespacedKey(plugin, "pot_pointer_" + c), PersistentDataType.INTEGER, data.currentPointer());
        pdc.set(new NamespacedKey(plugin, "pot_redstone_" + c), PersistentDataType.LONG, data.redstoneCredit());
        pdc.set(new NamespacedKey(plugin, "pot_powder_" + c), PersistentDataType.BYTE,
                data.powderInfused() ? (byte) 1 : (byte) 0);
        pdc.set(new NamespacedKey(plugin, "pot_wind_" + c), PersistentDataType.BYTE,
                data.windInfused() ? (byte) 1 : (byte) 0);
        pdc.set(new NamespacedKey(plugin, "pot_layers_" + c), PersistentDataType.INTEGER, data.matchingLayers());
        pdc.set(new NamespacedKey(plugin, "pot_duration_totals_" + c), PersistentDataType.STRING,
                data.durationTotals().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        pdc.set(new NamespacedKey(plugin, "pot_amplifier_totals_" + c), PersistentDataType.STRING,
                data.amplifierTotals().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        pdc.remove(new NamespacedKey(plugin, "pot_duration_total_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_amplifier_total_" + c));
        pdc.set(new NamespacedKey(plugin, "pot_types_" + c), PersistentDataType.STRING,
                String.join(",", data.mixedTypes().stream().map(Enum::name).toList()));
        StringBuilder fx = new StringBuilder();
        for (PotionEffect effect : data.customEffects()) {
            if (!fx.isEmpty()) fx.append(",");
            fx.append(effect.getType().getKey()).append(":").append(effect.getDuration()).append(":").append(effect.getAmplifier());
        }
        pdc.set(new NamespacedKey(plugin, "pot_fx_" + c), PersistentDataType.STRING, fx.toString());
    }

    private static void clearPotionPDC(Block block) {
        if (plugin == null) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String c = block.getX() + "_" + block.getY() + "_" + block.getZ();
        pdc.remove(new NamespacedKey(plugin, "pot_type_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_mix_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_pointer_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_redstone_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_powder_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_wind_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_layers_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_duration_totals_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_amplifier_totals_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_duration_total_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_amplifier_total_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_types_" + c));
        pdc.remove(new NamespacedKey(plugin, "pot_fx_" + c));
    }
}
