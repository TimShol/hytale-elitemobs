package com.frotty27.elitemobs.utils;

import com.frotty27.elitemobs.assets.TemplateNameGenerator;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateNameGeneratorExtraTest {

    @Test
    void tierSuffixSanitizesInvalidCharacters() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        cfg.assetGenerator.tierSuffixes = new String[]{"T#1", "T@2", "T 3", "T*4", "T/5"};

        for (int i = 0; i < 5; i++) {
            String suffix = TemplateNameGenerator.getTierSuffix(cfg, i);
            assertTrue(suffix != null && !suffix.isBlank());
            assertTrue(suffix.matches("[A-Za-z0-9_]+"));
        }
    }
}
