package org.example1.toolkit.cauldron;

final class CauldronCycle {
    static final int MAX_REDSTONE_DURATION = 20 * 60 * 10;
    static final int MAX_ARROW_DURATION = 240;

    private CauldronCycle() {}

    static int next(int pointer, int size) {
        return (Math.floorMod(pointer, size) + 1) % size;
    }

    static long redstonePrice(int amplifier) {
        if (amplifier < 0) return 8;
        if (amplifier >= 60) return Long.MAX_VALUE;
        return 8L << amplifier;
    }

    static int durationAfter(int ticks, boolean glowstone) {
        if (!glowstone) return ticks + 20;
        return ticks < 0 ? ticks : Math.max(1, ticks / 2);
    }

    static boolean canExtend(int ticks) {
        return ticks >= 0 && ticks <= MAX_REDSTONE_DURATION - 20;
    }

    static int arrowDuration(int ticks, boolean instant) {
        if (instant) return 1;
        if (ticks < 0) return MAX_ARROW_DURATION;
        return (int) Math.min(MAX_ARROW_DURATION, Math.max(120L, Math.round(ticks * 0.35)));
    }

    static double redstoneFailureChance(int newDuration, double maxRiskPercent) {
        return Math.min(newDuration, MAX_REDSTONE_DURATION) / (double) MAX_REDSTONE_DURATION
                * maxRiskPercent / 100;
    }
}
