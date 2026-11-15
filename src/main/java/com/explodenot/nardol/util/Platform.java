package com.explodenot.gli.util;

import java.io.File;
import java.nio.file.Path;

public final class Platform {
    private Platform() {}

    public static String osName() {
        return System.getProperty("os.name", "").toLowerCase();
    }

    public static boolean isWindows() {
        return osName().contains("win");
    }

    public static boolean isMac() {
        return osName().contains("mac") || osName().contains("darwin");
    }

    public static boolean isLinux() {
        return osName().contains("linux") || osName().contains("unix");
    }

    public static Path cacheDir() {
        String xdgCache = System.getenv("XDG_CACHE_HOME");
        if (xdgCache != null && !xdgCache.isBlank()) {
            return Path.of(xdgCache, "nardol");
        }
        String userHome = System.getProperty("user.home", ".");
        if (isMac()) {
            return Path.of(userHome, "Library", "Caches", "nardol");
        } else if (isWindows()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, "nardol");
            }
            return Path.of(userHome, "AppData", "Local", "nardol");
        }
        return Path.of(userHome, ".cache", "nardol");
    }

    public static Path configDir() {
        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfig != null && !xdgConfig.isBlank()) {
            return Path.of(xdgConfig, "nardol");
        }
        String userHome = System.getProperty("user.home", ".");
        if (isMac()) {
            return Path.of(userHome, "Library", "Application Support", "nardol");
        } else if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "nardol");
            }
            return Path.of(userHome, "AppData", "Roaming", "nardol");
        }
        return Path.of(userHome, ".config", "nardol");
    }
}
