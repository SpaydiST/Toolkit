package org.example1.toolkit.clock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;

public final class ClockHud {

    private ClockHud() {}

    public static void render(Player p, ItemStack clock, TimeSpeedHook hook) {
        World w = p.getWorld();
        long tick = w.getTime() % 24000;
        String time = formatTime(tick);
        Component msg = Msg.prefix(Lang.get(p, "time-label"), NamedTextColor.YELLOW)
                .append(Msg.sep())
                .append(Msg.action(time));
        String skip = skipInfo(p, w, tick, hook);
        if (skip != null && !skip.isEmpty()) {
            msg = msg.append(Msg.sep())
                    .append(Msg.dim(skip));
        }
        p.sendActionBar(msg);
    }

    static String skipInfo(Player p, World w, long tick, TimeSpeedHook hook) {
        if (w.getEnvironment() != World.Environment.NORMAL) return null;
        if (!Boolean.TRUE.equals(w.getGameRuleValue(GameRules.ADVANCE_TIME))) return null;

        double speed = hook != null && hook.available() ? hook.currentSpeed(w) : 0.0;
        boolean night = tick >= 12000 && tick < 23900;
        boolean thunder = w.isThundering();

        if (night || thunder) {
            long remaining = 24000 - tick;
            if (remaining <= 0) remaining = 0;
            double seconds = remaining / (Math.max(1.0, speed) * 20.0);
            if (speed > 1.0) {
                return Lang.get(p, "to-morning-fast").formatted(fmt(seconds), speed);
            }
            return Lang.get(p, "to-morning").formatted(fmt(seconds));
        }

        long remaining;
        if (tick >= 23900) {
            remaining = 12000 + (24000 - tick);
        } else {
            remaining = 12000 - tick;
        }
        return Lang.get(p, "to-night").formatted(fmt(remaining / 20.0));
    }

    static String formatTime(long tick) {
        int minutesOfDay = (int) Math.floor(tick * 60.0 / 1000.0);
        int hour = ((minutesOfDay / 60) + 6) % 24;
        int minute = minutesOfDay % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    static String fmt(double seconds) {
        int s = Math.max(0, (int) Math.round(seconds));
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
