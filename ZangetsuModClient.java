package com.zangetsumod.client;

import com.zangetsumod.ZangetsuMod;
import com.zangetsumod.net.TimerSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

public class ZangetsuModClient implements ClientModInitializer {
    private static volatile boolean active = false;
    private static volatile long remainingTicks = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TimerSyncPayload.TYPE, (payload, context) -> {
            active = payload.active();
            remainingTicks = payload.remainingTicks();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> active = false);

        HudElementRegistry.addLast(ZangetsuMod.id("timer_hud"), (graphics, tickCounter) -> {
            if (!active) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui) return;
            long secs = (remainingTicks + 19) / 20;
            String text = String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60);
            int w = mc.font.width(text);
            int x = graphics.guiWidth() - w - 6;
            graphics.fill(x - 3, 2, x + w + 3, 14, 0x90000000);
            graphics.drawString(mc.font, text, x, 4, secs <= 10 ? 0xFFFF5555 : 0xFFFFFFFF, true);
        });
    }
}
