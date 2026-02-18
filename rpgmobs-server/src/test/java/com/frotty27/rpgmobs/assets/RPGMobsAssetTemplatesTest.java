package com.frotty27.rpgmobs.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RPGMobsAssetTemplatesTest {

    @Test
    void deriveKeyFromTemplatePath() {
        String key1 = RPGMobsAssetTemplates.deriveKeyFromTemplatePath(
                "Item/RootInteractions/NPCs/RPGMobs/RPGMobs_Ability_ChargeLeap_RootInteraction.template.json");
        String key2 = RPGMobsAssetTemplates.deriveKeyFromTemplatePath(
                "Entity/Effects/RPGMobs/RPGMobs_EntityEffect_ProjectileResistance.template.json");
        String key3 = RPGMobsAssetTemplates.deriveKeyFromTemplatePath("  ");

        assertTrue(key1 != null && !key1.isBlank());
        assertTrue(key2 != null && !key2.isBlank());
        assertTrue(key3 == null || key3.isBlank());
    }

    @Test
    void addDerivesKeyWhenOnlyPathProvided() {
        RPGMobsAssetTemplates templates = new RPGMobsAssetTemplates();
        templates.add("Entity/Effects/RPGMobs/RPGMobs_EntityEffect_ProjectileResistance.template.json");
        assertTrue(templates.size() >= 1);
        assertTrue(templates.get("projectileResistance") != null);
    }
}
