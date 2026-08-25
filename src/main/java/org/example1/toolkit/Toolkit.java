package org.example1.toolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.example1.toolkit.coords.CoordObfuscator;
import org.example1.toolkit.coords.CoordOffsetListener;
import org.example1.toolkit.clock.ClockHud;
import org.example1.toolkit.clock.TimeSpeedHook;
import org.example1.toolkit.compass.CompassBindListener;
import org.example1.toolkit.compass.CompassTracker;
import org.example1.toolkit.deathcoords.DeathCoords;
import org.example1.toolkit.deathcoords.DeathCoordsCommand;
import org.example1.toolkit.deathcoords.DeathCoordsListener;
import org.example1.toolkit.debugstick.DebugStick;
import org.example1.toolkit.debugstick.DebugStickListener;
import org.example1.toolkit.debugstick.DebugStickRecipe;
import org.example1.toolkit.debugstick.EditPolicy;
import org.example1.toolkit.frames.InvisibleFrameListener;
import org.example1.toolkit.frames.InvisibleFrameRecipe;
import org.example1.toolkit.light.InvisibleLightListener;
import org.example1.toolkit.light.InvisibleLightRecipe;
import org.example1.toolkit.light.InvisibleLightTask;
import org.example1.toolkit.bannerhat.BannerHatListener;
import org.example1.toolkit.bannerhat.BannerHatRecipe;
import org.example1.toolkit.fire.FireElytraTask;
import org.example1.toolkit.cauldron.CauldronBedrock;
import org.example1.toolkit.cauldron.CauldronBedrockListener;
import org.example1.toolkit.parrot.ParrotTransplantListener;
import org.example1.toolkit.shield.ShieldEnchantListener;
import org.example1.toolkit.temple.JungleTempleListener;
import org.example1.toolkit.snowgolem.SnowGolemFreezeListener;
import org.example1.toolkit.bundlevanish.BundleVanishingListener;
import org.example1.toolkit.turtle.TurtleHelmListener;
import org.example1.toolkit.animal.AnimalSpawner;
import org.example1.toolkit.animal.AnimalSpawnerListener;
import org.example1.toolkit.status.StatusCommand;
import org.example1.toolkit.status.StatusListener;
import org.example1.toolkit.nms.Nms;
import org.jetbrains.annotations.NotNull;

public final class Toolkit extends JavaPlugin {

    private EditPolicy policy;
    private Hud hud;
    private InvisibleLightTask lightTask;
    private FireElytraTask fireElytraTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Config.reload(this);
        Lang.init(this);
        Lang.bindFallbacks();

        Nms.init();
        if (!Nms.available()) {
            getLogger().warning("НМС-рефлексия недоступна — дебагстик отключён");
        }

        DebugStick.init(this);
        CompassTracker.init(this);
        DeathCoords.init(this);
        DeathCoords.reload();

        try {
            CoordObfuscator.init(this);
        } catch (Throwable t) {
            getLogger().warning("Обфускация координат недоступна: " + t);
        }

        policy = EditPolicy.load(this);

        InvisibleFrameRecipe.register(this);
        DebugStickRecipe.register(this);
        InvisibleLightRecipe.register(this);
        BannerHatRecipe.register(this);

        InvisibleLightListener.loadAll(this);

        TimeSpeedHook timeSpeedHook = new TimeSpeedHook();
        getServer().getScheduler().runTaskLater(this, () -> {
            timeSpeedHook.init();
            if (!timeSpeedHook.available()) {
                getLogger().warning("Плагин TimeSpeed-plugin не найден — часы будут показывать только время");
            }
        }, 1L);

        registerListeners();
        F3Hider.applyAll();

        hud = new Hud(this, timeSpeedHook);
        hud.start();

        CompassTracker.startVerifyTask(this);

        lightTask = new InvisibleLightTask(this);
        lightTask.runTaskTimer(this, 10L, 10L);

        fireElytraTask = new FireElytraTask(this);
        fireElytraTask.runTaskTimer(this, 1L, 1L);

        CauldronBedrock.init(this);
        CauldronBedrock.startColorTask(this);
        CauldronBedrock.restoreAllDisplays();

        AnimalSpawner.reload(this);

        FlectonePulseCompat.init(this);

