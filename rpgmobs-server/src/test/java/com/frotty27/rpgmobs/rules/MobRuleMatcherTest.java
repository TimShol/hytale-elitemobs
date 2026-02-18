package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MobRuleMatcherTest {

    @Test
    void exactMatchWinsOverPrefixAndContains() {
        RPGMobsConfig cfg = new RPGMobsConfig();

        RPGMobsConfig.MobRule exact = new RPGMobsConfig.MobRule();
        exact.matchExact = List.of("Goblin_Duke");

        RPGMobsConfig.MobRule prefix = new RPGMobsConfig.MobRule();
        prefix.matchStartsWith = List.of("Goblin");

        RPGMobsConfig.MobRule contains = new RPGMobsConfig.MobRule();
        contains.matchContains = List.of("Duke");

        Map<String, RPGMobsConfig.MobRule> rules = new LinkedHashMap<>();
        rules.put("exact", exact);
        rules.put("prefix", prefix);
        rules.put("contains", contains);
        cfg.mobsConfig.defaultMobRules = rules;

        MobRuleMatcher.MatchResult result = new MobRuleMatcher().findBestMatch(cfg, "Goblin_Duke");
        assertNotNull(result);
    }

    @Test
    void tieBreakUsesKeyLexicographicOrder() {
        RPGMobsConfig cfg = new RPGMobsConfig();

        RPGMobsConfig.MobRule r1 = new RPGMobsConfig.MobRule();
        r1.matchContains = List.of("goblin");

        RPGMobsConfig.MobRule r2 = new RPGMobsConfig.MobRule();
        r2.matchContains = List.of("goblin");

        Map<String, RPGMobsConfig.MobRule> rules = new LinkedHashMap<>();
        rules.put("b_rule", r1);
        rules.put("a_rule", r2);
        cfg.mobsConfig.defaultMobRules = rules;

        MobRuleMatcher.MatchResult result = new MobRuleMatcher().findBestMatch(cfg, "goblin_scout");
        assertNotNull(result);
    }
}
