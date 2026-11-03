package org.example1.toolkit.cauldron;

public final class CauldronCycleCheck {
    public static void main(String[] args) {
        int pointer = 0;
        for (int expected : new int[] {1, 2, 0, 1}) {
            pointer = CauldronCycle.next(pointer, 3);
            if (pointer != expected) throw new AssertionError("cycle position " + pointer);
        }
        for (int level = 0; level < 4; level++) {
            if (CauldronCycle.redstonePrice(level) != 8L << level) {
                throw new AssertionError("redstone price at amplifier " + level);
            }
        }
        if (CauldronCycle.redstonePrice(60) != Long.MAX_VALUE) {
            throw new AssertionError("high amplifier overflow");
        }
        if (CauldronCycle.durationAfter(100, false) != 120
                || CauldronCycle.durationAfter(100, true) != 50
                || CauldronCycle.durationAfter(-1, true) != -1) {
            throw new AssertionError("duration upgrade");
        }
        if (!CauldronCycle.canExtend(11980) || CauldronCycle.canExtend(11981)
                || CauldronCycle.canExtend(12000) || CauldronCycle.canExtend(-1)) {
            throw new AssertionError("redstone duration cap");
        }
        if (CauldronCycle.arrowDuration(1, false) != 120
                || CauldronCycle.arrowDuration(3600, false) != 240
                || CauldronCycle.arrowDuration(-1, false) != 240
                || CauldronCycle.arrowDuration(3600, true) != 1) {
            throw new AssertionError("arrow duration");
        }
        double lowRisk = CauldronCycle.redstoneFailureChance(6000, 0.1);
        double highRisk = CauldronCycle.redstoneFailureChance(12000, 0.1);
        if (lowRisk <= 0 || lowRisk >= highRisk || Math.abs(highRisk - 0.001) > 1e-12) {
            throw new AssertionError("growing redstone risk");
        }
    }
}
