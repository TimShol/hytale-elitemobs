package com.frotty27.elitemobs.systems.ability.trigger;

public record CooldownExpiredTrigger() implements ITriggerCondition {

    public static final String TYPE = "cooldown_expired";


    public CooldownExpiredTrigger() {

    }

    @Override
    public boolean evaluate(TriggerContext context) {
        return context.cooldownTicksRemaining() <= 0;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "CooldownExpiredTrigger{}";
    }
}
