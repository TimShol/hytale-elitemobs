package com.frotty27.elitemobs.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobRuleMatcherExcludeTest {

    @Test
    void excludesPreventMatch() {
        EliteMobsConfig cfg = new EliteMobsConfig();

        EliteMobsConfig.MobRule rule = new EliteMobsConfig.MobRule();
        rule.matchContains = List.of("goblin");
        rule.matchExcludes = List.of("boss");

        Map<String, EliteMobsConfig.MobRule> rules = new LinkedHashMap<>();
        rules.put("goblin", rule);
        cfg.mobsConfig.defaultMobRules = rules;

        MobRuleMatcher.MatchResult allowed = new MobRuleMatcher().findBestMatch(cfg, "goblin_scout");
        MobRuleMatcher.MatchResult denied = new MobRuleMatcher().findBestMatch(cfg, "goblin_boss");

        assertNotNull(allowed);
        assertTrue(denied == null);
    }
}
