package com.frotty27.elitemobs.systems.ability.trigger;

public record HealthBelowPercentTrigger(float threshold) implements ITriggerCondition {

    public static final String TYPE = "health_below_percent";

    
    public HealthBelowPercentTrigger {
        if (threshold < 0.0f || threshold > 1.0f) {
            throw new IllegalArgumentException("Health threshold must be between 0.0 and 1.0, got: " + threshold);
        }
    }

    @Override
    public boolean evaluate(TriggerContext context) {
        return context.currentHealthPercent() <= threshold;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "HealthBelowPercentTrigger{threshold=" + (threshold * 100) + "%}";
    }
}
