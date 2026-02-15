package com.frotty27.elitemobs.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EliteMobsAssetTemplatesTest {

    @Test
    void deriveKeyFromTemplatePath() {
        String key1 = EliteMobsAssetTemplates.deriveKeyFromTemplatePath(
                "Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_ChargeLeap_RootInteraction.template.json");
        String key2 = EliteMobsAssetTemplates.deriveKeyFromTemplatePath(
                "Entity/Effects/EliteMobs/EliteMobs_EntityEffect_ProjectileResistance.template.json");
        String key3 = EliteMobsAssetTemplates.deriveKeyFromTemplatePath("  ");

        assertTrue(key1 != null && !key1.isBlank());
        assertTrue(key2 != null && !key2.isBlank());
        assertTrue(key3 == null || key3.isBlank());
    }

    @Test
    void addDerivesKeyWhenOnlyPathProvided() {
        EliteMobsAssetTemplates templates = new EliteMobsAssetTemplates();
        templates.add("Entity/Effects/EliteMobs/EliteMobs_EntityEffect_ProjectileResistance.template.json");
        assertTrue(templates.size() >= 1);
        assertTrue(templates.get("projectileResistance") != null);
    }
}
