package com.zangetsumod.system;

import com.zangetsumod.ModConfig;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.*;

/**
 * Getsuga Tensho, 100% del lado servidor: el "proyectil" es una simulación por tick (raycast de bloques
 * y entidades) y se dibuja con partículas vanilla que ven todos los jugadores. No hay entidad custom,
 * así que no hay nada que desincronizar ni que el cliente pueda falsificar.
 */
public final class GetsugaManager {
    private static final DustParticleOptions BLUE = new DustParticleOptions(0x2E8BFF, 2.0f);
    private static final DustParticleOptions BLUE_DARK = new DustParticleOptions(0x0B2C8F, 1.4f);
    private static final DustParticleOptions WHITE = new DustParticleOptions(0xE8F4FF, 1.0f);

    private static final Map<UUID, Integer> LAST_CAST_TICK = new HashMap<>();
    private static final List<Beam> BEAMS = new ArrayList<>();

    private GetsugaManager() {}

    private static final class Beam {
        final ServerLevel level; final UUID owner; final Vec3 dir;
        Vec3 pos; double travelled = 0;
        Beam(ServerLevel l, UUID o, Vec3 p, Vec3 d) { level = l; owner = o; pos = p; dir = d; }
    }

    /** Valida cooldown en el servidor y lanza el ataque. */
    public static boolean tryCast(ServerLevel level, ServerPlayer player) {
        ModConfig cfg = ModConfig.get();
        MinecraftServer server = level.getServer();
        int now = server.getTickCount();
        int cdTicks = cfg.zangetsuCooldownSeconds * 20;
        Integer last = LAST_CAST_TICK.get(player.getUUID());
        if (last != null && now - last < cdTicks) {
            int left = (int) Math.ceil((cdTicks - (now - last)) / 20.0);
            player.displayClientMessage(Component.literal("Getsuga Tensho en cooldown: " + left + "s"), true);
            return false;
        }
        LAST_CAST_TICK.put(player.getUUID(), now);

        Vec3 dir = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(dir.scale(1.0));
        BEAMS.add(new Beam(level, player.getUUID(), start, dir));
        launchEffects(level, player, start, dir);
        return true;
    }

    public static void clearPlayer(UUID id) { LAST_CAST_TICK.remove(id); }

    private static void launchEffects(ServerLevel level, ServerPlayer p, Vec3 start, Vec3 dir) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.2f, 1.4f);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
        // animación: barrido de espada + anillo de energía
        Vec3 from = p.position().add(0, p.getBbHeight() * 0.6, 0).add(dir.scale(1.2));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, from.x, from.y, from.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, 1, 0, 0, 0, 0);
        crescent(level, start, dir, 1.6, 2.0);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    /** Dibuja una media luna perpendicular a la dirección. */
    private static void crescent(ServerLevel level, Vec3 center, Vec3 dir, double radius, double scale) {
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();
        // media luna vertical (el clásico Getsuga es un tajo vertical)
        for (int i = -10; i <= 10; i++) {
            double a = Math.toRadians(i * 6.5);
            Vec3 pt = center.add(realUp.scale(Math.sin(a) * radius)).add(dir.scale((Math.cos(a) - 1) * radius * 0.55));
            level.sendParticles(BLUE, pt.x, pt.y, pt.z, 1, 0.04, 0.04, 0.04, 0);
            level.sendParticles(WHITE, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    public static void tick(MinecraftServer server) {
        if (BEAMS.isEmpty()) return;
        ModConfig cfg = ModConfig.get();
        Iterator<Beam> it = BEAMS.iterator();
        while (it.hasNext()) {
            Beam b = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(b.owner);
            if (owner == null || !owner.isAlive() || owner.level() != b.level) { it.remove(); continue; }
            if (step(b, owner, cfg)) it.remove();
        }
    }

    /** @return true cuando el proyectil termina. */
    private static boolean step(Beam b, ServerPlayer owner, ModConfig cfg) {
        double len = Math.min(cfg.getsugaSpeedBlocksPerTick, cfg.getsugaMaxRange - b.travelled);
        Vec3 from = b.pos;
        Vec3 to = from.add(b.dir.scale(len));

        // bloques
        BlockHitResult bhr = b.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 end = to;
        double blockDist = Double.MAX_VALUE;
        if (bhr.getType() != HitResult.Type.MISS) { end = bhr.getLocation(); blockDist = from.distanceToSqr(end); }

        // entidades (la más cercana; el dueño queda excluido)
        AABB sweep = new AABB(from, end).inflate(1.0);
        Entity hit = null; double best = Double.MAX_VALUE; Vec3 hitPos = null;
        for (Entity e : b.level.getEntities(owner, sweep, x -> x instanceof LivingEntity && x.isAlive() && !x.isSpectator() && x != owner)) {
            Optional<Vec3> c = e.getBoundingBox().inflate(0.6).clip(from, end);
            if (c.isPresent()) {
                double d = from.distanceToSqr(c.get());
                if (d < best) { best = d; hit = e; hitPos = c.get(); }
            }
        }

        trail(b.level, from, end, b.dir);

        if (hit != null && best <= blockDist) {
            impact(b.level, hitPos);
            if (hit instanceof LivingEntity le) {
                le.invulnerableTime = 0; // el daño no se descarta por marcos de invulnerabilidad
                le.hurtServer(b.level, b.level.damageSources().indirectMagic(owner, owner), cfg.getsugaDamage);
            }
            return true;
        }
        if (blockDist != Double.MAX_VALUE) { impact(b.level, end); return true; }

        b.pos = end;
        b.travelled += len;
        return b.travelled >= cfg.getsugaMaxRange - 1.0E-6;
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir) {
        double dist = from.distanceTo(to);
        int n = Math.max(1, (int) Math.ceil(dist / 0.4));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.lerp(to, i / (double) n);
            level.sendParticles(BLUE, p.x, p.y, p.z, 3, 0.35, 0.35, 0.35, 0);
            level.sendParticles(BLUE_DARK, p.x, p.y, p.z, 2, 0.5, 0.5, 0.5, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.3, 0.3, 0.3, 0.01);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.4, 0.4, 0.4, 0.05);
        }
        crescent(level, to, dir, 1.4, 1.0);
    }

    private static void impact(ServerLevel level, Vec3 p) {
        level.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        level.sendParticles(BLUE, p.x, p.y, p.z, 40, 0.8, 0.8, 0.8, 0.05);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 25, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
    }
}
