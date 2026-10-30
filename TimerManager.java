package com.zangetsumod.system;

import com.zangetsumod.ModConfig;
import com.zangetsumod.ModState;
import com.zangetsumod.net.TimerSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TimerManager {
    public static final String FINISHED_MESSAGE = "(Tiempo Acabado)";
    private static int tickCounter = 0;

    private TimerManager() {}

    public static void startGlobal(MinecraftServer server, long ticks) {
        ModState s = ModState.get();
        s.globalActive = true;
        s.globalRemainingTicks = ticks;
        broadcast(server);
        ModState.save();
    }

    public static void stopGlobal(MinecraftServer server) {
        ModState s = ModState.get();
        s.globalActive = false;
        s.globalRemainingTicks = 0;
        broadcast(server);
        ModState.save();
    }

    public static void startPlayer(ServerPlayer target, long ticks) {
        ModState s = ModState.get();
        String id = target.getUUID().toString();
        s.pendingFinished.remove(id);
        s.playerTimers.put(id, ticks);
        ModState.save();
    }

    public static void sendTo(ServerPlayer p) {
        ModState s = ModState.get();
        ServerPlayNetworking.send(p, new TimerSyncPayload(s.globalActive, s.globalRemainingTicks));
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendTo(p);
    }

    public static void onJoin(ServerPlayer p) {
        sendTo(p);
        ModState s = ModState.get();
        String id = p.getUUID().toString();
        if (s.pendingFinished.remove(id)) {
            if (ModConfig.get().notifyPlayerTimerOnJoin) p.sendSystemMessage(Component.literal(FINISHED_MESSAGE));
            ModState.save();
        }
    }

    public static void tick(MinecraftServer server) {
        ModState s = ModState.get();
        boolean changed = false;

        if (s.globalActive) {
            s.globalRemainingTicks--;
            if (s.globalRemainingTicks <= 0) {            // fin: desaparece, SIN mensaje
                s.globalActive = false;
                s.globalRemainingTicks = 0;
                broadcast(server);
                changed = true;
            } else if (s.globalRemainingTicks % 20 == 0) {
                broadcast(server);
            }
        }

        if (!s.playerTimers.isEmpty()) {
            boolean pause = ModConfig.get().playerTimerPausesWhileOffline;
            Iterator<Map.Entry<String, Long>> it = s.playerTimers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                ServerPlayer p = null;
                try { p = server.getPlayerList().getPlayer(UUID.fromString(e.getKey())); } catch (IllegalArgumentException ignored) {}
                if (p == null && pause) continue;
                long left = e.getValue() - 1;
                if (left <= 0) {
                    it.remove();
                    changed = true;
                    if (p != null) p.sendSystemMessage(Component.literal(FINISHED_MESSAGE));
                    else s.pendingFinished.add(e.getKey());   // se avisará al reconectar
                } else {
                    e.setValue(left);
                }
            }
        }

        if (changed || (++tickCounter % 200 == 0 && (s.globalActive || !s.playerTimers.isEmpty()))) ModState.save();
    }
}
