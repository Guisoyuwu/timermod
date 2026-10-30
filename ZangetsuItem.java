package com.zangetsumod.item;

import com.zangetsumod.system.GetsugaManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ZangetsuItem extends Item {
    public ZangetsuItem(Properties properties) { super(properties); }

    /** Clic derecho: Getsuga Tensho. Toda la lógica (cooldown/daño/colisión) se valida en el servidor. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            return GetsugaManager.tryCast(sl, sp) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }
}
