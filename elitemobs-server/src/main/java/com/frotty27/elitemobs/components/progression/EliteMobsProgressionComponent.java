package com.frotty27.elitemobs.components.progression;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record EliteMobsProgressionComponent(
        float distanceHealthBonus,
        float distanceDamageBonus,
        float spawnDistanceMeters
) implements Component<EntityStore> {

    private static final KeyedCodec<Float> K_DISTANCE_HEALTH_BONUS =
            new KeyedCodec<>("DistanceHealthBonus", new FloatCodec());
    private static final KeyedCodec<Float> K_DISTANCE_DAMAGE_BONUS =
            new KeyedCodec<>("DistanceDamageBonus", new FloatCodec());
    private static final KeyedCodec<Float> K_SPAWN_DISTANCE_METERS =
            new KeyedCodec<>("SpawnDistanceMeters", new FloatCodec());

    public static final BuilderCodec<EliteMobsProgressionComponent> CODEC =
            BuilderCodec.builder(EliteMobsProgressionComponent.class, EliteMobsProgressionComponent::new)
                    .append(K_DISTANCE_HEALTH_BONUS, (c, v) -> {}, c -> c.distanceHealthBonus).add()
                    .append(K_DISTANCE_DAMAGE_BONUS, (c, v) -> {}, c -> c.distanceDamageBonus).add()
                    .append(K_SPAWN_DISTANCE_METERS, (c, v) -> {}, c -> c.spawnDistanceMeters).add()
                    .build();

    public EliteMobsProgressionComponent() {
        this(0f, 0f, 0f);
    }

    public static EliteMobsProgressionComponent create(float spawnDistanceMeters, float healthBonusPerMeter, float damageBonusPerMeter) {
        return new EliteMobsProgressionComponent(
                spawnDistanceMeters * healthBonusPerMeter,
                spawnDistanceMeters * damageBonusPerMeter,
                spawnDistanceMeters
        );
    }

    @Override
    public Component<EntityStore> clone() {
        return new EliteMobsProgressionComponent(distanceHealthBonus, distanceDamageBonus, spawnDistanceMeters);
    }
}
