package com.frotty27.rpgmobs.assets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;

public final class RPGMobsAssetTemplates extends LinkedHashMap<String, String> {

    public @Nullable String getTemplate(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) return null;

        String templatePath = get(templateKey);
        if (templatePath == null || templatePath.isBlank()) return null;

        return templatePath.trim();
    }


    public void add(String templateKey, String templatePath) {
        if (templateKey == null || templateKey.isBlank()) return;
        if (templatePath == null || templatePath.isBlank()) return;

        put(templateKey.trim(), templatePath.trim());
    }


    public void add(String templatePath) {
        if (templatePath == null || templatePath.isBlank()) return;

        String derivedTemplateKey = deriveKeyFromTemplatePath(templatePath);
        if (derivedTemplateKey == null || derivedTemplateKey.isBlank()) return;

        put(derivedTemplateKey.trim(), templatePath.trim());
    }

    public static @Nullable String deriveKeyFromTemplatePath(String templatePath) {
        if (templatePath == null) return null;

        String normalizedTemplatePath = templatePath.trim().replace('\\', '/');
        if (normalizedTemplatePath.isEmpty()) return null;

        int lastSlashIndex = normalizedTemplatePath.lastIndexOf('/');
        String templateName = getTemplateBaseName(lastSlashIndex, normalizedTemplatePath).trim();
        if (templateName.isEmpty()) return null;

        return toLowerCamelCase(templateName);
    }

    private static @NonNull String getTemplateBaseName(int lastSlashIndex, String normalizedTemplatePath) {
        String fileName = (lastSlashIndex >= 0) ? normalizedTemplatePath.substring(lastSlashIndex + 1) : normalizedTemplatePath;

        String lowercaseFileName = fileName.toLowerCase(Locale.ROOT);
        if (lowercaseFileName.endsWith(".template.json")) {
            fileName = fileName.substring(0, fileName.length() - ".template.json".length());
        } else if (lowercaseFileName.endsWith(".json")) {
            fileName = fileName.substring(0, fileName.length() - ".json".length());
        }

        int lastUnderscoreIndex = fileName.lastIndexOf('_');
        return (lastUnderscoreIndex >= 0) ? fileName.substring(lastUnderscoreIndex + 1) : fileName;
    }

    private static String toLowerCamelCase(String text) {
        if (text == null || text.isEmpty()) return text;
        if (text.length() == 1) return text.toLowerCase(Locale.ROOT);
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
