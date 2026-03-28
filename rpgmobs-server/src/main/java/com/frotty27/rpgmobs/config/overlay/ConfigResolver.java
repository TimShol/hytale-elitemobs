package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.GlobalConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.utils.MobRuleCategoryHelpers;
import com.hypixel.hytale.logger.HytaleLogger;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ConfigResolver {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Yaml YAML = new Yaml();

    private ResolvedConfig baseResolved;

    private final Map<String, ResolvedConfig> worldConfigs = new LinkedHashMap<>();

    private final Map<String, ResolvedConfig> instanceConfigs = new LinkedHashMap<>();

    private final Map<String, ConfigOverlay> worldOverlays = new LinkedHashMap<>();

    private final Map<String, ConfigOverlay> instanceOverlays = new LinkedHashMap<>();

    private GlobalConfig globalConfig;

    private static final ResolvedConfig DISABLED;

    static {
        DISABLED = new ResolvedConfig();
        DISABLED.enabled = false;
    }

    public void loadAll(Path modDirectory, GlobalConfig globalConfig, RPGMobsConfig rpgMobsConfig) {
        this.globalConfig = globalConfig;
        worldConfigs.clear();
        instanceConfigs.clear();
        worldOverlays.clear();
        instanceOverlays.clear();

        baseResolved = buildBaseResolved(rpgMobsConfig);

        Path worldsDir = modDirectory.resolve("worlds");
        if (Files.isDirectory(worldsDir)) {
            loadOverlaysFromDirectory(worldsDir, worldConfigs, worldOverlays);
        }

        Path instancesDir = modDirectory.resolve("instances");
        if (Files.isDirectory(instancesDir)) {
            loadOverlaysFromDirectory(instancesDir, instanceConfigs, instanceOverlays);
            worldConfigs.keySet().forEach(wn -> {
                if (instanceConfigs.remove(wn) != null) instanceOverlays.remove(wn);
            });
        }

        LOGGER.atInfo().log(String.format("ConfigResolver loaded: %d world overlays, %d instance overlays, globalEnabled=%s, enabledByDefault=%s",
                                  worldConfigs.size(), instanceConfigs.size(),
                                  globalConfig != null ? String.valueOf(globalConfig.globalEnabled) : "null",
                                  globalConfig != null ? String.valueOf(globalConfig.enabledByDefault) : "null"));
        for (Map.Entry<String, ResolvedConfig> entry : worldConfigs.entrySet()) {
            LOGGER.atInfo().log(String.format("  World '%s': enabled=%s, mobRules=%d",
                                      entry.getKey(), entry.getValue().enabled, entry.getValue().mobRules.size()));
        }
        for (Map.Entry<String, ResolvedConfig> entry : instanceConfigs.entrySet()) {
            LOGGER.atInfo().log(String.format("  Instance '%s': enabled=%s, mobRules=%d",
                                      entry.getKey(), entry.getValue().enabled, entry.getValue().mobRules.size()));
        }
    }

    public ResolvedConfig getResolvedConfig(@Nullable String worldName) {
        if (globalConfig != null && !globalConfig.globalEnabled) {
            return DISABLED;
        }

        if (worldName == null) {
            return globalConfig != null && globalConfig.enabledByDefault ? baseResolved : DISABLED;
        }

        ResolvedConfig worldConfig = worldConfigs.get(worldName);
        if (worldConfig != null) return worldConfig;

        String template = resolveInstanceTemplate(worldName);
        if (template != null) {
            String matchedKey = findInstanceKeyIgnoreCase(template);
            if (matchedKey != null) return instanceConfigs.get(matchedKey);

            return (globalConfig != null && globalConfig.enabledByDefault) ? baseResolved : DISABLED;
        }

        return (globalConfig != null && globalConfig.enabledByDefault) ? baseResolved : DISABLED;
    }

    public void registerWorldIfAbsent(String worldName) {
        if (worldName == null || worldName.isBlank()) return;
        if (worldConfigs.containsKey(worldName)) return;

        String template = resolveInstanceTemplate(worldName);
        if (template != null) {
            String matchedKey = findInstanceKeyIgnoreCase(template);
            if (matchedKey != null) {
                worldConfigs.put(worldName, instanceConfigs.get(matchedKey));
                return;
            }
        }

        ResolvedConfig fallback = (globalConfig != null && globalConfig.enabledByDefault) ? baseResolved : DISABLED;
        worldConfigs.put(worldName, fallback);
        LOGGER.atInfo().log(String.format("Registered world '%s': enabled=%s (enabledByDefault=%s)",
                                  worldName, fallback.enabled,
                                  globalConfig != null ? String.valueOf(globalConfig.enabledByDefault) : "null"));
    }

    private @Nullable String findInstanceKeyIgnoreCase(String template) {
        if (instanceConfigs.containsKey(template)) return template;
        for (String key : instanceConfigs.keySet()) {
            if (key.equalsIgnoreCase(template)) return key;
        }
        return null;
    }

    public void registerInstanceIfAbsent(String templateName) {
        if (templateName == null || templateName.isBlank()) return;
        if (instanceConfigs.containsKey(templateName)) return;
        if (worldConfigs.containsKey(templateName)) return;
        instanceConfigs.put(templateName, DISABLED);
    }

    public ResolvedConfig getBaseResolved() {
        return baseResolved;
    }

    public Set<String> getWorldNames() {
        return Collections.unmodifiableSet(worldConfigs.keySet());
    }

    public Set<String> getInstanceTemplateNames() {
        return Collections.unmodifiableSet(instanceConfigs.keySet());
    }

    public @Nullable ConfigOverlay getEffectiveWorldOverlay(String worldName) {
        ConfigOverlay worldOvl = worldOverlays.get(worldName);
        if (worldOvl != null) return worldOvl;

        String template = resolveInstanceTemplate(worldName);
        if (template != null) {
            String matchedKey = findInstanceKeyIgnoreCase(template);
            if (matchedKey != null) return instanceOverlays.get(matchedKey);
        }
        return null;
    }

    public @Nullable ConfigOverlay getInstanceOverlay(String templateName) {
        return instanceOverlays.get(templateName);
    }

    public @Nullable ResolvedConfig getInstanceResolvedConfig(String templateName) {
        return instanceConfigs.get(templateName);
    }

    public @Nullable ResolvedConfig getWorldResolvedConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    public static @Nullable String resolveInstanceTemplate(@Nullable String worldName) {
        if (worldName == null) return null;
        if (!worldName.startsWith("instance-")) return null;

        if (worldName.length() <= 9 + 37) return null;
        return worldName.substring(9, worldName.length() - 37);
    }

    private ResolvedConfig buildBaseResolved(RPGMobsConfig config) {
        ResolvedConfig base = new ResolvedConfig();

        OverlayFieldRegistry.buildAllBase(config, base);

        base.progressionStyle = config.spawning.progressionStyle;
        base.spawnChancePerTier = Arrays.copyOf(config.spawning.spawnChancePerTier, config.spawning.spawnChancePerTier.length);

        base.resolvedAbilities = new LinkedHashMap<>();
        if (config.abilitiesConfig.defaultAbilities != null) {
            for (var entry : config.abilitiesConfig.defaultAbilities.entrySet()) {
                RPGMobsConfig.AbilityConfig ac = entry.getValue();
                var resolved = new ResolvedConfig.ResolvedAbilityConfig();
                resolved.enabled = ac.isEnabled;
                resolved.linkedMobEntries = new LinkedHashMap<>();
                for (String key : ac.linkedMobRuleKeys) {
                    resolved.linkedMobEntries.put(key, Arrays.copyOf(ac.isEnabledPerTier, ac.isEnabledPerTier.length));
                }
                base.resolvedAbilities.put(entry.getKey(), resolved);
            }
        }

        base.tierPrefixesByFamily = new LinkedHashMap<>(config.nameplatesConfig.defaultedTierPrefixesByFamily);

        base.tierOverrides = new LinkedHashMap<>();

        base.environmentTierRules = new LinkedHashMap<>();
        base.environmentTierRules.put("zone0", new double[]{100, 0, 0, 0, 0});
        base.environmentTierRules.put("zone1", new double[]{60, 25, 15, 0, 0});
        base.environmentTierRules.put("zone2", new double[]{50, 25, 18, 7, 0});
        base.environmentTierRules.put("zone3", new double[]{0, 32, 28, 22, 18});
        base.environmentTierRules.put("zone4", new double[]{0, 0, 40, 33, 27});

        if (config.mobsConfig.defaultMobRules != null) {
            base.mobRules = new LinkedHashMap<>(config.mobsConfig.defaultMobRules);
            backfillMobRuleWeaponArmorCategories(base.mobRules);
        }
        if (config.mobsConfig.categoryTree != null) {
            base.mobRuleCategoryTree = config.mobsConfig.categoryTree;
        }
        if (config.lootConfig.lootTemplates != null) {
            base.lootTemplates = new LinkedHashMap<>(config.lootConfig.lootTemplates);
        }
        if (config.lootConfig.lootTemplateTree != null) {
            base.lootTemplateCategoryTree = config.lootConfig.lootTemplateTree;
        }

        if (config.gearConfig.weaponCategoryTree != null) {
            base.weaponCategoryTree = config.gearConfig.weaponCategoryTree;
        }
        if (config.gearConfig.armorCategoryTree != null) {
            base.armorCategoryTree = config.gearConfig.armorCategoryTree;
            MobRuleCategoryHelpers.expandArmorMaterialsToFullIds(base.armorCategoryTree);
        }

        return base;
    }

    private static void backfillMobRuleWeaponArmorCategories(Map<String, RPGMobsConfig.MobRule> rules) {
        Map<String, RPGMobsConfig.MobRule> javaDefaults = RPGMobsConfig.defaultMobRules();
        for (var entry : rules.entrySet()) {
            RPGMobsConfig.MobRule loaded = entry.getValue();
            RPGMobsConfig.MobRule javaDefault = javaDefaults.get(entry.getKey());
            if (javaDefault == null) continue;
            if (loaded.allowedWeaponCategories.isEmpty() && !javaDefault.allowedWeaponCategories.isEmpty()) {
                loaded.allowedWeaponCategories = new ArrayList<>(javaDefault.allowedWeaponCategories);
            } else {
                loaded.allowedWeaponCategories = new ArrayList<>(loaded.allowedWeaponCategories);
            }
            if (loaded.allowedArmorCategories.isEmpty() && !javaDefault.allowedArmorCategories.isEmpty()) {
                loaded.allowedArmorCategories = new ArrayList<>(javaDefault.allowedArmorCategories);
            } else {
                loaded.allowedArmorCategories = new ArrayList<>(loaded.allowedArmorCategories);
            }
            migrateGearCategoryKeys(loaded.allowedWeaponCategories);
            migrateGearCategoryKeys(loaded.allowedArmorCategories);
        }
    }

    private static void migrateGearCategoryKeys(List<String> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (int i = 0; i < entries.size(); i++) {
            String e = entries.get(i);
            if (e != null && !e.startsWith("category:") && !e.contains("_")) {
                entries.set(i, "category:" + e);
            }
        }
    }

    private void loadOverlaysFromDirectory(Path directory, Map<String, ResolvedConfig> targetMap,
                                           Map<String, ConfigOverlay> rawOverlayMap) {
        try (var stream = Files.list(directory)) {
            stream.filter(p -> p.toString().endsWith(".yml"))
                  .forEach(overlayFile -> {
                      String fileName = overlayFile.getFileName().toString();
                      String name = fileName.substring(0, fileName.length() - 4);

                      ConfigOverlay overlay = loadOverlay(overlayFile);
                      if (overlay != null) {
                          ResolvedConfig resolved = mergeOverlay(baseResolved, overlay);
                          targetMap.put(name, resolved);
                          rawOverlayMap.put(name, overlay);
                          LOGGER.atInfo().log(String.format("Loaded overlay: %s", fileName));
                      }
                  });
        } catch (Exception e) {
            LOGGER.atWarning().log(String.format("Failed to load overlays from %s: %s",
                                         directory.toAbsolutePath(), e.getMessage()));
        }
    }

    public @Nullable ConfigOverlay loadOverlayFromPath(Path yamlFile) {
        return loadOverlay(yamlFile);
    }

    private @Nullable ConfigOverlay loadOverlay(Path yamlFile) {
        if (!Files.exists(yamlFile)) return null;

        try (Reader reader = Files.newBufferedReader(yamlFile, StandardCharsets.UTF_8)) {
            Object loaded = YAML.load(reader);
            if (!(loaded instanceof Map<?, ?> yamlMap)) return null;

            ConfigOverlay overlay = new ConfigOverlay();
            applyYamlToOverlay(overlay, toStringKeyMap(yamlMap));
            return overlay;
        } catch (Exception e) {
            LOGGER.atWarning().log(String.format("Failed to parse overlay %s: %s",
                                         yamlFile.getFileName(), e.getMessage()));
            return null;
        }
    }

    private void applyYamlToOverlay(ConfigOverlay overlay, Map<String, Object> yaml) {

        OverlayFieldRegistry.applyAllYaml(yaml, overlay);

        overlay.progressionStyle = getStringOrNull(yaml, "progressionStyle");
        overlay.spawnChancePerTier = getDoubleArrayOrNull(yaml, "spawnChancePerTier");

        Object envRulesRaw = yaml.get("environmentTierRules");
        if (envRulesRaw instanceof Map<?, ?> envMap) {
            Map<String, double[]> envRules = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : envMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof List<?> list) {
                    double[] arr = new double[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object el = list.get(i);
                        arr[i] = (el instanceof Number n) ? n.doubleValue() : 0.0;
                    }
                    envRules.put(key, arr);
                }
            }
            overlay.environmentTierRules = envRules;
        }

        Object abilityOverlaysRaw = yaml.get("abilityOverlays");
        if (abilityOverlaysRaw instanceof Map<?, ?> abilOverlayMap) {
            Map<String, ConfigOverlay.AbilityOverlay> overlays = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : abilOverlayMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> abilMap) {
                    var abilMapStr = toStringKeyMap(abilMap);
                    ConfigOverlay.AbilityOverlay ao = new ConfigOverlay.AbilityOverlay();
                    ao.enabled = getBooleanOrNull(abilMapStr, "enabled");

                    Object linkedEntriesRaw = abilMap.get("linkedEntries");
                    if (linkedEntriesRaw instanceof List<?> entriesList) {
                        ao.linkedEntries = new ArrayList<>();
                        for (Object entryObj : entriesList) {
                            if (entryObj instanceof Map<?, ?> entryMap) {
                                var ale = new ConfigOverlay.AbilityLinkedEntry();
                                Object keyObj = entryMap.get("key");
                                ale.key = keyObj != null ? String.valueOf(keyObj) : "";
                                Object eptObj = entryMap.get("enabledPerTier");
                                if (eptObj instanceof List<?> eptList) {
                                    for (int i = 0; i < Math.min(eptList.size(), 5); i++) {
                                        Object val = eptList.get(i);
                                        if (val instanceof Boolean b) ale.enabledPerTier[i] = b;
                                    }
                                }
                                ao.linkedEntries.add(ale);
                            }
                        }
                    } else {
                        Object linkedKeysRaw = abilMap.get("linkedMobRuleKeys");
                        if (linkedKeysRaw instanceof List<?> linkedList) {
                            ao.linkedEntries = new ArrayList<>();
                            for (Object item : linkedList) {
                                ao.linkedEntries.add(new ConfigOverlay.AbilityLinkedEntry(
                                        String.valueOf(item),
                                        new boolean[]{true, true, true, true, true}
                                ));
                            }
                        }
                    }
                    overlays.put(key, ao);
                }
            }
            if (!overlays.isEmpty()) overlay.abilityOverlays = overlays;
        }

        Object tierPrefixRaw = yaml.get("tierPrefixesByFamily");
        if (tierPrefixRaw instanceof Map<?, ?> tpMap) {
            Map<String, List<String>> families = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : tpMap.entrySet()) {
                String familyKey = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof List<?> prefixList) {
                    List<String> prefixes = new ArrayList<>();
                    for (Object p : prefixList) prefixes.add(p != null ? String.valueOf(p) : "");
                    families.put(familyKey, prefixes);
                }
            }
            if (!families.isEmpty()) overlay.tierPrefixesByFamily = families;
        }

        Object tierOverridesRaw = yaml.get("tierOverrides");
        if (tierOverridesRaw instanceof Map<?, ?> tierMap) {
            Map<String, ConfigOverlay.TierOverride> overrides = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : tierMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> overrideMap) {
                    ConfigOverlay.TierOverride to = parseTierOverride(toStringKeyMap(overrideMap));
                    overrides.put(key, to);
                }
            }
            if (!overrides.isEmpty()) overlay.tierOverrides = overrides;
        }

        Object disabledKeysRaw = yaml.get("disabledMobRuleKeys");
        if (disabledKeysRaw instanceof List<?> disabledList) {
            Set<String> disabledKeys = new LinkedHashSet<>();
            for (Object item : disabledList) {
                if (item != null) disabledKeys.add(String.valueOf(item));
            }
            overlay.disabledMobRuleKeys = disabledKeys;
        }

        Object lootTplsRaw = yaml.get("lootTemplates");
        if (lootTplsRaw instanceof Map<?, ?> tplsMap) {
            Map<String, RPGMobsConfig.LootTemplate> templates = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : tplsMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> tplMap) {
                    templates.put(key, parseLootTemplate(key, toStringKeyMap(tplMap)));
                }
            }
            overlay.lootTemplates = templates;
        }

        Object lootTreeRaw = yaml.get("lootTemplateCategoryTree");
        if (lootTreeRaw instanceof Map<?, ?> treeMap) {
            overlay.lootTemplateCategoryTree = parseLootTemplateCategory(toStringKeyMap(treeMap));
        }

        Object customPresetRaw = yaml.get("customPreset");
        if (customPresetRaw instanceof Map<?, ?> presetMap) {
            ConfigOverlay customPreset = new ConfigOverlay();
            applyYamlToOverlay(customPreset, toStringKeyMap(presetMap));
            overlay.customPreset = customPreset;
        }
    }

    private ResolvedConfig mergeOverlay(ResolvedConfig base, ConfigOverlay overlay) {
        ResolvedConfig merged = new ResolvedConfig();

        OverlayFieldRegistry.mergeAll(overlay, base, merged);

        if (overlay.progressionStyle != null) {
            try {
                merged.progressionStyle = RPGMobsConfig.ProgressionStyle.valueOf(
                        overlay.progressionStyle.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                LOGGER.atWarning().log("Unknown progressionStyle: " + overlay.progressionStyle);
                merged.progressionStyle = base.progressionStyle;
            }
        } else {
            merged.progressionStyle = base.progressionStyle;
        }
        merged.hasCustomSpawnChances = overlay.spawnChancePerTier != null;
        merged.spawnChancePerTier = overlay.spawnChancePerTier != null
                ? Arrays.copyOf(overlay.spawnChancePerTier, overlay.spawnChancePerTier.length)
                : Arrays.copyOf(base.spawnChancePerTier, base.spawnChancePerTier.length);

        merged.resolvedAbilities = new LinkedHashMap<>();
        for (var entry : base.resolvedAbilities.entrySet()) {
            String abilityId = entry.getKey();
            ResolvedConfig.ResolvedAbilityConfig baseAbil = entry.getValue();
            ResolvedConfig.ResolvedAbilityConfig mergedAbil = new ResolvedConfig.ResolvedAbilityConfig();
            mergedAbil.enabled = baseAbil.enabled;
            mergedAbil.linkedMobEntries = new LinkedHashMap<>();
            for (var me : baseAbil.linkedMobEntries.entrySet()) {
                mergedAbil.linkedMobEntries.put(me.getKey(), Arrays.copyOf(me.getValue(), me.getValue().length));
            }

            if (overlay.abilityOverlays != null) {
                ConfigOverlay.AbilityOverlay ao = overlay.abilityOverlays.get(abilityId);
                if (ao != null) {
                    if (ao.enabled != null) mergedAbil.enabled = ao.enabled;
                    if (ao.linkedEntries != null) {
                        mergedAbil.linkedMobEntries = new LinkedHashMap<>();
                        for (ConfigOverlay.AbilityLinkedEntry ale : ao.linkedEntries) {
                            mergedAbil.linkedMobEntries.put(ale.key, Arrays.copyOf(ale.enabledPerTier, ale.enabledPerTier.length));
                        }
                    }
                }
            }
            merged.resolvedAbilities.put(abilityId, mergedAbil);
        }

        if (overlay.tierPrefixesByFamily != null) {
            merged.tierPrefixesByFamily = new LinkedHashMap<>(overlay.tierPrefixesByFamily);
        } else {
            merged.tierPrefixesByFamily = new LinkedHashMap<>(base.tierPrefixesByFamily);
        }

        merged.tierOverrides = new LinkedHashMap<>(base.tierOverrides);
        if (overlay.tierOverrides != null) {
            merged.tierOverrides.putAll(overlay.tierOverrides);
        }

        if (overlay.environmentTierRules != null) {
            merged.environmentTierRules = new LinkedHashMap<>(overlay.environmentTierRules);
        } else {
            merged.environmentTierRules = new LinkedHashMap<>(base.environmentTierRules);
        }

        merged.mobRules = new LinkedHashMap<>(base.mobRules);
        merged.mobRuleCategoryTree = base.mobRuleCategoryTree;
        if (overlay.disabledMobRuleKeys != null) {
            merged.disabledMobRuleKeys = new LinkedHashSet<>(overlay.disabledMobRuleKeys);
            merged.disabledMobRuleKeys.retainAll(merged.mobRules.keySet());
        }

        if (overlay.lootTemplates != null) {
            merged.lootTemplates = new LinkedHashMap<>(overlay.lootTemplates);
            merged.lootTemplateCategoryTree = overlay.lootTemplateCategoryTree != null
                    ? overlay.lootTemplateCategoryTree : base.lootTemplateCategoryTree;
        } else {
            merged.lootTemplates = new LinkedHashMap<>(base.lootTemplates);
            merged.lootTemplateCategoryTree = base.lootTemplateCategoryTree;
        }

        merged.weaponCategoryTree = base.weaponCategoryTree;
        merged.armorCategoryTree = base.armorCategoryTree;

        return merged;
    }

    private static Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null) continue;
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    static @Nullable Boolean getBooleanOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    static @Nullable String getStringOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    static @Nullable Double getDoubleOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }

    static @Nullable Float getFloatOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.floatValue();
        try { return Float.parseFloat(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }

    static double @Nullable [] getDoubleArrayOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (!(v instanceof List<?> list)) return null;
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            arr[i] = (el instanceof Number n) ? n.doubleValue() : 0.0;
        }
        return arr;
    }

    static float @Nullable [] getFloatArrayOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (!(v instanceof List<?> list)) return null;
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            arr[i] = (el instanceof Number n) ? n.floatValue() : 0f;
        }
        return arr;
    }

    static int @Nullable [] getIntArrayOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (!(v instanceof List<?> list)) return null;
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            arr[i] = (el instanceof Number n) ? n.intValue() : 0;
        }
        return arr;
    }

    static boolean @Nullable [] getBooleanArrayOrNull(Map<String, Object> yaml, String key) {
        Object v = yaml.get(key);
        if (!(v instanceof List<?> list)) return null;
        boolean[] arr = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = toBoolean(list.get(i));
        }
        return arr;
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    static RPGMobsConfig.@Nullable ExtraDropRule parseExtraDropRule(Map<String, Object> map) {
        RPGMobsConfig.ExtraDropRule rule = new RPGMobsConfig.ExtraDropRule();
        Object itemId = map.get("itemId");
        if (itemId == null) return null;
        rule.itemId = String.valueOf(itemId);
        rule.chance = getDoubleOr(map, "chance", 0.0);
        Object eptObj = map.get("enabledPerTier");
        if (eptObj instanceof List<?> eptList) {
            for (int i = 0; i < Math.min(eptList.size(), 5); i++) {
                Object val = eptList.get(i);
                if (val instanceof Boolean b) rule.enabledPerTier[i] = b;
            }
        } else if (map.containsKey("minTierInclusive") || map.containsKey("maxTierInclusive")) {
            int minT = getIntOr(map, "minTierInclusive", 0);
            int maxT = getIntOr(map, "maxTierInclusive", 4);
            for (int i = 0; i < 5; i++) {
                rule.enabledPerTier[i] = i >= minT && i <= maxT;
            }
        }
        rule.minQty = getIntOr(map, "minQty", 1);
        rule.maxQty = getIntOr(map, "maxQty", 1);
        return rule;
    }

    private static ConfigOverlay.TierOverride parseTierOverride(Map<String, Object> map) {
        ConfigOverlay.TierOverride to = new ConfigOverlay.TierOverride();
        boolean[] allowedTiers = getBooleanArrayOrNull(map, "allowedTiers");
        if (allowedTiers != null) to.allowedTiers = allowedTiers;
        return to;
    }

    static double getDoubleOr(Map<String, Object> map, String key, double defaultValue) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v != null) {
            try { return Double.parseDouble(String.valueOf(v)); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    static int getIntOr(Map<String, Object> map, String key, int defaultValue) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    static RPGMobsConfig.MobRule parseMobRule(Map<String, Object> map) {
        RPGMobsConfig.MobRule rule = new RPGMobsConfig.MobRule();
        Boolean enabled = getBooleanOrNull(map, "enabled");
        if (enabled != null) rule.enabled = enabled;
        rule.matchExact = getStringList(map, "matchExact");
        rule.matchStartsWith = getStringList(map, "matchStartsWith");
        rule.matchContains = getStringList(map, "matchContains");
        rule.matchExcludes = getStringList(map, "matchExcludes");
        boolean[] wpnTiers = getBooleanArrayOrNull(map, "enableWeaponOverrideForTier");
        if (wpnTiers != null) rule.enableWeaponOverrideForTier = wpnTiers;
        String mode = getStringOrNull(map, "weaponOverrideMode");
        if (mode != null) {
            try { rule.weaponOverrideMode = RPGMobsConfig.WeaponOverrideMode.valueOf(mode); } catch (IllegalArgumentException ignored) {}
        }
        rule.allowedWeaponCategories = getStringList(map, "allowedWeaponCategories");
        rule.allowedArmorCategories = getStringList(map, "allowedArmorCategories");
        migrateGearCategoryKeys(rule.allowedWeaponCategories);
        migrateGearCategoryKeys(rule.allowedArmorCategories);
        rule.allowedArmorSlots = getStringList(map, "allowedArmorSlots");
        return rule;
    }

    static RPGMobsConfig.MobRuleCategory parseMobRuleCategory(Map<String, Object> map) {
        RPGMobsConfig.MobRuleCategory cat = new RPGMobsConfig.MobRuleCategory();
        String name = getStringOrNull(map, "name");
        if (name != null) cat.name = name;
        cat.mobRuleKeys = getStringList(map, "mobRuleKeys");
        Object childrenRaw = map.get("children");
        if (childrenRaw instanceof List<?> childList) {
            cat.children = new ArrayList<>();
            for (Object child : childList) {
                if (child instanceof Map<?, ?> childMap) {
                    cat.children.add(parseMobRuleCategory(toStringKeyMap(childMap)));
                }
            }
        }
        return cat;
    }

    private static RPGMobsConfig.LootTemplate parseLootTemplate(String name, Map<String, Object> map) {
        RPGMobsConfig.LootTemplate template = new RPGMobsConfig.LootTemplate();
        template.name = name;
        template.linkedMobRuleKeys = getStringList(map, "linkedMobRuleKeys");
        Object dropsRaw = map.get("drops");
        if (dropsRaw instanceof List<?> dropList) {
            template.drops = new ArrayList<>();
            for (Object drop : dropList) {
                if (drop instanceof Map<?, ?> dropMap) {
                    RPGMobsConfig.ExtraDropRule rule = parseExtraDropRule(toStringKeyMap(dropMap));
                    if (rule != null) template.drops.add(rule);
                }
            }
        }
        return template;
    }

    private static RPGMobsConfig.LootTemplateCategory parseLootTemplateCategory(Map<String, Object> map) {
        RPGMobsConfig.LootTemplateCategory cat = new RPGMobsConfig.LootTemplateCategory();
        String name = getStringOrNull(map, "name");
        if (name != null) cat.name = name;
        cat.templateKeys = getStringList(map, "templateKeys");
        Object childrenRaw = map.get("children");
        if (childrenRaw instanceof List<?> childList) {
            cat.children = new ArrayList<>();
            for (Object child : childList) {
                if (child instanceof Map<?, ?> childMap) {
                    cat.children.add(parseLootTemplateCategory(toStringKeyMap(childMap)));
                }
            }
        }
        return cat;
    }

    static List<String> getStringList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof List<?> list)) return List.of();
        var result = new ArrayList<String>();
        for (Object item : list) {
            if (item != null) result.add(String.valueOf(item));
        }
        return result;
    }
}
