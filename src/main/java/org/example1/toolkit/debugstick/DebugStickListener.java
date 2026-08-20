package org.example1.toolkit.debugstick;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.nms.Nms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DebugStickListener implements Listener {

    private final EditPolicy policy;
    private final Map<String, String> selection = new HashMap<>();

    public DebugStickListener(EditPolicy policy) {
        this.policy = policy;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!Config.DEBUG_STICK_ENABLED) return;
        ItemStack item = event.getItem();
        if (item == null || !DebugStick.isDebugStick(item)) return;
        if (!Nms.available()) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType().isAir()) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean forward = !player.isSneaking();

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                DebugStick.saveState(item, block);
                player.sendActionBar(Component.text(
                        Lang.get(player, "stick-state-copied").formatted(block.getType().name().toLowerCase()),
                        NamedTextColor.GREEN));
            } else {
                selectProperty(player, block, forward);
            }
        } else {
            if (DebugStick.hasCopiedState(item)
                    && block.getType().name().equals(DebugStick.getCopiedType(item))) {
                applyState(player, block, item);
            } else {
                if (modifyValue(player, block, forward)) {
                    DebugStick.consumeUse(item);
                    if (!DebugStick.isDebugStick(item)) {
                        player.sendActionBar(Component.text(Lang.get(player, "stick-broken"), NamedTextColor.RED));
                    }
                }
            }
        }
    }

    private void applyState(Player player, Block block, ItemStack item) {
        BlockData newData = DebugStick.getCopiedState(item);
        if (newData == null) {
            player.sendActionBar(Component.text(Lang.get(player, "stick-state-fail"), NamedTextColor.RED));
            return;
        }
        block.setBlockData(newData);
        DebugStick.consumeUse(item);
        if (!DebugStick.isDebugStick(item)) {
            player.sendActionBar(Component.text(Lang.get(player, "stick-broken"), NamedTextColor.RED));
        } else {
            player.sendActionBar(Component.text(
                    Lang.get(player, "stick-state-applied").formatted(block.getType().name().toLowerCase()),
                    NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (!Config.DEBUG_STICK_ENABLED) return;
        ItemStack item = event.getItemInHand();
        if (item != null && DebugStick.isDebugStick(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!Config.DEBUG_STICK_ENABLED) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item != null && DebugStick.isDebugStick(item)) {
            event.setCancelled(true);
        }
    }

    private void selectProperty(Player player, Block block, boolean forward) {
        Map<String, List<String>> editable = editable(block);
        List<String> names = new ArrayList<>(editable.keySet());
        if (names.isEmpty()) {
            player.sendActionBar(Component.text(Lang.get(player, "no-editable"), NamedTextColor.RED));
            return;
        }

        String key = selectionKey(player, block);
        String selected = selection.get(key);
        int index = names.indexOf(selected);
        if (index < 0) {
            index = forward ? 0 : names.size() - 1;
        } else {
            index = forward ? (index + 1) % names.size() : (index - 1 + names.size()) % names.size();
        }

        String propName = names.get(index);
        selection.put(key, propName);
        player.sendActionBar(Component.text(
                Lang.get(player, "selected").formatted(propName),
                NamedTextColor.GRAY));
    }

    private boolean modifyValue(Player player, Block block, boolean forward) {
        Map<String, List<String>> editable = editable(block);
        if (editable.isEmpty()) {
            player.sendActionBar(Component.text(Lang.get(player, "no-editable"), NamedTextColor.RED));
            return false;
        }

        List<String> names = new ArrayList<>(editable.keySet());
        String key = selectionKey(player, block);
        String propName = selection.get(key);
        if (propName == null || !editable.containsKey(propName)) {
            propName = names.get(0);
        }

        List<Object> values = resolveValues(block.getBlockData(), propName, editable.get(propName));
        if (values.size() < 2) {
            player.sendActionBar(Component.text(Lang.get(player, "no-editable"), NamedTextColor.RED));
            return false;
        }

        Object cur = Nms.currentValue(block.getBlockData(), propName);
        int curIdx = values.indexOf(cur);
        if (curIdx < 0) {
            player.sendActionBar(Component.text(Lang.get(player, "no-editable"), NamedTextColor.RED));
            return false;
        }

        int newIdx = forward ? (curIdx + 1) % values.size() : (curIdx - 1 + values.size()) % values.size();
        BlockData newData = Nms.withValue(block.getBlockData(), propName, values.get(newIdx));
        if (newData == null || newData.equals(block.getBlockData())) {
            return false;
        }
        block.setBlockData(newData);

        player.sendActionBar(Component.text(
                Lang.get(player, "changed").formatted(block.getType().name().toLowerCase(), propName, String.valueOf(values.get(newIdx))),
                NamedTextColor.GRAY));
        return true;
    }

    private Map<String, List<String>> editable(Block block) {
        BlockData data = block.getBlockData();
        List<String> names = Nms.propertyNames(data);
        Map<String, Object> current = new LinkedHashMap<>();
        for (String n : names) current.put(n, Nms.currentValue(data, n));
        return policy.editableProperties(block.getType(), names, current);
    }

    private static List<Object> resolveValues(BlockData data, String propName, List<String> allowed) {
        List<Object> all = Nms.possibleValues(data, propName);
        if (allowed == null || allowed.isEmpty() || allowed.contains(EditPolicy.ALL)) return all;
        List<String> lower = allowed.stream()
                .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .toList();
        List<Object> out = new ArrayList<>();
        for (Object v : all) {
            if (lower.contains(String.valueOf(v).toLowerCase(java.util.Locale.ROOT))) out.add(v);
        }
        return out;
    }

    private static String selectionKey(Player p, Block b) {
        return p.getUniqueId() + ":" + b.getType().getKey();
    }
}
