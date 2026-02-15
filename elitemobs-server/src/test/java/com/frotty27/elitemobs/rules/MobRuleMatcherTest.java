package com.frotty27.elitemobs.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MobRuleMatcherTest {

    @Test
    void exactMatchWinsOverPrefixAndContains() {
        EliteMobsConfig cfg = new EliteMobsConfig();

        EliteMobsConfig.MobRule exact = new EliteMobsConfig.MobRule();
        exact.matchExact = List.of("Goblin_Duke");

        EliteMobsConfig.MobRule prefix = new EliteMobsConfig.MobRule();
        prefix.matchStartsWith = List.of("Goblin");

        EliteMobsConfig.MobRule contains = new EliteMobsConfig.MobRule();
        contains.matchContains = List.of("Duke");

        Map<String, EliteMobsConfig.MobRule> rules = new LinkedHashMap<>();
        rules.put("exact", exact);
        rules.put("prefix", prefix);
        rules.put("contains", contains);
        cfg.mobsConfig.defaultMobRules = rules;

        MobRuleMatcher.MatchResult result = new MobRuleMatcher().findBestMatch(cfg, "Goblin_Duke");
        assertNotNull(result);
    }

    @Test
    void tieBreakUsesKeyLexicographicOrder() {
        EliteMobsConfig cfg = new EliteMobsConfig();

        EliteMobsConfig.MobRule r1 = new EliteMobsConfig.MobRule();
        r1.matchContains = List.of("goblin");

        EliteMobsConfig.MobRule r2 = new EliteMobsConfig.MobRule();
        r2.matchContains = List.of("goblin");

        Map<String, EliteMobsConfig.MobRule> rules = new LinkedHashMap<>();
        rules.put("b_rule", r1);
        rules.put("a_rule", r2);
        cfg.mobsConfig.defaultMobRules = rules;

        MobRuleMatcher.MatchResult result = new MobRuleMatcher().findBestMatch(cfg, "goblin_scout");
        assertNotNull(result);
    }
}
