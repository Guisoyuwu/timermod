package com.zangetsumod.mixin;

import com.zangetsumod.duck.TrackedEntityDuck;
import com.zangetsumod.system.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Impide que los jugadores en vanish sean enviados (entidad visible) a otros jugadores. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements TrackedEntityDuck {
    @Shadow @Final Entity entity;
    @Shadow public abstract void updatePlayer(ServerPlayer player);
    @Shadow public abstract void removePlayer(ServerPlayer player);

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void zm$hideVanished(ServerPlayer viewer, CallbackInfo ci) {
        if (this.entity instanceof ServerPlayer sp && sp != viewer && VanishManager.isVanished(sp)) {
            ci.cancel();
        }
    }

    @Override
    public void zm$refresh(Iterable<ServerPlayer> viewers) {
        for (ServerPlayer v : viewers) {
            if (v == this.entity) continue;
            this.removePlayer(v);
            this.updatePlayer(v);
        }
    }
}
