package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateNameGeneratorTest {

    @Test
    void baseTemplateNameExtraction() {
        String name1 = TemplateNameGenerator.getBaseTemplateNameFromPath(
                "ServerTemplates/Foo/RPGMobs_Leap_Slam.template.json");
        String name2 = TemplateNameGenerator.getBaseTemplateNameFromPath("RPGMobs_Leap_Slam.json");
        String name3 = TemplateNameGenerator.getBaseTemplateNameFromPath("RPGMobs_Leap_Slam");
        String name4 = TemplateNameGenerator.getBaseTemplateNameFromPath("   ");

        assertTrue(name1 != null && name1.contains("RPGMobs_Leap_Slam"));
        assertTrue(name2 != null && name2.contains("RPGMobs_Leap_Slam"));
        assertTrue(name3 != null && name3.contains("RPGMobs_Leap_Slam"));
        assertTrue(name4 == null || name4.isBlank());
    }

    @Test
    void tierSuffixUsesConfigWhenPresent() {
        RPGMobsConfig cfg = new RPGMobsConfig();
        String suffix = TemplateNameGenerator.getTierSuffix(cfg, 2);
        String appended = TemplateNameGenerator.appendTierSuffix("RPGMobs_Leap_Slam", cfg, 2);
        assertTrue(suffix != null && !suffix.isBlank());
        assertTrue(appended != null && appended.contains("RPGMobs_Leap_Slam"));
        assertTrue(appended != null && appended.contains(suffix));
    }

    @Test
    void tierSuffixFallsBackWhenMissing() {
        RPGMobsConfig cfg = new RPGMobsConfig();
        cfg.assetGenerator.tierSuffixes = new String[0];
        String suffix = TemplateNameGenerator.getTierSuffix(cfg, 0);
        assertTrue(suffix != null && !suffix.isBlank());
    }
}
