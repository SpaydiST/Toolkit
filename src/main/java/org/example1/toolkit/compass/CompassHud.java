package org.example1.toolkit.compass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CompassHud {

    private static final Map<UUID, Hint> HINTS = new HashMap<>();

    private record Hint(String text, long until) {}

    private CompassHud() {}

    public static void hint(Player p, String text) {
        HINTS.put(p.getUniqueId(), new Hint(text, System.currentTimeMillis() + 2500));
    }

    public static void render(Player p, ItemStack compass) {
        Hint hint = HINTS.get(p.getUniqueId());
        if (hint != null) {
            if (hint.until() > System.currentTimeMillis()) {
                p.sendActionBar(Msg.prefix(Lang.get(p, "compass"), NamedTextColor.RED)
                        .append(Msg.sep())
                        .append(Msg.action(hint.text)));
                return;
            }
            HINTS.remove(p.getUniqueId());
        }

        if (CompassTracker.isPlayerBound(compass)) {
            UUID id = CompassTracker.trackedPlayer(compass);
            Player target = id == null ? null : Bukkit.getPlayer(id);
            if (target == null || !target.isOnline()) {
                Location lastKnown = CompassTracker.getLastKnown(compass);
                Component msg = Msg.prefix(Lang.get(p, "compass"), NamedTextColor.GRAY)
                        .append(Msg.sep())
                        .append(Msg.action(Lang.get(p, "tracking-offline")));
                if (lastKnown != null) {
                    msg = msg.append(Msg.sep())
                            .append(Msg.accent("X: ", NamedTextColor.RED))
                            .append(Msg.val(String.valueOf(lastKnown.getBlockX())))
                            .append(Msg.accent(" Y: ", NamedTextColor.GREEN))
                            .append(Msg.val(String.valueOf(lastKnown.getBlockY())))
                            .append(Msg.accent(" Z: ", NamedTextColor.BLUE))
                            .append(Msg.val(String.valueOf(lastKnown.getBlockZ())));
                }
                p.sendActionBar(msg);
                return;
            }
            if (CompassTracker.isLost(compass)) {
                Location lastKnown = CompassTracker.getLastKnown(compass);
                Component msg = Msg.prefix(Lang.get(p, "compass"), NamedTextColor.GRAY)
                        .append(Msg.sep())
                        .append(Msg.action(Lang.get(p, "tracking-lost")));
                if (lastKnown != null) {
                    msg = msg.append(Msg.sep())
                            .append(Msg.accent("X: ", NamedTextColor.RED))
                            .append(Msg.val(String.valueOf(lastKnown.getBlockX())))
                            .append(Msg.accent(" Y: ", NamedTextColor.GREEN))
                            .append(Msg.val(String.valueOf(lastKnown.getBlockY())))
                            .append(Msg.accent(" Z: ", NamedTextColor.BLUE))
                            .append(Msg.val(String.valueOf(lastKnown.getBlockZ())));
                }
                p.sendActionBar(msg);
                return;
            }
            Component msg = Msg.prefix(Lang.get(p, "compass"), NamedTextColor.AQUA)
                    .append(Msg.sep())
                    .append(Msg.action(target.getName()));
            if (CompassTracker.isOnCooldown(compass)) {
                msg = msg.append(Msg.sep())
                        .append(Msg.dim(Lang.get(p, "cooldown").formatted(CompassTracker.cooldownSeconds(compass))));
            }
            p.sendActionBar(msg);
            return;
        }
        if (CompassTracker.isLodestoneBound(compass)) return;
        renderCoords(p);
    }

    private static void renderCoords(Player p) {
        Location l = p.getLocation();
        Component msg = Msg.prefix(Lang.get(p, "compass"), NamedTextColor.AQUA)
                .append(Msg.sep())
                .append(Msg.accent("X: ", NamedTextColor.RED))
                .append(Msg.action(String.valueOf(l.getBlockX())))
                .append(Msg.accent(" Y: ", NamedTextColor.GREEN))
                .append(Msg.action(String.valueOf(l.getBlockY())))
                .append(Msg.accent(" Z: ", NamedTextColor.BLUE))
                .append(Msg.action(String.valueOf(l.getBlockZ())));
        p.sendActionBar(msg);
    }
}
