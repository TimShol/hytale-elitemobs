package com.frotty27.elitemobs.nameplates;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class EliteMobsNameplateService {

    private static final String DEFAULT_MPC_NAME = "NPC";
    private static final String DEFAULT_FAMILY_KEY = "default";
    private static final java.util.Set<String> NOISE_SEGMENTS = java.util.Set.of(
            "patrol",
            "wander",
            "big",
            "small"
    );

    private static final java.util.Set<String> VARIANT_SEGMENTS = java.util.Set.of(
            "burnt",
            "frost",
            "sand",
            "pirate",
            "incandescent",
            "aberrant",
            "void"
    );

    private static ComponentType<EntityStore, Nameplate> cachedNameplateComponentType;

    private static ComponentType<EntityStore, Nameplate> getNameplateComponentType() {
        // Lazy: during early bootstrap this may be null; later it becomes non-null.
        if (cachedNameplateComponentType == null) {
            cachedNameplateComponentType = Nameplate.getComponentType();
        }
        return cachedNameplateComponentType;
    }

    public void applyOrUpdateNameplate(EliteMobsConfig config, Ref<EntityStore> entityRef, Store<EntityStore> entityStore,
                                       CommandBuffer<EntityStore> commandBuffer, String roleName, int tierIndex) {
        if (config == null) return;
        if (entityRef == null || entityStore == null || commandBuffer == null) return;

        int clampedTierIndex = clampTierIndex(tierIndex);

        // If Nameplate component isn't registered yet, do nothing this tick.
        ComponentType<EntityStore, Nameplate> nameplateComponentType = getNameplateComponentType();
        if (nameplateComponentType == null) return;

        boolean enabled = config.nameplates.nameplatesEnabled 
                && areNameplatesEnabledForTier(config, clampedTierIndex) 
                && passesRoleFilters(config, roleName);

        Nameplate existing = entityStore.getComponent(entityRef, nameplateComponentType);

        if (!enabled) {
            if (existing != null) {
                if (config.debug != null && config.debug.isDebugModeEnabled) {
                    EliteMobsLogger.debug(
                            HytaleLogger.forEnclosingClass(),
                            "[Nameplate] remove: global=%s perTier=%s filtered=%s tier=%d role=%s",
                            EliteMobsLogLevel.INFO,
                            String.valueOf(config.nameplates.nameplatesEnabled),
                            String.valueOf(areNameplatesEnabledForTier(config, clampedTierIndex)),
                            String.valueOf(passesRoleFilters(config, roleName)),
                            clampedTierIndex,
                            String.valueOf(roleName)
                    );
                }
                commandBuffer.removeComponent(entityRef, nameplateComponentType);
            }
            return;
        }

        String fullNameplateText = buildNameplateText(config, roleName, clampedTierIndex);

        if (fullNameplateText.isBlank()) {
            if (existing != null) commandBuffer.removeComponent(entityRef, nameplateComponentType);
            return;
        }

        if (existing != null) {
            if (fullNameplateText.equals(existing.getText())) return;
            existing.setText(fullNameplateText);
            commandBuffer.replaceComponent(entityRef, nameplateComponentType, existing);
            return;
        }

        commandBuffer.putComponent(entityRef, nameplateComponentType, new Nameplate(fullNameplateText));
    }

    public String buildNameplateText(EliteMobsConfig config, String roleName, int clampedTierIndex) {
        if (config == null || !config.nameplates.nameplatesEnabled) return "";
        if (!passesRoleFilters(config, roleName)) return "";
        if (!areNameplatesEnabledForTier(config, clampedTierIndex)) return "";

        String nameplatePrefix = getNameplatePrefixForTier(config, clampedTierIndex);

        EliteMobsConfig.NameplateMode nameplateMode =
                (config.nameplates.nameplateMode != null) ? config.nameplates.nameplateMode : EliteMobsConfig.NameplateMode.RANKED_ROLE;

        String nameplateBody = switch (nameplateMode) {
            case SIMPLE -> joinNonBlank(resolveTierPrefixForRole(config, roleName, clampedTierIndex),
                                        resolveRoleWithoutFamily(roleName)
            );
            case RANKED_ROLE -> joinNonBlank(resolveTierPrefixForRole(config, roleName, clampedTierIndex),
                                             resolveDisplayRoleName(roleName)
            );
        };

        return joinNonBlank(nameplatePrefix, nameplateBody);
    }


    // ------------------------------------------------------------
    // Tier gating helpers
    // ------------------------------------------------------------

    private static boolean areNameplatesEnabledForTier(EliteMobsConfig config, int clampedTierIndex) {
        if (config.nameplates.nameplatesEnabledPerTier == null) return true;
        if (config.nameplates.nameplatesEnabledPerTier.length <= clampedTierIndex) return true;
        return config.nameplates.nameplatesEnabledPerTier[clampedTierIndex];
    }

    private static String getNameplatePrefixForTier(EliteMobsConfig config, int clampedTierIndex) {
        if (config.nameplates.nameplatePrefixPerTier == null) return "";
        if (config.nameplates.nameplatePrefixPerTier.length <= clampedTierIndex) return "";
        return safe(config.nameplates.nameplatePrefixPerTier[clampedTierIndex]);
    }

    // ------------------------------------------------------------
    // Filters (config-driven)
    // ------------------------------------------------------------

    private static boolean passesRoleFilters(EliteMobsConfig config, String roleName) {
        String roleNameLowercase = (roleName == null) ? "" : roleName.toLowerCase(Locale.ROOT);

        // Deny first
        List<String> denyList = config.nameplates.nameplateMustNotContainRoles;
        if (denyList != null) {
            for (String forbiddenFragment : denyList) {
                if (forbiddenFragment == null || forbiddenFragment.isBlank()) continue;
                if (roleNameLowercase.contains(forbiddenFragment.toLowerCase(Locale.ROOT))) return false;
            }
        }

        // Allow any-of. If no allow rules => allow all.
        List<String> allowList = config.nameplates.nameplateMustContainRoles;
        if (allowList == null || allowList.isEmpty()) return true;

        boolean hasAnyAllowRule = false;
        for (String requiredFragment : allowList) {
            if (requiredFragment == null || requiredFragment.isBlank()) continue;

            hasAnyAllowRule = true;
            if (roleNameLowercase.contains(requiredFragment.toLowerCase(Locale.ROOT))) return true;
        }

        return !hasAnyAllowRule;
    }

    // ------------------------------------------------------------
    // Formatting helpers
    // ------------------------------------------------------------

    private static String safe(String text) {
        return (text == null) ? "" : text.trim();
    }

    private static String joinNonBlank(String left, String right) {
        String leftText = safe(left);
        String rightText = safe(right);

        if (leftText.isEmpty()) return rightText;
        if (rightText.isEmpty()) return leftText;

        return leftText + " " + rightText;
    }

 

    // ------------------------------------------------------------
    // Name resolution (family, role, tier prefix)
    // ------------------------------------------------------------

    private static String resolveRoleWithoutFamily(String roleName) {
        if (roleName == null || roleName.isBlank()) return DEFAULT_MPC_NAME;

        String[] segments = roleName.split("_");
        if (segments.length <= 1) return prettifyString(roleName);

        if (segments.length == 2 && isVariantSegment(segments[1])) {
            return prettifyString(segments[0]);
        }

        return prettifyString(joinSegments(segments, 1, segments.length));
    }


    private static String resolveDisplayRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) return DEFAULT_MPC_NAME;

        String[] segments = roleName.split("_");
        if (segments.length == 0) return DEFAULT_MPC_NAME;

        int endExclusive = segments.length;
        while (endExclusive > 0 && isNoiseSegment(segments[endExclusive - 1])) {
            endExclusive--;
        }
        if (endExclusive <= 0) return DEFAULT_MPC_NAME;

        int startInclusive = 1;
        if (endExclusive > 1 && isVariantSegment(segments[1])) {
            startInclusive = 2;
        }

        if (endExclusive <= startInclusive) {
            if (segments.length >= 2 && startInclusive == 2 && isVariantSegment(segments[1])) {
                return prettifyString(segments[0]);
            }

            return prettifyString(joinSegments(segments, 0, endExclusive));
        }

        return prettifyString(joinSegments(segments, startInclusive, endExclusive));
    }

    private static boolean isNoiseSegment(String segment) {
        if (segment == null) return true;

        String segmentLowercase = segment.toLowerCase(Locale.ROOT);
        return NOISE_SEGMENTS.contains(segmentLowercase);
    }

    private static boolean isVariantSegment(String segment) {
        if (segment == null) return false;

        String segmentLowercase = segment.toLowerCase(Locale.ROOT);
        return VARIANT_SEGMENTS.contains(segmentLowercase);
    }

    private static String joinSegments(String[] segments, int startInclusive, int endExclusive) {
        StringBuilder joined = new StringBuilder();

        for (int index = startInclusive; index < endExclusive; index++) {
            String segment = segments[index];
            if (segment == null || segment.isBlank()) continue;

            if (!joined.isEmpty()) joined.append('_');
            joined.append(segment);
        }

        return joined.toString();
    }

    private static String prettifyString(String text) {
        if (text == null || text.isBlank()) return DEFAULT_MPC_NAME;

        String[] parts = text.replace('_', ' ').split("\\s+");
        StringBuilder pretty = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;

            if (!pretty.isEmpty()) pretty.append(' ');
            pretty.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) pretty.append(part.substring(1).toLowerCase(Locale.ROOT));
        }

        return pretty.toString();
    }

    private static String resolveTierPrefixForRole(EliteMobsConfig config, String roleName, int tierIndex) {
        Map<String, List<String>> tierPrefixesByFamily = config.nameplates.tierPrefixesByFamily;
        if (tierPrefixesByFamily == null || tierPrefixesByFamily.isEmpty()) return "";

        String familyKey = classifyFamily(roleName);

        List<String> tierPrefixes = tierPrefixesByFamily.get(familyKey);
        if (tierPrefixes == null) tierPrefixes = tierPrefixesByFamily.get(DEFAULT_FAMILY_KEY);
        if (tierPrefixes == null || tierPrefixes.isEmpty()) return "";

        int clampedTierIndex = clampTierIndex(tierIndex);
        if (clampedTierIndex < 0 || clampedTierIndex >= tierPrefixes.size()) return "";

        return safe(tierPrefixes.get(clampedTierIndex));
    }

    private static String classifyFamily(String roleName) {
        if (roleName == null) return DEFAULT_FAMILY_KEY;
        String roleNameLowercase = roleName.toLowerCase(Locale.ROOT);

        if (roleNameLowercase.contains("_void") || roleNameLowercase.startsWith("crawler_") || roleNameLowercase.startsWith(
                "eye_") || roleNameLowercase.startsWith("spawn_") || roleNameLowercase.startsWith("spectre_") || roleNameLowercase.startsWith(
                "scythe_")) {
            return "void";
        }

        if (roleNameLowercase.startsWith("zombie")) {
            if (roleNameLowercase.contains("_burnt")) return "zombie_burnt";
            if (roleNameLowercase.contains("_frost")) return "zombie_frost";
            if (roleNameLowercase.contains("_sand")) return "zombie_sand";
            if (roleNameLowercase.contains("_aberrant")) return "zombie_aberrant";
            return "zombie";
        }

        if (roleNameLowercase.startsWith("skeleton")) {
            if (roleNameLowercase.contains("_burnt")) return "skeleton_burnt";
            if (roleNameLowercase.contains("_frost")) return "skeleton_frost";
            if (roleNameLowercase.contains("_sand")) return "skeleton_sand";
            if (roleNameLowercase.contains("_pirate")) return "skeleton_pirate";
            if (roleNameLowercase.contains("_incandescent")) return "skeleton_incandescent";
            return "skeleton";
        }

        if (roleNameLowercase.startsWith("goblin")) return "goblin";
        if (roleNameLowercase.startsWith("trork")) return "trork";
        if (roleNameLowercase.startsWith("outlander")) return "outlander";

        return DEFAULT_FAMILY_KEY;
    }
}
