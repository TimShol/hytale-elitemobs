package com.frotty27.rpgmobs.integrations;

import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.events.RPGMobsDeathEvent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.config.schema.YamlSerializer;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RPGLevelingIntegration implements IRPGMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int MAX_CACHE_SIZE = 256;

    private final RPGMobsPlugin plugin;
    private volatile RPGLevelingBalanceConfig balanceConfig;
    private final Map<UUID, DeathXPData> deathCache = new ConcurrentHashMap<>();

    private record DeathXPData(int tier, int abilityCount, String worldName, boolean minion) {
    }

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

    private void loadBalanceConfig() {
        Path modDirectory = plugin.getModDirectory();
        RPGLevelingBalanceConfig defaults = new RPGLevelingBalanceConfig();
        balanceConfig = YamlSerializer.loadOrCreate(modDirectory, defaults);
    }

    public void reloadBalanceConfig() {
        loadBalanceConfig();
        LOGGER.atInfo().log("[RPGMobs] RPGLeveling balance config reloaded.");
    }

    @Override
    public void onRPGMobDeath(RPGMobsDeathEvent event) {
        UUID entityUuid = event.getEntityUuid();
        if (entityUuid == null) return;

        Ref<EntityStore> ref = event.getEntityRef();
        Store<EntityStore> store = ref.getStore();

        int abilityCount = 0;
        if (store != null) {
            abilityCount = countActiveAbilities(ref, store);
        }

        String worldName = event.getWorld() != null ? event.getWorld().getName() : null;

        if (deathCache.size() >= MAX_CACHE_SIZE) {
            deathCache.clear();
        }

        deathCache.put(entityUuid, new DeathXPData(event.getTier(), abilityCount, worldName, event.isMinion()));
    }

    private void onXPGained(Object event, UUID killedEntityUuid) {
        DeathXPData data = deathCache.remove(killedEntityUuid);
        if (data == null) return;

        ResolvedConfig resolved = plugin.getResolvedConfig(data.worldName);
        if (!resolved.rpgLevelingEnabled) return;

        var xpEvent = (org.zuxaw.plugin.api.ExperienceGainedEvent) event;
        double baseXP = xpEvent.getXpAmount();

        if (data.minion) {
            double minionXP = baseXP * resolved.minionXPMultiplier;
            xpEvent.setXpAmount(minionXP);

            RPGMobsConfig debugCfg = plugin.getConfig();
            if (debugCfg != null && debugCfg.debugConfig.isDebugModeEnabled) {
                LOGGER.atInfo().log("[RPGMobs] Minion kill — XP: %.0f → %.0f (×%.2f)",
                                    baseXP, minionXP, resolved.minionXPMultiplier);
            }
            return;
        }

        int clampedTier = Math.min(data.tier, resolved.xpMultiplierPerTier.length - 1);
        float tierMult = resolved.xpMultiplierPerTier[clampedTier];

        double scaledXP = baseXP * tierMult + resolved.xpBonusPerAbility * data.abilityCount;
        xpEvent.setXpAmount(scaledXP);

        RPGMobsConfig debugCfg = plugin.getConfig();
        if (debugCfg != null && debugCfg.debugConfig.isDebugModeEnabled) {
            LOGGER.atInfo().log("[RPGMobs] Elite kill — tier=%d baseXP=%.0f tierMult=%.1f abilities=%d bonusPerAbility=%.0f → XP=%.0f (world=%s)",
                                data.tier, baseXP, tierMult, data.abilityCount, resolved.xpBonusPerAbility, scaledXP, data.worldName);
        }
    }

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
