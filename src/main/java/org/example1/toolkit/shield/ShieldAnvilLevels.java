package org.example1.toolkit.shield;

final class ShieldAnvilLevels {
    private ShieldAnvilLevels() {}

    static int combinePush(int current, int incoming) {
        if (current <= 0) return incoming;
        if (incoming <= 0) return current;
        if (current == Integer.MAX_VALUE) return current;
        if (current == incoming || (current >= 2 && incoming >= 2 && incoming < current)) {
            return current + 1;
        }
        return Math.max(current, incoming);
    }
}
