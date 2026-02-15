package com.frotty27.elitemobs.assets;

public enum AssetType {

    ABILITIES("abilities"),
    EFFECTS("effects"),
    CONSUMABLES("consumables");

    private final String id;

    AssetType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
