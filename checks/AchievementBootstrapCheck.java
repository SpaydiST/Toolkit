import java.nio.file.Files;
import java.nio.file.Path;
import org.example1.toolkit.AchievementBootstrap;

/** Run in a disposable server directory with level-name=custom-world. */
public final class AchievementBootstrapCheck {
    public static void main(String[] args) throws Exception {
        new AchievementBootstrap().bootstrap(null);
        Path pack = Path.of("custom-world/datapacks/toolkit-achievements");
        if (!Files.isRegularFile(pack.resolve("pack.mcmeta"))) {
            throw new AssertionError("Datapack must be installed before world loading");
        }
        try (var files = Files.walk(pack.resolve("data"))) {
            if (files.noneMatch(Files::isRegularFile)) throw new AssertionError("Missing advancement data");
        }
        new AchievementBootstrap().bootstrap(null);
        if (!Files.readString(pack.resolve("pack.mcmeta")).contains("pack")) {
            throw new AssertionError("Repeated bootstrap damaged datapack");
        }
    }
}
