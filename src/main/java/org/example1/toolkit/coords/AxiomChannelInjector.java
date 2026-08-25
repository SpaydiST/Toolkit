package org.example1.toolkit.coords;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.example1.toolkit.Config;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Нетти-инжектор для C2S-payload'ов Axiom.
 *
 * AxiomPaper разбирает свои каналы СВОИМ декодером ("axiom-big-payload-handler"),
 * стоящим в пайплайне ДО ванильного "decoder", поэтому такие кадры никогда не
 * доходят до PacketEvents. Регистрируемся раньше него и переводим кадры через
 * AxiomTranslator до того, как их увидит AxiomPaper.
 *
 * Игроки без обфускации (bypass/нулевой оффсет) проходят без изменений,
 * при отсутствии мода Axiom кадры с каналами axiom:* просто не встречаются.
 */
public final class AxiomChannelInjector implements Listener {

    public static final String HANDLER_NAME = "toolkit-axiom-c2s";
    private static final String AXIOM_HANDLER = "axiom-big-payload-handler";
    private static final ConcurrentMap<String, Boolean> loggedFailures = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        attach(event.getPlayer());
    }

    private static void attach(Player player) {
        if (!Config.COORD_OFFSET_ENABLED || !Config.COORD_OFFSET_AXIOM_FIX) {
            return;
        }
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object gameConnection = handle.getClass().getField("connection").get(handle);
            Object connection = gameConnection.getClass().getField("connection").get(gameConnection);
            Channel channel = extractChannel(connection);
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                return;
            }
            // addBefore ставит нас ближе к head => мы обрабатываем кадр РАНЬШЕ якоря.
            // Якоримся на обработчик Axiom, если он есть; иначе на ванильный decoder
            // (в обоих случаях наш обработчик оказывается первым из пары).
            String anchor = pipeline.get(AXIOM_HANDLER) != null ? AXIOM_HANDLER : "decoder";
            if (pipeline.get(anchor) == null) {
                return;
            }
            pipeline.addBefore(anchor, HANDLER_NAME, new FrameTranslator(player.getUniqueId()));
            Bukkit.getLogger().info("[Toolkit] Переводчик Axiom подключен для " + player.getName()
                    + (AXIOM_HANDLER.equals(anchor) ? "" : " (обработчик Axiom не найден)"));
        } catch (Throwable t) {
            if (loggedFailures.putIfAbsent("attach", Boolean.TRUE) == null) {
                Bukkit.getLogger().warning("[Toolkit] Не удалось подключить переводчик Axiom: " + t);
            }
        }
    }

    private static final class FrameTranslator extends MessageToMessageDecoder<ByteBuf> {
        private final UUID playerId;
        private static final java.util.concurrent.atomic.AtomicInteger DIAG = new java.util.concurrent.atomic.AtomicInteger();

        FrameTranslator(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf frame, List<Object> out) {
            PlayerOffsets offsets = CoordObfuscator.offsets(playerId);
            FixedOffset offset = offsets == null || offsets.bypass() || offsets.isCurrentZero()
                    ? null
                    : offsets.current();
            if (offset == null) {
                String ch = sniffChannel(frame);
                if (ch != null && ch.startsWith("axiom:") && DIAG.incrementAndGet() <= 40) {
                    Bukkit.getLogger().info("[Toolkit][axiom-diag] " + ch + ": игрок без оффсета — кадр прошёл как есть");
                }
                out.add(frame.retain());
                return;
            }
            byte[] src = new byte[frame.readableBytes()];
            frame.getBytes(frame.readerIndex(), src);
            byte[] translated = AxiomTranslator.translateClientFrame(src, offset.x(), offset.z());
            String ch = sniffChannel(frame);
            if (ch != null && ch.startsWith("axiom:") && DIAG.incrementAndGet() <= 40) {
                boolean changed = !java.util.Arrays.equals(src, translated);
                Bukkit.getLogger().info("[Toolkit][axiom-diag] " + ch + ": "
                        + (changed ? "переведён " + src.length + "Б -> " + translated.length + "Б"
                                   : "без изменений (" + src.length + "Б)")
                        + ", оффсет " + offset.x() + "/" + offset.z());
            }
            out.add(Unpooled.wrappedBuffer(translated));
        }

        private static String sniffChannel(ByteBuf frame) {
            try {
                int idx = frame.readerIndex();
                int[] lens = new int[2];
                for (int k = 0; k < 2; k++) {
                    int value = 0, shift = 0;
                    while (true) {
                        byte b = frame.getByte(idx++);
                        value |= (b & 0x7F) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                        if (shift > 28) return null;
                    }
                    lens[k] = value;
                }
                int slen = lens[1];
                if (slen <= 0 || slen > 256 || idx + slen > frame.writerIndex()) return null;
                byte[] ch = new byte[slen];
                frame.getBytes(idx, ch);
                return new String(ch, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    private static Channel extractChannel(Object connection) throws Exception {
        try {
            return (Channel) connection.getClass().getMethod("channel").invoke(connection);
        } catch (NoSuchMethodException ignored) {
            for (Class<?> c = connection.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field field = c.getDeclaredField("channel");
                    field.setAccessible(true);
                    return (Channel) field.get(connection);
                } catch (NoSuchFieldException ignored2) {
                }
            }
            throw new NoSuchFieldException("net.minecraft.network.Connection.channel");
        }
    }
}
