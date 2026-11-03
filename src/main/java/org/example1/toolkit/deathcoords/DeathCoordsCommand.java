package org.example1.toolkit.deathcoords;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example1.toolkit.Config;
import org.example1.toolkit.Msg;
import org.jetbrains.annotations.NotNull;

public final class DeathCoordsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только для игроков", Config.COLOR_ERROR));
            return true;
        }
        if (args.length == 0) {
            DeathCoords.showLast(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "on" -> {
                DeathCoords.setEnabled(player, true);
                player.sendMessage(Component.text(Config.MSG_DEATH_ON, Msg.EMERALD));
            }
            case "off" -> {
                DeathCoords.setEnabled(player, false);
                player.sendMessage(Component.text(Config.MSG_DEATH_OFF, Msg.EMERALD));
            }
            case "show", "last" -> DeathCoords.showLast(player);
            default -> player.sendMessage(Component.text(Config.MSG_DEATH_USAGE, Msg.EMERALD));
        }
        return true;
    }
}
