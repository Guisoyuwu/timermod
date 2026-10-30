package com.zangetsumod.system;

import com.zangetsumod.ModConfig;
import com.zangetsumod.ModState;
import com.zangetsumod.duck.TrackedEntityDuck;
import com.zangetsumod.mixin.ChunkMapAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Vanish administrativo: el jugador desaparece (entidad + TAB) para los demás y parece desconectado.
 * Limitaciones (ver README): sigue apareciendo en /list, selectores @a, chat, sonidos y logros.
 */
public final class VanishManager {
    private VanishManager() {}

    public static boolean isVanished(ServerPlayer p) {
        return ModState.get().vanished.containsKey(p.getUUID().toString());
    }

    public static boolean isVanishedName(String name) {
        return ModState.get().vanished.containsValue(name);
    }

    public static void vanish(MinecraftServer server, ServerPlayer p) {
        ModState.get().vanished.put(p.getUUID().toString(), p.getScoreboardName());
        hideFromOthers(server, p, true);
        ModState.save();
    }

    public static void unvanish(MinecraftServer server, ServerPlayer p) {
        ModState.get().vanished.remove(p.getUUID().toString());
        // Primero volver a TAB, luego a la entidad, luego mensaje de entrada.
        ClientboundPlayerInfoUpdatePacket add = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p));
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o == p) continue;
            o.connection.send(add);
            o.sendSystemMessage(Component.translatable("multiplayer.player.joined", p.getDisplayName()).withStyle(ChatFormatting.YELLOW));
        }
        refreshTracking(p);
        ModState.save();
    }

    /** Al entrar un jugador: aplica vanish persistido y oculta a los vanished del nuevo jugador. */
    public static void onJoin(MinecraftServer server, ServerPlayer joined) {
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != joined && isVanished(other)) {
                joined.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(other.getUUID())));
            }
        }
        if (isVanished(joined)) {
            if (ModConfig.get().persistVanish) {
                hideFromOthers(server, joined, false);
                joined.sendSystemMessage(Component.literal("Vanish activo (persistente).").withStyle(ChatFormatting.GRAY));
            } else {
                ModState.get().vanished.remove(joined.getUUID().toString());
                ModState.save();
            }
        }
    }

    private static void hideFromOthers(MinecraftServer server, ServerPlayer p, boolean fakeLeaveMessage) {
        ClientboundPlayerInfoRemovePacket rm = new ClientboundPlayerInfoRemovePacket(List.of(p.getUUID()));
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o == p) continue;
            o.connection.send(rm);
            if (fakeLeaveMessage) {
                o.sendSystemMessage(Component.translatable("multiplayer.player.left", p.getDisplayName()).withStyle(ChatFormatting.YELLOW));
            }
        }
        refreshTracking(p);
    }

    private static void refreshTracking(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Object tracked = ((ChunkMapAccessor) level.getChunkSource().chunkMap).zm$getEntityMap().get(p.getId());
        if (tracked instanceof TrackedEntityDuck duck) duck.zm$refresh(level.players());
    }
}
