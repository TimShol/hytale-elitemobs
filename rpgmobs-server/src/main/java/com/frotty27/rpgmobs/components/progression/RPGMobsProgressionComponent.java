package com.frotty27.rpgmobs.components.progression;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record RPGMobsProgressionComponent(float distanceHealthBonus, float distanceDamageBonus,
                                          float spawnDistanceMeters) implements Component<EntityStore> {

    private static final KeyedCodec<Float> K_DISTANCE_HEALTH_BONUS = new KeyedCodec<>("DistanceHealthBonus",
                                                                                      new FloatCodec()
    );
    private static final KeyedCodec<Float> K_DISTANCE_DAMAGE_BONUS = new KeyedCodec<>("DistanceDamageBonus",
                                                                                      new FloatCodec()
    );
    private static final KeyedCodec<Float> K_SPAWN_DISTANCE_METERS = new KeyedCodec<>("SpawnDistanceMeters",
                                                                                      new FloatCodec()
    );

    public static final BuilderCodec<RPGMobsProgressionComponent> CODEC = BuilderCodec.builder(
            RPGMobsProgressionComponent.class,
            RPGMobsProgressionComponent::new
    ).append(K_DISTANCE_HEALTH_BONUS, (_, _) -> {
             }, c -> c.distanceHealthBonus
    ).add().append(K_DISTANCE_DAMAGE_BONUS, (_, _) -> {
                   }, c -> c.distanceDamageBonus
    ).add().append(K_SPAWN_DISTANCE_METERS, (_, _) -> {
                   }, c -> c.spawnDistanceMeters
    ).add().build();

    public RPGMobsProgressionComponent() {
        this(0f, 0f, 0f);
    }

    @Override
    public Component<EntityStore> clone() {
        return new RPGMobsProgressionComponent(distanceHealthBonus, distanceDamageBonus, spawnDistanceMeters);
    }
}
