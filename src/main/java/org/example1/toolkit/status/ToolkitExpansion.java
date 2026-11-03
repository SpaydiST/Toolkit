package org.example1.toolkit.status;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.deathcoords.DeathCoords;
import org.jetbrains.annotations.NotNull;

public class ToolkitExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;

    public ToolkitExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "toolkit";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getRequiredPlugin() {
        return "Toolkit";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        StatusManager.PlayerStatus status = StatusManager.get(player.getUniqueId());

        if (params.equalsIgnoreCase("status")) {
            return status.id();
        }
        if (params.equalsIgnoreCase("status_name")) {
            return status.displayName();
        }
        if (params.equalsIgnoreCase("status_symbol")) {
            return status.symbol();
        }
        if (params.equalsIgnoreCase("status_prefix") || params.equalsIgnoreCase("status_prefix_colored")) {
            return status.legacyPrefix();
        }
        if (params.equalsIgnoreCase("status_prefix_legacy") || params.equalsIgnoreCase("status_prefix_amp")) {
            return status.ampersandPrefix();
        }
        if (params.equalsIgnoreCase("status_prefix_mm")) {
            return status.miniMessagePrefix();
        }
        if (params.equalsIgnoreCase("status_prefix_raw")) {
            return status.prefixString();
        }
        if (params.equalsIgnoreCase("status_display")) {
            return status.displayLegacy();
        }
        if (params.equalsIgnoreCase("status_display_mm")) {
            return status.displayMiniMessage();
        }
        if (params.equalsIgnoreCase("status_color")) {
            return status.colorName();
        }
        if (params.equalsIgnoreCase("status_hex")) {
            return status.hexColor();
        }

        if (params.equalsIgnoreCase("death_coords")) {
            DeathCoords.DeathRecord death = DeathCoords.getRecord(player.getUniqueId());
            if (death == null) return "";
            return death.x() + " " + death.y() + " " + death.z() + " (" + death.dimension() + ")";
        }
        if (params.equalsIgnoreCase("death_x")) {
            DeathCoords.DeathRecord death = DeathCoords.getRecord(player.getUniqueId());
            return death == null ? "" : String.valueOf(death.x());
        }
        if (params.equalsIgnoreCase("death_y")) {
            DeathCoords.DeathRecord death = DeathCoords.getRecord(player.getUniqueId());
            return death == null ? "" : String.valueOf(death.y());
        }
        if (params.equalsIgnoreCase("death_z")) {
            DeathCoords.DeathRecord death = DeathCoords.getRecord(player.getUniqueId());
            return death == null ? "" : String.valueOf(death.z());
        }
        if (params.equalsIgnoreCase("death_dimension")) {
            DeathCoords.DeathRecord death = DeathCoords.getRecord(player.getUniqueId());
            return death == null ? "" : death.dimension();
        }

        return null;
    }
}
