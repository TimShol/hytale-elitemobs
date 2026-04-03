package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.CombatStyle;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class CaeConfigGenerator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String CATEGORY_PREFIX = "category:";

    public static final Set<String> NON_HUMANOID_PREFIXES = Set.of(
            "bear", "crocodile", "golem", "firesteel", "scarak", "cactee",
            "rat", "deer", "chicken", "cow", "frog", "sabertooth", "raptor",
            "kweebec", "scarab", "snake", "spider", "wolf", "boar", "bird",
            "penguin", "pig", "sheep", "bat", "fish", "turtle", "butterfly",
            "hedera", "moose", "crawler", "eye", "spawn_void", "spectre",
            "antelope", "archaeopteryx", "horse", "donkey", "cat", "dog",
            "parrot", "rabbit", "fox", "owl", "vulture", "jellyfish",
            "crab", "squid", "eel", "stingray"
    );

    public static boolean isNonHumanoid(String roleName) {
        String lower = roleName.toLowerCase();
        for (String prefix : NON_HUMANOID_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        return false;
    }

    private static final Map<String, String> FACTION_TO_STYLE = Map.of(
            "Skeleton", "Disciplined",
            "Trork", "Berserker",
            "Outlander", "Tactical",
            "Goblin", "Chaotic"
    );

    private CaeConfigGenerator() {
    }

    public static GenerationResult generateAll(Path outputDir, RPGMobsConfig config) {
        var combatAI = config.combatAIConfig;
        var mobRules = config.mobsConfig.defaultMobRules;
        int totalFiles = 0;
        int filesWritten = 0;
        int filesDeleted = 0;

        Path caeDir = outputDir.resolve("NPC/Balancing/RPGMobs");
        Path rolesDir = outputDir.resolve("NPC/Roles/RPGMobs");

        try {
            Files.createDirectories(caeDir);
            Files.createDirectories(rolesDir);
            cleanupOldEliteDir(outputDir.resolve("NPC/Roles/RPGMobs/Elite"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create CAE output directories", e);
        }

        Set<String> expectedCaeFiles = new HashSet<>();
        Set<String> expectedRoleFiles = new HashSet<>();
        Set<String> generatedCaeConfigs = new HashSet<>();

        for (var entry : mobRules.entrySet()) {
            var mobRuleKey = entry.getKey();
            var mobRule = entry.getValue();
            if (!mobRule.enabled) continue;

            if (isNonHumanoid(mobRuleKey)) continue;

            var faction = detectFaction(mobRuleKey, mobRule.combatStyle);
            var styleName = FACTION_TO_STYLE.getOrDefault(faction, "Tactical");
            var factionStyle = combatAI.factionStyles.get(styleName);
            if (factionStyle == null) continue;

            var weaponCategories = resolveWeaponCategories(mobRule, combatAI.weaponParams);

            for (var weaponName : weaponCategories) {
                var weaponParams = combatAI.weaponParams.get(weaponName);
                if (weaponParams == null) continue;
                if (weaponParams.isRanged) continue;

                for (int tierIdx = 0; tierIdx < 5; tierIdx++) {
                    var tier = safeTierBehavior(combatAI.tierBehaviors, tierIdx);
                    int tierNum = tierIdx + 1;

                    String caeName = "CAE_RPGMobs_" + styleName + "_" + weaponName + "_T" + tierNum;
                    String caeFileName = caeName + ".json";
                    expectedCaeFiles.add(caeFileName);

                    if (generatedCaeConfigs.add(caeName)) {
                        var caeJson = buildCaeConfig(combatAI, factionStyle, tier, weaponParams, weaponName, faction);
                        if (writeJsonIfChanged(caeDir.resolve(caeFileName), caeJson)) filesWritten++;
                        totalFiles++;
                    }

                    var roleFileName = "RPGMobs_" + mobRuleKey + "_" + weaponName + "_T" + tierNum + ".json";
                    expectedRoleFiles.add(roleFileName);

                    var roleJson = buildRoleVariant(faction, mobRuleKey, weaponName, tierNum, caeName, factionStyle, tier);
                    if (writeJsonIfChanged(rolesDir.resolve(roleFileName), roleJson)) filesWritten++;
                    totalFiles++;
                }
            }
        }

        filesDeleted += deleteStaleFiles(caeDir, "CAE_RPGMobs_", expectedCaeFiles, Set.of());
        filesDeleted += deleteStaleFiles(rolesDir, "RPGMobs_", expectedRoleFiles,
                Set.of("RPGMobs_Summon_"));

        var attackResult = generateAttackInteractions(outputDir, config);
        totalFiles += attackResult.totalFiles;
        filesWritten += attackResult.filesWritten;
        filesDeleted += attackResult.filesDeleted;

        return new GenerationResult(totalFiles, filesWritten, filesDeleted);
    }

    public record GenerationResult(int totalFiles, int filesWritten, int filesDeleted) {
        public boolean hasChanges() { return filesWritten > 0 || filesDeleted > 0; }
    }

    private static Map<String, Object> buildCaeConfig(
            RPGMobsConfig.CombatAIConfig combatAI,
            RPGMobsConfig.FactionStyle faction,
            RPGMobsConfig.TierBehavior tier,
            RPGMobsConfig.WeaponCombatParams weapon,
            String weaponName,
            String factionName
    ) {
        double speedMult = weapon.speedMultiplier;
        double blendedCdMin = ((faction.attackCooldownMin + tier.cooldownMin) / 2.0) * speedMult;
        double blendedCdMax = ((faction.attackCooldownMax + tier.cooldownMax) / 2.0) * speedMult;
        double blendedStrafeMin = ((faction.strafeCooldownMin + tier.strafeCooldownMin) / 2.0) * speedMult;
        double blendedStrafeMax = ((faction.strafeCooldownMax + tier.strafeCooldownMax) / 2.0) * speedMult;

        var attackRoot = weapon.attackRootInteraction;
        if (attackRoot == null || attackRoot.isEmpty()) {
            attackRoot = "Root_RPGMobs_Attack_Melee";
        }

        double maxRange = weapon.maxRange;
        double strafeMaxRange = maxRange + 0.5;

        var caeActions = new LinkedHashMap<String, Object>();
        caeActions.put("SelectTarget", buildSelectTarget());

        if (tier.hasShield && !weapon.isRanged) {
            caeActions.put("ShieldBlock", buildShieldBlock(tier, faction));
        }

        if (tier.hasBackOff) {
            caeActions.put("BackOff", buildBackOff(faction, tier));
        }

        if (tier.hasRetreat) {
            caeActions.put("HealthRetreat", buildHealthRetreat(faction));
        }

        if ((tier.hasBackOff || tier.hasRetreat)) {
            caeActions.put("ReEngageFromStrafe", buildReEngageFromStrafe(faction));
        }

        if (tier.hasGroupObserve && faction.enableGroupObserve) {
            caeActions.put("GroupObserve", buildGroupObserve(faction));
        }

        if (tier.hasFlanking && faction.enableFlanking) {
            caeActions.put("FlankPosition", buildFlankPosition(weaponName, attackRoot, faction));
        }

        var defaultActions = new ArrayList<String>();
        defaultActions.add("SelectTarget");
        if (tier.hasShield && !weapon.isRanged) defaultActions.add("ShieldBlock");
        if (tier.hasBackOff) defaultActions.add("BackOff");
        if (tier.hasRetreat) defaultActions.add("HealthRetreat");
        if (tier.hasGroupObserve && faction.enableGroupObserve) defaultActions.add("GroupObserve");
        if (tier.hasFlanking && faction.enableFlanking) defaultActions.add("FlankPosition");

        var strafeActions = new ArrayList<String>();
        strafeActions.add("SelectTarget");
        if (tier.hasBackOff || tier.hasRetreat) strafeActions.add("ReEngageFromStrafe");
        if (tier.hasRetreat) strafeActions.add("HealthRetreat");
        if (tier.hasFlanking && faction.enableFlanking) strafeActions.add("FlankPosition");

        var defaultBasicAttacks = buildBasicAttacks(attackRoot, maxRange, blendedCdMin, blendedCdMax);
        var strafeBasicAttacks = buildBasicAttacks(attackRoot, strafeMaxRange, blendedStrafeMin, blendedStrafeMax);

        var defaultActionSet = new LinkedHashMap<String, Object>();
        defaultActionSet.put("BasicAttacks", defaultBasicAttacks);
        defaultActionSet.put("Actions", defaultActions);

        var strafeActionSet = new LinkedHashMap<String, Object>();
        strafeActionSet.put("BasicAttacks", strafeBasicAttacks);
        strafeActionSet.put("Actions", strafeActions);

        var actionSets = new LinkedHashMap<String, Object>();
        actionSets.put("Default", defaultActionSet);
        actionSets.put("Strafe", strafeActionSet);

        var caeInner = new LinkedHashMap<String, Object>();
        caeInner.put("RunConditions", buildRunConditions());
        caeInner.put("MinRunUtility", combatAI.minRunUtility);
        caeInner.put("MinActionUtility", combatAI.minActionUtility);
        caeInner.put("AvailableActions", caeActions);
        caeInner.put("ActionSets", actionSets);

        var root = new LinkedHashMap<String, Object>();
        root.put("Type", "CombatActionEvaluator");
        root.put("TargetMemoryDuration", combatAI.targetMemoryDuration);
        root.put("CombatActionEvaluator", caeInner);

        return root;
    }

    private static Map<String, Object> buildRoleVariant(
            String faction, String mobRuleKey, String weaponName, int tierNum,
            String caeConfigName, RPGMobsConfig.FactionStyle factionStyle,
            RPGMobsConfig.TierBehavior tier
    ) {
        var disableDamageGroups = List.of("Self", faction);

        var parameters = new LinkedHashMap<String, Object>();
        var attitudeGroup = new LinkedHashMap<String, Object>();
        attitudeGroup.put("Value", faction);
        attitudeGroup.put("Description", "RPGMobs elite - " + faction);
        parameters.put("AttitudeGroup", attitudeGroup);

        var damageGroupsParam = new LinkedHashMap<String, Object>();
        damageGroupsParam.put("Value", disableDamageGroups);
        parameters.put("DisableDamageGroups", damageGroupsParam);

        var nameKeyParam = new LinkedHashMap<String, Object>();
        nameKeyParam.put("Value", "server.npcRoles." + mobRuleKey + ".name");
        nameKeyParam.put("Description", "Translation key for NPC name display");
        parameters.put("NameTranslationKey", nameKeyParam);

        if (tier.movementSpeedMultiplier != 1.0) {
            int scaledSpeed = (int) Math.round(10.0 * tier.movementSpeedMultiplier);
            var maxSpeedParam = new LinkedHashMap<String, Object>();
            maxSpeedParam.put("Value", scaledSpeed);
            parameters.put("MaxSpeed", maxSpeedParam);
        }

        var nameTranslationKeyCompute = new LinkedHashMap<String, Object>();
        nameTranslationKeyCompute.put("Compute", "NameTranslationKey");

        var modify = new LinkedHashMap<String, Object>();
        modify.put("UseCombatActionEvaluator", true);
        modify.put("_CombatConfig", caeConfigName);
        modify.put("NameTranslationKey", nameTranslationKeyCompute);

        var root = new LinkedHashMap<String, Object>();
        root.put("Type", "Variant");
        root.put("Reference", "Template_Intelligent");
        root.put("Parameters", parameters);
        root.put("Modify", modify);

        return root;
    }

    private static List<Object> buildRunConditions() {
        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var curve = new LinkedHashMap<String, Object>();
        curve.put("ResponseCurve", "Linear");
        curve.put("XRange", List.of(0.0, 3.0));
        timeSinceLastUsed.put("Curve", curve);

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", 0.8);
        randomiser.put("MaxValue", 1.0);

        return List.of(timeSinceLastUsed, randomiser);
    }

    private static Map<String, Object> buildSelectTarget() {
        var selectTarget = new LinkedHashMap<String, Object>();
        selectTarget.put("Type", "SelectBasicAttackTarget");

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", 0.9);
        randomiser.put("MaxValue", 1.0);

        selectTarget.put("Conditions", List.of(randomiser));
        selectTarget.put("WeightCoefficient", 1.0);
        return selectTarget;
    }

    private static Map<String, Object> buildBasicAttacks(String attackRoot, double maxRange,
                                                          double cdMin, double cdMax) {
        var attacks = new LinkedHashMap<String, Object>();
        attacks.put("Attacks", List.of(attackRoot));
        attacks.put("Randomise", true);
        attacks.put("UseProjectedDistance", true);
        attacks.put("MaxRange", maxRange);
        attacks.put("Timeout", 2);
        attacks.put("CooldownRange", List.of(cdMin, cdMax));
        return attacks;
    }

    private static Map<String, Object> buildShieldBlock(RPGMobsConfig.TierBehavior tier,
                                                         RPGMobsConfig.FactionStyle faction) {
        var action = new LinkedHashMap<String, Object>();
        action.put("Type", "Ability");
        action.put("Target", "Hostile");
        action.put("AbilityType", "Secondary");
        action.put("OffhandSlot", 0);
        action.put("Ability", "Root_RPGMobs_Guard_Shield_Sustained");
        action.put("SubState", "Default");
        action.put("AttackDistanceRange", List.of(0.5, 10.0));
        action.put("PostExecuteDistanceRange", List.of(1.5, 2.0));
        action.put("ChargeFor", tier.shieldChargeFor);
        action.put("WeightCoefficient", 1.25);

        var conditions = new ArrayList<Map<String, Object>>();

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", faction.guardRandomiserMin);
        randomiser.put("MaxValue", faction.guardRandomiserMax);
        conditions.add(randomiser);

        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var switchCurve = new LinkedHashMap<String, Object>();
        switchCurve.put("Type", "Switch");
        switchCurve.put("SwitchPoint", tier.shieldGuardCooldown);
        timeSinceLastUsed.put("Curve", switchCurve);
        conditions.add(timeSinceLastUsed);

        var recentDamage = new LinkedHashMap<String, Object>();
        recentDamage.put("Type", "RecentSustainedDamage");
        var damageCurve = new LinkedHashMap<String, Object>();
        damageCurve.put("ResponseCurve", "InverseExponential");
        damageCurve.put("XRange", List.of(0.0, 3.0));
        recentDamage.put("Curve", damageCurve);
        conditions.add(recentDamage);

        action.put("Conditions", conditions);
        return action;
    }

    private static Map<String, Object> buildBackOff(RPGMobsConfig.FactionStyle faction,
                                                     RPGMobsConfig.TierBehavior tier) {
        var action = new LinkedHashMap<String, Object>();
        action.put("Type", "State");
        action.put("State", "Combat");
        action.put("SubState", "Strafe");
        action.put("PostExecuteDistanceRange", List.of(faction.backOffDistanceMin, faction.backOffDistanceMax));
        action.put("Target", "Hostile");
        action.put("WeightCoefficient", 1.25);

        var conditions = new ArrayList<Map<String, Object>>();

        var recentDamage = new LinkedHashMap<String, Object>();
        recentDamage.put("Type", "RecentSustainedDamage");
        var damageCurve = new LinkedHashMap<String, Object>();
        damageCurve.put("ResponseCurve", "InverseExponential");
        damageCurve.put("XRange", List.of(0.0, 3.0));
        recentDamage.put("Curve", damageCurve);
        conditions.add(recentDamage);

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", faction.backOffRandomiserMin);
        randomiser.put("MaxValue", faction.backOffRandomiserMax);
        conditions.add(randomiser);

        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var switchCurve = new LinkedHashMap<String, Object>();
        switchCurve.put("Type", "Switch");
        switchCurve.put("SwitchPoint", faction.backOffSwitchPoint);
        timeSinceLastUsed.put("Curve", switchCurve);
        conditions.add(timeSinceLastUsed);

        action.put("Conditions", conditions);
        return action;
    }

    private static Map<String, Object> buildHealthRetreat(RPGMobsConfig.FactionStyle faction) {
        var action = new LinkedHashMap<String, Object>();
        action.put("Type", "State");
        action.put("State", "Combat");
        action.put("SubState", "Strafe");
        action.put("PostExecuteDistanceRange", List.of(faction.healthRetreatDistanceMin, faction.healthRetreatDistanceMax));
        action.put("Target", "Hostile");
        action.put("WeightCoefficient", faction.healthRetreatWeight);

        var conditions = new ArrayList<Map<String, Object>>();

        var ownStat = new LinkedHashMap<String, Object>();
        ownStat.put("Type", "OwnStatPercent");
        ownStat.put("Stat", "Health");
        ownStat.put("Curve", "ReverseLinear");
        conditions.add(ownStat);

        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var switchCurve = new LinkedHashMap<String, Object>();
        switchCurve.put("Type", "Switch");
        switchCurve.put("SwitchPoint", faction.retreatCooldown);
        timeSinceLastUsed.put("Curve", switchCurve);
        conditions.add(timeSinceLastUsed);

        action.put("Conditions", conditions);
        return action;
    }

    private static Map<String, Object> buildReEngageFromStrafe(RPGMobsConfig.FactionStyle faction) {
        var action = new LinkedHashMap<String, Object>();
        action.put("Type", "State");
        action.put("State", "Combat");
        action.put("SubState", "Default");
        action.put("PostExecuteDistanceRange", List.of(faction.reEngageDistanceMin, faction.reEngageDistanceMax));
        action.put("Target", "Hostile");

        var conditions = new ArrayList<Map<String, Object>>();

        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var curve = new LinkedHashMap<String, Object>();
        curve.put("ResponseCurve", "Linear");
        curve.put("XRange", List.of(faction.reEngageXRangeMin, faction.reEngageXRangeMax));
        timeSinceLastUsed.put("Curve", curve);
        conditions.add(timeSinceLastUsed);

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", faction.reEngageRandomiserMin);
        randomiser.put("MaxValue", faction.reEngageRandomiserMax);
        conditions.add(randomiser);

        action.put("Conditions", conditions);
        return action;
    }

    private static Map<String, Object> buildGroupObserve(RPGMobsConfig.FactionStyle faction) {
        var action = new LinkedHashMap<String, Object>();
        action.put("Type", "State");
        action.put("State", "Combat");
        action.put("SubState", "Strafe");
        action.put("PostExecuteDistanceRange", List.of(faction.groupObserveDistanceMin, faction.groupObserveDistanceMax));
        action.put("Target", "Hostile");
        action.put("WeightCoefficient", 1.2);

        var conditions = new ArrayList<Map<String, Object>>();

        String npcGroup = faction.npcGroupName;
        if (npcGroup == null || npcGroup.isEmpty()) {
            npcGroup = "Outlander";
        }

        var nearbyCount = new LinkedHashMap<String, Object>();
        nearbyCount.put("Type", "NearbyCount");
        nearbyCount.put("NPCGroup", npcGroup);
        nearbyCount.put("Range", 10);
        var nearbyCurve = new LinkedHashMap<String, Object>();
        nearbyCurve.put("ResponseCurve", "Linear");
        nearbyCurve.put("XRange", List.of(1.0, 4.0));
        nearbyCount.put("Curve", nearbyCurve);
        conditions.add(nearbyCount);

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", 0.6);
        randomiser.put("MaxValue", 1.0);
        conditions.add(randomiser);

        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var switchCurve = new LinkedHashMap<String, Object>();
        switchCurve.put("Type", "Switch");
        switchCurve.put("SwitchPoint", 8.0);
        timeSinceLastUsed.put("Curve", switchCurve);
        conditions.add(timeSinceLastUsed);

        action.put("Conditions", conditions);
        return action;
    }

    private static Map<String, Object> buildFlankPosition(String weaponName, String attackRoot,
                                                            RPGMobsConfig.FactionStyle faction) {
        var action = new LinkedHashMap<String, Object>();
        action.put("Type", "Ability");
        action.put("Target", "Hostile");
        action.put("Positioning", "Flank");
        action.put("PositionFirst", true);
        action.put("WeaponSlot", 0);
        action.put("Ability", attackRoot);
        action.put("SubState", "Default");
        action.put("AttackDistanceRange", List.of(1.5, 3.0));
        action.put("PostExecuteDistanceRange", List.of(2.0, 4.0));
        action.put("FailureTimeout", 3);
        action.put("WeightCoefficient", 1.3);

        var conditions = new ArrayList<Map<String, Object>>();

        var randomiser = new LinkedHashMap<String, Object>();
        randomiser.put("Type", "Randomiser");
        randomiser.put("MinValue", 0.5);
        randomiser.put("MaxValue", 1.0);
        conditions.add(randomiser);

        var timeSinceLastUsed = new LinkedHashMap<String, Object>();
        timeSinceLastUsed.put("Type", "TimeSinceLastUsed");
        var switchCurve = new LinkedHashMap<String, Object>();
        switchCurve.put("Type", "Switch");
        switchCurve.put("SwitchPoint", 15.0);
        timeSinceLastUsed.put("Curve", switchCurve);
        conditions.add(timeSinceLastUsed);

        action.put("Conditions", conditions);
        return action;
    }

    private static GenerationResult generateAttackInteractions(Path outputDir, RPGMobsConfig config) {
        var weaponParams = config.combatAIConfig.weaponParams;

        Path interactionsDir = outputDir.resolve("Item/Interactions/NPCs/RPGMobs/BasicAttacks");
        Path rootInteractionsDir = outputDir.resolve("Item/RootInteractions/NPCs/RPGMobs/BasicAttacks");

        try {
            Files.createDirectories(interactionsDir);
            Files.createDirectories(rootInteractionsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create attack interaction directories", e);
        }

        int totalFiles = 0;
        int filesWritten = 0;

        Set<String> expectedInteractionFiles = new HashSet<>();
        Set<String> expectedRootFiles = new HashSet<>();

        for (var entry : weaponParams.entrySet()) {
            var weaponName = entry.getKey();
            var wp = entry.getValue();
            if (wp.isRanged) continue;

            String expectedRoot = "Root_RPGMobs_Attack_" + weaponName;
            if (!expectedRoot.equals(wp.attackRootInteraction)) continue;

            var chain = wp.attackChainAnimations;
            if (chain == null || chain.isEmpty()) continue;

            String damageFileName = "RPGMobs_Damage_" + weaponName + ".json";
            expectedInteractionFiles.add(damageFileName);
            var damageJson = buildDamageInteraction(wp.impactSoundId, wp.hitParticleId);
            if (writeJsonIfChanged(interactionsDir.resolve(damageFileName), damageJson)) filesWritten++;
            totalFiles++;

            var uniqueSwings = new LinkedHashSet<>(chain);
            for (var swingDir : uniqueSwings) {
                String swingFileName = "RPGMobs_Attack_" + weaponName + "_" + swingDir + ".json";
                expectedInteractionFiles.add(swingFileName);
                var swingJson = buildSwingInteraction(weaponName, swingDir, wp);
                if (writeJsonIfChanged(interactionsDir.resolve(swingFileName), swingJson)) filesWritten++;
                totalFiles++;
            }

            String rootFileName = "Root_RPGMobs_Attack_" + weaponName + ".json";
            expectedRootFiles.add(rootFileName);
            var rootJson = buildRootAttackInteraction(weaponName, chain);
            if (writeJsonIfChanged(rootInteractionsDir.resolve(rootFileName), rootJson)) filesWritten++;
            totalFiles++;
        }

        var defaultWp = weaponParams.getOrDefault("Swords", new RPGMobsConfig.WeaponCombatParams());

        String genericDamageFile = "RPGMobs_Damage_Generic.json";
        expectedInteractionFiles.add(genericDamageFile);
        var genericDamageJson = buildDamageInteraction("SFX_Light_Melee_T1_Impact", "Impact_Mace_Basic");
        if (writeJsonIfChanged(interactionsDir.resolve(genericDamageFile), genericDamageJson)) filesWritten++;
        totalFiles++;

        String genericSwingLeftFile = "RPGMobs_Attack_SwingLeft.json";
        expectedInteractionFiles.add(genericSwingLeftFile);
        var genericSwingLeft = buildGenericSwingInteraction("SwingLeft", defaultWp);
        if (writeJsonIfChanged(interactionsDir.resolve(genericSwingLeftFile), genericSwingLeft)) filesWritten++;
        totalFiles++;

        String genericSwingRightFile = "RPGMobs_Attack_SwingRight.json";
        expectedInteractionFiles.add(genericSwingRightFile);
        var genericSwingRight = buildGenericSwingInteraction("SwingRight", defaultWp);
        if (writeJsonIfChanged(interactionsDir.resolve(genericSwingRightFile), genericSwingRight)) filesWritten++;
        totalFiles++;

        String genericRootFile = "Root_RPGMobs_Attack_Melee.json";
        expectedRootFiles.add(genericRootFile);
        var genericRootJson = buildGenericRootAttackInteraction();
        if (writeJsonIfChanged(rootInteractionsDir.resolve(genericRootFile), genericRootJson)) filesWritten++;
        totalFiles++;

        int filesDeleted = 0;
        filesDeleted += deleteStaleFiles(interactionsDir, "RPGMobs_", expectedInteractionFiles, Set.of());
        filesDeleted += deleteStaleFiles(rootInteractionsDir, "Root_RPGMobs_Attack_", expectedRootFiles, Set.of());

        return new GenerationResult(totalFiles, filesWritten, filesDeleted);
    }

    private static Map<String, Object> buildSwingInteraction(
            String weaponName, String swingDir, RPGMobsConfig.WeaponCombatParams wp) {
        boolean isLeft = swingDir.equals("SwingLeft") || swingDir.equals("SwingDownLeft");
        String selectorDirection = isLeft ? "ToLeft" : "ToRight";
        int yawOffset = -wp.hitboxConeYaw;

        var selectorMap = new LinkedHashMap<String, Object>();
        selectorMap.put("Id", "Horizontal");
        selectorMap.put("Direction", selectorDirection);
        selectorMap.put("TestLineOfSight", true);
        selectorMap.put("ExtendTop", 0.5);
        selectorMap.put("ExtendBottom", 0.5);
        selectorMap.put("StartDistance", 0.5);
        selectorMap.put("EndDistance", wp.hitboxEndDistance);
        selectorMap.put("Length", wp.hitboxConeLength);
        selectorMap.put("RollOffset", 30);
        selectorMap.put("YawStartOffset", yawOffset);

        var hitEntity = new LinkedHashMap<String, Object>();
        hitEntity.put("Interactions", List.of("RPGMobs_Damage_" + weaponName));

        var damageSelector = new LinkedHashMap<String, Object>();
        damageSelector.put("Type", "Selector");
        damageSelector.put("RunTime", 0.10);
        damageSelector.put("Selector", selectorMap);
        damageSelector.put("HitEntity", hitEntity);
        damageSelector.put("Next", buildSimple(wp.swingRecoveryTime));

        var trailEntry = new LinkedHashMap<String, Object>();
        trailEntry.put("PositionOffset", buildXYZ(0.8, 0, 0));
        trailEntry.put("RotationOffset", buildPitchRollYaw(0, 90, 0));
        trailEntry.put("TargetNodeName", "Handle");
        trailEntry.put("TrailId", wp.weaponTrailId.isEmpty() ? "Medium_Default" : wp.weaponTrailId);

        var vfxEffects = new LinkedHashMap<String, Object>();
        vfxEffects.put("Trails", List.of(trailEntry));
        vfxEffects.put("WorldSoundEventId", wp.swingSoundId.isEmpty() ? "SFX_Sword_T1_Swing" : wp.swingSoundId);

        var trailSimple = new LinkedHashMap<String, Object>();
        trailSimple.put("Type", "Simple");
        trailSimple.put("RunTime", wp.swingRecoveryTime);
        trailSimple.put("Effects", vfxEffects);

        var hitboxBranch = new LinkedHashMap<String, Object>();
        hitboxBranch.put("Interactions", List.of(damageSelector));

        var trailBranch = new LinkedHashMap<String, Object>();
        trailBranch.put("Interactions", List.of(trailSimple));

        var parallel = new LinkedHashMap<String, Object>();
        parallel.put("Type", "Parallel");
        parallel.put("Interactions", List.of(hitboxBranch, trailBranch));

        var windUpEffects = new LinkedHashMap<String, Object>();
        windUpEffects.put("ItemPlayerAnimationsId", wp.animationSetId);
        windUpEffects.put("ItemAnimationId", swingDir);

        var root = new LinkedHashMap<String, Object>();
        root.put("Type", "Simple");
        root.put("Effects", windUpEffects);
        root.put("RunTime", wp.swingWindUpTime);
        root.put("Next", parallel);

        return root;
    }

    private static Map<String, Object> buildGenericSwingInteraction(
            String swingDir, RPGMobsConfig.WeaponCombatParams defaultWp) {
        boolean isLeft = swingDir.equals("SwingLeft") || swingDir.equals("SwingDownLeft");
        String selectorDirection = isLeft ? "ToLeft" : "ToRight";
        int yawOffset = -defaultWp.hitboxConeYaw;

        var selectorMap = new LinkedHashMap<String, Object>();
        selectorMap.put("Id", "Horizontal");
        selectorMap.put("Direction", selectorDirection);
        selectorMap.put("TestLineOfSight", true);
        selectorMap.put("ExtendTop", 0.5);
        selectorMap.put("ExtendBottom", 0.5);
        selectorMap.put("StartDistance", 0.5);
        selectorMap.put("EndDistance", defaultWp.hitboxEndDistance);
        selectorMap.put("Length", defaultWp.hitboxConeLength);
        selectorMap.put("RollOffset", 30);
        selectorMap.put("YawStartOffset", yawOffset);

        var hitEntity = new LinkedHashMap<String, Object>();
        hitEntity.put("Interactions", List.of("RPGMobs_Damage_Generic"));

        var damageSelector = new LinkedHashMap<String, Object>();
        damageSelector.put("Type", "Selector");
        damageSelector.put("RunTime", 0.10);
        damageSelector.put("Selector", selectorMap);
        damageSelector.put("HitEntity", hitEntity);
        damageSelector.put("Next", buildSimple(defaultWp.swingRecoveryTime));

        var trailEntry = new LinkedHashMap<String, Object>();
        trailEntry.put("PositionOffset", buildXYZ(0.8, 0, 0));
        trailEntry.put("RotationOffset", buildPitchRollYaw(0, 90, 0));
        trailEntry.put("TargetNodeName", "Handle");
        trailEntry.put("TrailId", "Medium_Default");

        var vfxEffects = new LinkedHashMap<String, Object>();
        vfxEffects.put("Trails", List.of(trailEntry));
        vfxEffects.put("WorldSoundEventId", defaultWp.swingSoundId.isEmpty() ? "SFX_Sword_T1_Swing" : defaultWp.swingSoundId);

        var trailSimple = new LinkedHashMap<String, Object>();
        trailSimple.put("Type", "Simple");
        trailSimple.put("RunTime", defaultWp.swingRecoveryTime);
        trailSimple.put("Effects", vfxEffects);

        var hitboxBranch = new LinkedHashMap<String, Object>();
        hitboxBranch.put("Interactions", List.of(damageSelector));

        var trailBranch = new LinkedHashMap<String, Object>();
        trailBranch.put("Interactions", List.of(trailSimple));

        var parallel = new LinkedHashMap<String, Object>();
        parallel.put("Type", "Parallel");
        parallel.put("Interactions", List.of(hitboxBranch, trailBranch));

        var windUpEffects = new LinkedHashMap<String, Object>();
        windUpEffects.put("ItemPlayerAnimationsId", "Default");
        windUpEffects.put("ItemAnimationId", swingDir);

        var root = new LinkedHashMap<String, Object>();
        root.put("Type", "Simple");
        root.put("Effects", windUpEffects);
        root.put("RunTime", defaultWp.swingWindUpTime);
        root.put("Next", parallel);

        return root;
    }

    private static Map<String, Object> buildDamageInteraction(String impactSound, String hitParticle) {
        var baseDamage = new LinkedHashMap<String, Object>();
        baseDamage.put("Physical", 5);

        var damageCalc = new LinkedHashMap<String, Object>();
        damageCalc.put("BaseDamage", baseDamage);

        var knockback = new LinkedHashMap<String, Object>();
        knockback.put("Force", 1);
        knockback.put("RelativeX", 0);
        knockback.put("RelativeZ", -1);
        knockback.put("VelocityY", 3);
        knockback.put("VelocityType", "Set");

        var damageEffects = new LinkedHashMap<String, Object>();
        damageEffects.put("Knockback", knockback);
        damageEffects.put("WorldSoundEventId", impactSound);
        damageEffects.put("LocalSoundEventId", impactSound);

        String particleId = hitParticle.isEmpty() ? "Impact_Sword_Basic" : hitParticle;
        var particleEntry = new LinkedHashMap<String, Object>();
        particleEntry.put("SystemId", particleId);
        damageEffects.put("WorldParticles", List.of(particleEntry));

        var root = new LinkedHashMap<String, Object>();
        root.put("Parent", "DamageEntityParent");
        root.put("DamageCalculator", damageCalc);
        root.put("DamageEffects", damageEffects);

        return root;
    }

    private static Map<String, Object> buildRootAttackInteraction(String weaponName, List<String> chain) {
        var chainIds = new ArrayList<String>();
        for (var swingDir : chain) {
            chainIds.add("RPGMobs_Attack_" + weaponName + "_" + swingDir);
        }

        var chaining = new LinkedHashMap<String, Object>();
        chaining.put("Type", "Chaining");
        chaining.put("ChainId", "RPGMobs_Combo_" + weaponName);
        chaining.put("ChainingAllowance", 15);
        chaining.put("Next", chainIds);

        var tags = new LinkedHashMap<String, Object>();
        tags.put("Attack", List.of("Melee"));

        var root = new LinkedHashMap<String, Object>();
        root.put("Interactions", List.of(chaining));
        root.put("Tags", tags);

        return root;
    }

    private static Map<String, Object> buildGenericRootAttackInteraction() {
        var chaining = new LinkedHashMap<String, Object>();
        chaining.put("Type", "Chaining");
        chaining.put("ChainId", "RPGMobs_MeleeCombo");
        chaining.put("ChainingAllowance", 15);
        chaining.put("Next", List.of(
                "RPGMobs_Attack_SwingLeft", "RPGMobs_Attack_SwingRight",
                "RPGMobs_Attack_SwingLeft", "RPGMobs_Attack_SwingRight"
        ));

        var tags = new LinkedHashMap<String, Object>();
        tags.put("Attack", List.of("Melee"));

        var root = new LinkedHashMap<String, Object>();
        root.put("Interactions", List.of(chaining));
        root.put("Tags", tags);

        return root;
    }

    private static Map<String, Object> buildSimple(double runTime) {
        var simple = new LinkedHashMap<String, Object>();
        simple.put("Type", "Simple");
        simple.put("RunTime", runTime);
        return simple;
    }

    private static Map<String, Object> buildXYZ(double x, double y, double z) {
        var map = new LinkedHashMap<String, Object>();
        map.put("X", x);
        map.put("Y", y);
        map.put("Z", z);
        return map;
    }

    private static Map<String, Object> buildPitchRollYaw(int pitch, int roll, int yaw) {
        var map = new LinkedHashMap<String, Object>();
        map.put("Pitch", pitch);
        map.put("Roll", roll);
        map.put("Yaw", yaw);
        return map;
    }

    private static String detectFaction(String mobRuleKey, String combatStyleStr) {
        var style = CombatStyle.parse(combatStyleStr);
        if (style != CombatStyle.AUTO) {
            return style.factionName();
        }

        var lower = mobRuleKey.toLowerCase();
        if (lower.contains("trork")) return "Trork";
        if (lower.contains("outlander")) return "Outlander";
        if (lower.contains("goblin")) return "Goblin";
        if (lower.contains("skeleton") || lower.contains("wraith")
                || lower.contains("zombie") || lower.contains("risen")
                || lower.contains("feran")) return "Skeleton";
        return "Outlander";
    }

    private static List<String> resolveWeaponCategories(RPGMobsConfig.MobRule mobRule,
                                                         Map<String, RPGMobsConfig.WeaponCombatParams> allWeapons) {
        var allowed = mobRule.allowedWeaponCategories;
        if (allowed == null || allowed.isEmpty()) {
            return new ArrayList<>(allWeapons.keySet());
        }

        var result = new ArrayList<String>();
        for (var cat : allowed) {
            var name = cat;
            if (name.startsWith(CATEGORY_PREFIX)) {
                name = name.substring(CATEGORY_PREFIX.length());
            }
            if (allWeapons.containsKey(name)) {
                result.add(name);
            }
        }
        return result;
    }

    private static RPGMobsConfig.TierBehavior safeTierBehavior(List<RPGMobsConfig.TierBehavior> tiers, int index) {
        if (tiers == null || tiers.isEmpty()) return new RPGMobsConfig.TierBehavior();
        if (index < 0) return tiers.getFirst();
        if (index >= tiers.size()) return tiers.getLast();
        return tiers.get(index);
    }

    private static boolean writeJsonIfChanged(Path path, Map<String, Object> data) {
        try {
            String newContent = GSON.toJson(data);
            if (Files.exists(path)) {
                String existing = Files.readString(path, StandardCharsets.UTF_8);
                if (existing.equals(newContent)) return false;
            }
            Files.writeString(path, newContent, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CAE config: " + path, e);
        }
    }

    private static void cleanupOldEliteDir(Path eliteDir) throws IOException {
        if (!Files.isDirectory(eliteDir)) return;
        try (var stream = Files.list(eliteDir)) {
            stream.forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            });
        }
        Files.deleteIfExists(eliteDir);
    }

    private static int deleteStaleFiles(Path dir, String prefix, Set<String> expectedFiles, Set<String> excludePrefixes) {
        if (!Files.isDirectory(dir)) return 0;
        int deleted = 0;
        try (var stream = Files.list(dir)) {
            var stale = stream.filter(path -> {
                String name = path.getFileName().toString();
                if (!name.startsWith(prefix)) return false;
                for (String excl : excludePrefixes) {
                    if (name.startsWith(excl)) return false;
                }
                return !expectedFiles.contains(name);
            }).toList();
            for (var path : stale) {
                try {
                    Files.deleteIfExists(path);
                    deleted++;
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
        return deleted;
    }
}
