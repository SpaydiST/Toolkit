package org.example1.toolkit.shield;

public final class ShieldAnvilLevelsCheck {
    public static void main(String[] args) {
        int level = ShieldAnvilLevels.combinePush(0, 2);
        if (level != 2) throw new AssertionError("first book");
        level = ShieldAnvilLevels.combinePush(level, 2);
        if (level != 3) throw new AssertionError("second book");
        level = ShieldAnvilLevels.combinePush(level, 2);
        if (level != 4) throw new AssertionError("third book");
        if (ShieldAnvilLevels.combinePush(1, 1) != 2
                || ShieldAnvilLevels.combinePush(4, 1) != 4) {
            throw new AssertionError("ordinary merging");
        }
    }
}
