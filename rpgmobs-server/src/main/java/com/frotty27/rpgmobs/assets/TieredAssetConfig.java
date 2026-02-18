package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.schema.FixedArraySize;

import static com.frotty27.rpgmobs.utils.Constants.TIERS_AMOUNT;

public abstract class TieredAssetConfig extends AssetConfig {
    public boolean isEnabled = true;
    @FixedArraySize(value = TIERS_AMOUNT)
    public boolean[] isEnabledPerTier = new boolean[]{false, false, false, true, true};

    public final RPGMobsAssetTemplates templates = new RPGMobsAssetTemplates();
}
