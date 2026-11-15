package com.explodenot.gli.cli;

import com.explodenot.gli.util.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Config {
        public String mode = "tcp";
        public Integer samples = 3;
        public Integer timeoutMs = 3000;
        public Integer concurrency = 64;
        public Double lat = null;
        public Double lon = null;
        public String theme = "default";
        public Boolean ascii = false;
        public String colorDepth = "truecolor";
    }

    public static Path path() {
        return Platform.configDir().resolve("config.json");
    }

    public static Config load() {
        Path p = path();
        if (!Files.exists(p)) return new Config();
        try {
            String str = Files.readString(p);
            Config cfg = GSON.fromJson(str, Config.class);
            return cfg != null ? cfg : new Config();
        } catch (IOException e) {
            return new Config();
        }
    }

    public static void save(Config cfg) throws IOException {
        Path p = path();
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        Files.writeString(p, GSON.toJson(cfg));
    }

    public static void reset() throws IOException {
        Path p = path();
        Files.deleteIfExists(p);
    }
}
