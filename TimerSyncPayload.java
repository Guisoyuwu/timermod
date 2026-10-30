package com.zangetsumod.net;

import com.zangetsumod.ZangetsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor -> clientes: estado del temporizador global. */
public record TimerSyncPayload(boolean active, long remainingTicks) implements CustomPacketPayload {
    public static final Type<TimerSyncPayload> TYPE = new Type<>(ZangetsuMod.id("timer_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimerSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TimerSyncPayload::active,
            ByteBufCodecs.VAR_LONG, TimerSyncPayload::remainingTicks,
            TimerSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
