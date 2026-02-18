package com.frotty27.rpgmobs.integrations;

import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.RPGMobsAPI;
import com.frotty27.rpgmobs.api.events.RPGMobsSpawnedEvent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.schema.YamlSerializer;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;

public final class RPGLevelingIntegration implements IRPGMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final RPGMobsPlugin plugin;
    private volatile World world;
    private volatile RPGLevelingBalanceConfig balanceConfig;

    public RPGLevelingIntegration(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean tryActivate() {
        RPGMobsConfig cfg = plugin.getConfig();
        if (cfg == null || !cfg.integrationsConfig.rpgLeveling.enabled) {
            LOGGER.atInfo().log("[RPGMobs] RPGLeveling integration disabled by config.");
            return false;
        }

        try {
            if (!RPGLevelingBridge.isAvailable()) {
                LOGGER.atInfo().log("[RPGMobs] RPGLeveling not detected — integration inactive.");
                return false;
            }
        } catch (NoClassDefFoundError e) {
            LOGGER.atInfo().log("[RPGMobs] RPGLeveling not detected — integration inactive.");
            return false;
        }

        loadBalanceConfig();

        new RPGLevelingBridge().registerXPListener(this::onXPGained);
        plugin.getEventBus().registerListener(this);
        LOGGER.atInfo().log("[RPGMobs] RPGLeveling integration active — balance config loaded from rpgleveling.yml.");
        return true;
    }

    // ── Config ──────────────────────────────────────────────────────────────────

    private void loadBalanceConfig() {
        Path modDirectory = plugin.getModDirectory();
        RPGLevelingBalanceConfig defaults = new RPGLevelingBalanceConfig();
        balanceConfig = YamlSerializer.loadOrCreate(modDirectory, defaults);
    }

    /**
     * Reloads the balance config from disk. Called by {@code /rpgmobs reload}.
     */
    public void reloadBalanceConfig() {
        loadBalanceConfig();
        LOGGER.atInfo().log("[RPGMobs] RPGLeveling balance config reloaded.");
    }

    /**
     * Returns the active balance config, or {@code null} if not yet loaded.
     */
    public @Nullable RPGLevelingBalanceConfig getBalanceConfig() {
        return balanceConfig;
    }

    // ── Events ──────────────────────────────────────────────────────────────────

    @Override
    public void onRPGMobSpawned(RPGMobsSpawnedEvent event) {
        if (world != null) return;
        World w = event.getWorld();
        if (w != null) {
            world = w;
        }
    }

    // ── XP Scaling ──────────────────────────────────────────────────────────────

    private void onXPGained(Object event, UUID killedEntityUuid) {
        World w = world;
        if (w == null) return;

        RPGMobsConfig cfg = plugin.getConfig();
        if (cfg == null || !cfg.integrationsConfig.rpgLeveling.enabled) return;

        RPGLevelingBalanceConfig bc = balanceConfig;
        if (bc == null) return;

        Ref<EntityStore> ref = w.getEntityRef(killedEntityUuid);
        if (ref == null || !ref.isValid()) return;

        var xpEvent = (org.zuxaw.plugin.api.ExperienceGainedEvent) event;
        double baseXP = xpEvent.getXpAmount();

        // Minion XP reduction
        if (RPGMobsAPI.query().isMinion(ref)) {
            double minionXP = baseXP * bc.minionXPMultiplier;
            xpEvent.setXpAmount(minionXP);

            if (plugin.getConfig().debugConfig.isDebugModeEnabled) {
                LOGGER.atInfo().log("[RPGMobs] Minion kill — XP: %.0f → %.0f (×%.2f)",
                                    baseXP, minionXP, bc.minionXPMultiplier);
            }
            return;
        }

        // Elite XP scaling
        RPGMobsAPI.query().getTier(ref).ifPresent(tier -> {
            int clampedTier = Math.min(tier, bc.xpMultiplierPerTier.length - 1);
            float tierMult = bc.xpMultiplierPerTier[clampedTier];

            Store<EntityStore> store = ref.getStore();
            int abilityCount = countActiveAbilities(ref, store);

            double scaledXP = baseXP * tierMult + bc.xpBonusPerAbility * abilityCount;
            xpEvent.setXpAmount(scaledXP);

            if (plugin.getConfig().debugConfig.isDebugModeEnabled) {
                LOGGER.atInfo().log("[RPGMobs] Elite kill — tier=%d baseXP=%.0f tierMult=%.1f abilities=%d → XP=%.0f",
                                    tier, baseXP, tierMult, abilityCount, scaledXP);
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private int countActiveAbilities(Ref<EntityStore> ref, Store<EntityStore> store) {
        int count = 0;

        ChargeLeapAbilityComponent chargeLeap = store.getComponent(ref,
                                                                    plugin.getChargeLeapAbilityComponentType()
        );
        if (chargeLeap != null && chargeLeap.abilityEnabled) count++;

        HealLeapAbilityComponent healLeap = store.getComponent(ref,
                                                                plugin.getHealLeapAbilityComponentType()
        );
        if (healLeap != null && healLeap.abilityEnabled) count++;

        SummonUndeadAbilityComponent summonUndead = store.getComponent(ref,
                                                                        plugin.getSummonUndeadAbilityComponentType()
        );
        if (summonUndead != null && summonUndead.abilityEnabled) count++;

        return count;
    }
}
