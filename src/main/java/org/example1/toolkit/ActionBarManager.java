package org.example1.toolkit;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Combines ActionBar requests from Toolkit and compatible plugins into one display.
 * Other plugins can opt in through {@link Toolkit#submitActionBar}.
 */
public final class ActionBarManager {
    private static final long ROTATION_WINDOW_TICKS = 20L;
    private static ActionBarManager instance;

    private final Plugin owner;
    private final Map<UUID, Map<String, Request>> requests = new HashMap<>();
    private final BukkitTask task;
    private long currentTick;

    private ActionBarManager(Plugin owner) {
        this.owner = owner;
        task = owner.getServer().getScheduler().runTaskTimer(owner, this::render, 1L, 1L);
    }

    public static synchronized void start(Plugin owner) {
        stop();
        instance = new ActionBarManager(owner);
    }

    public static synchronized void stop() {
        if (instance == null) return;
        instance.task.cancel();
        instance.requests.clear();
        instance = null;
    }

    public static synchronized void show(Player player, String source, Component message, int durationTicks, int priority) {
        if (player == null || message == null || source == null || source.isBlank()) return;
        ActionBarManager manager = instance;
        if (manager == null) {
            if (Bukkit.isPrimaryThread()) player.sendActionBar(message);
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            manager.owner.getServer().getScheduler().runTask(manager.owner, () -> {
                if (instance == manager && player.isOnline()) {
                    manager.put(player, source, message, durationTicks, priority);
                }
            });
            return;
        }
        manager.put(player, source, message, durationTicks, priority);
    }

    private void put(Player player, String source, Component message, int durationTicks, int priority) {
        long expiresAt = currentTick + Math.max(1, durationTicks) + 1L;
        requests.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(source, new Request(message, expiresAt, priority));
    }

    private void render() {
        currentTick++;
        requests.entrySet().removeIf(playerRequests -> {
            Player player = Bukkit.getPlayer(playerRequests.getKey());
            if (player == null || !player.isOnline()) return true;

            Map<String, Request> active = playerRequests.getValue();
            active.values().removeIf(request -> request.expiresAt <= currentTick);
            if (active.isEmpty()) return true;

            int highestPriority = active.values().stream()
                    .mapToInt(request -> request.priority)
                    .max()
                    .orElse(Integer.MIN_VALUE);
            List<Map.Entry<String, Request>> candidates = active.entrySet().stream()
                    .filter(entry -> entry.getValue().priority == highestPriority)
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .toList();
            int selected = (int) ((currentTick / ROTATION_WINDOW_TICKS) % candidates.size());
            player.sendActionBar(candidates.get(selected).getValue().message);
            return false;
        });
    }

    private record Request(Component message, long expiresAt, int priority) {}
}
