package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public interface WeaponSwappable {

    boolean isSwapActive();

    void setSwapActive(boolean active);

    byte getSwapSlot();

    void setSwapSlot(byte slot);

    @Nullable ItemStack getSwapPreviousItem();

    void setSwapPreviousItem(@Nullable ItemStack item);
}
