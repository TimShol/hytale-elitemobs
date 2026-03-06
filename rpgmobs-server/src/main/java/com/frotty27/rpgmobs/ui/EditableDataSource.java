package com.frotty27.rpgmobs.ui;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

public interface EditableDataSource {

    void applyToConfig(RPGMobsConfig config);

    void snapshot();

    boolean hasChanges();
}
