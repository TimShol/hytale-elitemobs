package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RPGMobsFeatureRegistryTest {

    @Test
    void registryContainsCoreFeatures() {
        RPGMobsFeatureRegistry registry = new RPGMobsFeatureRegistry(null);

        assertNotNull(registry.getFeature("Damage"), "Damage feature should be registered");
        assertNotNull(registry.getFeature("Spawning"), "Spawning feature should be registered");
        assertNotNull(registry.getFeature("ChargeLeap"), "ChargeLeap feature should be registered");
    }

    @Test
    void retrieveByAssetId() {
        RPGMobsFeatureRegistry registry = new RPGMobsFeatureRegistry(null);

        IRPGMobsFeature feature = registry.getFeatureByAssetId(AbilityIds.CHARGE_LEAP);
        assertNotNull(feature);
        assertEquals("ChargeLeap", feature.getFeatureKey());
    }
}
