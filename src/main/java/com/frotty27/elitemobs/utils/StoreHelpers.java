package com.frotty27.elitemobs.utils;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class StoreHelpers {

    private StoreHelpers() {
    }

    @FunctionalInterface
    public interface EntityChunkConsumer {
        void accept(ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer, int index);
    }

    public static boolean withEntity(
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            EntityChunkConsumer consumer
    ) {
        if (store == null || ref == null || !ref.isValid() || consumer == null) return false;

        final boolean[] found = {false};

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int index = 0; index < chunk.size(); index++) {
                Ref<EntityStore> candidate = chunk.getReferenceTo(index);
                if (!ref.equals(candidate)) continue;
                consumer.accept(chunk, commandBuffer, index);
                found[0] = true;
                return true;
            }
            return false;
        });

        return found[0];
    }
}
