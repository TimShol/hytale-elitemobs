package com.frotty27.rpgmobs.components.lifecycle;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RPGMobsScannedComponent implements Component<EntityStore> {

    public int scannedAtConfigVersion;

    private static final KeyedCodec<Integer> K_SCANNED_AT_CONFIG_VERSION = new KeyedCodec<>("ScannedAtConfigVersion",
                                                                                            new IntegerCodec()
    );

    public static final BuilderCodec<RPGMobsScannedComponent> CODEC = BuilderCodec.builder(RPGMobsScannedComponent.class,
                                                                                            RPGMobsScannedComponent::new
    ).append(K_SCANNED_AT_CONFIG_VERSION, (c, v) -> c.scannedAtConfigVersion = v, c -> c.scannedAtConfigVersion).add().build();

    public RPGMobsScannedComponent() {
        this.scannedAtConfigVersion = 0;
    }

    public RPGMobsScannedComponent(int version) {
        this.scannedAtConfigVersion = version;
    }

    @Override
    public Component<EntityStore> clone() {
        return new RPGMobsScannedComponent(scannedAtConfigVersion);
    }
}
