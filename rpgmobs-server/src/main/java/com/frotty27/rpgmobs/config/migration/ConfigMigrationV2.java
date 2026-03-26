package com.frotty27.rpgmobs.config.migration;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class ConfigMigrationV2 {

    private static final Logger LOGGER = Logger.getLogger(ConfigMigrationV2.class.getName());

    private static final String[] BASE_FILES = {
            "spawning.yml", "stats.yml", "abilities.yml", "loot.yml",
            "gear.yml", "effects.yml", "mobrules.yml"
    };

    private ConfigMigrationV2() {}

    public static boolean migrateIfNeeded(Path modDirectory) {
        Path baseDir = modDirectory.resolve("base");

        if (Files.isDirectory(baseDir)) return false;

        boolean hasOldFiles = false;
        for (String file : BASE_FILES) {
            if (Files.exists(modDirectory.resolve(file))) {
                hasOldFiles = true;
                break;
            }
        }
        if (!hasOldFiles) return false;

        LOGGER.info("Detected old config layout  - migrating to v2 folder structure...");

        try {

            Files.createDirectories(baseDir);
            Files.createDirectories(modDirectory.resolve("worlds"));
            Files.createDirectories(modDirectory.resolve("instances"));

            for (String file : BASE_FILES) {
                Path src = modDirectory.resolve(file);
                if (Files.exists(src)) {
                    Path dest = baseDir.resolve(file);
                    Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Moved " + file + " → base/" + file);
                }
            }

            migrateInstancesYml(modDirectory);

            LOGGER.info("Migration to v2 complete!");
            return true;
        } catch (Exception e) {
            LOGGER.warning("Migration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void migrateInstancesYml(Path modDirectory) {
        Path instancesFile = modDirectory.resolve("instances.yml");
        if (!Files.exists(instancesFile)) return;

        try (Reader reader = Files.newBufferedReader(instancesFile, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map<?, ?> rootMap)) return;

            Map<String, Object> root = toStringKeyMap(rootMap);

            Map<String, Object> instanceRules = null;
            Object instancesGroup = root.get("Instances");
            if (instancesGroup instanceof Map<?, ?> igMap) {
                Map<String, Object> ig = toStringKeyMap(igMap);
                Object rules = ig.get("instanceRules");
                if (rules instanceof Map<?, ?> rulesMap) {
                    instanceRules = toStringKeyMap(rulesMap);
                }
            }

            if (instanceRules == null) {
                Object rules = root.get("instanceRules");
                if (rules instanceof Map<?, ?> rulesMap) {
                    instanceRules = toStringKeyMap(rulesMap);
                }
            }

            if (instanceRules == null || instanceRules.isEmpty()) {
                LOGGER.info("No instance rules found in instances.yml to migrate.");
            } else {
                Path instancesDir = modDirectory.resolve("instances");
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                dumperOptions.setPrettyFlow(true);
                Yaml dumper = new Yaml(dumperOptions);

                for (Map.Entry<String, Object> entry : instanceRules.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    if (!(value instanceof Map<?, ?> ruleMap)) continue;

                    Map<String, Object> ruleData = toStringKeyMap(ruleMap);

                    ruleData.values().removeIf(Objects::isNull);

                    if (ruleData.isEmpty()) continue;

                    Path overlayFile = instancesDir.resolve(name + ".yml");
                    try (Writer writer = Files.newBufferedWriter(overlayFile, StandardCharsets.UTF_8)) {
                        writer.write("# Migrated from instances.yml\n");
                        dumper.dump(ruleData, writer);
                        LOGGER.info("Migrated instance rule: " + name + " → instances/" + name + ".yml");
                    }
                }
            }

            Path backup = modDirectory.resolve("instances.yml.migrated");
            Files.move(instancesFile, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Renamed instances.yml → instances.yml.migrated");

        } catch (Exception e) {
            LOGGER.warning("Failed to migrate instances.yml: " + e.getMessage());
        }
    }

    private static Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null) continue;
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }
}
