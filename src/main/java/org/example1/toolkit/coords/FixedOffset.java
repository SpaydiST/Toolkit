package org.example1.toolkit.coords;

public record FixedOffset(int x, int z) {

    private static final FixedOffset NONE = new FixedOffset(0, 0);

    public FixedOffset {
        x = align(x);
        z = align(z);
    }

    public static FixedOffset none() {
        return NONE;
    }

    public static FixedOffset alignChunk(int chunkX, int chunkZ) {
        return new FixedOffset(chunkX << 4, chunkZ << 4);
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    public int apply(int value) {
        return value - x;
    }

    public double apply(double value) {
        return value - x;
    }

    public int applyChunkX(int chunkX) {
        return chunkX - (x >> 4);
    }

    public int applyChunkZ(int chunkZ) {
        return chunkZ - (z >> 4);
    }

    public int applyTimes8(int value) {
        return value - x * 8;
    }

    public FixedOffset scaled(int divisor) {
        return new FixedOffset(x / divisor, z / divisor);
    }

    public FixedOffset negate() {
        return new FixedOffset(-x, -z);
    }

    public boolean isZero() {
        return x == 0 && z == 0;
    }

    private static int align(int value) {
        return value & ~0xF;
    }
}
