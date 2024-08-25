package io.github.prismwork.emitrades.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.prismwork.emitrades.EMITradesPlugin;

import java.io.*;
import java.nio.file.Files;

public class EMITradesConfig {
    @SuppressWarnings({"SwitchStatementWithTooFewBranches"})
    public static Config load(File file) {
        if (!file.getName().endsWith(".json"))
            throw new RuntimeException("Failed to read config");
        Config cfg = null;
        if (file.exists()) {

            try (JsonReader reader = new JsonReader(Files.newBufferedReader(file.toPath()))) {
                cfg = new Config();
                reader.beginObject();
                while (reader.hasNext()) {
                    String nextName = reader.nextName();
                    switch (nextName) {
                        case "enable3DVillagerModelInRecipes"
                                -> cfg.enable3DVillagerModelInRecipes = reader.nextBoolean();
                        default -> reader.skipValue();
                    }
                }
                reader.endObject();
                return cfg;
            } catch (IOException e) {
                EMITradesPlugin.LOGGER.error("Failed to parse config", e);
            }
        }
        if (cfg == null) cfg = new Config();
        save(file, cfg);
        return cfg;
    }

    public static void save(File file, Config cfg) {
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(file.toPath()))) {
            writer.beginObject();
            writer.name("_comment").value("Declares whether the villager entity model is shown in the recipe UI.");
            writer.name("enable3DVillagerModelInRecipes").value(cfg.enable3DVillagerModelInRecipes);
            writer.endObject();
        } catch (IOException e) {
            EMITradesPlugin.LOGGER.error("Failed to save config", e);
        }
    }

    public static class Config {
        public boolean enable3DVillagerModelInRecipes = true;
    }
}
