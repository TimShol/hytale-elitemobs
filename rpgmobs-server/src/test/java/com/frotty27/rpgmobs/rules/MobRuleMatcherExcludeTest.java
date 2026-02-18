package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobRuleMatcherExcludeTest {

    @Test
    void excludesPreventMatch() {
        RPGMobsConfig cfg = new RPGMobsConfig();

        RPGMobsConfig.MobRule rule = new RPGMobsConfig.MobRule();
        rule.matchContains = List.of("goblin");
        rule.matchExcludes = List.of("boss");

        Map<String, RPGMobsConfig.MobRule> rules = new LinkedHashMap<>();
        rules.put("goblin", rule);
        cfg.mobsConfig.defaultMobRules = rules;

        MobRuleMatcher.MatchResult allowed = new MobRuleMatcher().findBestMatch(cfg, "goblin_scout");
        MobRuleMatcher.MatchResult denied = new MobRuleMatcher().findBestMatch(cfg, "goblin_boss");

        assertNotNull(allowed);
        assertTrue(denied == null);
    }
}
