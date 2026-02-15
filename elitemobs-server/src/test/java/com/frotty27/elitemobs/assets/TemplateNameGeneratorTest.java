package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateNameGeneratorTest {

    @Test
    void baseTemplateNameExtraction() {
        String name1 = TemplateNameGenerator.getBaseTemplateNameFromPath("ServerTemplates/Foo/EliteMobs_Leap_Slam.template.json");
        String name2 = TemplateNameGenerator.getBaseTemplateNameFromPath("EliteMobs_Leap_Slam.json");
        String name3 = TemplateNameGenerator.getBaseTemplateNameFromPath("EliteMobs_Leap_Slam");
        String name4 = TemplateNameGenerator.getBaseTemplateNameFromPath("   ");

        assertTrue(name1 != null && name1.contains("EliteMobs_Leap_Slam"));
        assertTrue(name2 != null && name2.contains("EliteMobs_Leap_Slam"));
        assertTrue(name3 != null && name3.contains("EliteMobs_Leap_Slam"));
        assertTrue(name4 == null || name4.isBlank());
    }

    @Test
    void tierSuffixUsesConfigWhenPresent() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        String suffix = TemplateNameGenerator.getTierSuffix(cfg, 2);
        String appended = TemplateNameGenerator.appendTierSuffix("EliteMobs_Leap_Slam", cfg, 2);
        assertTrue(suffix != null && !suffix.isBlank());
        assertTrue(appended != null && appended.contains("EliteMobs_Leap_Slam"));
        assertTrue(appended != null && appended.contains(suffix));
    }

    @Test
    void tierSuffixFallsBackWhenMissing() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        cfg.assetGenerator.tierSuffixes = new String[0];
        String suffix = TemplateNameGenerator.getTierSuffix(cfg, 0);
        assertTrue(suffix != null && !suffix.isBlank());
    }
}
