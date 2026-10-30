package com.zangetsumod.system;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

/** Busca una posición segura cerca de (0,0): suelo sólido, 2 bloques libres, sin lava/fuego/cactus/etc. */
public final class SafeSpot {
    private static final Set<Block> DANGEROUS = Set.of(
            Blocks.CACTUS, Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.SWEET_BERRY_BUSH,
            Blocks.WITHER_ROSE, Blocks.POWDER_SNOW, Blocks.COBWEB, Blocks.POINTED_DRIPSTONE);

    private SafeSpot() {}

    /** Primero la columna exacta (0,0); si no hay sitio, columnas vecinas hasta 16 bloques. */
    public static BlockPos find(ServerLevel level) {
        for (int r = 0; r <= 16; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos p = column(level, dx, dz);
                    if (p != null) return p;
                }
            }
        }
        return null;
    }

    private static BlockPos column(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4); // fuerza la carga del chunk
        int minY = level.getMinY();
        int top = minY + level.dimensionType().logicalHeight() - 1;
        int start = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), top - 2);
        for (int y = start; y > minY; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (isSafe(level, feet)) return feet;
        }
        return null;
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        BlockPos floorPos = feet.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!floor.isFaceSturdy(level, floorPos, Direction.UP)) return false;
        if (DANGEROUS.contains(floor.getBlock()) || floor.is(BlockTags.FIRE) || !floor.getFluidState().isEmpty()) return false;
        if (!isFree(level, feet) || !isFree(level, feet.above())) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = feet.relative(d);
            if (level.getFluidState(n).is(FluidTags.LAVA) || level.getBlockState(n).is(BlockTags.FIRE)
                    || level.getBlockState(n).is(Blocks.CACTUS)) return false;
        }
        return true;
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getCollisionShape(level, pos).isEmpty()
                && s.getFluidState().isEmpty()
                && !s.is(BlockTags.FIRE)
                && !DANGEROUS.contains(s.getBlock());
    }
}
