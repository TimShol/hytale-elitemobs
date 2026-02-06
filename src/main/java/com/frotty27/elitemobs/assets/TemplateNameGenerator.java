package com.frotty27.elitemobs.assets;

import com.frotty27.elitemobs.config.EliteMobsConfig;

import java.util.Locale;

public final class TemplateNameGenerator {

    public static final String TEMPLATE_SUFFIX = ".template.json";

    private TemplateNameGenerator() {}

    /**
     * Extracts the base template name from a path by removing:
     * - directory prefixes
     * - ".template.json" or ".json" suffixes
     *
     * Example:
     *   "ServerTemplates/Foo/EliteMobs_Leap_Slam.template.json"
     *   -> "EliteMobs_Leap_Slam"
     */
    public static String getBaseTemplateNameFromPath(String templatePath) {
        if (templatePath == null) return null;

        String normalizedPath = templatePath.trim().replace('\\', '/');
        if (normalizedPath.isEmpty()) return null;

        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        String fileName =
                (lastSlashIndex >= 0)
                        ? normalizedPath.substring(lastSlashIndex + 1)
                        : normalizedPath;

        String lowercaseFileName = fileName.toLowerCase(Locale.ROOT);

        if (lowercaseFileName.endsWith(TEMPLATE_SUFFIX)) {
            return fileName.substring(0, fileName.length() - TEMPLATE_SUFFIX.length());
        }

        if (lowercaseFileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - ".json".length());
        }

        return fileName;
    }

    /**
     * Returns the tier suffix for the given tier index.
     *
     * Uses {@link EliteMobsConfig.AssetGeneratorConfig#tierSuffixes} if available,
     * otherwise falls back to "T{tierIndex + 1}".
     */
    public static String getTierSuffix(EliteMobsConfig config, int tierIndex) {
        String fallbackSuffix = "T" + (tierIndex + 1);

        if (config == null) return fallbackSuffix;
        if (config.assetGenerator.tierSuffixes == null) return fallbackSuffix;
        if (tierIndex < 0 || tierIndex >= config.assetGenerator.tierSuffixes.length) return fallbackSuffix;

        String configuredSuffix = config.assetGenerator.tierSuffixes[tierIndex];
        if (configuredSuffix == null || configuredSuffix.isBlank()) return fallbackSuffix;

        return configuredSuffix
                .trim()
                .replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    /**
     * Appends a tier suffix to a base template id.
     *
     * Example:
     *   baseTemplateId = "EliteMobs_Leap_Slam"
     *   tierIndex = 2
     *   -> "EliteMobs_Leap_Slam_T3"
     */
    public static String appendTierSuffix(
            String baseTemplateId,
            EliteMobsConfig config,
            int tierIndex
    ) {
        if (baseTemplateId == null || baseTemplateId.isBlank()) return null;
        return baseTemplateId + "_" + getTierSuffix(config, tierIndex);
    }

    /**
     * Converts a template path directly into a tiered template id.
     *
     * Example:
     *   "ServerTemplates/Foo/EliteMobs_Leap_Slam.template.json"
     *   tierIndex = 1
     *   -> "EliteMobs_Leap_Slam_T2"
     */
    public static String getTemplateNameWithTierFromPath(
            String templatePath,
            EliteMobsConfig config,
            int tierIndex
    ) {
        String baseTemplateId = getBaseTemplateNameFromPath(templatePath);
        if (baseTemplateId == null || baseTemplateId.isBlank()) return null;

        return appendTierSuffix(baseTemplateId, config, tierIndex);
    }
}
