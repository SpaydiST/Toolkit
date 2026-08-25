package org.example1.toolkit.coords;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * Перевод координат внутри payload'ов мода Axiom (каналы "axiom:*").
 *
 * Клиентский Axiom считает все координаты в сдвинутом мире наблюдателя, а
 * AxiomPaper применяет их к реальному миру напрямую, минуя ванильные пакеты.
 * Без перевода постройки, сущности и буферы уезжают ровно на оффсет игрока.
 *
 * Форматы каналов сняты с исходников AxiomPaperPlugin (Moulberry, MIT).
 * Оффсеты генерируются кратными 128 блокам, поэтому секции не перекрываются:
 * paletted-контейнеры и сжатые блочные сущности копируются байт-в-байт,
 * сдвигаются только ключи-позиции.
 *
 * Любая ошибка разбора => возвращаются исходные байты (кадр пройдёт без
 * перевода), повреждение потока исключено.
 */
public final class AxiomTranslator {

    /** BlockPos.asLong(-33554432, -2048, -33554432) — терминатор списков в буферах/ответах. */
    public static final long MIN_POSITION_LONG =
            0b1000000000000000000000000010000000000000000000000000100000000000L;

    public static final String RESPONSE_CHANNEL = "axiom:response_chunk_data";

    private static final Set<String> COORD_CHANNELS = Set.of(
            "set_block", "spawn_entity", "manipulate_entity", "teleport",
            "request_chunk_data", "set_buffer");

    private static final int NBT_MAX_DEPTH = 48;
    private static final int NBT_MAX_ARRAY = 1 << 24;

    private AxiomTranslator() {
    }

