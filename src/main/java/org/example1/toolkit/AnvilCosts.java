package org.example1.toolkit;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;

/** Расчёт цены наковальни для результатов, которые подставляет сам Toolkit. */
public final class AnvilCosts {

    private AnvilCosts() {}

    /** Штраф предыдущих работ предмета (ванильный RepairCost). */
    public static int penalty(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof Repairable r)) return 0;
        return Math.max(0, r.getRepairCost());
    }

    /** Применяет текст из поля переименования, если он задан. Возвращает true, если имя изменено. */
    public static boolean applyRename(PrepareAnvilEvent event, ItemStack result) {
        String text = event.getView().getRenameText();
        if (text == null || text.isBlank()) return false;
        Component name = Component.text(text);
        Component current = result.getItemMeta() == null ? null : result.getItemMeta().displayName();
        if (name.equals(current)) return false;
        result.editMeta(meta -> meta.displayName(name));
        return true;
    }

    /**
     * Ставит результат и итоговую цену. Если цена не меньше максимума (и игрок не в творческом режиме),
     * результат убирается, как «Слишком дорого» в ванили.
     */
    public static void finish(PrepareAnvilEvent event, ItemStack result, ItemStack left, int cost) {
        int total = Math.max(1, cost);
        HumanEntity viewer = event.getView().getPlayer();
        boolean creative = viewer.getGameMode() == GameMode.CREATIVE;
        if (!creative && total >= event.getView().getMaximumRepairCost()) {
            event.setResult(null);
            event.getView().setRepairCost(total);
            return;
        }
        result.editMeta(Repairable.class, meta -> meta.setRepairCost(Math.max(penalty(left), penalty(event.getInventory().getItem(1))) * 2 + 1));
        event.setResult(result);
        event.getView().setRepairCost(total);
    }
}
