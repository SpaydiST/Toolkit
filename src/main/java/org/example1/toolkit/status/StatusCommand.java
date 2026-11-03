package org.example1.toolkit.status;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StatusCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только для игроков", Config.COLOR_ERROR));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text(Lang.get(player, "status-current") + " ", Msg.EMERALD)
                    .append(statusLabel(player, StatusManager.get(player.getUniqueId()))));
            player.sendMessage(Component.text("/status <none|streaming|recording|dnd>", Msg.EMERALD));
            player.sendMessage(Component.text(Lang.get(player, "status-info-usage"), Msg.EMERALD));
            return true;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("info") && args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(Component.text(Lang.get(player, "player-not-found"), Config.COLOR_ERROR));
                return true;
            }
            player.sendMessage(Component.text(target.getName() + ": ", Msg.EMERALD)
                    .append(statusLabel(player, StatusManager.get(target.getUniqueId()))));
            return true;
        }

        StatusManager.PlayerStatus status;
        try {
            status = StatusManager.PlayerStatus.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text(Lang.get(player, "status-unknown"), Config.COLOR_ERROR));
            return true;
        }

        StatusManager.set(player, status);
        player.sendMessage(Component.text(Lang.get(player, "status-current") + " ", Msg.EMERALD)
                .append(statusLabel(player, status)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String s : new String[]{"none", "streaming", "recording", "dnd", "info"}) {
                if (s.startsWith(prefix)) result.add(s);
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            String prefix = args[1].toLowerCase();
            List<String> result = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) result.add(p.getName());
            }
            return result;
        }
        return List.of();
    }

    private static Component statusLabel(Player viewer, StatusManager.PlayerStatus status) {
        String name = Lang.get(viewer, "status-" + status.id());
        String symbol = status.symbol();
        return Component.text((symbol.isEmpty() ? "" : symbol + " ") + name,
                status.color() != null ? status.color() : net.kyori.adventure.text.format.NamedTextColor.GRAY);
    }
}
