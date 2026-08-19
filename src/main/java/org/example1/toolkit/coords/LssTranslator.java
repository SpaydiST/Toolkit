package org.example1.toolkit.coords;

import java.util.Arrays;

/**
 * Перевод координат в payload пакетов VSS (VoX/LOD Server-Side).
 *
 * Пространства координат:
 *  - реальные (R): то, что реально существует в мире сервера;
 *  - базовые (B): то, что видит обфусцированный клиент, B = R - offset.
 *
 * Клиент Voxy запрашивает колонки вокруг своей базовой позиции и ожидает их
 * ровно на тех же базовых позициях. Чтобы VSS обслужил правильные РЕАЛЬНЫЕ
 * колонки, а клиент отрисовал их в базовом пространстве рядом с собой:
 *  - C2S lss:batch_chunk_req: позиции сдвигаются на +offset (чанки);
 *  - S2C lss:voxel_column, lss:batch_response, lss:dirty_columns:
 *    позиции сдвигаются на -offset (чанки).
 *
 * Wire-форматы (протокол LSS):
 *  - batch_chunk_req:  VarInt count, затем count x (long packedPos, long timestamp);
 *  - batch_response:   VarInt count, затем count x (byte responseType, long packedPos);
 *  - dirty_columns:    VarInt len, затем len x long packedPos;
 *  - voxel_column:     int chunkX, int chunkZ, ... (первые 8 байт — только они меняются).
 *
 * packedPos = (chunkX << 32) | (chunkZ & 0xFFFFFFFFL).
 */
public final class LssTranslator {

    private static final int MAX_BATCH_CHUNK_REQUESTS = 1024;
    private static final int MAX_BATCH_RESPONSES = 4096;
    private static final int MAX_DIRTY_COLUMN_POSITIONS = 10240;

    private LssTranslator() {
    }

    /**
     * C2S lss:batch_chunk_req: позиции запрошенных колонок в базовых чанковых
     * координатах сдвигаются на +offChunkX/+offChunkZ, чтобы VSS обслужил
     * правильные реальные колонки. Возвращает исходный массив, если сдвиг нулевой
     * или payload повреждён.
     */
    public static byte[] translateRequest(byte[] data, int offChunkX, int offChunkZ) {
        if (offChunkX == 0 && offChunkZ == 0) {
            return data;
        }
        Reader reader = new Reader(data);
        int count = reader.readVarInt();
        if (count < 0 || count > MAX_BATCH_CHUNK_REQUESTS || !reader.has((long) count * 16)) {
            return data;
        }
        Writer writer = new Writer(data.length);
        writer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            long packed = reader.readLong();
            long timestamp = reader.readLong();
            writer.writeLong(shift(packed, offChunkX, offChunkZ));
            writer.writeLong(timestamp);
        }
        return writer.toByteArray();
    }

    /**
     * S2C lss:batch_response: позиции колонок сдвигаются в базовое пространство
     * (-offChunkX/-offChunkZ).
     */
    public static byte[] translateBatchResponse(byte[] data, int offChunkX, int offChunkZ) {
        if (offChunkX == 0 && offChunkZ == 0) {
            return data;
        }
        Reader reader = new Reader(data);
        int count = reader.readVarInt();
        if (count < 0 || count > MAX_BATCH_RESPONSES || !reader.has((long) count * 9)) {
            return data;
        }
        Writer writer = new Writer(data.length);
        writer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            int type = reader.readByte();
            long packed = reader.readLong();
            writer.writeByte(type);
            writer.writeLong(shift(packed, -offChunkX, -offChunkZ));
        }
        return writer.toByteArray();
    }

    /**
     * S2C lss:dirty_columns: позиции изменённых колонок сдвигаются в базовое
     * пространство (-offChunkX/-offChunkZ).
     */
    public static byte[] translateDirtyColumns(byte[] data, int offChunkX, int offChunkZ) {
        if (offChunkX == 0 && offChunkZ == 0) {
            return data;
        }
        Reader reader = new Reader(data);
        int len = reader.readVarInt();
        if (len < 0 || len > MAX_DIRTY_COLUMN_POSITIONS || !reader.has((long) len * 8)) {
            return data;
        }
        Writer writer = new Writer(data.length);
        writer.writeVarInt(len);
        for (int i = 0; i < len; i++) {
            writer.writeLong(shift(reader.readLong(), -offChunkX, -offChunkZ));
        }
        return writer.toByteArray();
    }

    /**
     * S2C lss:voxel_column: chunkX/chunkZ (первые два int) сдвигаются в базовое
     * пространство (-offChunkX/-offChunkZ); остальное содержимое пакета не меняется.
     */
    public static byte[] translateColumn(byte[] data, int offChunkX, int offChunkZ) {
        if (offChunkX == 0 && offChunkZ == 0) {
            return data;
        }
        if (data == null || data.length < 8) {
            return data;
        }
        int x = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16)
                | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int z = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16)
                | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
        byte[] out = data.clone();
        writeInt(out, 0, x - offChunkX);
        writeInt(out, 4, z - offChunkZ);
        return out;
    }

    private static long shift(long packed, int offChunkX, int offChunkZ) {
        int x = (int) (packed >> 32);
        int z = (int) packed;
        return ((long) (x + offChunkX) << 32) | ((z + offChunkZ) & 0xFFFFFFFFL);
    }

    private static void writeInt(byte[] out, int offset, int value) {
        out[offset] = (byte) (value >> 24);
        out[offset + 1] = (byte) (value >> 16);
        out[offset + 2] = (byte) (value >> 8);
        out[offset + 3] = (byte) value;
    }

    private static final class Reader {
        private final byte[] data;
        private int pos;

        Reader(byte[] data) {
            this.data = data == null ? new byte[0] : data;
        }

        boolean has(long n) {
            return n >= 0 && pos + n <= data.length;
        }

        int readVarInt() {
            int value = 0;
            int shift = 0;
            while (pos < data.length && shift <= 28) {
                byte b = data[pos++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return value;
                }
                shift += 7;
            }
            return -1;
        }

        int readByte() {
            return data[pos++] & 0xFF;
        }

        long readLong() {
            long value = 0;
            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (data[pos++] & 0xFF);
            }
            return value;
        }
    }

    private static final class Writer {
        private byte[] buf;
        private int pos;

        Writer(int capacity) {
            this.buf = new byte[Math.max(16, capacity)];
        }

        void writeVarInt(int value) {
            while ((value & ~0x7F) != 0) {
                writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            writeByte(value);
        }

        void writeByte(int value) {
            ensure(1);
            buf[pos++] = (byte) value;
        }

        void writeLong(long value) {
            ensure(8);
            for (int i = 7; i >= 0; i--) {
                buf[pos++] = (byte) (value >> (i * 8));
            }
        }

        private void ensure(int n) {
            if (pos + n <= buf.length) {
                return;
            }
            int newLength = Math.max(buf.length * 2, pos + n);
            buf = Arrays.copyOf(buf, newLength);
        }

        byte[] toByteArray() {
            return Arrays.copyOf(buf, pos);
        }
    }
}
