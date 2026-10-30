package com.zangetsumod.mixin;

import com.zangetsumod.system.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Oculta los mensajes reales de entrada/salida de jugadores en vanish. */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void zm$hideJoinLeave(Component message, boolean overlay, CallbackInfo ci) {
        if (message.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if (key.startsWith("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
                Object[] args = tc.getArgs();
                if (args.length > 0) {
                    String name = args[0] instanceof Component c ? c.getString() : String.valueOf(args[0]);
                    if (VanishManager.isVanishedName(name)) ci.cancel();
                }
            }
        }
    }
}
