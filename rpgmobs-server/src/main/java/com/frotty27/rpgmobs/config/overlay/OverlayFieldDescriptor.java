package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.Map;

interface OverlayFieldDescriptor {

    void buildBase(RPGMobsConfig config, ResolvedConfig resolved);

    void applyYaml(Map<String, Object> yaml, ConfigOverlay overlay);

    void merge(ConfigOverlay overlay, ResolvedConfig base, ResolvedConfig result);

    void write(ConfigOverlay overlay, Map<String, Object> map);

    boolean effectivelyEquals(ConfigOverlay a, ConfigOverlay b, ResolvedConfig base);
}
