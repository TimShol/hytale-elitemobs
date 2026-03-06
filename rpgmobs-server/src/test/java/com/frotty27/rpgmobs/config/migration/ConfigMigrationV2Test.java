package com.frotty27.rpgmobs.config.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigrationV2Test {

    @Test
    void skipsWhenBaseDirectoryExists(@TempDir Path tempDir) throws Exception {
        Files.createDirectory(tempDir.resolve("base"));

        boolean result = ConfigMigrationV2.migrateIfNeeded(tempDir);

        assertFalse(result);
    }

    @Test
    void migratesRootFilesToBase(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("spawning.yml"), "spawnChance: 0.5\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("stats.yml"), "healthMultiplier: 2.0\n", StandardCharsets.UTF_8);

        boolean result = ConfigMigrationV2.migrateIfNeeded(tempDir);

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve("base/spawning.yml")));
        assertTrue(Files.exists(tempDir.resolve("base/stats.yml")));
        assertFalse(Files.exists(tempDir.resolve("spawning.yml")));
        assertFalse(Files.exists(tempDir.resolve("stats.yml")));

        String spawningContent = Files.readString(tempDir.resolve("base/spawning.yml"), StandardCharsets.UTF_8);
        assertTrue(spawningContent.contains("spawnChance: 0.5"));
    }

    @Test
    void createsWorldsAndInstancesDirectories(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("spawning.yml"), "spawnChance: 0.5\n", StandardCharsets.UTF_8);

        ConfigMigrationV2.migrateIfNeeded(tempDir);

        assertTrue(Files.isDirectory(tempDir.resolve("worlds")));
        assertTrue(Files.isDirectory(tempDir.resolve("instances")));
    }

    @Test
    void migratesInstancesYmlToIndividualFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("spawning.yml"), "spawnChance: 0.5\n", StandardCharsets.UTF_8);

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> instances = new LinkedHashMap<>();
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("DungeonA", Map.of("enabled", true));
        rules.put("DungeonB", Map.of("enabled", false));
        instances.put("instanceRules", rules);
        root.put("Instances", instances);

        try (Writer writer = Files.newBufferedWriter(tempDir.resolve("instances.yml"), StandardCharsets.UTF_8)) {
            new Yaml().dump(root, writer);
        }

        boolean result = ConfigMigrationV2.migrateIfNeeded(tempDir);

        assertTrue(result);
        assertTrue(Files.exists(tempDir.resolve("instances/DungeonA.yml")));
        assertTrue(Files.exists(tempDir.resolve("instances/DungeonB.yml")));
        assertTrue(Files.exists(tempDir.resolve("instances.yml.migrated")));
        assertFalse(Files.exists(tempDir.resolve("instances.yml")));

        String dungeonA = Files.readString(tempDir.resolve("instances/DungeonA.yml"), StandardCharsets.UTF_8);
        assertTrue(dungeonA.contains("enabled: true"));

        String dungeonB = Files.readString(tempDir.resolve("instances/DungeonB.yml"), StandardCharsets.UTF_8);
        assertTrue(dungeonB.contains("enabled: false"));
    }

    @Test
    void handlesEmptyDirectoryGracefully(@TempDir Path tempDir) {
        boolean result = ConfigMigrationV2.migrateIfNeeded(tempDir);

        assertFalse(result);
        assertFalse(Files.exists(tempDir.resolve("base")));
        assertFalse(Files.exists(tempDir.resolve("worlds")));
        assertFalse(Files.exists(tempDir.resolve("instances")));
    }
}
