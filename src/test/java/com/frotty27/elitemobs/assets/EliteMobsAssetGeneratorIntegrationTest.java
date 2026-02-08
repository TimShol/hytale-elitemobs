package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.features.EliteMobsFeatureRegistry;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EliteMobsAssetGeneratorIntegrationTest {

    @Test
    void generatesTieredAssetsAndCopiesJson(@TempDir Path tempDir) throws IOException {
        EliteMobsConfig cfg = new EliteMobsConfig();
        
        new EliteMobsFeatureRegistry(null); // Initializes the singleton instance

        EliteMobsAssetGenerator.generateAll(tempDir, cfg, true);

        Path outputRoot = tempDir.resolve("Server");
        Path t1 = outputRoot.resolve("Entity/Effects/EliteMobs/EliteMobs_Effect_ProjectileResistance_Tier_1.json");
        Path t5 = outputRoot.resolve("Entity/Effects/EliteMobs/EliteMobs_Effect_ProjectileResistance_Tier_5.json");
        Path nonTemplate = outputRoot.resolve("Entity/Trails/EliteMobs/EliteMobs_Trail_ChargeLeap_Blue.json");

        assertTrue(Files.exists(t1), "Tier 1 asset missing at: " + t1.toAbsolutePath());
        assertTrue(Files.exists(t5), "Tier 5 asset missing at: " + t5.toAbsolutePath());
        assertTrue(Files.exists(nonTemplate), "Non-template asset missing at: " + nonTemplate.toAbsolutePath());

        String t1Text = Files.readString(t1, StandardCharsets.UTF_8);
        assertFalse(t1Text.contains("${"), "Tier 1 asset still contains placeholders");
    }
}
