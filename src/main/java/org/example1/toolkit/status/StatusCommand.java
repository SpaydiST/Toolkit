package org.example1.toolkit.status;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StatusCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только для игроков", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Текущий статус: ", NamedTextColor.GRAY)
                    .append(statusLabel(StatusManager.get(player.getUniqueId()))));
            player.sendMessage(Component.text("/status <none|streaming|recording|dnd>", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/status info <игрок>", NamedTextColor.GRAY));
            return true;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("info") && args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(Component.text("Игрок не найден", NamedTextColor.RED));
                return true;
            }
            player.sendMessage(Component.text(target.getName() + ": ", NamedTextColor.GRAY)
                    .append(statusLabel(StatusManager.get(target.getUniqueId()))));
            return true;
        }

        StatusManager.PlayerStatus status;
        try {
            status = StatusManager.PlayerStatus.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Неизвестный статус", NamedTextColor.RED));
            return true;
        }

        StatusManager.set(player, status);
        player.sendMessage(Component.text("Текущий статус: ", NamedTextColor.GRAY)
                .append(statusLabel(status)));
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

    private static Component statusLabel(StatusManager.PlayerStatus status) {
        return switch (status) {
            case STREAMING -> Component.text("● Стримит", NamedTextColor.DARK_PURPLE);
            case RECORDING -> Component.text("● Запись", NamedTextColor.RED);
            case DND -> Component.text("■ Не беспокоить", NamedTextColor.DARK_RED);
            default -> Component.text("● Нет статуса", NamedTextColor.GREEN);
        };
    }
}
