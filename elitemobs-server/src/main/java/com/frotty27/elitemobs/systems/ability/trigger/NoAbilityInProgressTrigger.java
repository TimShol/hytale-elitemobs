package com.frotty27.elitemobs.systems.ability.trigger;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class NoAbilityInProgressTrigger implements ITriggerCondition {

    public static final String TYPE = "no_ability_in_progress";

    
    public NoAbilityInProgressTrigger() {
        
    }

    @Override
    public boolean evaluate(TriggerContext context) {
        return true;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "NoAbilityInProgressTrigger{}";
    }
}
