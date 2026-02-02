package world.cals.supercollidersnippetmanager;

import java.nio.file.Path;

public final class AppPaths {
    private AppPaths() {}

    public static Path appRootDir() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".supercollider-snippet-manager");
    }

    public static Path dataDir() {
        return appRootDir().resolve("data");
    }

    public static Path configFile() {
        return appRootDir().resolve("config.json");
    }
}
