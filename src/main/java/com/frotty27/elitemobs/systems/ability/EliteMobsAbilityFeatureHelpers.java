package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.assets.TemplateNameGenerator;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.AbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonAbilityConfig;
import com.frotty27.elitemobs.config.EliteMobsConfig.SummonMarkerEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class EliteMobsAbilityFeatureHelpers {

    private static final String DEFAULT_IDENTIFIER = "default";

    private EliteMobsAbilityFeatureHelpers() {
    }

    static Set<String> getSummonRoleNames(SummonAbilityConfig config, String normalizedIdentifier) {
        if (config == null) return Set.of();
        List<SummonMarkerEntry> entries = null;
        if (config.spawnMarkerEntriesByRole != null && normalizedIdentifier != null) {
            entries = config.spawnMarkerEntriesByRole.get(normalizedIdentifier);
        }
        if ((entries == null || entries.isEmpty()) && config.spawnMarkerEntriesByRole != null) {
            entries = config.spawnMarkerEntriesByRole.get("default");
            if (entries == null || entries.isEmpty()) {
                entries = config.spawnMarkerEntriesByRole.get("Default");
            }
        }
        if (entries == null || entries.isEmpty()) {
            entries = config.spawnMarkerEntries;
        }
        if (entries == null || entries.isEmpty()) return Set.of();

        HashSet<String> roleNames = new HashSet<>();
        for (SummonMarkerEntry entry : entries) {
            if (entry == null || entry.Name == null) continue;
            String name = entry.Name.trim();
            if (!name.isEmpty()) roleNames.add(name);
        }
        return roleNames;
    }

    static String resolveSummonRoleIdentifier(AbilityConfig summonConfig, String roleName) {
        if (!(summonConfig instanceof SummonAbilityConfig s)) return DEFAULT_IDENTIFIER;
        if (roleName == null || roleName.isBlank()) return DEFAULT_IDENTIFIER;
        if (s.roleIdentifiers == null || s.roleIdentifiers.isEmpty()) return DEFAULT_IDENTIFIER;

        String roleLower = roleName.toLowerCase();
        for (String identifier : s.roleIdentifiers) {
            if (identifier == null || identifier.isBlank()) continue;
            if (roleLower.contains(identifier.toLowerCase())) {
                return identifier;
            }
        }
        return DEFAULT_IDENTIFIER;
    }

    static String buildRoleSpecificSummonRootId(EliteMobsConfig config, String normalizedRoleIdentifier, int tierIndex) {
        if (config == null || normalizedRoleIdentifier == null || normalizedRoleIdentifier.isBlank()) return null;
        String baseId = "EliteMobs_Ability_UndeadSummon_RootInteraction_" + normalizedRoleIdentifier;
        return TemplateNameGenerator.appendTierSuffix(baseId, config, tierIndex);
    }
}
