package org.example1.toolkit.compass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Lang;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CompassHud {

    private static final Map<UUID, Hint> HINTS = new HashMap<>();

    private record Hint(String text, long until) {}

    private CompassHud() {}

    /** Показать временное сообщение вместо обычного HUD (ошибки взаимодействия). */
    public static void hint(Player p, String text) {
        HINTS.put(p.getUniqueId(), new Hint(text, System.currentTimeMillis() + 2500));
    }

    public static void render(Player p, ItemStack compass) {
        Hint hint = HINTS.get(p.getUniqueId());
        if (hint != null) {
            if (hint.until() > System.currentTimeMillis()) {
                p.sendActionBar(Component.text(hint.text, NamedTextColor.RED));
                return;
            }
            HINTS.remove(p.getUniqueId());
        }

        if (CompassTracker.isPlayerBound(compass)) {
            UUID id = CompassTracker.trackedPlayer(compass);
            Player target = id == null ? null : Bukkit.getPlayer(id);
            if (target == null || !target.isOnline()) {
                p.sendActionBar(Component.text(Lang.get(p, "tracking-offline"), NamedTextColor.GRAY));
                return;
            }
            if (CompassTracker.isLost(compass)) {
                p.sendActionBar(Component.text(Lang.get(p, "tracking-lost"), NamedTextColor.GRAY));
                return;
            }
            Component msg = Component.text(
                    Lang.get(p, "tracking").formatted(target.getName()), NamedTextColor.AQUA);
            if (CompassTracker.isOnCooldown(compass)) {
                msg = msg.append(Component.text(
                        Lang.get(p, "cooldown").formatted(CompassTracker.cooldownSeconds(compass)),
                        NamedTextColor.GRAY));
            }
            p.sendActionBar(msg);
            return;
        }
        if (CompassTracker.isLodestoneBound(compass)) return;
        renderCoords(p);
    }

    private static void renderCoords(Player p) {
        Location l = p.getLocation();
        Component msg = Component.text(Lang.get(p, "coords") + " ", NamedTextColor.AQUA)
                .append(Component.text("X: ", NamedTextColor.RED))
                .append(Component.text(String.valueOf(l.getBlockX()), NamedTextColor.WHITE))
                .append(Component.text(" Y: ", NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(l.getBlockY()), NamedTextColor.WHITE))
                .append(Component.text(" Z: ", NamedTextColor.BLUE))
                .append(Component.text(String.valueOf(l.getBlockZ()), NamedTextColor.WHITE));
        p.sendActionBar(msg);
    }
}
