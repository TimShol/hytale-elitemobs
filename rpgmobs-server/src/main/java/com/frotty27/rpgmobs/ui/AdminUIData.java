package com.frotty27.rpgmobs.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.function.BiConsumer;
import java.util.function.Function;

final class AdminUIData {

    private static BuilderCodec.Builder<AdminUIData> b() {
        return BuilderCodec.builder(AdminUIData.class, AdminUIData::new);
    }

    private static BuilderCodec.Builder<AdminUIData> addString(BuilderCodec.Builder<AdminUIData> builder, String key,
                                                                BiConsumer<AdminUIData, String> setter,
                                                                Function<AdminUIData, String> getter) {
        return builder.append(new KeyedCodec<>(key, Codec.STRING), setter, getter).add();
    }

    static final int MAX_FAMILIES = 20;
    static final int MAX_ENV_RULES = 16;
    static final int TIERS_COUNT = 5;

    static final int TIER_OVERRIDE_PAGE_SIZE = 10;

    static final int TREE_ROW_COUNT = 20;

    static final int LOOT_TEMPLATE_DROPS_PER_PAGE = 5;

    static final BuilderCodec<AdminUIData> CODEC;
    static {
        var builder = b();
        builder = addString(builder, "Action", (d, v) -> d.action = v, d -> d.action);

        builder = addString(builder, "@DebugScanInterval", (d, v) -> d.debugScanInterval = v, d -> d.debugScanInterval);

        builder = addString(builder, "@SpawnChance0", (d, v) -> d.spawnChance0 = v, d -> d.spawnChance0);
        builder = addString(builder, "@SpawnChance1", (d, v) -> d.spawnChance1 = v, d -> d.spawnChance1);
        builder = addString(builder, "@SpawnChance2", (d, v) -> d.spawnChance2 = v, d -> d.spawnChance2);
        builder = addString(builder, "@SpawnChance3", (d, v) -> d.spawnChance3 = v, d -> d.spawnChance3);
        builder = addString(builder, "@SpawnChance4", (d, v) -> d.spawnChance4 = v, d -> d.spawnChance4);

        builder = addString(builder, "@DistPerTier", (d, v) -> d.distPerTier = v, d -> d.distPerTier);
        builder = addString(builder, "@DistBonusInterval", (d, v) -> d.distBonusInterval = v, d -> d.distBonusInterval);
        builder = addString(builder, "@DistHealthBonus", (d, v) -> d.distHealthBonus = v, d -> d.distHealthBonus);
        builder = addString(builder, "@DistDamageBonus", (d, v) -> d.distDamageBonus = v, d -> d.distDamageBonus);
        builder = addString(builder, "@DistHealthCap", (d, v) -> d.distHealthCap = v, d -> d.distHealthCap);
        builder = addString(builder, "@DistDamageCap", (d, v) -> d.distDamageCap = v, d -> d.distDamageCap);

        builder = addString(builder, "@Health0", (d, v) -> d.health0 = v, d -> d.health0);
        builder = addString(builder, "@Health1", (d, v) -> d.health1 = v, d -> d.health1);
        builder = addString(builder, "@Health2", (d, v) -> d.health2 = v, d -> d.health2);
        builder = addString(builder, "@Health3", (d, v) -> d.health3 = v, d -> d.health3);
        builder = addString(builder, "@Health4", (d, v) -> d.health4 = v, d -> d.health4);
        builder = addString(builder, "@Damage0", (d, v) -> d.damage0 = v, d -> d.damage0);
        builder = addString(builder, "@Damage1", (d, v) -> d.damage1 = v, d -> d.damage1);
        builder = addString(builder, "@Damage2", (d, v) -> d.damage2 = v, d -> d.damage2);
        builder = addString(builder, "@Damage3", (d, v) -> d.damage3 = v, d -> d.damage3);
        builder = addString(builder, "@Damage4", (d, v) -> d.damage4 = v, d -> d.damage4);

        builder = addString(builder, "@ExtraRolls0", (d, v) -> d.extraRolls0 = v, d -> d.extraRolls0);
        builder = addString(builder, "@ExtraRolls1", (d, v) -> d.extraRolls1 = v, d -> d.extraRolls1);
        builder = addString(builder, "@ExtraRolls2", (d, v) -> d.extraRolls2 = v, d -> d.extraRolls2);
        builder = addString(builder, "@ExtraRolls3", (d, v) -> d.extraRolls3 = v, d -> d.extraRolls3);
        builder = addString(builder, "@ExtraRolls4", (d, v) -> d.extraRolls4 = v, d -> d.extraRolls4);

        builder = addString(builder, "@HealthVariance", (d, v) -> d.healthVariance = v, d -> d.healthVariance);
        builder = addString(builder, "@DamageVariance", (d, v) -> d.damageVariance = v, d -> d.damageVariance);
        builder = addString(builder, "@DropWeapon", (d, v) -> d.dropWeapon = v, d -> d.dropWeapon);
        builder = addString(builder, "@DropArmor", (d, v) -> d.dropArmor = v, d -> d.dropArmor);
        builder = addString(builder, "@DropOffhand", (d, v) -> d.dropOffhand = v, d -> d.dropOffhand);

        builder = addString(builder, "@ModelScale0", (d, v) -> d.modelScale0 = v, d -> d.modelScale0);
        builder = addString(builder, "@ModelScale1", (d, v) -> d.modelScale1 = v, d -> d.modelScale1);
        builder = addString(builder, "@ModelScale2", (d, v) -> d.modelScale2 = v, d -> d.modelScale2);
        builder = addString(builder, "@ModelScale3", (d, v) -> d.modelScale3 = v, d -> d.modelScale3);
        builder = addString(builder, "@ModelScale4", (d, v) -> d.modelScale4 = v, d -> d.modelScale4);
        builder = addString(builder, "@ModelVariance", (d, v) -> d.modelVariance = v, d -> d.modelVariance);

        builder = addString(builder, "@NameplatePrefix0", (d, v) -> d.nameplatePrefix0 = v, d -> d.nameplatePrefix0);
        builder = addString(builder, "@NameplatePrefix1", (d, v) -> d.nameplatePrefix1 = v, d -> d.nameplatePrefix1);
        builder = addString(builder, "@NameplatePrefix2", (d, v) -> d.nameplatePrefix2 = v, d -> d.nameplatePrefix2);
        builder = addString(builder, "@NameplatePrefix3", (d, v) -> d.nameplatePrefix3 = v, d -> d.nameplatePrefix3);
        builder = addString(builder, "@NameplatePrefix4", (d, v) -> d.nameplatePrefix4 = v, d -> d.nameplatePrefix4);

        for (int i = 0; i < 20; i++) {
            final int fi = i;
            builder = addString(builder, "@FamilyKey" + i, (d, v) -> d.familyKeys[fi] = v, d -> d.familyKeys[fi]);
            builder = addString(builder, "@FamilyT1r" + i, (d, v) -> d.familyT1[fi] = v, d -> d.familyT1[fi]);
            builder = addString(builder, "@FamilyT2r" + i, (d, v) -> d.familyT2[fi] = v, d -> d.familyT2[fi]);
            builder = addString(builder, "@FamilyT3r" + i, (d, v) -> d.familyT3[fi] = v, d -> d.familyT3[fi]);
            builder = addString(builder, "@FamilyT4r" + i, (d, v) -> d.familyT4[fi] = v, d -> d.familyT4[fi]);
            builder = addString(builder, "@FamilyT5r" + i, (d, v) -> d.familyT5[fi] = v, d -> d.familyT5[fi]);
        }

        for (int i = 0; i < 16; i++) {
            final int ei = i;
            builder = addString(builder, "@EnvKey" + i, (d, v) -> d.envRuleKeys[ei] = v, d -> d.envRuleKeys[ei]);
            builder = addString(builder, "@EnvT1r" + i, (d, v) -> d.envRuleT1[ei] = v, d -> d.envRuleT1[ei]);
            builder = addString(builder, "@EnvT2r" + i, (d, v) -> d.envRuleT2[ei] = v, d -> d.envRuleT2[ei]);
            builder = addString(builder, "@EnvT3r" + i, (d, v) -> d.envRuleT3[ei] = v, d -> d.envRuleT3[ei]);
            builder = addString(builder, "@EnvT4r" + i, (d, v) -> d.envRuleT4[ei] = v, d -> d.envRuleT4[ei]);
            builder = addString(builder, "@EnvT5r" + i, (d, v) -> d.envRuleT5[ei] = v, d -> d.envRuleT5[ei]);
        }

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@OverlayXPMult" + i,
                    (d, v) -> d.overlayXPMult[fi] = v, d -> d.overlayXPMult[fi]);
        }
        builder = addString(builder, "@OverlayXPBonusPerAbility",
                (d, v) -> d.overlayXPBonusPerAbility = v, d -> d.overlayXPBonusPerAbility);
        builder = addString(builder, "@OverlayMinionXPMult",
                (d, v) -> d.overlayMinionXPMult = v, d -> d.overlayMinionXPMult);

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@EffectDetailMult" + i,
                    (d, v) -> d.effectDetailMult[fi] = v, d -> d.effectDetailMult[fi]);
        }

        builder = addString(builder, "@TierOvrFilter",
                (d, v) -> d.tierOvrFilter = v, d -> d.tierOvrFilter);

        builder = addString(builder, "@GlobMobRuleMatchExact",
                (d, v) -> d.globMobRuleMatchExact = v, d -> d.globMobRuleMatchExact);
        builder = addString(builder, "@GlobMobRuleMatchPrefix",
                (d, v) -> d.globMobRuleMatchPrefix = v, d -> d.globMobRuleMatchPrefix);
        builder = addString(builder, "@GlobMobRuleMatchContains",
                (d, v) -> d.globMobRuleMatchContains = v, d -> d.globMobRuleMatchContains);
        builder = addString(builder, "@GlobMobRuleMatchExcludes",
                (d, v) -> d.globMobRuleMatchExcludes = v, d -> d.globMobRuleMatchExcludes);

        builder = addString(builder, "@WpnCatTreeFilter",
                (d, v) -> d.wpnCatTreeFilter = v, d -> d.wpnCatTreeFilter);
        builder = addString(builder, "@ArmCatTreeFilter",
                (d, v) -> d.armCatTreeFilter = v, d -> d.armCatTreeFilter);

        builder = addString(builder, "@GlobMobRuleWpnCatFilter",
                (d, v) -> d.mobRuleWpnCatFilter = v, d -> d.mobRuleWpnCatFilter);
        builder = addString(builder, "@GlobMobRuleArmCatFilter",
                (d, v) -> d.mobRuleArmCatFilter = v, d -> d.mobRuleArmCatFilter);

        builder = addString(builder, "@RenamePopupField",
                (d, v) -> d.renamePopupField = v, d -> d.renamePopupField);

        for (int i = 0; i < LOOT_TEMPLATE_DROPS_PER_PAGE; i++) {
            final int fi = i;
            builder = addString(builder, "@LootTplDropChance" + i,
                    (d, v) -> d.lootTplDropChances[fi] = v, d -> d.lootTplDropChances[fi]);
            builder = addString(builder, "@LootTplDropMinQty" + i,
                    (d, v) -> d.lootTplDropMinQtys[fi] = v, d -> d.lootTplDropMinQtys[fi]);
            builder = addString(builder, "@LootTplDropMaxQty" + i,
                    (d, v) -> d.lootTplDropMaxQtys[fi] = v, d -> d.lootTplDropMaxQtys[fi]);
        }

        builder = addString(builder, "@ItemPickerFilter",
                (d, v) -> d.itemPickerFilter = v, d -> d.itemPickerFilter);
        builder = addString(builder, "@ItemPickerCustomId",
                (d, v) -> d.itemPickerCustomId = v, d -> d.itemPickerCustomId);

        builder = addString(builder, "@NpcPickerFilter",
                (d, v) -> d.npcPickerFilter = v, d -> d.npcPickerFilter);
        builder = addString(builder, "@NpcPickerCustomId",
                (d, v) -> d.npcPickerCustomId = v, d -> d.npcPickerCustomId);

        builder = addString(builder, "@LootTplMobFilter",
                (d, v) -> d.lootTplMobFilter = v, d -> d.lootTplMobFilter);

        builder = addString(builder, "@AbilTreeFilter",
                (d, v) -> d.abilTreeFilter = v, d -> d.abilTreeFilter);
        builder = addString(builder, "@AbilMobFilter",
                (d, v) -> d.abilMobFilter = v, d -> d.abilMobFilter);

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgCLChance" + i,
                    (d, v) -> d.abilCfgCLChance[fi] = v, d -> d.abilCfgCLChance[fi]);
            builder = addString(builder, "@AbilCfgCLCooldown" + i,
                    (d, v) -> d.abilCfgCLCooldown[fi] = v, d -> d.abilCfgCLCooldown[fi]);
        }

        builder = addString(builder, "@AbilCfgCLMinRange",
                (d, v) -> d.abilCfgCLMinRange = v, d -> d.abilCfgCLMinRange);
        builder = addString(builder, "@AbilCfgCLMaxRange",
                (d, v) -> d.abilCfgCLMaxRange = v, d -> d.abilCfgCLMaxRange);
        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgCLSlamRange" + i,
                    (d, v) -> d.abilCfgCLSlamRange[fi] = v, d -> d.abilCfgCLSlamRange[fi]);
            builder = addString(builder, "@AbilCfgCLSlamDmg" + i,
                    (d, v) -> d.abilCfgCLSlamDmg[fi] = v, d -> d.abilCfgCLSlamDmg[fi]);
            builder = addString(builder, "@AbilCfgCLForce" + i,
                    (d, v) -> d.abilCfgCLForce[fi] = v, d -> d.abilCfgCLForce[fi]);
            builder = addString(builder, "@AbilCfgCLKBLift" + i,
                    (d, v) -> d.abilCfgCLKBLift[fi] = v, d -> d.abilCfgCLKBLift[fi]);
            builder = addString(builder, "@AbilCfgCLKBPush" + i,
                    (d, v) -> d.abilCfgCLKBPush[fi] = v, d -> d.abilCfgCLKBPush[fi]);
            builder = addString(builder, "@AbilCfgCLKBForce" + i,
                    (d, v) -> d.abilCfgCLKBForce[fi] = v, d -> d.abilCfgCLKBForce[fi]);
        }

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgHLChance" + i,
                    (d, v) -> d.abilCfgHLChance[fi] = v, d -> d.abilCfgHLChance[fi]);
            builder = addString(builder, "@AbilCfgHLCooldown" + i,
                    (d, v) -> d.abilCfgHLCooldown[fi] = v, d -> d.abilCfgHLCooldown[fi]);
        }
        builder = addString(builder, "@AbilCfgHLMinHealth",
                (d, v) -> d.abilCfgHLMinHealth = v, d -> d.abilCfgHLMinHealth);
        builder = addString(builder, "@AbilCfgHLMaxHealth",
                (d, v) -> d.abilCfgHLMaxHealth = v, d -> d.abilCfgHLMaxHealth);
        builder = addString(builder, "@AbilCfgHLInstantChance",
                (d, v) -> d.abilCfgHLInstantChance = v, d -> d.abilCfgHLInstantChance);
        builder = addString(builder, "@AbilCfgHLDrinkDur",
                (d, v) -> d.abilCfgHLDrinkDur = v, d -> d.abilCfgHLDrinkDur);
        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgHLHeal" + i,
                    (d, v) -> d.abilCfgHLHeal[fi] = v, d -> d.abilCfgHLHeal[fi]);
            builder = addString(builder, "@AbilCfgHLForce" + i,
                    (d, v) -> d.abilCfgHLForce[fi] = v, d -> d.abilCfgHLForce[fi]);
        }
        builder = addString(builder, "@AbilCfgHLInterrupt",
                (d, v) -> d.abilCfgHLInterrupt = v, d -> d.abilCfgHLInterrupt);

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgSMChance" + i,
                    (d, v) -> d.abilCfgSMChance[fi] = v, d -> d.abilCfgSMChance[fi]);
            builder = addString(builder, "@AbilCfgSMCooldown" + i,
                    (d, v) -> d.abilCfgSMCooldown[fi] = v, d -> d.abilCfgSMCooldown[fi]);
        }
        builder = addString(builder, "@AbilCfgSMMaxMinions",
                (d, v) -> d.abilCfgSMMaxMinions = v, d -> d.abilCfgSMMaxMinions);
        builder = addString(builder, "@AbilCfgSMSkelW",
                (d, v) -> d.abilCfgSMSkelW = v, d -> d.abilCfgSMSkelW);
        builder = addString(builder, "@AbilCfgSMZombW",
                (d, v) -> d.abilCfgSMZombW = v, d -> d.abilCfgSMZombW);
        builder = addString(builder, "@AbilCfgSMWraithW",
                (d, v) -> d.abilCfgSMWraithW = v, d -> d.abilCfgSMWraithW);
        builder = addString(builder, "@AbilCfgSMAbrrW",
                (d, v) -> d.abilCfgSMAbrrW = v, d -> d.abilCfgSMAbrrW);
        builder = addString(builder, "@AbilCfgSUMinCount",
                (d, v) -> d.abilCfgSUMinCount = v, d -> d.abilCfgSUMinCount);
        builder = addString(builder, "@AbilCfgSUMaxCount",
                (d, v) -> d.abilCfgSUMaxCount = v, d -> d.abilCfgSUMaxCount);
        builder = addString(builder, "@AbilCfgSURadius",
                (d, v) -> d.abilCfgSURadius = v, d -> d.abilCfgSURadius);
        builder = addString(builder, "@AbilCfgSUMinionMin",
                (d, v) -> d.abilCfgSUMinionMin = v, d -> d.abilCfgSUMinionMin);
        builder = addString(builder, "@AbilCfgSUMinionMax",
                (d, v) -> d.abilCfgSUMinionMax = v, d -> d.abilCfgSUMinionMax);

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgDRChance" + i,
                    (d, v) -> d.abilCfgDRChance[fi] = v, d -> d.abilCfgDRChance[fi]);
            builder = addString(builder, "@AbilCfgDRCooldown" + i,
                    (d, v) -> d.abilCfgDRCooldown[fi] = v, d -> d.abilCfgDRCooldown[fi]);
        }
        builder = addString(builder, "@AbilCfgDRDodgeForce",
                (d, v) -> d.abilCfgDRDodgeForce = v, d -> d.abilCfgDRDodgeForce);
        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgDRDodgeChance" + i,
                    (d, v) -> d.abilCfgDRDodgeChance[fi] = v, d -> d.abilCfgDRDodgeChance[fi]);
        }
        builder = addString(builder, "@AbilCfgDRInvulnDur",
                (d, v) -> d.abilCfgDRInvulnDur = v, d -> d.abilCfgDRInvulnDur);
        builder = addString(builder, "@AbilCfgDRChargedMult",
                (d, v) -> d.abilCfgDRChargedMult = v, d -> d.abilCfgDRChargedMult);

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgMSChance" + i,
                    (d, v) -> d.abilCfgMSChance[fi] = v, d -> d.abilCfgMSChance[fi]);
            builder = addString(builder, "@AbilCfgMSCooldown" + i,
                    (d, v) -> d.abilCfgMSCooldown[fi] = v, d -> d.abilCfgMSCooldown[fi]);
        }
        builder = addString(builder, "@AbilCfgMSMeleeRange",
                (d, v) -> d.abilCfgMSMeleeRange = v, d -> d.abilCfgMSMeleeRange);
        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgMSTrigger" + i,
                    (d, v) -> d.abilCfgMSTrigger[fi] = v, d -> d.abilCfgMSTrigger[fi]);
            builder = addString(builder, "@AbilCfgMSDamage" + i,
                    (d, v) -> d.abilCfgMSDamage[fi] = v, d -> d.abilCfgMSDamage[fi]);
            builder = addString(builder, "@AbilCfgMSDrift" + i,
                    (d, v) -> d.abilCfgMSDrift[fi] = v, d -> d.abilCfgMSDrift[fi]);
            builder = addString(builder, "@AbilCfgMSKB" + i,
                    (d, v) -> d.abilCfgMSKB[fi] = v, d -> d.abilCfgMSKB[fi]);
        }

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgENChance" + i,
                    (d, v) -> d.abilCfgENChance[fi] = v, d -> d.abilCfgENChance[fi]);
            builder = addString(builder, "@AbilCfgENCooldown" + i,
                    (d, v) -> d.abilCfgENCooldown[fi] = v, d -> d.abilCfgENCooldown[fi]);
        }
        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgENHealthPct" + i,
                    (d, v) -> d.abilCfgENHealthPct[fi] = v, d -> d.abilCfgENHealthPct[fi]);
            builder = addString(builder, "@AbilCfgENDmgMult" + i,
                    (d, v) -> d.abilCfgENDmgMult[fi] = v, d -> d.abilCfgENDmgMult[fi]);
            builder = addString(builder, "@AbilCfgENSpdMult" + i,
                    (d, v) -> d.abilCfgENSpdMult[fi] = v, d -> d.abilCfgENSpdMult[fi]);
        }

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgVLChance" + i,
                    (d, v) -> d.abilCfgVLChance[fi] = v, d -> d.abilCfgVLChance[fi]);
            builder = addString(builder, "@AbilCfgVLCooldown" + i,
                    (d, v) -> d.abilCfgVLCooldown[fi] = v, d -> d.abilCfgVLCooldown[fi]);
        }
        builder = addString(builder, "@AbilCfgVLMinRange",
                (d, v) -> d.abilCfgVLMinRange = v, d -> d.abilCfgVLMinRange);
        builder = addString(builder, "@AbilCfgVLMaxRange",
                (d, v) -> d.abilCfgVLMaxRange = v, d -> d.abilCfgVLMaxRange);
        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@AbilCfgVLTrigger" + i,
                    (d, v) -> d.abilCfgVLTrigger[fi] = v, d -> d.abilCfgVLTrigger[fi]);
            builder = addString(builder, "@AbilCfgVLProjectiles" + i,
                    (d, v) -> d.abilCfgVLProjectiles[fi] = v, d -> d.abilCfgVLProjectiles[fi]);
            builder = addString(builder, "@AbilCfgVLSpread" + i,
                    (d, v) -> d.abilCfgVLSpread[fi] = v, d -> d.abilCfgVLSpread[fi]);
            builder = addString(builder, "@AbilCfgVLDamage" + i,
                    (d, v) -> d.abilCfgVLDamage[fi] = v, d -> d.abilCfgVLDamage[fi]);
        }

        builder = addString(builder, "@EffectTreeFilter",
                (d, v) -> d.effectTreeFilter = v, d -> d.effectTreeFilter);

        builder = addString(builder, "@MobRuleTreeFilter",
                (d, v) -> d.mobRuleTreeFilter = v, d -> d.mobRuleTreeFilter);

        builder = addString(builder, "@LootTreeFilter",
                (d, v) -> d.lootTreeFilter = v, d -> d.lootTreeFilter);

        builder = addString(builder, "@LootDurMin",
                (d, v) -> d.lootDurMin = v, d -> d.lootDurMin);
        builder = addString(builder, "@LootDurMax",
                (d, v) -> d.lootDurMax = v, d -> d.lootDurMax);

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@RarityArmorPieces" + i,
                    (d, v) -> d.rarityArmorPieces[fi] = v, d -> d.rarityArmorPieces[fi]);
        }

        for (int i = 0; i < TIERS_COUNT; i++) {
            final int fi = i;
            builder = addString(builder, "@RarityShieldChance" + i,
                    (d, v) -> d.rarityShieldChance[fi] = v, d -> d.rarityShieldChance[fi]);
        }

        for (int t = 0; t < TIERS_COUNT; t++) {
            for (int r = 0; r < TIERS_COUNT; r++) {
                final int ft = t, fr = r;
                builder = addString(builder, "@RarityWt" + t + r,
                        (d, v) -> d.rarityWt[ft][fr] = v, d -> d.rarityWt[ft][fr]);
            }
        }
        builder = addString(builder, "@GlobalCdMin",
                (d, v) -> d.globalCdMin = v, d -> d.globalCdMin);
        builder = addString(builder, "@GlobalCdMax",
                (d, v) -> d.globalCdMax = v, d -> d.globalCdMax);

        builder = addString(builder, "@CaiFacAtkCdMin", (d, v) -> d.caiFacAtkCdMin = v, d -> d.caiFacAtkCdMin);
        builder = addString(builder, "@CaiFacAtkCdMax", (d, v) -> d.caiFacAtkCdMax = v, d -> d.caiFacAtkCdMax);
        builder = addString(builder, "@CaiFacShieldCharge", (d, v) -> d.caiFacShieldCharge = v, d -> d.caiFacShieldCharge);
        builder = addString(builder, "@CaiFacShieldSwitch", (d, v) -> d.caiFacShieldSwitch = v, d -> d.caiFacShieldSwitch);
        builder = addString(builder, "@CaiFacBoDistMin", (d, v) -> d.caiFacBoDistMin = v, d -> d.caiFacBoDistMin);
        builder = addString(builder, "@CaiFacBoDistMax", (d, v) -> d.caiFacBoDistMax = v, d -> d.caiFacBoDistMax);
        builder = addString(builder, "@CaiFacBoSwitch", (d, v) -> d.caiFacBoSwitch = v, d -> d.caiFacBoSwitch);
        builder = addString(builder, "@CaiFacRetDistMin", (d, v) -> d.caiFacRetDistMin = v, d -> d.caiFacRetDistMin);
        builder = addString(builder, "@CaiFacRetDistMax", (d, v) -> d.caiFacRetDistMax = v, d -> d.caiFacRetDistMax);
        builder = addString(builder, "@CaiFacRetWeight", (d, v) -> d.caiFacRetWeight = v, d -> d.caiFacRetWeight);
        builder = addString(builder, "@CaiFacReEngMin", (d, v) -> d.caiFacReEngMin = v, d -> d.caiFacReEngMin);
        builder = addString(builder, "@CaiFacReEngMax", (d, v) -> d.caiFacReEngMax = v, d -> d.caiFacReEngMax);
        builder = addString(builder, "@CaiFacReEngRandMin", (d, v) -> d.caiFacReEngRandMin = v, d -> d.caiFacReEngRandMin);
        builder = addString(builder, "@CaiFacReEngRandMax", (d, v) -> d.caiFacReEngRandMax = v, d -> d.caiFacReEngRandMax);
        builder = addString(builder, "@CaiFacStrafeCdMin", (d, v) -> d.caiFacStrafeCdMin = v, d -> d.caiFacStrafeCdMin);
        builder = addString(builder, "@CaiFacStrafeCdMax", (d, v) -> d.caiFacStrafeCdMax = v, d -> d.caiFacStrafeCdMax);
        builder = addString(builder, "@CaiFacGuardRandMin", (d, v) -> d.caiFacGuardRandMin = v, d -> d.caiFacGuardRandMin);
        builder = addString(builder, "@CaiFacGuardRandMax", (d, v) -> d.caiFacGuardRandMax = v, d -> d.caiFacGuardRandMax);
        builder = addString(builder, "@CaiFacBoRandMin", (d, v) -> d.caiFacBoRandMin = v, d -> d.caiFacBoRandMin);
        builder = addString(builder, "@CaiFacBoRandMax", (d, v) -> d.caiFacBoRandMax = v, d -> d.caiFacBoRandMax);
        builder = addString(builder, "@CaiFacRetCooldown", (d, v) -> d.caiFacRetCooldown = v, d -> d.caiFacRetCooldown);
        builder = addString(builder, "@CaiFacReEngDistMin", (d, v) -> d.caiFacReEngDistMin = v, d -> d.caiFacReEngDistMin);
        builder = addString(builder, "@CaiFacReEngDistMax", (d, v) -> d.caiFacReEngDistMax = v, d -> d.caiFacReEngDistMax);
        builder = addString(builder, "@CaiFacObsDistMin", (d, v) -> d.caiFacObsDistMin = v, d -> d.caiFacObsDistMin);
        builder = addString(builder, "@CaiFacObsDistMax", (d, v) -> d.caiFacObsDistMax = v, d -> d.caiFacObsDistMax);
        builder = addString(builder, "@CaiFacFlankAngle", (d, v) -> d.caiFacFlankAngle = v, d -> d.caiFacFlankAngle);
        builder = addString(builder, "@CaiTierCdMin", (d, v) -> d.caiTierCdMin = v, d -> d.caiTierCdMin);
        builder = addString(builder, "@CaiTierCdMax", (d, v) -> d.caiTierCdMax = v, d -> d.caiTierCdMax);
        builder = addString(builder, "@CaiTierStrCdMin", (d, v) -> d.caiTierStrCdMin = v, d -> d.caiTierStrCdMin);
        builder = addString(builder, "@CaiTierStrCdMax", (d, v) -> d.caiTierStrCdMax = v, d -> d.caiTierStrCdMax);
        builder = addString(builder, "@CaiTierShieldCharge", (d, v) -> d.caiTierShieldCharge = v, d -> d.caiTierShieldCharge);
        builder = addString(builder, "@CaiTierGuardCd", (d, v) -> d.caiTierGuardCd = v, d -> d.caiTierGuardCd);
        builder = addString(builder, "@CaiTierRetHealth", (d, v) -> d.caiTierRetHealth = v, d -> d.caiTierRetHealth);
        builder = addString(builder, "@CaiWpnRange", (d, v) -> d.caiWpnRange = v, d -> d.caiWpnRange);
        builder = addString(builder, "@CaiWpnSpeed", (d, v) -> d.caiWpnSpeed = v, d -> d.caiWpnSpeed);
        builder = addString(builder, "@AssetPickerFilter", (d, v) -> d.assetPickerFilter = v, d -> d.assetPickerFilter);

        CODEC = builder.build();
    }

    String action;

    String debugScanInterval;

    String spawnChance0, spawnChance1, spawnChance2, spawnChance3, spawnChance4;

    String distPerTier, distBonusInterval, distHealthBonus, distDamageBonus, distHealthCap, distDamageCap;

    String health0, health1, health2, health3, health4;
    String healthVariance;
    String damage0, damage1, damage2, damage3, damage4;
    String damageVariance;

    String extraRolls0, extraRolls1, extraRolls2, extraRolls3, extraRolls4;
    String dropWeapon, dropArmor, dropOffhand;

    String effectTreeFilter;
    String[] effectDetailMult = new String[TIERS_COUNT];

    String modelScale0, modelScale1, modelScale2, modelScale3, modelScale4;
    String modelVariance;

    String nameplatePrefix0, nameplatePrefix1, nameplatePrefix2, nameplatePrefix3, nameplatePrefix4;

    final String[] familyKeys = new String[MAX_FAMILIES];
    final String[] familyT1 = new String[MAX_FAMILIES];
    final String[] familyT2 = new String[MAX_FAMILIES];
    final String[] familyT3 = new String[MAX_FAMILIES];
    final String[] familyT4 = new String[MAX_FAMILIES];
    final String[] familyT5 = new String[MAX_FAMILIES];

    final String[] envRuleKeys = new String[MAX_ENV_RULES];
    final String[] envRuleT1 = new String[MAX_ENV_RULES];
    final String[] envRuleT2 = new String[MAX_ENV_RULES];
    final String[] envRuleT3 = new String[MAX_ENV_RULES];
    final String[] envRuleT4 = new String[MAX_ENV_RULES];
    final String[] envRuleT5 = new String[MAX_ENV_RULES];

    final String[] overlayXPMult = new String[TIERS_COUNT];
    String overlayXPBonusPerAbility;
    String overlayMinionXPMult;

    String tierOvrFilter;

    String globMobRuleMatchExact, globMobRuleMatchPrefix, globMobRuleMatchContains, globMobRuleMatchExcludes;

    String wpnCatTreeFilter, armCatTreeFilter;

    String mobRuleWpnCatFilter, mobRuleArmCatFilter;

    String renamePopupField;

    final String[] lootTplDropChances = new String[LOOT_TEMPLATE_DROPS_PER_PAGE];
    final String[] lootTplDropMinQtys = new String[LOOT_TEMPLATE_DROPS_PER_PAGE];
    final String[] lootTplDropMaxQtys = new String[LOOT_TEMPLATE_DROPS_PER_PAGE];

    String itemPickerFilter;
    String itemPickerCustomId;

    String npcPickerFilter;
    String npcPickerCustomId;

    String lootTplMobFilter;

    String abilTreeFilter;
    String abilMobFilter;

    String[] abilCfgCLChance = new String[TIERS_COUNT];
    String[] abilCfgCLCooldown = new String[TIERS_COUNT];
    String[] abilCfgHLChance = new String[TIERS_COUNT];
    String[] abilCfgHLCooldown = new String[TIERS_COUNT];
    String[] abilCfgSMChance = new String[TIERS_COUNT];
    String[] abilCfgSMCooldown = new String[TIERS_COUNT];

    String abilCfgCLMinRange;
    String abilCfgCLMaxRange;
    String[] abilCfgCLSlamRange = new String[TIERS_COUNT];
    String[] abilCfgCLSlamDmg = new String[TIERS_COUNT];
    String[] abilCfgCLForce = new String[TIERS_COUNT];
    String[] abilCfgCLKBLift = new String[TIERS_COUNT];
    String[] abilCfgCLKBPush = new String[TIERS_COUNT];
    String[] abilCfgCLKBForce = new String[TIERS_COUNT];

    String abilCfgHLMinHealth;
    String abilCfgHLMaxHealth;
    String abilCfgHLInstantChance;
    String abilCfgHLDrinkDur;
    String[] abilCfgHLHeal = new String[TIERS_COUNT];
    String[] abilCfgHLForce = new String[TIERS_COUNT];
    String abilCfgHLInterrupt;

    String abilCfgSMMaxMinions;
    String abilCfgSMSkelW;
    String abilCfgSMZombW;
    String abilCfgSMWraithW;
    String abilCfgSMAbrrW;
    String abilCfgSUMinCount;
    String abilCfgSUMaxCount;
    String abilCfgSURadius;
    String abilCfgSUMinionMin;
    String abilCfgSUMinionMax;

    String[] abilCfgDRChance = new String[TIERS_COUNT];
    String[] abilCfgDRCooldown = new String[TIERS_COUNT];
    String abilCfgDRDodgeForce;
    String[] abilCfgDRDodgeChance = new String[TIERS_COUNT];
    String abilCfgDRInvulnDur;
    String abilCfgDRChargedMult;

    String[] abilCfgMSChance = new String[TIERS_COUNT];
    String[] abilCfgMSCooldown = new String[TIERS_COUNT];
    String abilCfgMSMeleeRange;
    String[] abilCfgMSTrigger = new String[TIERS_COUNT];
    String[] abilCfgMSDamage = new String[TIERS_COUNT];
    String[] abilCfgMSDrift = new String[TIERS_COUNT];
    String[] abilCfgMSKB = new String[TIERS_COUNT];

    String[] abilCfgENChance = new String[TIERS_COUNT];
    String[] abilCfgENCooldown = new String[TIERS_COUNT];
    String[] abilCfgENHealthPct = new String[TIERS_COUNT];
    String[] abilCfgENDmgMult = new String[TIERS_COUNT];
    String[] abilCfgENSpdMult = new String[TIERS_COUNT];

    String[] abilCfgVLChance = new String[TIERS_COUNT];
    String[] abilCfgVLCooldown = new String[TIERS_COUNT];
    String abilCfgVLMinRange;
    String abilCfgVLMaxRange;
    String[] abilCfgVLTrigger = new String[TIERS_COUNT];
    String[] abilCfgVLProjectiles = new String[TIERS_COUNT];
    String[] abilCfgVLSpread = new String[TIERS_COUNT];
    String[] abilCfgVLDamage = new String[TIERS_COUNT];

    String mobRuleTreeFilter;

    String lootTreeFilter;

    String lootDurMin, lootDurMax;

    final String[] rarityArmorPieces = new String[TIERS_COUNT];
    final String[] rarityShieldChance = new String[TIERS_COUNT];
    final String[][] rarityWt = new String[TIERS_COUNT][TIERS_COUNT];

    String globalCdMin;
    String globalCdMax;

    String caiFacAtkCdMin, caiFacAtkCdMax;
    String caiFacShieldCharge, caiFacShieldSwitch;
    String caiFacBoDistMin, caiFacBoDistMax, caiFacBoSwitch;
    String caiFacRetDistMin, caiFacRetDistMax, caiFacRetWeight;
    String caiFacReEngMin, caiFacReEngMax;
    String caiFacReEngRandMin, caiFacReEngRandMax;
    String caiFacStrafeCdMin, caiFacStrafeCdMax;
    String caiFacGuardRandMin, caiFacGuardRandMax;
    String caiFacBoRandMin, caiFacBoRandMax;
    String caiFacRetCooldown;
    String caiFacReEngDistMin, caiFacReEngDistMax;
    String caiFacObsDistMin, caiFacObsDistMax;
    String caiFacFlankAngle;
    String caiTierCdMin, caiTierCdMax;
    String caiTierStrCdMin, caiTierStrCdMax;
    String caiTierShieldCharge, caiTierGuardCd, caiTierRetHealth;
    String caiWpnRange, caiWpnSpeed;
    String assetPickerFilter;

}
