package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;

import java.util.Map;

public final class AssetConfigHelpers {

    private AssetConfigHelpers() {
    }

    // --------------------------------------------
    // Generic access by (AssetType, key)
    // --------------------------------------------

    public static AssetConfig getAssetConfig(EliteMobsConfig config, AssetType type, String key) {
        if (config == null || type == null || key == null || key.isBlank()) return null;

        Map<String, ? extends AssetConfig> map = config.getAssetConfigForType(type);
        if (map == null) return null;

        return map.get(key);
    }

    public static boolean isAssetConfigEnabledForTier(AssetConfig cfg, int tierIndex) {
        if (cfg == null) return false;
        if (cfg instanceof TieredAssetConfig t) return isTieredAssetConfigEnabledForTier(t, tierIndex);
        return true;
    }

    public static boolean isTieredAssetConfigEnabledForTier(TieredAssetConfig cfg, int tierIndex) {
        if (cfg == null || !cfg.isEnabled) return false;

        boolean[] perTier = cfg.isEnabledPerTier;
        if (perTier == null) return true;
        return tierIndex >= 0 && tierIndex < perTier.length && perTier[tierIndex];
    }

    // --------------------------------------------
    // Templates
    // --------------------------------------------

    public static String getTemplatePath(TieredAssetConfig cfg, String templateKey) {
        if (cfg == null) return null;
        if (templateKey == null || templateKey.isBlank()) return null;

        String path = cfg.templates.getTemplate(templateKey);
        return (path == null || path.isBlank()) ? null : path.trim();
    }

    public static String getOnlyTemplatePath(TieredAssetConfig cfg) {
        if (cfg == null || cfg.templates.isEmpty()) return null;

        for (String v : cfg.templates.values()) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    // --------------------------------------------
    // Template -> tiered asset id
    // --------------------------------------------

    public static String getTieredAssetIdFromTemplateKey(EliteMobsConfig rootCfg, AssetConfig cfg, String templateKey,
                                                         int tierIndex) {
        if (!(cfg instanceof TieredAssetConfig tieredCfg)) return null;

        String templatePath = getTemplatePath(tieredCfg, templateKey);
        if (templatePath == null) return null;

        return TemplateNameGenerator.getTemplateNameWithTierFromPath(templatePath, rootCfg, tierIndex);
    }

    public static String getTieredAssetIdFromOnlyTemplate(EliteMobsConfig rootCfg, AssetConfig cfg, int tierIndex) {
        if (!(cfg instanceof TieredAssetConfig tieredCfg)) return null;

        String templatePath = getOnlyTemplatePath(tieredCfg);
        if (templatePath == null) return null;

        return TemplateNameGenerator.getTemplateNameWithTierFromPath(templatePath, rootCfg, tierIndex);
    }

    // --------------------------------------------
    // Tier-safe array readers
    // --------------------------------------------

    public static float getFloatForTier(float[] arr, int tierIndex, float fallback) {
        if (arr == null) return fallback;
        if (tierIndex < 0 || tierIndex >= arr.length) return fallback;
        return arr[tierIndex];
    }

    public static int getIntForTier(int[] arr, int tierIndex, int fallback) {
        if (arr == null) return fallback;
        if (tierIndex < 0 || tierIndex >= arr.length) return fallback;
        return arr[tierIndex];
    }

    public static boolean getBoolForTier(boolean[] arr, int tierIndex, boolean fallback) {
        if (arr == null) return fallback;
        if (tierIndex < 0 || tierIndex >= arr.length) return fallback;
        return arr[tierIndex];
    }
}
