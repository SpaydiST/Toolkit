package org.example1.toolkit.bannerhat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example1.toolkit.Config;
import org.example1.toolkit.Lang;
import org.example1.toolkit.Msg;

public final class BannerHatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!Config.BANNER_HAT_ENABLED) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        boolean isHat = BannerHatRecipe.isBannerHat(item);
        boolean isBanner = BannerHatRecipe.isBanner(item.getType());
        if (!isHat && !isBanner) return;

        event.setCancelled(true);

        ItemStack hat = isHat ? item : BannerHatRecipe.createBannerHat(item);

        ItemStack oldHelmet = player.getInventory().getHelmet();
        if (oldHelmet != null && oldHelmet.getType() != Material.AIR
                && player.getGameMode() != GameMode.CREATIVE) {
            player.getWorld().dropItemNaturally(player.getLocation(), oldHelmet);
        }

        player.getInventory().setHelmet(hat.asOne());
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        player.sendActionBar(Msg.prefix(Lang.get(player, "banner-hat-prefix"), NamedTextColor.GOLD)
                .append(Msg.sep())
                .append(Msg.action(Lang.get(player, "banner-hat-equipped"))));
    }
}
