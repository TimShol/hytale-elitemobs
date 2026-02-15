package com.frotty27.elitemobs.systems.ability.trigger;

public record InCombatTrigger() implements ITriggerCondition {

    public static final String TYPE = "in_combat";

    
    public InCombatTrigger() {
        
    }

    @Override
    public boolean evaluate(TriggerContext context) {
        return context.isInCombat();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "InCombatTrigger{}";
    }
}