    /**
     * Точка входа для C2S-кадров из netty-пайплайна: по шапке кадра определяет,
     * является ли он axiom-payload'ом с координатами, и переводит его из
     * сдвинутого пространства клиента в реальное (+dx, +dz).
     * Возвращает исходный массив, если кадр не относится к делу или не разобран.
     */
    public static byte[] translateClientFrame(byte[] src, int offX, int offZ) {
        int packetId;
        String channel;
        try {
            Reader r = new Reader(src);
            packetId = r.readVarInt();
            if (packetId < 0 || packetId > 0xFFFFFF) {
                return src;
            }
            channel = r.readMcString();
        } catch (Throwable t) {
            return src;
        }
        if (!channel.startsWith("axiom:")) {
            return src;
        }
        String suffix = channel.substring("axiom:".length());
        if (!COORD_CHANNELS.contains(suffix)) {
            return src;
        }
        Reader r = new Reader(src);
        try {
            r.readVarInt();
            r.readMcString();
            Writer w = new Writer();
            w.writeVarInt(packetId);
            w.writeMcString(channel);
            boolean ok = switch (suffix) {
                case "set_block" -> translateSetBlock(r, w, offX, offZ);
                case "teleport" -> translateTeleport(r, w, offX, offZ);
                case "spawn_entity" -> translateSpawnEntity(r, w, offX, offZ);
                case "manipulate_entity" -> translateManipulateEntity(r, w, offX, offZ);
                case "request_chunk_data" -> translateRequestChunkData(r, w, offX, offZ);
                case "set_buffer" -> translateSetBuffer(r, w, offX, offZ);
                default -> false;
            };
            if (!ok) {
                coordDbg(suffix, "превышен лимит коллекции", src);
                return src;
            }
            if (r.remaining() != 0) {
                coordDbg(suffix, "неразобранный хвост " + r.remaining() + "Б", src);
                return src;
            }
            return w.toArray();
        } catch (Throwable t) {
            coordDbg(suffix, "сбой на байте " + r.pos() + " (секций разобрано " + lastSectionCount
                    + ", последний ключ " + lastKeyBeforeFail + "): " + t, src);
            return src;
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger DBG = new java.util.concurrent.atomic.AtomicInteger();

    private static void coordDbg(String suffix, String problem, byte[] frame) {
        if (DBG.incrementAndGet() <= 20) {
            System.out.println("[Toolkit][axiom-diag] C2S " + suffix + ": " + problem
                    + (frame == null ? "" : ", hex=" + hexHead(frame))
                    + (frame == null ? "" : ", tail=" + hexTail(frame)));
        }
    }

    private static String hexTail(byte[] data) {
        int n = Math.min(data.length, 64);
        StringBuilder sb = new StringBuilder(n * 3);
        for (int i = data.length - n; i < data.length; i++) {
            sb.append(String.format("%02x", data[i])).append(' ');
        }
        return sb.toString().trim();
    }

    private static String hexHead(byte[] data) {
        int n = Math.min(data.length, 512);
        StringBuilder sb = new StringBuilder(n * 3);
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%02x", data[i])).append(' ');
        }
        return sb.toString().trim();
    }

    /**
     * S2C-ответ на request_chunk_data: сервер эхом возвращает ключи из нашего
     * (сдвинутого) запроса, переводим их обратно в пространство клиента.
     * null — не удалось разобрать, пропускать как есть.
     */
    public static byte[] translateResponseChunkData(byte[] src, int offX, int offZ) {
        try {
            Reader r = new Reader(src);
            Writer w = new Writer();
            w.writeLong(r.readLong());
            while (true) {
                long key = r.readLong();
                if (key == MIN_POSITION_LONG) {
                    w.writeLong(key);
                    break;
                }
                w.writeLong(shiftPacked(key, -offX, -offZ, 1));
                copyCompressedBlockEntity(r, w);
            }
            while (true) {
                long key = r.readLong();
                if (key == MIN_POSITION_LONG) {
                    w.writeLong(key);
                    break;
                }
                w.writeLong(shiftPacked(key, -offX, -offZ, 16));
                boolean present = r.readBoolean();
                w.writeBoolean(present);
                if (present) {
                    copyPalettedContainer(r, w, 8, 4096);
                }
            }
            w.writeBoolean(r.readBoolean());
            if (r.remaining() != 0) {
                return null;
            }
            return w.toArray();
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Каналы C2S
    // ------------------------------------------------------------------

    private static boolean translateSetBlock(Reader r, Writer w, int dx, int dz) throws Exception {
        int count = capped(r.readVarInt(), 1 << 20);
        w.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            w.writeLong(shiftPacked(r.readLong(), dx, dz, 1));
            w.writeVarInt(r.readVarInt());
        }
        boolean updateNeighbors = r.readBoolean();
        w.writeBoolean(updateNeighbors);
        if (updateNeighbors) {
            int n = capped(r.readVarInt(), 1 << 20);
            w.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                w.writeLong(shiftPacked(r.readLong(), dx, dz, 1));
            }
        }
        w.writeVarInt(r.readVarInt());                       // reason
        boolean breaking = r.readBoolean();                  // breaking
        w.writeBoolean(breaking);
        w.writeLong(shiftPacked(r.readLong(), dx, dz, 1));   // BlockHitResult.blockPos
        w.writeVarInt(r.readVarInt());                       // direction ordinal
        for (int i = 0; i < 3; i++) {                        // относительный хит-вектор
            w.writeFloat(r.readFloat());
        }
        w.writeBoolean(r.readBoolean());                     // inside
        w.writeBoolean(r.readBoolean());                     // доп. флаг BlockHitResult (MC 26.x)
        w.writeVarInt(r.readVarInt());                       // hand
        w.writeVarInt(r.readVarInt());                       // sequence
        return true;
    }

    private static boolean translateTeleport(Reader r, Writer w, int dx, int dz) throws Exception {
        w.writeMcString(r.readMcString());
        double x = r.readDouble();
        double y = r.readDouble();
        double z = r.readDouble();
        w.writeDouble(x + dx);
        w.writeDouble(y);
        w.writeDouble(z + dz);
        w.writeFloat(r.readFloat());
        w.writeFloat(r.readFloat());
        return true;
    }

    private static boolean translateSpawnEntity(Reader r, Writer w, int dx, int dz) throws Exception {
        int count = capped(r.readVarInt(), 4096);
        w.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            copyRaw(r, w, 16);                               // новый uuid
            double x = r.readDouble();
            double y = r.readDouble();
            double z = r.readDouble();
            w.writeDouble(x + dx);
            w.writeDouble(y);
            w.writeDouble(z + dz);
            w.writeFloat(r.readFloat());                     // yaw
            w.writeFloat(r.readFloat());                     // pitch
            boolean copyFrom = r.readBoolean();
            w.writeBoolean(copyFrom);
            if (copyFrom) {
                copyRaw(r, w, 16);
            }
            copyRootTag(r, w, dx, dz);
        }
        return true;
    }

