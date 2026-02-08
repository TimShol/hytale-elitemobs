package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

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

        EliteMobsFeature feature = registry.getFeatureByAssetId("charge_leap");
        assertNotNull(feature);
        assertEquals("ChargeLeap", feature.getFeatureKey());
    }
}
