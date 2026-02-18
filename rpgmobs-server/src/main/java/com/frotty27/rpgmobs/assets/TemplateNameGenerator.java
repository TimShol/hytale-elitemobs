package com.frotty27.rpgmobs.assets;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.Locale;

public final class TemplateNameGenerator {

    public static final String TEMPLATE_SUFFIX = ".template.json";

    private TemplateNameGenerator() {
    }


    public static String getBaseTemplateNameFromPath(String templatePath) {
        if (templatePath == null) return null;

        String normalizedPath = templatePath.trim().replace('\\', '/');
        if (normalizedPath.isEmpty()) return null;

        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        String fileName = (lastSlashIndex >= 0) ? normalizedPath.substring(lastSlashIndex + 1) : normalizedPath;

        String lowercaseFileName = fileName.toLowerCase(Locale.ROOT);

        if (lowercaseFileName.endsWith(TEMPLATE_SUFFIX)) {
            return fileName.substring(0, fileName.length() - TEMPLATE_SUFFIX.length());
        }

        if (lowercaseFileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - ".json".length());
        }

        return fileName;
    }


    public static String getTierSuffix(RPGMobsConfig config, int tierIndex) {
        String fallbackSuffix = "T" + (tierIndex + 1);

        if (config == null) return fallbackSuffix;
        if (config.assetGenerator.tierSuffixes == null) return fallbackSuffix;
        if (tierIndex < 0 || tierIndex >= config.assetGenerator.tierSuffixes.length) return fallbackSuffix;

        String configuredSuffix = config.assetGenerator.tierSuffixes[tierIndex];
        if (configuredSuffix == null || configuredSuffix.isBlank()) return fallbackSuffix;

        return configuredSuffix.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
    }


    public static String appendTierSuffix(String baseTemplateId, RPGMobsConfig config, int tierIndex) {
        if (baseTemplateId == null || baseTemplateId.isBlank()) return null;
        return baseTemplateId + "_" + getTierSuffix(config, tierIndex);
    }


    public static String getTemplateNameWithTierFromPath(String templatePath, RPGMobsConfig config, int tierIndex) {
        String baseTemplateId = getBaseTemplateNameFromPath(templatePath);
        if (baseTemplateId == null || baseTemplateId.isBlank()) return null;

        return appendTierSuffix(baseTemplateId, config, tierIndex);
    }
}
