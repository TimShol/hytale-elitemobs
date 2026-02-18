package com.frotty27.rpgmobs.components.lifecycle;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RPGMobsMigrationComponent implements Component<EntityStore> {

    public int migrationVersion;

    private static final KeyedCodec<Integer> K_MIGRATION_VERSION = new KeyedCodec<>("MigrationVersion",
                                                                                    new IntegerCodec()
    );

    public static final BuilderCodec<RPGMobsMigrationComponent> CODEC = BuilderCodec.builder(RPGMobsMigrationComponent.class,
                                                                                             RPGMobsMigrationComponent::new
    ).append(K_MIGRATION_VERSION, (c, v) -> c.migrationVersion = v, c -> c.migrationVersion).add().build();

    public RPGMobsMigrationComponent() {
        this.migrationVersion = 0;
    }

    public RPGMobsMigrationComponent(int version) {
        this.migrationVersion = version;
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsMigrationComponent c = new RPGMobsMigrationComponent();
        c.migrationVersion = this.migrationVersion;
        return c;
    }

    public boolean needsMigration() {
        return migrationVersion < 2;
    }
}
