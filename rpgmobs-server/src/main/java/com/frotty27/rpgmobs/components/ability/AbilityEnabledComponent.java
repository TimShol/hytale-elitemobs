package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface AbilityEnabledComponent extends Component<EntityStore> {

    boolean isAbilityEnabled();

    void setAbilityEnabled(boolean enabled);

    long getCooldownTicksRemaining();
}
