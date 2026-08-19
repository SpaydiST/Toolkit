package org.example1.toolkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
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

        registerToolkitCommand();
        registerDeathCoordsCommand();
    }

    private void registerDeathCoordsCommand() {
        Command deathCoords = new Command("deathcoords") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return new DeathCoordsCommand().onCommand(sender, this, commandLabel, args);
            }
        };
        deathCoords.setDescription("Координаты смерти: on/off — уведомления, без аргументов — показать последнюю смерть");
        deathCoords.setUsage("/deathcoords <on|off|show>");
        getServer().getCommandMap().register("deathcoords", deathCoords);
    }

    private void registerToolkitCommand() {
        Command toolkit = new Command("toolkit") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return Toolkit.this.onCommand(sender, this, commandLabel, args);
            }
        };
        toolkit.setDescription("Управление плагином Toolkit");
        toolkit.setUsage("/toolkit <reload>");
        toolkit.setPermission("toolkit.reload");
        getServer().getCommandMap().register("toolkit", toolkit);
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
        getServer().getPluginManager().registerEvents(new F3Hider(), this);
        getServer().getPluginManager().registerEvents(new DeathCoordsListener(), this);
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
