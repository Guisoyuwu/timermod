package com.zangetsumod;

import com.zangetsumod.net.TimerSyncPayload;
import com.zangetsumod.system.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.Set;

public class ZangetsuMod implements ModInitializer {
    public static final String MOD_ID = "zangetsu_mod";

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override
    public void onInitialize() {
        ModConfig.load();
        ModItems.register();
        PayloadTypeRegistry.playS2C().register(TimerSyncPayload.TYPE, TimerSyncPayload.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(ModState::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ModState.save());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TimerManager.tick(server);
            GetsugaManager.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            VanishManager.onJoin(server, p);
            TimerManager.onJoin(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GetsugaManager.clearPlayer(handler.getPlayer().getUUID()));

        // Los palos solo actúan al GOLPEAR a otro jugador (AttackEntityCallback se ejecuta en el servidor).
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer attacker) || !(entity instanceof ServerPlayer target) || target == attacker) return InteractionResult.PASS;
            var held = attacker.getMainHandItem();

            if (held.is(ModItems.A_CASA_PA)) {
                target.connection.disconnect(Component.literal(ModConfig.get().casaPaKickMessage));
                return InteractionResult.SUCCESS;
            }
            if (held.is(ModItems.TP_0_0)) {
                if (target.level() instanceof ServerLevel level) {
                    var spot = SafeSpot.find(level);
                    if (spot == null) {
                        attacker.sendSystemMessage(Component.literal("No se encontró un lugar seguro cerca de X:0 Z:0."));
                    } else {
                        target.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                                Set.of(), target.getYRot(), target.getXRot(), true);
                        target.resetFallDistance();
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
