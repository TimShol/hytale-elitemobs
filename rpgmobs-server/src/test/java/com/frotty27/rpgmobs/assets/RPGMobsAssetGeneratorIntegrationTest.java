package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.features.RPGMobsFeatureRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RPGMobsAssetGeneratorIntegrationTest {

    @Test
    void generatesTieredAssetsAndCopiesJson(@TempDir Path tempDir) throws IOException {
        RPGMobsConfig cfg = new RPGMobsConfig();

        new RPGMobsFeatureRegistry(null);

        RPGMobsAssetGenerator.generateAll(tempDir, cfg, true);

        Path outputRoot = tempDir.resolve("Server");
        Path t1 = outputRoot.resolve("Entity/Effects/RPGMobs/RPGMobs_Effect_ProjectileResistance_Tier_1.json");
        Path t5 = outputRoot.resolve("Entity/Effects/RPGMobs/RPGMobs_Effect_ProjectileResistance_Tier_5.json");
        Path nonTemplate = outputRoot.resolve("Entity/Trails/RPGMobs/RPGMobs_Trail_ChargeLeap_Blue.json");

        assertTrue(Files.exists(t1), "Tier 1 asset missing at: " + t1.toAbsolutePath());
        assertTrue(Files.exists(t5), "Tier 5 asset missing at: " + t5.toAbsolutePath());
        assertTrue(Files.exists(nonTemplate), "Non-template asset missing at: " + nonTemplate.toAbsolutePath());

        String t1Text = Files.readString(t1, StandardCharsets.UTF_8);
        assertFalse(t1Text.contains("${"), "Tier 1 asset still contains placeholders");
    }
}
