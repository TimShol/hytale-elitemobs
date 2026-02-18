package com.frotty27.rpgmobs.utils;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

public final class InventoryHelpers {

    private InventoryHelpers() {
    }

    public static ItemStack copyExactSingle(ItemStack src) {
        if (src == null) return null;
        return new ItemStack(src.getItemId(), 1, src.getDurability(), src.getMaxDurability(), src.getMetadata());
    }

    public static int getContainerSizeSafe(ItemContainer c) {
        try {
            var m = c.getClass().getMethod("getSize");
            Object r = m.invoke(c);
            if (r instanceof Integer v) return v;
        } catch (Throwable ignored) {
        }

        try {
            var m = c.getClass().getMethod("capacity");
            Object r = m.invoke(c);
            if (r instanceof Integer v) return v;
        } catch (Throwable ignored) {
        }

        try {
            var m = c.getClass().getMethod("getCapacity");
            Object r = m.invoke(c);
            if (r instanceof Integer v) return v;
        } catch (Throwable ignored) {
        }

        try {
            c.getItemStack((short) 0);
            return 1;
        } catch (Throwable ignored) {
        }

        return 0;
    }
}
