package com.zangetsumod.duck;

/** Interfaz implementada por el mixin de ChunkMap.TrackedEntity. */
public interface TrackedEntityDuck {
    /** Re-evalúa qué jugadores ven esta entidad (quita y vuelve a evaluar). */
    void zm$refresh(Iterable<net.minecraft.server.level.ServerPlayer> viewers);
}
