package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetConfigHelpersTest {

    @Test
    void tieredAssetIdFromTemplateKey() {
        EliteMobsConfig cfg = new EliteMobsConfig();

        EliteMobsConfig.AbilityConfig ability = new EliteMobsConfig.AbilityConfig();
        ability.templates.add(EliteMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                "Item/RootInteractions/NPCs/EliteMobs/EliteMobs_Ability_ChargeLeap_RootInteraction.template.json");

        String id = AssetConfigHelpers.getTieredAssetIdFromTemplateKey(
                cfg,
                ability,
                EliteMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION,
                0
        );

        assertTrue(id.startsWith("EliteMobs_Ability_ChargeLeap_"));
        assertTrue(id.contains("RootInteraction"));
        assertTrue(id.endsWith("Tier_1"));
    }

    @Test
    void onlyTemplatePathReturnsTieredId() {
        EliteMobsConfig cfg = new EliteMobsConfig();

        EliteMobsConfig.EntityEffectConfig effect = new EliteMobsConfig.EntityEffectConfig();
        effect.templates.add("Entity/Effects/EliteMobs/EliteMobs_EntityEffect_ProjectileResistance.template.json");

        String id = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(cfg, effect, 4);

        assertTrue(id.startsWith("EliteMobs_EntityEffect_ProjectileResistance_"));
        assertTrue(id.endsWith("Tier_5"));
    }

    @Test
    void enabledPerTierRespectsFlags() {
        TieredAssetConfig cfg = new EliteMobsConfig.AbilityConfig();
        cfg.isEnabled = true;
        cfg.isEnabledPerTier = new boolean[]{true, false, true, true, true};

        assertTrue(AssetConfigHelpers.isTieredAssetConfigEnabledForTier(cfg, 0));
        assertFalse(AssetConfigHelpers.isTieredAssetConfigEnabledForTier(cfg, 1));
    }

    @Test
    void safeCastReturnsNullForNonTiered() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        EliteMobsConfig.ConsumableConfig consumable = new EliteMobsConfig.ConsumableConfig();

        String id = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(cfg, consumable, 0);
        assertTrue(id == null || id.isBlank());
    }
}
