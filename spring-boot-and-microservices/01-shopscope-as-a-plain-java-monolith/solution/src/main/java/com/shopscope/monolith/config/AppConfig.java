package com.shopscope.monolith.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration by hand: a {@code .properties} file on the classpath, optionally overridden by a file
 * named in the {@code shopscope.config} system property. Every value is parsed where it is read, with
 * no validation, no types and no defaults beyond what each getter invents. Problem 4 of the register.
 */
public final class AppConfig {

    private final Properties properties = new Properties();

    private AppConfig() {
    }

    public static AppConfig load() {
        AppConfig config = new AppConfig();
        try (InputStream in = AppConfig.class.getResourceAsStream("/shopscope.properties")) {
            if (in == null) {
                throw new IllegalStateException("shopscope.properties is missing from the classpath");
            }
            config.properties.load(in);
            String override = System.getProperty("shopscope.config");
            if (override != null) {
                try (InputStream file = Files.newInputStream(Path.of(override))) {
                    config.properties.load(file);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return config;
    }

    public int port() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }

    public String country() {
        return properties.getProperty("shop.country", "IN");
    }

    public String currency() {
        return properties.getProperty("shop.currency", "INR");
    }

    public String channel() {
        return properties.getProperty("shop.channel", "web");
    }

    public long catalogSeedDelayMillis() {
        return Long.parseLong(properties.getProperty("catalog.seed.delay-ms", "0"));
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
