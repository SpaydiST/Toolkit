package org.example1.toolkit.nms;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Доступ к состояниям блоков (NMS) через рефлексию для версии 26.2.
 * Использует официальные (mojang) маппинги Paper.
 */
public final class Nms {

    private static final String C_BLOCK_STATE = "net.minecraft.world.level.block.state.BlockState";
    private static final String C_PROPERTY = "net.minecraft.world.level.block.state.properties.Property";
    private static final String C_STATE_DEFINITION = "net.minecraft.world.level.block.state.StateDefinition";
    private static final String C_BLOCK = "net.minecraft.world.level.block.Block";

    private static Class<?> BLOCK_STATE;
    private static Class<?> PROPERTY;
    private static Class<?> STATE_DEFINITION;
    private static Class<?> BLOCK;
    private static Method GET_BLOCK;
    private static Method GET_STATE_DEFINITION;
    private static Method GET_PROPERTIES;
    private static Method GET_PROPERTY;
    private static Method GET_VALUE;
    private static Method SET_VALUE;
    private static Method GET_NAME;
    private static Method GET_POSSIBLE_VALUES;
    private static boolean failed;

    private Nms() {}

    public static void init() {
        try {
            ClassLoader serverLoader = Bukkit.getServer().getClass().getClassLoader();
            BLOCK_STATE = Class.forName(C_BLOCK_STATE, false, serverLoader);
            PROPERTY = Class.forName(C_PROPERTY, false, serverLoader);
            STATE_DEFINITION = Class.forName(C_STATE_DEFINITION, false, serverLoader);
            BLOCK = Class.forName(C_BLOCK, false, serverLoader);
            GET_BLOCK = BLOCK_STATE.getMethod("getBlock");
            GET_STATE_DEFINITION = BLOCK.getMethod("getStateDefinition");
            GET_PROPERTIES = STATE_DEFINITION.getMethod("getProperties");
            GET_PROPERTY = STATE_DEFINITION.getMethod("getProperty", String.class);
            GET_VALUE = BLOCK_STATE.getMethod("getValue", PROPERTY);
            SET_VALUE = findMethod(BLOCK_STATE, "setValue", PROPERTY, Comparable.class);
            GET_NAME = PROPERTY.getMethod("getName");
            GET_POSSIBLE_VALUES = PROPERTY.getMethod("getPossibleValues");
            failed = SET_VALUE == null;
            if (failed) {
                throw new NoSuchMethodException("setValue not found");
            }
        } catch (Throwable t) {
            failed = true;
                System.err.println("[Toolkit] NMS init failure: " + t);
        }
    }

    public static boolean available() {
        return !failed && BLOCK_STATE != null;
    }

    public static List<String> propertyNames(BlockData data) {
        List<String> names = new ArrayList<>();
        try {
            Object state = stateOf(data);
            Object block = GET_BLOCK.invoke(state);
            Object def = GET_STATE_DEFINITION.invoke(block);
            Collection<?> props = (Collection<?>) GET_PROPERTIES.invoke(def);
            for (Object prop : props) {
                names.add((String) GET_NAME.invoke(prop));
            }
        } catch (Throwable ignored) {
        }
        return names;
    }

    public static List<Object> possibleValues(BlockData data, String propertyName) {
        List<Object> out = new ArrayList<>();
        try {
            Object state = stateOf(data);
            Object block = GET_BLOCK.invoke(state);
            Object def = GET_STATE_DEFINITION.invoke(block);
            Object prop = GET_PROPERTY.invoke(def, propertyName);
            if (prop == null) return out;
            Collection<?> vals = (Collection<?>) GET_POSSIBLE_VALUES.invoke(prop);
            out.addAll(vals);
            out.sort((a, b) -> ((Comparable<Object>) a).compareTo(b));
        } catch (Throwable ignored) {
        }
        return out;
    }

