package org.example1.toolkit;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.jar.JarFile;

public final class AchievementBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        // Paper 26.3 may compare plugin pack titles before its registry is ready.
        // A normal world datapack avoids Adventure serialization during discovery.
        try {
            Path server = Path.of("").toAbsolutePath();
            Properties properties = new Properties();
            Path settings = server.resolve("server.properties");
            if (Files.exists(settings)) {
                try (InputStream input = Files.newInputStream(settings)) {
                    properties.load(input);
                }
            }
            Path destination = server.resolve(properties.getProperty("level-name", "world"))
                    .resolve("datapacks/toolkit-achievements").normalize();
            Path pluginJar = Path.of(AchievementBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jar = new JarFile(pluginJar.toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().startsWith("custom_datapack/")) continue;
                    Path target = destination.resolve(entry.getName().substring("custom_datapack/".length())).normalize();
                    if (!target.startsWith(destination)) throw new IOException("Invalid datapack entry: " + entry.getName());
                    Files.createDirectories(target.getParent());
                    try (InputStream input = jar.getInputStream(entry)) {
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception exception) {
            context.getLogger().error("Не удалось установить datapack достижений Toolkit.", exception);
        }
    }
}
