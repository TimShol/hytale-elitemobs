package com.frotty27.elitemobs.systems.ability.trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.logger.HytaleLogger;

public final class TriggerFactory {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private TriggerFactory() {
        
    }

    
    public static List<ITriggerCondition> parseTriggers(List<Map<String, Object>> triggerConfigs) {
        if (triggerConfigs == null || triggerConfigs.isEmpty()) {
            return List.of();  
        }

        List<ITriggerCondition> triggers = new ArrayList<>();

        for (Map<String, Object> config : triggerConfigs) {
            try {
                ITriggerCondition trigger = createTrigger(config);
                if (trigger != null) {
                    triggers.add(trigger);
                }
            } catch (Exception e) {
                
            }
        }

        return List.copyOf(triggers);  
    }

    
    private static ITriggerCondition createTrigger(Map<String, Object> config) {
        String type = (String) config.get("type");
        if (type == null) {
            return null;
        }

        return switch (type) {
            case HealthBelowPercentTrigger.TYPE -> {
                float threshold = getFloat(config, "threshold", 0.3f);
                yield new HealthBelowPercentTrigger(threshold);
            }

            case InCombatTrigger.TYPE -> new InCombatTrigger();

            case CooldownExpiredTrigger.TYPE -> new CooldownExpiredTrigger();

            case TargetWithinRangeTrigger.TYPE -> {
                float range = getFloat(config, "range", 15.0f);
                yield new TargetWithinRangeTrigger(range);
            }

            case NoAbilityInProgressTrigger.TYPE -> new NoAbilityInProgressTrigger();

            default -> null;
        };
    }

    
    private static float getFloat(Map<String, Object> config, String key, float defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number number) {
            return number.floatValue();
        }

        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            
            return defaultValue;
        }
    }
}
