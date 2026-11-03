package org.example1.toolkit.cauldron;

import java.util.List;

public final class PotionLayerAveragesCheck {
    public static void main(String[] args) {
        long duration = PotionLayerAverages.totalFor(2400, 2) + 1200;
        long amplifier = PotionLayerAverages.totalFor(1, 2);
        if (PotionLayerAverages.average(duration, 3) != 2000
                || PotionLayerAverages.average(amplifier, 3) != 1) {
            throw new AssertionError("two Regen II layers plus one Regen I layer");
        }
        long exactLevels = PotionLayerAverages.totalFor(0, 1) + 1 + 0;
        if (PotionLayerAverages.average(exactLevels, 3) != 0) {
            throw new AssertionError("round only after all three layers");
        }
        if (PotionLayerAverages.total(7, 2, 3, 5) != 15
                || PotionLayerAverages.totalFor(-1, 3) != -1) {
            throw new AssertionError("legacy state and infinite duration");
        }
        long twoLayerAmplifierTotal = 0 + 1;
        long twoLayerDurationTotal = PotionLayerAverages.totalFor(1200, 2);
        twoLayerDurationTotal = PotionLayerAverages.addToEachLayer(twoLayerDurationTotal, 2, 20);
        if (PotionLayerAverages.average(twoLayerDurationTotal + 1200, 3) != 1213
                || PotionLayerAverages.average(twoLayerAmplifierTotal, 3) != 0
                || PotionLayerAverages.addToEachLayer(twoLayerAmplifierTotal, 2, 1) != 3) {
            throw new AssertionError("redstone between second and third layers must not round levels early");
        }
        if (!PotionLayerAverages.sameDistinctTypes(List.of("speed", "regen", "strength"),
                    List.of("strength", "speed", "regen"))
                || PotionLayerAverages.sameDistinctTypes(List.of("speed", "regen", "strength"),
                    List.of("speed", "regen", "poison"))) {
            throw new AssertionError("three-effect mixtures must match independently of order or base potion");
        }
        if (!PotionLayerAverages.overlaps(List.of("poison", "blindness", "darkness"), List.of("darkness"))
                || !PotionLayerAverages.overlaps(List.of("poison", "blindness", "darkness"), List.of("poison"))
                || PotionLayerAverages.overlaps(List.of("poison", "blindness", "darkness"), List.of("speed"))) {
            throw new AssertionError("one matching effect must allow another liquid layer");
        }
        long poisonTotal = PotionLayerAverages.totalFor(1200, 2);
        long darknessTotal = PotionLayerAverages.totalFor(1200, 2);
        if (PotionLayerAverages.average(poisonTotal + 1200, 3) != 1200
                || PotionLayerAverages.average(darknessTotal + 2400, 3) != 1600) {
            throw new AssertionError("partial overlap keeps poison and averages darkness");
        }
        if (!PotionLayerAverages.isPlainWater(true, 0)
                || PotionLayerAverages.isPlainWater(true, 3)) {
            throw new AssertionError("water-based custom potions must retain their effects");
        }
        long[] oldDurations = {2400, 1200, 600};
        long[] addedDurations = {1200, 1800, 900};
        int[] expectedDurations = {2000, 1400, 700};
        for (int i = 0; i < oldDurations.length; i++) {
            if (PotionLayerAverages.average(oldDurations[i] * 2 + addedDurations[i], 3)
                    != expectedDurations[i]) {
                throw new AssertionError("per-effect weighted average at index " + i);
            }
        }
    }
}
