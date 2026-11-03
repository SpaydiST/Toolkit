package org.example1.toolkit.frames;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example1.toolkit.Toolkit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Невидимые рамки: пустая рамка рисуется настоящей моделью рамки,
 * с предметом — полностью невидимая (предмет «парит» в рамке).
 */
public final class InvisibleFrameOverlay implements Listener {

    private static final Set<UUID> tracked = new HashSet<>();

    private static NamespacedKey frameKey() {
        return new NamespacedKey(Toolkit.getPlugin(Toolkit.class), "invisible_frame");
    }

    public InvisibleFrameOverlay() {}

    public static void start(JavaPlugin plugin) {
        reconcileLoadedFrames();
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            reconcile(entity);
        }
    }

    private static void reconcileLoadedFrames() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                reconcile(entity);
            }
        }
    }

    private static void reconcile(Entity entity) {
        if (entity instanceof ItemFrame frame
                && frame.getPersistentDataContainer().has(frameKey())) {
            track(frame);
            frame.setVisible(frame.getItem().getType().isAir());
        }
    }

    public static void mark(ItemFrame frame) {
        frame.getPersistentDataContainer().set(frameKey(), PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isMarked(ItemFrame frame) {
        return frame.getPersistentDataContainer().has(frameKey(), PersistentDataType.BYTE);
    }

    public static void track(ItemFrame frame) {
        if (tracked.add(frame.getUniqueId())) {
            frame.setVisible(true);
        }
    }

    public static void cleanup() {
        for (UUID id : tracked) {
            Entity e = Bukkit.getEntity(id);
            if (e instanceof ItemFrame frame) {
                frame.setVisible(false);
            }
        }
        tracked.clear();
    }

    private static void tick() {
        Iterator<UUID> it = tracked.iterator();
        while (it.hasNext()) {
            Entity entity = Bukkit.getEntity(it.next());
            if (!(entity instanceof ItemFrame frame) || !frame.isValid()) {
                it.remove();
                continue;
            }
            boolean empty = frame.getItem().getType().isAir();
            frame.setVisible(empty);
        }
    }
}
