package com.frotty27.rpgmobs.services;

import com.frotty27.nameplatebuilder.api.NameplateAPI;
import com.frotty27.nameplatebuilder.api.NameplateData;
import com.frotty27.nameplatebuilder.api.SegmentTarget;
import com.frotty27.rpgmobs.components.ability.*;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.NameplateHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public final class RPGMobsNameplateService {

    public static final String SEGMENT_PREFIX = "elite-tier-prefix";
    public static final String SEGMENT_TIER = "elite-tier";
    public static final String SEGMENT_NAME = "elite-npc-type";
    public static final String SEGMENT_DEBUG = "elite-debug-info";

    public static final String DISPLAY_PREFIX = "Elite Tier Prefix";
    public static final String DISPLAY_TIER = "Elite Tier";
    public static final String DISPLAY_NAME = "Elite Type";
    public static final String DISPLAY_DEBUG = "Debug Info";

    public static final String EXAMPLE_PREFIX = "• • •";
    public static final String EXAMPLE_TIER = "Common, ...";
    public static final String EXAMPLE_NAME = "Zombie, ...";
    public static final String EXAMPLE_DEBUG = "CL MSS DR | MSS";

    public void describeSegments(JavaPlugin plugin) {
        NameplateAPI.describe(plugin, SEGMENT_PREFIX, DISPLAY_PREFIX, SegmentTarget.NPCS, EXAMPLE_PREFIX);
        NameplateAPI.describe(plugin, SEGMENT_TIER, DISPLAY_TIER, SegmentTarget.NPCS, EXAMPLE_TIER);
        NameplateAPI.describe(plugin, SEGMENT_NAME, DISPLAY_NAME, SegmentTarget.NPCS, EXAMPLE_NAME);
        NameplateAPI.describe(plugin, SEGMENT_DEBUG, DISPLAY_DEBUG, SegmentTarget.NPCS, EXAMPLE_DEBUG);
    }

    public void applyOrUpdateNameplate(RPGMobsConfig config,
                                       @Nullable ResolvedConfig resolved,
                                       Ref<EntityStore> entityRef, Store<EntityStore> entityStore,
                                       CommandBuffer<EntityStore> commandBuffer, String roleName, int tierIndex) {

        Map<String, List<String>> familyPrefixes = (resolved != null) ? resolved.tierPrefixesByFamily : null;
        applyOrUpdateNameplate(config, resolved, familyPrefixes, entityRef, entityStore, commandBuffer, roleName, tierIndex);
    }

    private void applyOrUpdateNameplate(RPGMobsConfig config,
                                        @Nullable ResolvedConfig resolved,
                                        @Nullable Map<String, List<String>> resolvedFamilyPrefixes,
                                        Ref<EntityStore> entityRef, Store<EntityStore> entityStore,
                                        CommandBuffer<EntityStore> commandBuffer, String roleName, int tierIndex) {
        if (config == null) return;
        if (entityRef == null || entityStore == null || commandBuffer == null) return;

        int clampedTierIndex = clampTierIndex(tierIndex);

        boolean enableNameplates = resolved != null ? resolved.enableNameplates : config.nameplatesConfig.enableMobNameplates;
        boolean tierEnabled = resolved != null
                ? (resolved.nameplateTierEnabled != null && clampedTierIndex < resolved.nameplateTierEnabled.length
                   && resolved.nameplateTierEnabled[clampedTierIndex])
                : areNameplatesEnabledForTier(config, clampedTierIndex);

        boolean enabled = enableNameplates && tierEnabled;

        if (!enabled) {
            removeAllSegments(entityStore, entityRef);
            return;
        }

        String prefixText = resolved != null && resolved.nameplatePrefixPerTier != null
                && clampedTierIndex < resolved.nameplatePrefixPerTier.length
                ? safe(resolved.nameplatePrefixPerTier[clampedTierIndex])
                : getNameplatePrefixForTier(config, clampedTierIndex);

        String tierText = resolveTierPrefixForRole(config, resolvedFamilyPrefixes, roleName, clampedTierIndex);

        String nameText = resolved != null
                ? resolveNameTextFromMode(resolved.nameplateMode, roleName)
                : resolveNameText(config, roleName);

        if (prefixText.isBlank() && tierText.isBlank() && nameText.isBlank()) {
            removeAllSegments(entityStore, entityRef);
            return;
        }

        ComponentType<EntityStore, NameplateData> type = NameplateAPI.getComponentType();
        NameplateData data = entityStore.getComponent(entityRef, type);
        boolean isNew = data == null;
        if (isNew) {
            data = new NameplateData();
        }

        setOrRemove(data, SEGMENT_PREFIX, prefixText);
        setOrRemove(data, SEGMENT_TIER, tierText);
        setOrRemove(data, SEGMENT_NAME, nameText);

        if (isNew) {
            commandBuffer.putComponent(entityRef, type, data);
        }
    }

    private static void setOrRemove(NameplateData data, String segmentId, String text) {
        if (text.isBlank()) {
            data.removeText(segmentId);
        } else {
            data.setText(segmentId, text);
        }
    }

    public static void removeAllSegments(Store<EntityStore> entityStore, Ref<EntityStore> entityRef) {
        ComponentType<EntityStore, NameplateData> type = NameplateAPI.getComponentType();
        NameplateData data = entityStore.getComponent(entityRef, type);
        if (data != null) {
            data.removeText(SEGMENT_PREFIX);
            data.removeText(SEGMENT_TIER);
            data.removeText(SEGMENT_NAME);
            data.removeText(SEGMENT_DEBUG);
        }
    }

    public void updateDebugSegment(RPGMobsPlugin plugin, Ref<EntityStore> entityRef,
                                    Store<EntityStore> entityStore,
                                    CommandBuffer<EntityStore> commandBuffer,
                                    boolean debugEnabled) {
        ComponentType<EntityStore, NameplateData> type = NameplateAPI.getComponentType();
        NameplateData data = entityStore.getComponent(entityRef, type);
        if (data == null) return;

        if (!debugEnabled) {
            data.removeText(SEGMENT_DEBUG);
            return;
        }

        var tier = entityStore.getComponent(entityRef, plugin.getRPGMobsComponentType());
        int tierDisplay = (tier != null) ? tier.tierIndex + 1 : 0;

        var sb = new StringBuilder();

        List<String> enabledAbilities = new ArrayList<>();
        ChargeLeapAbilityComponent cl = entityStore.getComponent(entityRef, plugin.getChargeLeapAbilityComponentType());
        if (cl != null && cl.abilityEnabled) enabledAbilities.add("CL");
        HealLeapAbilityComponent hl = entityStore.getComponent(entityRef, plugin.getHealLeapAbilityComponentType());
        if (hl != null && hl.abilityEnabled) enabledAbilities.add("HL");
        SummonUndeadAbilityComponent su = entityStore.getComponent(entityRef, plugin.getSummonUndeadAbilityComponentType());
        if (su != null && su.abilityEnabled) enabledAbilities.add("SU");
        DodgeRollAbilityComponent dr = entityStore.getComponent(entityRef, plugin.getDodgeRollAbilityComponentType());
        if (dr != null && dr.abilityEnabled) enabledAbilities.add("DR");
        MultiSlashShortComponent msShort = entityStore.getComponent(entityRef, plugin.getMultiSlashShortComponentType());
        if (msShort != null && msShort.abilityEnabled) enabledAbilities.add("MSS");
        MultiSlashMediumComponent msMedium = entityStore.getComponent(entityRef, plugin.getMultiSlashMediumComponentType());
        if (msMedium != null && msMedium.abilityEnabled) enabledAbilities.add("MSM");
        MultiSlashLongComponent msLong = entityStore.getComponent(entityRef, plugin.getMultiSlashLongComponentType());
        if (msLong != null && msLong.abilityEnabled) enabledAbilities.add("MSL");
        EnrageAbilityComponent en = entityStore.getComponent(entityRef, plugin.getEnrageAbilityComponentType());
        if (en != null && en.abilityEnabled) enabledAbilities.add("EN");
        VolleyAbilityComponent vl = entityStore.getComponent(entityRef, plugin.getVolleyAbilityComponentType());
        if (vl != null && vl.abilityEnabled) enabledAbilities.add("VL");

        sb.append(String.join(" ", enabledAbilities));

        RPGMobsAbilityLockComponent lock = entityStore.getComponent(entityRef, plugin.getAbilityLockComponentType());
        if (lock != null && lock.activeAbilityId != null) {
            sb.append(" | >").append(abbreviateAbilityId(lock.activeAbilityId));
        } else if (lock != null && lock.globalCooldownTicksRemaining > 0) {
            sb.append(" | cooldown ").append(lock.globalCooldownTicksRemaining / 30).append("s");
        }

        setOrRemove(data, SEGMENT_DEBUG, sb.toString());
    }

    private static String abbreviateAbilityId(String abilityId) {
        return switch (abilityId) {
            case "charge_leap" -> "CL";
            case "heal_leap" -> "HL";
            case "undead_summon" -> "SU";
            case "dodge_roll" -> "DR";
            case "multi_slash_short" -> "MSS";
            case "multi_slash_medium" -> "MSM";
            case "multi_slash_long" -> "MSL";
            case "enrage" -> "EN";
            case "volley" -> "VL";
            default -> abilityId;
        };
    }

    private static String resolveNameTextFromMode(@Nullable String modeName, String roleName) {
        if ("SIMPLE".equalsIgnoreCase(modeName)) return NameplateHelpers.resolveRoleWithoutFamily(roleName);
        return NameplateHelpers.resolveDisplayRoleName(roleName);
    }

    private static String resolveNameText(RPGMobsConfig config, String roleName) {
        RPGMobsConfig.NameplateMode nameplateMode = (config.nameplatesConfig.nameplateMode != null) ? config.nameplatesConfig.nameplateMode : RPGMobsConfig.NameplateMode.RANKED_ROLE;

        return switch (nameplateMode) {
            case SIMPLE -> NameplateHelpers.resolveRoleWithoutFamily(roleName);
            case RANKED_ROLE -> NameplateHelpers.resolveDisplayRoleName(roleName);
        };
    }

    private static boolean areNameplatesEnabledForTier(RPGMobsConfig config, int clampedTierIndex) {
        if (config.nameplatesConfig.mobNameplatesEnabledPerTier == null) return true;
        if (config.nameplatesConfig.mobNameplatesEnabledPerTier.length <= clampedTierIndex) return true;
        return config.nameplatesConfig.mobNameplatesEnabledPerTier[clampedTierIndex];
    }

    private static String getNameplatePrefixForTier(RPGMobsConfig config, int clampedTierIndex) {
        if (config.nameplatesConfig.monNameplatePrefixPerTier == null) return "";
        if (config.nameplatesConfig.monNameplatePrefixPerTier.length <= clampedTierIndex) return "";
        return safe(config.nameplatesConfig.monNameplatePrefixPerTier[clampedTierIndex]);
    }

    private static String safe(String text) {
        return (text == null) ? "" : text.trim();
    }

    private static String resolveTierPrefixForRole(RPGMobsConfig config,
                                                    @Nullable Map<String, List<String>> resolvedFamilyPrefixes,
                                                    String roleName, int tierIndex) {
        Map<String, List<String>> tierPrefixesByFamily = resolvedFamilyPrefixes != null
                ? resolvedFamilyPrefixes
                : config.nameplatesConfig.defaultedTierPrefixesByFamily;
        if (tierPrefixesByFamily == null || tierPrefixesByFamily.isEmpty()) return "";

        String familyKey = NameplateHelpers.classifyFamily(roleName, tierPrefixesByFamily);

        List<String> tierPrefixes = tierPrefixesByFamily.get(familyKey);
        if (tierPrefixes == null) tierPrefixes = tierPrefixesByFamily.get("default");
        if (tierPrefixes == null || tierPrefixes.isEmpty()) return "";

        int clampedTierIndex = clampTierIndex(tierIndex);
        if (clampedTierIndex < 0 || clampedTierIndex >= tierPrefixes.size()) return "";

        return safe(tierPrefixes.get(clampedTierIndex));
    }

}
