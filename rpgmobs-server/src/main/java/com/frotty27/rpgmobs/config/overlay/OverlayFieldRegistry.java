package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class OverlayFieldRegistry {

    private static final List<OverlayFieldDescriptor> FIELDS = List.of(

            boolField("enabled",
                    _ -> true, overlay -> overlay.enabled, (overlay, value) -> overlay.enabled = value,
                    resolved -> resolved.enabled, (resolved, value) -> resolved.enabled = value),

            doubleField("distancePerTier",
                    config -> config.spawning.distancePerTier, overlay -> overlay.distancePerTier, (overlay, value) -> overlay.distancePerTier = value,
                    resolved -> resolved.distancePerTier, (resolved, value) -> resolved.distancePerTier = value),
            doubleField("distanceBonusInterval",
                    config -> config.spawning.distanceBonusInterval, overlay -> overlay.distanceBonusInterval, (overlay, value) -> overlay.distanceBonusInterval = value,
                    resolved -> resolved.distanceBonusInterval, (resolved, value) -> resolved.distanceBonusInterval = value),
            floatField("distanceHealthBonusPerInterval",
                    config -> config.spawning.distanceHealthBonusPerInterval, overlay -> overlay.distanceHealthBonusPerInterval, (overlay, value) -> overlay.distanceHealthBonusPerInterval = value,
                    resolved -> resolved.distanceHealthBonusPerInterval, (resolved, value) -> resolved.distanceHealthBonusPerInterval = value),
            floatField("distanceDamageBonusPerInterval",
                    config -> config.spawning.distanceDamageBonusPerInterval, overlay -> overlay.distanceDamageBonusPerInterval, (overlay, value) -> overlay.distanceDamageBonusPerInterval = value,
                    resolved -> resolved.distanceDamageBonusPerInterval, (resolved, value) -> resolved.distanceDamageBonusPerInterval = value),
            floatField("distanceHealthBonusCap",
                    config -> config.spawning.distanceHealthBonusCap, overlay -> overlay.distanceHealthBonusCap, (overlay, value) -> overlay.distanceHealthBonusCap = value,
                    resolved -> resolved.distanceHealthBonusCap, (resolved, value) -> resolved.distanceHealthBonusCap = value),
            floatField("distanceDamageBonusCap",
                    config -> config.spawning.distanceDamageBonusCap, overlay -> overlay.distanceDamageBonusCap, (overlay, value) -> overlay.distanceDamageBonusCap = value,
                    resolved -> resolved.distanceDamageBonusCap, (resolved, value) -> resolved.distanceDamageBonusCap = value),

            boolField("enableHealthScaling",
                    config -> config.healthConfig.enableMobHealthScaling, overlay -> overlay.enableHealthScaling, (overlay, value) -> overlay.enableHealthScaling = value,
                    resolved -> resolved.enableHealthScaling, (resolved, value) -> resolved.enableHealthScaling = value),
            floatArrayField("healthMultiplierPerTier",
                    config -> config.healthConfig.mobHealthMultiplierPerTier, overlay -> overlay.healthMultiplierPerTier, (overlay, value) -> overlay.healthMultiplierPerTier = value,
                    resolved -> resolved.healthMultiplierPerTier, (resolved, value) -> resolved.healthMultiplierPerTier = value),
            floatField("healthRandomVariance",
                    config -> config.healthConfig.mobHealthRandomVariance, overlay -> overlay.healthRandomVariance, (overlay, value) -> overlay.healthRandomVariance = value,
                    resolved -> resolved.healthRandomVariance, (resolved, value) -> resolved.healthRandomVariance = value),
            boolField("enableDamageScaling",
                    config -> config.damageConfig.enableMobDamageMultiplier, overlay -> overlay.enableDamageScaling, (overlay, value) -> overlay.enableDamageScaling = value,
                    resolved -> resolved.enableDamageScaling, (resolved, value) -> resolved.enableDamageScaling = value),
            floatArrayField("damageMultiplierPerTier",
                    config -> config.damageConfig.mobDamageMultiplierPerTier, overlay -> overlay.damageMultiplierPerTier, (overlay, value) -> overlay.damageMultiplierPerTier = value,
                    resolved -> resolved.damageMultiplierPerTier, (resolved, value) -> resolved.damageMultiplierPerTier = value),
            floatField("damageRandomVariance",
                    config -> config.damageConfig.mobDamageRandomVariance, overlay -> overlay.damageRandomVariance, (overlay, value) -> overlay.damageRandomVariance = value,
                    resolved -> resolved.damageRandomVariance, (resolved, value) -> resolved.damageRandomVariance = value),

            intArrayField("vanillaDroplistExtraRollsPerTier",
                    config -> config.lootConfig.vanillaDroplistExtraRollsPerTier, overlay -> overlay.vanillaDroplistExtraRollsPerTier, (overlay, value) -> overlay.vanillaDroplistExtraRollsPerTier = value,
                    resolved -> resolved.vanillaDroplistExtraRollsPerTier, (resolved, value) -> resolved.vanillaDroplistExtraRollsPerTier = value),
            doubleField("dropWeaponChance",
                    config -> config.lootConfig.dropWeaponChance, overlay -> overlay.dropWeaponChance, (overlay, value) -> overlay.dropWeaponChance = value,
                    resolved -> resolved.dropWeaponChance, (resolved, value) -> resolved.dropWeaponChance = value),
            doubleField("dropArmorPieceChance",
                    config -> config.lootConfig.dropArmorPieceChance, overlay -> overlay.dropArmorPieceChance, (overlay, value) -> overlay.dropArmorPieceChance = value,
                    resolved -> resolved.dropArmorPieceChance, (resolved, value) -> resolved.dropArmorPieceChance = value),
            doubleField("dropOffhandItemChance",
                    config -> config.lootConfig.dropOffhandItemChance, overlay -> overlay.dropOffhandItemChance, (overlay, value) -> overlay.dropOffhandItemChance = value,
                    resolved -> resolved.dropOffhandItemChance, (resolved, value) -> resolved.dropOffhandItemChance = value),
            doubleField("droppedGearDurabilityMin",
                    config -> config.gearConfig.spawnGearDurabilityMin, overlay -> overlay.droppedGearDurabilityMin, (overlay, value) -> overlay.droppedGearDurabilityMin = value,
                    resolved -> resolved.droppedGearDurabilityMin, (resolved, value) -> resolved.droppedGearDurabilityMin = value),
            doubleField("droppedGearDurabilityMax",
                    config -> config.gearConfig.spawnGearDurabilityMax, overlay -> overlay.droppedGearDurabilityMax, (overlay, value) -> overlay.droppedGearDurabilityMax = value,
                    resolved -> resolved.droppedGearDurabilityMax, (resolved, value) -> resolved.droppedGearDurabilityMax = value),
            stringField("defaultLootTemplate",
                    _ -> "", overlay -> overlay.defaultLootTemplate, (overlay, value) -> overlay.defaultLootTemplate = value,
                    resolved -> resolved.defaultLootTemplate, (resolved, value) -> resolved.defaultLootTemplate = value),

            boolField("eliteFallDamageDisabled",
                    _ -> true, overlay -> overlay.eliteFallDamageDisabled, (overlay, value) -> overlay.eliteFallDamageDisabled = value,
                    resolved -> resolved.eliteFallDamageDisabled, (resolved, value) -> resolved.eliteFallDamageDisabled = value),

            boolField("enableNameplates",
                    config -> config.nameplatesConfig.enableMobNameplates, overlay -> overlay.enableNameplates, (overlay, value) -> overlay.enableNameplates = value,
                    resolved -> resolved.enableNameplates, (resolved, value) -> resolved.enableNameplates = value),
            stringField("nameplateMode",
                    config -> config.nameplatesConfig.nameplateMode.name(), overlay -> overlay.nameplateMode, (overlay, value) -> overlay.nameplateMode = value,
                    resolved -> resolved.nameplateMode, (resolved, value) -> resolved.nameplateMode = value),
            boolArrayField("nameplateTierEnabled",
                    config -> config.nameplatesConfig.mobNameplatesEnabledPerTier, overlay -> overlay.nameplateTierEnabled, (overlay, value) -> overlay.nameplateTierEnabled = value,
                    resolved -> resolved.nameplateTierEnabled, (resolved, value) -> resolved.nameplateTierEnabled = value),
            stringArrayField("nameplatePrefixPerTier",
                    config -> config.nameplatesConfig.monNameplatePrefixPerTier, overlay -> overlay.nameplatePrefixPerTier, (overlay, value) -> overlay.nameplatePrefixPerTier = value,
                    resolved -> resolved.nameplatePrefixPerTier, (resolved, value) -> resolved.nameplatePrefixPerTier = value),

            boolField("enableModelScaling",
                    config -> config.modelConfig.enableMobModelScaling, overlay -> overlay.enableModelScaling, (overlay, value) -> overlay.enableModelScaling = value,
                    resolved -> resolved.enableModelScaling, (resolved, value) -> resolved.enableModelScaling = value),
            floatArrayField("modelScalePerTier",
                    config -> config.modelConfig.mobModelScaleMultiplierPerTier, overlay -> overlay.modelScalePerTier, (overlay, value) -> overlay.modelScalePerTier = value,
                    resolved -> resolved.modelScalePerTier, (resolved, value) -> resolved.modelScalePerTier = value),
            floatField("modelScaleVariance",
                    config -> config.modelConfig.mobModelScaleRandomVariance, overlay -> overlay.modelScaleVariance, (overlay, value) -> overlay.modelScaleVariance = value,
                    resolved -> resolved.modelScaleVariance, (resolved, value) -> resolved.modelScaleVariance = value),

            boolField("rpgLevelingEnabled",
                    config -> config.integrationsConfig.rpgLeveling.enabled, overlay -> overlay.rpgLevelingEnabled, (overlay, value) -> overlay.rpgLevelingEnabled = value,
                    resolved -> resolved.rpgLevelingEnabled, (resolved, value) -> resolved.rpgLevelingEnabled = value),
            floatArrayField("xpMultiplierPerTier",
                    config -> config.integrationsConfig.rpgLeveling.xpMultiplierPerTier, overlay -> overlay.xpMultiplierPerTier, (overlay, value) -> overlay.xpMultiplierPerTier = value,
                    resolved -> resolved.xpMultiplierPerTier, (resolved, value) -> resolved.xpMultiplierPerTier = value),
            doubleField("xpBonusPerAbility",
                    config -> config.integrationsConfig.rpgLeveling.xpBonusPerAbility, overlay -> overlay.xpBonusPerAbility, (overlay, value) -> overlay.xpBonusPerAbility = value,
                    resolved -> resolved.xpBonusPerAbility, (resolved, value) -> resolved.xpBonusPerAbility = value),
            doubleField("minionXPMultiplier",
                    config -> config.integrationsConfig.rpgLeveling.minionXPMultiplier, overlay -> overlay.minionXPMultiplier, (overlay, value) -> overlay.minionXPMultiplier = value,
                    resolved -> resolved.minionXPMultiplier, (resolved, value) -> resolved.minionXPMultiplier = value),

            floatField("globalCooldownMinSeconds",
                    config -> config.abilitiesConfig.globalCooldownMinSeconds, overlay -> overlay.globalCooldownMinSeconds, (overlay, value) -> overlay.globalCooldownMinSeconds = value,
                    resolved -> resolved.globalCooldownMinSeconds, (resolved, value) -> resolved.globalCooldownMinSeconds = value),
            floatField("globalCooldownMaxSeconds",
                    config -> config.abilitiesConfig.globalCooldownMaxSeconds, overlay -> overlay.globalCooldownMaxSeconds, (overlay, value) -> overlay.globalCooldownMaxSeconds = value,
                    resolved -> resolved.globalCooldownMaxSeconds, (resolved, value) -> resolved.globalCooldownMaxSeconds = value)
    );

    private OverlayFieldRegistry() {}

    public static void buildAllBase(RPGMobsConfig config, ResolvedConfig resolved) {
        for (var field : FIELDS) field.buildBase(config, resolved);
    }

    public static void applyAllYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
        for (var field : FIELDS) field.applyYaml(yaml, overlay);
    }

    public static void mergeAll(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
        for (var field : FIELDS) field.merge(overlay, base, result);
    }

    public static void writeAll(ConfigOverlay overlay, Map<String, Object> map) {
        for (var field : FIELDS) field.write(overlay, map);
    }

    public static boolean allEffectivelyEqual(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
        for (var field : FIELDS) {
            if (!field.effectivelyEquals(first, second, base)) return false;
        }
        return true;
    }

    private static OverlayFieldDescriptor boolField(String key,
            Function<RPGMobsConfig, Boolean> baseGetter,
            Function<ConfigOverlay, @Nullable Boolean> overlayGetter, BiConsumer<ConfigOverlay, @Nullable Boolean> overlaySetter,
            Function<ResolvedConfig, Boolean> resolvedGetter, BiConsumer<ResolvedConfig, Boolean> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                resolvedSetter.accept(resolved, baseGetter.apply(config));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                overlaySetter.accept(overlay, rawValue instanceof Boolean boolValue ? boolValue : null);
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                Boolean overlayValue = overlayGetter.apply(overlay);
                resolvedSetter.accept(result, overlayValue != null ? overlayValue : resolvedGetter.apply(base));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                Boolean value = overlayGetter.apply(overlay);
                if (value != null) map.put(key, value);
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                boolean valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                boolean valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return valueA == valueB;
            }
        };
    }

    private static OverlayFieldDescriptor floatField(String key,
            Function<RPGMobsConfig, Float> baseGetter,
            Function<ConfigOverlay, @Nullable Float> overlayGetter, BiConsumer<ConfigOverlay, @Nullable Float> overlaySetter,
            Function<ResolvedConfig, Float> resolvedGetter, BiConsumer<ResolvedConfig, Float> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                resolvedSetter.accept(resolved, baseGetter.apply(config));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                overlaySetter.accept(overlay, rawValue instanceof Number number ? number.floatValue() : null);
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                Float overlayValue = overlayGetter.apply(overlay);
                resolvedSetter.accept(result, overlayValue != null ? overlayValue : resolvedGetter.apply(base));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                Float value = overlayGetter.apply(overlay);
                if (value != null) map.put(key, (double) value);
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                float valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                float valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return valueA == valueB;
            }
        };
    }

    private static OverlayFieldDescriptor doubleField(String key,
            Function<RPGMobsConfig, Double> baseGetter,
            Function<ConfigOverlay, @Nullable Double> overlayGetter, BiConsumer<ConfigOverlay, @Nullable Double> overlaySetter,
            Function<ResolvedConfig, Double> resolvedGetter, BiConsumer<ResolvedConfig, Double> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                resolvedSetter.accept(resolved, baseGetter.apply(config));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                overlaySetter.accept(overlay, rawValue instanceof Number number ? number.doubleValue() : null);
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                Double overlayValue = overlayGetter.apply(overlay);
                resolvedSetter.accept(result, overlayValue != null ? overlayValue : resolvedGetter.apply(base));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                Double value = overlayGetter.apply(overlay);
                if (value != null) map.put(key, value);
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                double valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                double valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return valueA == valueB;
            }
        };
    }

    private static OverlayFieldDescriptor stringField(String key,
            Function<RPGMobsConfig, String> baseGetter,
            Function<ConfigOverlay, @Nullable String> overlayGetter, BiConsumer<ConfigOverlay, @Nullable String> overlaySetter,
            Function<ResolvedConfig, String> resolvedGetter, BiConsumer<ResolvedConfig, String> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                resolvedSetter.accept(resolved, baseGetter.apply(config));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                overlaySetter.accept(overlay, rawValue != null ? String.valueOf(rawValue) : null);
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                String overlayValue = overlayGetter.apply(overlay);
                resolvedSetter.accept(result, overlayValue != null ? overlayValue : resolvedGetter.apply(base));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                String value = overlayGetter.apply(overlay);
                if (value != null) map.put(key, value);
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                String valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                String valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return Objects.equals(valueA, valueB);
            }
        };
    }

    private static OverlayFieldDescriptor floatArrayField(String key,
            Function<RPGMobsConfig, float[]> baseGetter,
            Function<ConfigOverlay, float @Nullable []> overlayGetter, BiConsumer<ConfigOverlay, float @Nullable []> overlaySetter,
            Function<ResolvedConfig, float[]> resolvedGetter, BiConsumer<ResolvedConfig, float[]> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                float[] source = baseGetter.apply(config);
                resolvedSetter.accept(resolved, Arrays.copyOf(source, source.length));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                if (rawValue instanceof List<?> list) {
                    float[] array = new float[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object element = list.get(i);
                        array[i] = element instanceof Number number ? number.floatValue() : 0f;
                    }
                    overlaySetter.accept(overlay, array);
                }
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                float[] overlayValue = overlayGetter.apply(overlay);
                float[] baseValue = resolvedGetter.apply(base);
                resolvedSetter.accept(result, Arrays.copyOf(overlayValue != null ? overlayValue : baseValue,
                        (overlayValue != null ? overlayValue : baseValue).length));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                float[] value = overlayGetter.apply(overlay);
                if (value != null) {
                    List<Double> list = new ArrayList<>();
                    for (float floatValue : value) list.add((double) floatValue);
                    map.put(key, list);
                }
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                float[] valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                float[] valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return Arrays.equals(valueA, valueB);
            }
        };
    }

    private static OverlayFieldDescriptor intArrayField(String key,
            Function<RPGMobsConfig, int[]> baseGetter,
            Function<ConfigOverlay, int @Nullable []> overlayGetter, BiConsumer<ConfigOverlay, int @Nullable []> overlaySetter,
            Function<ResolvedConfig, int[]> resolvedGetter, BiConsumer<ResolvedConfig, int[]> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                int[] source = baseGetter.apply(config);
                resolvedSetter.accept(resolved, Arrays.copyOf(source, source.length));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                if (rawValue instanceof List<?> list) {
                    int[] array = new int[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object element = list.get(i);
                        array[i] = element instanceof Number number ? number.intValue() : 0;
                    }
                    overlaySetter.accept(overlay, array);
                }
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                int[] overlayValue = overlayGetter.apply(overlay);
                int[] baseValue = resolvedGetter.apply(base);
                resolvedSetter.accept(result, Arrays.copyOf(overlayValue != null ? overlayValue : baseValue,
                        (overlayValue != null ? overlayValue : baseValue).length));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                int[] value = overlayGetter.apply(overlay);
                if (value != null) {
                    List<Integer> list = new ArrayList<>();
                    for (int intValue : value) list.add(intValue);
                    map.put(key, list);
                }
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                int[] valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                int[] valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return Arrays.equals(valueA, valueB);
            }
        };
    }

    private static OverlayFieldDescriptor boolArrayField(String key,
            Function<RPGMobsConfig, boolean[]> baseGetter,
            Function<ConfigOverlay, boolean @Nullable []> overlayGetter, BiConsumer<ConfigOverlay, boolean @Nullable []> overlaySetter,
            Function<ResolvedConfig, boolean[]> resolvedGetter, BiConsumer<ResolvedConfig, boolean[]> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                boolean[] source = baseGetter.apply(config);
                resolvedSetter.accept(resolved, Arrays.copyOf(source, source.length));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                if (rawValue instanceof List<?> list) {
                    boolean[] array = new boolean[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object element = list.get(i);
                        array[i] = element instanceof Boolean boolValue && boolValue;
                    }
                    overlaySetter.accept(overlay, array);
                }
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                boolean[] overlayValue = overlayGetter.apply(overlay);
                boolean[] baseValue = resolvedGetter.apply(base);
                resolvedSetter.accept(result, Arrays.copyOf(overlayValue != null ? overlayValue : baseValue,
                        (overlayValue != null ? overlayValue : baseValue).length));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                boolean[] value = overlayGetter.apply(overlay);
                if (value != null) {
                    List<Boolean> list = new ArrayList<>();
                    for (boolean boolValue : value) list.add(boolValue);
                    map.put(key, list);
                }
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                boolean[] valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                boolean[] valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return Arrays.equals(valueA, valueB);
            }
        };
    }

    private static OverlayFieldDescriptor stringArrayField(String key,
            Function<RPGMobsConfig, String[]> baseGetter,
            Function<ConfigOverlay, @Nullable String[]> overlayGetter, BiConsumer<ConfigOverlay, @Nullable String[]> overlaySetter,
            Function<ResolvedConfig, String[]> resolvedGetter, BiConsumer<ResolvedConfig, String[]> resolvedSetter) {
        return new OverlayFieldDescriptor() {
            @Override public void buildBase(RPGMobsConfig config, ResolvedConfig resolved) {
                String[] source = baseGetter.apply(config);
                resolvedSetter.accept(resolved, Arrays.copyOf(source, source.length));
            }
            @Override public void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay) {
                Object rawValue = yaml.get(key);
                if (rawValue instanceof List<?> list) {
                    String[] array = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        array[i] = list.get(i) != null ? String.valueOf(list.get(i)) : "";
                    }
                    overlaySetter.accept(overlay, array);
                }
            }
            @Override public void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result) {
                String[] overlayValue = overlayGetter.apply(overlay);
                String[] baseValue = resolvedGetter.apply(base);
                resolvedSetter.accept(result, Arrays.copyOf(overlayValue != null ? overlayValue : baseValue,
                        (overlayValue != null ? overlayValue : baseValue).length));
            }
            @Override public void write(ConfigOverlay overlay, Map<String, Object> map) {
                String[] value = overlayGetter.apply(overlay);
                if (value != null) map.put(key, Arrays.asList(value));
            }
            @Override public boolean effectivelyEquals(ConfigOverlay first, ConfigOverlay second, ResolvedConfig base) {
                String[] valueA = overlayGetter.apply(first) != null ? overlayGetter.apply(first) : resolvedGetter.apply(base);
                String[] valueB = overlayGetter.apply(second) != null ? overlayGetter.apply(second) : resolvedGetter.apply(base);
                return Arrays.equals(valueA, valueB);
            }
        };
    }
}
