package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.schema.YamlIgnore;

public abstract class AssetConfig {

    @YamlIgnore
    private transient String key;


    public final String key() {
        return key;
    }

    public final void setKeyIfBlank(String k) {
        if (k == null || k.isBlank()) return;
        if (this.key == null || this.key.isBlank()) this.key = k.trim();
    }

    public abstract AssetType namespace();
}