        registerToolkitCommand();
        registerDeathCoordsCommand();
        registerCauldronTestCommand();
        registerStatusCommand();
    }

    private void registerDeathCoordsCommand() {
        Command deathCoords = new Command("deathcoords") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return new DeathCoordsCommand().onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull java.util.List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                if (args.length == 1) return java.util.List.of("on", "off", "show");
                return java.util.List.of();
            }
        };
        deathCoords.setDescription("Координаты смерти: on/off — уведомления, без аргументов — показать последнюю смерть");
        deathCoords.setUsage("/deathcoords <on|off|show>");
        getServer().getCommandMap().register("deathcoords", deathCoords);
    }

    private void registerCauldronTestCommand() {
        Command cmd = new Command("cauldron") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players");
                    return true;
                }
                var item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HORSE_ARMOR);
                var meta = (org.bukkit.inventory.meta.LeatherArmorMeta) item.getItemMeta();
                meta.setColor(org.bukkit.Color.RED);
                meta.setItemModel(new NamespacedKey("toolkit", "cauldron_water"));
                item.setItemMeta(meta);
                player.getInventory().addItem(item);
                player.sendMessage(Component.text("Выдана тестовая котловая броня!", NamedTextColor.GREEN));
                return true;
            }
        };
        cmd.setDescription("Debug: give cauldron water test item");
        cmd.setPermission("toolkit.reload");
        getServer().getCommandMap().register("cauldron", cmd);
    }

    private void registerToolkitCommand() {
        Command toolkit = new Command("toolkit") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return Toolkit.this.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull java.util.List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                if (args.length == 1) return java.util.List.of("reload");
                return java.util.List.of();
            }
        };
        toolkit.setDescription("Управление плагином Toolkit");
        toolkit.setUsage("/toolkit <reload>");
        toolkit.setPermission("toolkit.reload");
        getServer().getCommandMap().register("toolkit", toolkit);
    }

    private void registerStatusCommand() {
        Command status = new Command("status") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return new StatusCommand().onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull java.util.List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                return new StatusCommand().onTabComplete(sender, this, alias, args);
            }
        };
        status.setDescription("Статус игрока: streaming, recording, dnd");
        status.setUsage("/status <none|streaming|recording|dnd|info>");
        getServer().getCommandMap().register("status", status);
    }

    private void registerListeners() {
        HandlerList.unregisterAll(this);
        getServer().getPluginManager().registerEvents(new CompassBindListener(), this);
        getServer().getPluginManager().registerEvents(new DebugStickListener(policy), this);
        getServer().getPluginManager().registerEvents(new InvisibleFrameListener(), this);
        getServer().getPluginManager().registerEvents(new InvisibleLightListener(), this);
        getServer().getPluginManager().registerEvents(new BannerHatListener(), this);
        getServer().getPluginManager().registerEvents(new CauldronBedrockListener(), this);
        getServer().getPluginManager().registerEvents(new ParrotTransplantListener(), this);
        getServer().getPluginManager().registerEvents(new ShieldEnchantListener(), this);
        getServer().getPluginManager().registerEvents(new JungleTempleListener(), this);
        getServer().getPluginManager().registerEvents(new SnowGolemFreezeListener(), this);
        getServer().getPluginManager().registerEvents(new BundleVanishingListener(), this);
        getServer().getPluginManager().registerEvents(new TurtleHelmListener(), this);
        getServer().getPluginManager().registerEvents(new AnimalSpawnerListener(), this);
        getServer().getPluginManager().registerEvents(new F3Hider(), this);
        getServer().getPluginManager().registerEvents(new DeathCoordsListener(), this);
        getServer().getPluginManager().registerEvents(new StatusListener(), this);
        if (CoordObfuscator.isAvailable()) {
            getServer().getPluginManager().registerEvents(new CoordOffsetListener(), this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        InvisibleFrameRecipe.unregister();
        DebugStickRecipe.unregister();
        InvisibleLightRecipe.unregister();
        CauldronBedrock.removeAllDisplays();
        CoordObfuscator.shutdown(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Использование: /toolkit reload", NamedTextColor.GRAY));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("toolkit.reload")) {
                    sender.sendMessage(Component.text("Недостаточно прав", NamedTextColor.RED));
                    return true;
                }
                reloadConfig();
                Config.reload(this);
                Lang.bindFallbacks();
                DeathCoords.reload();
                policy = EditPolicy.load(this);
                AnimalSpawner.reload(this);
                InvisibleFrameRecipe.unregister();
                DebugStickRecipe.unregister();
                InvisibleLightRecipe.unregister();
                BannerHatRecipe.unregister();
                InvisibleFrameRecipe.register(this);
                DebugStickRecipe.register(this);
                InvisibleLightRecipe.register(this);
                BannerHatRecipe.register(this);
                registerListeners();
                F3Hider.applyAll();
                try {
                    CoordObfuscator.reload(this);
                } catch (Throwable t) {
                    getLogger().warning("Ошибка перезагрузки обфускации координат: " + t);
                }
                sender.sendMessage(Component.text("Конфигурация Toolkit перезагружена", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Использование: /toolkit reload", NamedTextColor.GRAY));
        }
        return true;
    }
}
