package com.frotty27.elitemobs.systems.ability.trigger;

public interface ITriggerCondition {

    
    boolean evaluate(TriggerContext context);

    
    String getType();
}