    public static Object currentValue(BlockData data, String propertyName) {
        try {
            Object state = stateOf(data);
            Object block = GET_BLOCK.invoke(state);
            Object def = GET_STATE_DEFINITION.invoke(block);
            Object prop = GET_PROPERTY.invoke(def, propertyName);
            if (prop == null) return null;
            return GET_VALUE.invoke(state, prop);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockData withValue(BlockData data, String propertyName, Object value) {
        try {
            Object state = stateOf(data);
            Object block = GET_BLOCK.invoke(state);
            Object def = GET_STATE_DEFINITION.invoke(block);
            Object prop = GET_PROPERTY.invoke(def, propertyName);
            if (prop == null) return data;
            Object newState = SET_VALUE.invoke(state, prop, (Comparable) value);
            Method newData = findNewData(data.getClass(), BLOCK_STATE);
            if (newData == null) return data;
            return (BlockData) newData.invoke(null, newState);
        } catch (Throwable t) {
            return data;
        }
    }

    private static Object stateOf(BlockData data) throws ReflectiveOperationException {
        Field field = findField(data.getClass(), "state");
        if (field != null) {
            return field.get(data);
        }
        Method getState = findMethod(data.getClass(), "getState");
        if (getState != null) {
            return getState.invoke(data);
        }
        Method getNms = findMethod(data.getClass(), "getNMS");
        if (getNms != null) {
            return getNms.invoke(data);
        }
        throw new NoSuchFieldException("state/getState/getNMS");
    }

    private static Method findNewData(Class<?> clazz, Class<?> param) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                        && (m.getName().equals("newData") || m.getName().equals("createData"))
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(param)) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                try {
                    if (!field.trySetAccessible()) return null;
                } catch (RuntimeException ignored) {
                    return null;
                }
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    public static List<int[]> getBookshelfOffsets() {
        List<int[]> result = new ArrayList<>();
        try {
            ClassLoader cl = Bukkit.getServer().getClass().getClassLoader();
            Class<?> etcBlock = Class.forName("net.minecraft.world.level.block.EnchantingTableBlock", false, cl);
            Field field = etcBlock.getDeclaredField("BOOKSHELF_OFFSETS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> offsets = (List<Object>) field.get(null);
            Method getX = offsets.get(0).getClass().getMethod("getX");
            Method getY = offsets.get(0).getClass().getMethod("getY");
            Method getZ = offsets.get(0).getClass().getMethod("getZ");
            for (Object pos : offsets) {
                result.add(new int[]{(int) getX.invoke(pos), (int) getY.invoke(pos), (int) getZ.invoke(pos)});
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private static Class<?> PLAYER_NMS;
    private static Class<?> COMPOUND_TAG;
    private static Method GET_HANDLE_LEFT;
    private static Method GET_HANDLE_RIGHT;
    private static Method SET_LEFT;
    private static Method SET_RIGHT;
    private static Method TAG_IS_EMPTY;
    private static Method TAG_GET_INT;

    static {
        try {
            ClassLoader cl = Bukkit.getServer().getClass().getClassLoader();
            PLAYER_NMS = Class.forName("net.minecraft.world.entity.player.Player", false, cl);
            COMPOUND_TAG = Class.forName("net.minecraft.nbt.CompoundTag", false, cl);
            GET_HANDLE_LEFT = PLAYER_NMS.getMethod("getShoulderEntityLeft");
            GET_HANDLE_RIGHT = PLAYER_NMS.getMethod("getShoulderEntityRight");
            SET_LEFT = PLAYER_NMS.getMethod("setShoulderEntityLeft", COMPOUND_TAG);
            SET_RIGHT = PLAYER_NMS.getMethod("setShoulderEntityRight", COMPOUND_TAG);
            TAG_IS_EMPTY = COMPOUND_TAG.getMethod("isEmpty");
            TAG_GET_INT = COMPOUND_TAG.getMethod("getInt", String.class);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static boolean hasShoulderParrot(Player player, boolean left) {
        try {
            Object nmsPlayer = toNmsPlayer(player);
            Method method = left ? GET_HANDLE_LEFT : GET_HANDLE_RIGHT;
            if (method == null || nmsPlayer == null) return false;
            Object nbtRaw = method.invoke(nmsPlayer);
            return nbtRaw != null && !((Boolean) TAG_IS_EMPTY.invoke(nbtRaw));
        } catch (Throwable e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static int getShoulderParrotVariant(Player player, boolean left) {
        try {
            Object nmsPlayer = toNmsPlayer(player);
            Method method = left ? GET_HANDLE_LEFT : GET_HANDLE_RIGHT;
            if (method == null || nmsPlayer == null) return 0;
            Object nbtRaw = method.invoke(nmsPlayer);
            if (nbtRaw == null || TAG_GET_INT == null) return 0;
            Object res = TAG_GET_INT.invoke(nbtRaw, "Variant");
            if (res instanceof java.util.Optional<?> opt) {
                Object val = opt.orElse(null);
                return val instanceof Number num ? num.intValue() : 0;
            }
            if (res instanceof Number num) {
                return num.intValue();
            }
            return 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    public static void clearShoulderEntity(Player player, boolean left) {
        try {
            Object nmsPlayer = toNmsPlayer(player);
            Method method = left ? SET_LEFT : SET_RIGHT;
            if (method == null || nmsPlayer == null || COMPOUND_TAG == null) return;
            Object empty = COMPOUND_TAG.getDeclaredConstructor().newInstance();
            method.invoke(nmsPlayer, empty);
        } catch (Throwable ignored) {}
    }

    private static Object toNmsPlayer(Player player) throws ReflectiveOperationException {
        Method getHandle = player.getClass().getMethod("getHandle");
        return getHandle.invoke(player);
    }
}
