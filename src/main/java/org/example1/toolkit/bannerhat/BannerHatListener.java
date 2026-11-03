package org.example1.toolkit.bannerhat;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Config;
import org.example1.toolkit.ActionBarManager;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;
import org.example1.toolkit.progression.AchievementProgression;

import java.util.Map;

public final class BannerHatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!Config.BANNER_HAT_ENABLED) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        boolean isHat = BannerHatRecipe.isBannerHat(item);
        boolean isBanner = BannerHatRecipe.isBanner(item.getType());
        if (!isHat && !isBanner) return;

        ItemStack oldHelmet = player.getInventory().getHelmet();
        if (oldHelmet != null && oldHelmet.getType() != Material.AIR
                && player.getGameMode() != GameMode.CREATIVE
                && oldHelmet.containsEnchantment(Enchantment.BINDING_CURSE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack hat = isHat ? item.clone() : BannerHatRecipe.createBannerHat(item);

        player.getInventory().setHelmet(hat.asOne());
        AchievementProgression.award(player, "toolkit_hat");
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
            if (oldHelmet != null && oldHelmet.getType() != Material.AIR) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(oldHelmet);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        ActionBarManager.show(player, "toolkit:banner-hat", Msg.prefix(Lang.get(player, "banner-hat-prefix"), Msg.EMERALD)
                .append(Msg.sep())
                .append(Msg.action(Lang.get(player, "banner-hat-equipped"))), 40, 50);
    }
}
