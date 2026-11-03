package org.example1.toolkit.cauldron;

import java.util.HashSet;
import java.util.List;

final class PotionLayerAverages {
    private PotionLayerAverages() {}

    static long total(long stored, int storedLayers, int actualLayers, int shownValue) {
        return storedLayers == actualLayers ? stored : totalFor(shownValue, actualLayers);
    }

    static long totalFor(int value, int layers) {
        return value < 0 ? -1 : (long) value * layers;
    }

    static long addToEachLayer(long total, int layers, int added) {
        return total < 0 ? -1 : total + (long) added * layers;
    }

    static int average(long total, int layers) {
        return (int) Math.round((double) total / layers);
    }

    static <T> boolean sameDistinctTypes(List<T> first, List<T> second) {
        return !first.isEmpty() && first.size() == second.size()
                && new HashSet<>(first).size() == first.size()
                && new HashSet<>(first).equals(new HashSet<>(second));
    }

    static <T> boolean overlaps(List<T> first, List<T> second) {
        return first.stream().anyMatch(second::contains);
    }

    static boolean isPlainWater(boolean waterBase, int customEffectCount) {
        return waterBase && customEffectCount == 0;
    }
}
