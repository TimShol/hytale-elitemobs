package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.systems.ability.AbilityIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EliteMobsFeatureRegistryTest {

    @Test
    void registryContainsCoreFeatures() {
        EliteMobsFeatureRegistry registry = new EliteMobsFeatureRegistry(null);

        assertNotNull(registry.getFeature("Damage"), "Damage feature should be registered");
        assertNotNull(registry.getFeature("Spawning"), "Spawning feature should be registered");
        assertNotNull(registry.getFeature("ChargeLeap"), "ChargeLeap feature should be registered");
    }

    @Test
    void retrieveByAssetId() {
        EliteMobsFeatureRegistry registry = new EliteMobsFeatureRegistry(null);

        IEliteMobsFeature feature = registry.getFeatureByAssetId(AbilityIds.CHARGE_LEAP);
        assertNotNull(feature);
        assertEquals("ChargeLeap", feature.getFeatureKey());
    }
}