    private static boolean translateManipulateEntity(Reader r, Writer w, int dx, int dz) throws Exception {
        int count = capped(r.readVarInt(), 4096);
        w.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            copyRaw(r, w, 16);                               // uuid цели
            int flags = r.readByte();                        // знаковый: <0 => позиции нет
            w.writeByte(flags);
            if (flags >= 0) {
                boolean relX = (flags & 1) != 0;
                boolean relZ = (flags & 4) != 0;
                double x = r.readDouble();
                double y = r.readDouble();
                double z = r.readDouble();
                w.writeDouble(relX ? x : x + dx);
                w.writeDouble(y);
                w.writeDouble(relZ ? z : z + dz);
                w.writeFloat(r.readFloat());                 // yaw
                w.writeFloat(r.readFloat());                 // pitch
            }
            copyRootTag(r, w, dx, dz);
            int passengerManipulation = capped(r.readVarInt(), 3);
            w.writeVarInt(passengerManipulation);
            if (passengerManipulation == 2 || passengerManipulation == 3) {
                int n = capped(r.readVarInt(), 4096);
                w.writeVarInt(n);
                for (int j = 0; j < n; j++) {
                    copyRaw(r, w, 16);
                }
            }
        }
        return true;
    }

    private static boolean translateRequestChunkData(Reader r, Writer w, int dx, int dz) throws Exception {
        w.writeLong(r.readLong());
        w.writeMcString(r.readMcString());
        w.writeBoolean(r.readBoolean());
        int beCount = capped(r.readVarInt(), 1 << 16);
        w.writeVarInt(beCount);
        for (int i = 0; i < beCount; i++) {                  // блоковые позиции
            w.writeLong(shiftPacked(r.readLong(), dx, dz, 1));
        }
        int chunkCount = capped(r.readVarInt(), 1 << 16);
        w.writeVarInt(chunkCount);
        for (int i = 0; i < chunkCount; i++) {               // координаты секций
            w.writeLong(shiftPacked(r.readLong(), dx, dz, 16));
        }
        return true;
    }

    static volatile int lastSectionCount;
    static volatile long lastKeyBeforeFail;

    private static boolean translateSetBuffer(Reader r, Writer w, int dx, int dz) throws Exception {
        w.writeMcString(r.readMcString());
        copyRaw(r, w, 16);                                   // uuid буфера (сервер отбрасывает)
        int type = r.readUnsignedByte();
        if (type == 0) {
            w.writeByte(type);
            int sections = 0;
            while (true) {
                long key = r.readLong();
                lastKeyBeforeFail = key;
                if (key == MIN_POSITION_LONG) {
                    w.writeLong(key);
                    break;
                }
                sections++;
                lastSectionCount = sections;
                w.writeLong(shiftPacked(key, dx, dz, 16));   // ключ секции блоков
                int before = r.pos();
                int[] c = copyPalettedContainer(r, w, 8, 4096);
                traceBuf("секция#" + sections + " pos=" + before + " bits=" + c[0] + " палитра=" + c[1]
                        + " longs=" + c[2] + " конец=" + r.pos());
                int beCount = capped(r.readVarInt(), 4096);
                w.writeVarInt(beCount);
                for (int i = 0; i < beCount; i++) {
                    short off = (short) r.readShortU();      // оффсет внутри секции
                    if (i == 0) {
                        traceAlways("секция#" + sections + " БЭ: count=" + beCount + " первый оффсет=" + (off & 0xFFFF)
                                + " байты=[" + r.peekHex(48) + "]");
                    }
                    w.writeShortU(off & 0xFFFF);
                    copyCompressedBlockEntity(r, w);
                }
                traceBuf("секция#" + sections + " закрыта pos=" + r.pos());
            }
        } else if (type == 1) {
            w.writeByte(type);
            int paletteSize = r.readUnsignedByte();
            if (paletteSize > 255) {
                return false;
            }
            w.writeByte(paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                w.writeMcString(r.readMcString());
            }
            w.writeByte(r.readUnsignedByte());               // defaultValue
            while (true) {
                long key = r.readLong();
                if (key == MIN_POSITION_LONG) {
                    w.writeLong(key);
                    break;
                }
                w.writeLong(shiftPacked(key, dx, dz, 64));   // регион 64^3 квартов
                copyRaw(r, w, 4096);
            }
        } else {
            return false;
        }
        int dispatchSends = capped(r.readVarInt(), 1 << 20);
        traceAlways("ФИНИШ: поз=" + r.pos() + " из=" + r.length() + " остаток=" + r.remaining()
                + " dispatch=" + dispatchSends + " хвост=[" + r.peekHexAt(Math.max(0, r.pos() - 16), 32) + "]");
        if (r.remaining() != 0) {
            traceAlways("!!! ЛИШНИЙ ХВОСТ [" + r.peekHex(128) + "]");
        }
        w.writeVarInt(dispatchSends);
        return true;
    }

    // ------------------------------------------------------------------
    // Примитивы формата
    // ------------------------------------------------------------------

    /** Сдвиг запакованного BlockPos: packed поля X/Z уже поделены на divisor. */
    private static long shiftPacked(long packed, int dxBlocks, int dzBlocks, int divisor) {
        int x = (int) (packed >> 38) + dxBlocks / divisor;
        int z = (int) (packed << 26 >> 38) + dzBlocks / divisor;
        int y = (int) (packed << 52 >> 52);
        return (((long) (x & 0x3FFFFFF)) << 38) | (((long) (z & 0x3FFFFFF)) << 12) | ((long) (y & 0xFFF));
    }

    private static final java.util.concurrent.atomic.AtomicInteger BUF_TRACE = new java.util.concurrent.atomic.AtomicInteger();

    private static void traceBuf(String message) {
        if (BUF_TRACE.incrementAndGet() <= 200) {
            System.out.println("[Toolkit][axiom-diag][buf] " + message);
        }
    }

    private static void traceAlways(String message) {
        System.out.println("[Toolkit][axiom-diag][buf] " + message);
    }

    /**
     * Копия палитрового контейнера (формат MC 26.x): varint длины массива данных
     * удалён — размер выводится из bits и числа ячеек; single-value данных не имеет вовсе.
     * Возвращает {bits, paletteN, longs} для трассировки.
     */
    private static int[] copyPalettedContainer(Reader r, Writer w, int globalThresholdBits, int entries) throws Exception {
        int bits = r.readUnsignedByte();
        if (bits > 16) {
            throw new IllegalStateException("подозрительные bits=" + bits + " на байте " + (r.pos() - 1)
                    + ", байты=[" + r.peekHexAt(r.pos() - 1, 96) + "]");
        }
        w.writeByte(bits);
        if (bits == 0) {
            int single = r.readVarInt();
            w.writeVarInt(single);
            return new int[]{0, 0, 0};
        }
        int n = 0;
        if (bits <= globalThresholdBits) {
            n = capped(r.readVarInt(), 1 << 16);
            w.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                w.writeVarInt(r.readVarInt());
            }
        } // иначе глобальная палитра: доп. полей нет
        int longs = (entries * bits + 63) >>> 6;
        copyRaw(r, w, longs * 8);
        return new int[]{bits, n, longs};
    }

    private static void copyCompressedBlockEntity(Reader r, Writer w) throws Exception {
        w.writeVarInt(capped(r.readVarInt(), NBT_MAX_ARRAY));// originalSize
        w.writeByte(r.readUnsignedByte());                   // словарь сжатия
        int len = capped(r.readVarInt(), NBT_MAX_ARRAY);
        w.writeVarInt(len);
        copyRaw(r, w, len);
    }

    private static void copyRaw(Reader r, Writer w, int n) throws Exception {
        byte[] chunk = new byte[Math.min(n, 8192)];
        int left = n;
        while (left > 0) {
            int step = Math.min(left, chunk.length);
            r.readFully(chunk, step);
            w.writeBytes(chunk, 0, step);
            left -= step;
        }
    }

    private static int capped(int value, int cap) throws Exception {
        if (value < 0 || value > cap) {
            throw new IllegalStateException("значение вне лимита: " + value);
        }
        return value;
    }

    // ------------------------------------------------------------------
    // Сетевой NBT: копирование с переписыванием Pos / TileX / TileZ
    // ------------------------------------------------------------------

    private static void copyRootTag(Reader r, Writer w, int dx, int dz) throws Exception {
        int type = r.readUnsignedByte();
        w.writeByte(type);
        copyTagPayload(r, w, type, dx, dz, 0);
    }

    private static void copyTagPayload(Reader r, Writer w, int type, int dx, int dz, int depth) throws Exception {
        if (depth > NBT_MAX_DEPTH) {
            throw new IllegalStateException("nbt depth");
        }
        switch (type) {
            case 1 -> w.writeByte(r.readByte());
            case 2 -> w.writeShortU(r.readShortU());
            case 3 -> w.writeInt(r.readInt());
            case 4 -> w.writeLong(r.readLong());
            case 5 -> w.writeFloat(r.readFloat());
            case 6 -> w.writeDouble(r.readDouble());
            case 7 -> {
                int n = capped(r.readVarInt(), NBT_MAX_ARRAY);
                w.writeVarInt(n);
                copyRaw(r, w, n);
            }
            case 8 -> {
                int n = r.readShortU() & 0xFFFF;
                if (n > r.remaining()) {
                    throw new IllegalStateException("nbt string");
                }
                w.writeShortU(n);
                copyRaw(r, w, n);
            }
            case 9 -> {
                int elem = r.readUnsignedByte();
                int n = capped(r.readVarInt(), 1 << 20);
                w.writeByte(elem);
                w.writeVarInt(n);
                for (int i = 0; i < n; i++) {
                    copyTagPayload(r, w, elem, dx, dz, depth + 1);
                }
            }
            case 10 -> copyCompound(r, w, dx, dz, depth);
            case 11 -> {
                int n = capped(r.readVarInt(), NBT_MAX_ARRAY / 4);
                w.writeVarInt(n);
                copyRaw(r, w, n * 4);
            }
            case 12 -> {
                int n = capped(r.readVarInt(), NBT_MAX_ARRAY / 8);
                w.writeVarInt(n);
                copyRaw(r, w, n * 8);
            }
            default -> throw new IllegalStateException("nbt type " + type);
        }
    }

    private static void copyCompound(Reader r, Writer w, int dx, int dz, int depth) throws Exception {
        while (true) {
            int t = r.readUnsignedByte();
            w.writeByte(t);
            if (t == 0) {
                return;
            }
            int nameLen = r.readShortU() & 0xFFFF;
            if (nameLen > r.remaining()) {
                throw new IllegalStateException("nbt name");
            }
            byte[] name = new byte[nameLen];
            r.readFully(name, nameLen);
            w.writeShortU(nameLen);
            w.writeBytes(name, 0, nameLen);

            if (t == 9 && nameIs(name, "Pos")) {
                copyPosList(r, w, dx, dz, depth);
                continue;
            }
            if (t == 3 && (nameIs(name, "TileX") || nameIs(name, "TileZ"))) {
                int v = r.readInt();
                w.writeInt(v + (nameIs(name, "TileX") ? dx : dz));
                continue;
            }
            copyTagPayload(r, w, t, dx, dz, depth + 1);
        }
    }

    private static void copyPosList(Reader r, Writer w, int dx, int dz, int depth) throws Exception {
        int elem = r.readUnsignedByte();
        int n = capped(r.readVarInt(), 4096);
        w.writeByte(elem);
        w.writeVarInt(n);
        if (elem == 6 && n == 3) {
            double x = r.readDouble();
            double y = r.readDouble();
            double z = r.readDouble();
            w.writeDouble(x + dx);
            w.writeDouble(y);
            w.writeDouble(z + dz);
        } else {
            for (int i = 0; i < n; i++) {
                copyTagPayload(r, w, elem, dx, dz, depth + 1);
            }
        }
    }

    private static boolean nameIs(byte[] name, String expected) {
        if (name.length != expected.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (name[i] != expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Чтение/запись
    // ------------------------------------------------------------------

    static final class Reader {
        private final byte[] data;
        private int pos;

        Reader(byte[] data) {
            this.data = data;
        }

        int remaining() {
            return data.length - pos;
        }

        /** Hex ближайших n байт без смещения позиции — для диагностики. */
        String peekHex(int n) {
            return peekHexAt(pos, n);
        }

        String peekHexAt(int start, int n) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n && start + i < data.length && start + i >= 0; i++) {
                sb.append(String.format("%02x ", data[start + i]));
            }
            return sb.toString().trim();
        }

        int pos() {
            return pos;
        }

        int length() {
            return data.length;
        }

        int readVarInt() throws Exception {
            int value = 0;
            for (int shift = 0; shift <= 28; shift += 7) {
                require(1);
                byte b = data[pos++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return value;
                }
            }
            throw new IllegalStateException("varint");
        }

        boolean readBoolean() throws Exception {
            require(1);
            return data[pos++] != 0;
        }

        byte readByte() throws Exception {
            require(1);
            return data[pos++];
        }

        int readUnsignedByte() throws Exception {
            return readByte() & 0xFF;
        }

        int readShortU() throws Exception {
            require(2);
            int v = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            return v;
        }

        int readInt() throws Exception {
            require(4);
            int v = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
                    | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
            pos += 4;
            return v;
        }

        long readLong() throws Exception {
            require(8);
            long v = 0L;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (data[pos + i] & 0xFFL);
            }
            pos += 8;
            return v;
        }

        float readFloat() throws Exception {
            return Float.intBitsToFloat(readInt());
        }

        double readDouble() throws Exception {
            return Double.longBitsToDouble(readLong());
        }

        String readMcString() throws Exception {
            int len = readVarInt();
            if (len < 0 || len > remaining()) {
                throw new IllegalStateException("string");
            }
            String s = new String(data, pos, len, StandardCharsets.UTF_8);
            pos += len;
            return s;
        }

        void readFully(byte[] dst, int len) throws Exception {
            require(len);
            System.arraycopy(data, pos, dst, 0, len);
            pos += len;
        }

        void copyTo(Writer w, int len) throws Exception {
            require(len);
            w.writeBytes(data, pos, len);
            pos += len;
        }

        private void require(int n) throws Exception {
            if (n < 0 || pos + n > data.length) {
                throw new IllegalStateException("eof");
            }
        }
    }

    static final class Writer {
        private byte[] buf = new byte[256];
        private int size;

        byte[] toArray() {
            return Arrays.copyOf(buf, size);
        }

        void writeByte(int b) {
            ensure(1);
            buf[size++] = (byte) b;
        }

        void writeBoolean(boolean b) {
            writeByte(b ? 1 : 0);
        }

        void writeBytes(byte[] src, int off, int len) {
            ensure(len);
            System.arraycopy(src, off, buf, size, len);
            size += len;
        }

        void writeVarInt(int v) {
            ensure(5);
            while ((v & ~0x7F) != 0) {
                buf[size++] = (byte) ((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            buf[size++] = (byte) v;
        }

        void writeShortU(int v) {
            ensure(2);
            buf[size++] = (byte) (v >>> 8);
            buf[size++] = (byte) v;
        }

        void writeInt(int v) {
            ensure(4);
            buf[size++] = (byte) (v >>> 24);
            buf[size++] = (byte) (v >>> 16);
            buf[size++] = (byte) (v >>> 8);
            buf[size++] = (byte) v;
        }

        void writeLong(long v) {
            ensure(8);
            for (int i = 7; i >= 0; i--) {
                buf[size++] = (byte) (v >>> (i * 8));
            }
        }

        void writeFloat(float f) {
            writeInt(Float.floatToIntBits(f));
        }

        void writeDouble(double d) {
            writeLong(Double.doubleToLongBits(d));
        }

        void writeMcString(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            writeVarInt(b.length);
            writeBytes(b, 0, b.length);
        }

        private void ensure(int n) {
            if (size + n > buf.length) {
                int cap = buf.length;
                while (cap < size + n) {
                    cap *= 2;
                }
                buf = Arrays.copyOf(buf, cap);
            }
        }
    }
}
