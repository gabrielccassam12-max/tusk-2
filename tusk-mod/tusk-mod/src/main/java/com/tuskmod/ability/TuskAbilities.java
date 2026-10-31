package com.tuskmod.ability;

import com.tuskmod.entity.TuskNailEntity;
import com.tuskmod.entity.TuskNailEntity.Mode;
import com.tuskmod.entity.TuskStandEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Server-side implementation of every Tusk attack. */
public final class TuskAbilities {
    private TuskAbilities() {}

    public static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.0f, 0.78f, 0.15f), 1.1f);

    public static void execute(ServerPlayerEntity p, TuskAttack attack) {
        ServerWorld w = (ServerWorld) p.getWorld();
        findStand(p).ifPresent(s -> s.playAnim(attack.anim));
        switch (attack) {
            case NAIL_SHOT -> nailShot(p, w);
            case NAIL_FAN -> nailFan(p, w);
            case RICOCHET -> ricochet(p, w);
            case RAPID_FIRE -> rapidFire(p, w);
            case SPIN_SHOT -> spinShot(p, w);
            case DRILL_SHOT -> drillShot(p, w);
            case NAIL_RAIN -> nailRain(p, w);
            case SPIRAL_VOLLEY -> spiralVolley(p, w);
            case WORMHOLE_NAIL -> wormholeNail(p, w);
            case HOMING_SPIRAL -> homingSpiral(p, w);
            case TUSK_GUARD -> tuskGuard(p, w);
            case ROTATION_SHOCKWAVE -> rotationShockwave(p, w);
            case INFINITE_ROTATION -> infiniteRotation(p, w);
            case WORMHOLE_DASH -> wormholeDash(p, w);
            case GOLDEN_ENCIRCLEMENT -> goldenEncirclement(p, w);
            case INFINITE_VOID -> infiniteVoid(p, w);
        }
    }

    // =====================================================================================
    // ACT 1 - raw nails
    // =====================================================================================

    /** One fast, accurate nail. */
    private static void nailShot(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d aim = aimPoint(p, w, 64);
        fire(p, w, from, aim.subtract(from), Mode.NORMAL, 4f, 3.4f, 0.3f, 0, 0, 60);
        shotFx(w, p, from, 1.9f);
    }

    /** Seven nails in a horizontal fan. */
    private static void nailFan(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d base = aimPoint(p, w, 64).subtract(from).normalize();
        for (int i = -3; i <= 3; i++) {
            Vec3d dir = base.rotateY((float) Math.toRadians(i * 6.5)).add(0, (i % 2 == 0 ? 0.015 : -0.015), 0);
            fire(p, w, from, dir, Mode.NORMAL, 3f, 3.2f, 0.2f, 0, 0, 50);
        }
        shotFx(w, p, from, 1.5f);
    }

    /** A nail that bounces off up to four surfaces, hitting harder each time. */
    private static void ricochet(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d aim = aimPoint(p, w, 64);
        fire(p, w, from, aim.subtract(from), Mode.RICOCHET, 4f, 2.8f, 0.1f, 0, 4, 100);
        shotFx(w, p, from, 1.2f);
    }

    /** Eight quick nails, re-aimed on every shot. */
    private static void rapidFire(ServerPlayerEntity p, ServerWorld w) {
        for (int i = 0; i < 8; i++) {
            Scheduler.later(1 + i * 2, () -> {
                if (!valid(p, w)) return;
                Vec3d from = muzzle(p);
                Vec3d aim = aimPoint(p, w, 64);
                fire(p, w, from, aim.subtract(from), Mode.NORMAL, 2f, 3.5f, 1.4f, 0, 0, 50);
                shotFx(w, p, from, 2.0f);
            });
        }
    }

    // =====================================================================================
    // ACT 2 - spinning nails
    // =====================================================================================

    /** Heavy rotating nail that causes bleeding (wither). */
    private static void spinShot(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d aim = aimPoint(p, w, 64);
        fire(p, w, from, aim.subtract(from), Mode.SPIN, 8f, 3.8f, 0.1f, 0, 0, 70);
        shotFx(w, p, from, 1.0f);
        sfx(w, p, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 1.6f);
    }

    /** Pierces up to five enemies and chews through glass, leaves and other soft blocks. */
    private static void drillShot(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d aim = aimPoint(p, w, 64);
        fire(p, w, from, aim.subtract(from), Mode.DRILL, 6f, 4.2f, 0.1f, 5, 0, 60);
        shotFx(w, p, from, 0.8f);
    }

    /** Marks the spot you look at and rains nails from above. */
    private static void nailRain(ServerPlayerEntity p, ServerWorld w) {
        Vec3d aim = aimPoint(p, w, 40);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            w.spawnParticles(GOLD, aim.x + Math.cos(a) * 4.5, aim.y + 0.15, aim.z + Math.sin(a) * 4.5, 1, 0, 0, 0, 0);
        }
        sfx(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);
        for (int i = 0; i < 18; i++) {
            Scheduler.later(6 + i, () -> {
                if (!valid(p, w)) return;
                double a = w.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(w.random.nextDouble()) * 4.5;
                Vec3d top = aim.add(Math.cos(a) * r, 14, Math.sin(a) * r);
                fire(p, w, top, new Vec3d(0, -1, 0), Mode.RAIN, 4f, 2.6f, 0.3f, 0, 0, 40);
                w.spawnParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 2, 0.1, 0.1, 0.1, 0.01);
            });
        }
    }

    /** Eight nails launched from a rotating ring: they form a corkscrew and converge on the target. */
    private static void spiralVolley(ServerPlayerEntity p, ServerWorld w) {
        for (int i = 0; i < 8; i++) {
            final int k = i;
            Scheduler.later(1 + k * 2, () -> {
                if (!valid(p, w)) return;
                Vec3d look = p.getRotationVec(1f);
                Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
                if (right.lengthSquared() < 1e-4) right = new Vec3d(1, 0, 0);
                right = right.normalize();
                Vec3d up = right.crossProduct(look).normalize();
                double ang = k * Math.PI / 4;
                Vec3d offset = right.multiply(Math.cos(ang)).add(up.multiply(Math.sin(ang)));
                Vec3d from = muzzle(p).add(offset);
                Vec3d aim = aimPoint(p, w, 64);
                fire(p, w, from, aim.subtract(from), Mode.SPIN, 5f, 3.2f, 0f, 0, 0, 70);
                w.spawnParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 2, 0.05, 0.05, 0.05, 0.01);
                sfx(w, p, SoundEvents.ENTITY_ARROW_SHOOT, 0.5f, 1.6f + k * 0.05f);
            });
        }
    }

    // =====================================================================================
    // ACT 3 - holes and pursuit
    // =====================================================================================

    /** The nail opens a hole in whatever it hits and cripples living targets. */
    private static void wormholeNail(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d aim = aimPoint(p, w, 64);
        fire(p, w, from, aim.subtract(from), Mode.WORMHOLE, 10f, 3.2f, 0f, 0, 0, 60);
        w.spawnParticles(ParticleTypes.PORTAL, from.x, from.y, from.z, 25, 0.2, 0.2, 0.2, 0.6);
        sfx(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);
    }

    /** Five nails burst outwards and curve back to hunt the nearest enemies. */
    private static void homingSpiral(ServerPlayerEntity p, ServerWorld w) {
        double[] angles = {-50, -25, 0, 25, 50};
        for (int i = 0; i < angles.length; i++) {
            final double deg = angles[i];
            Scheduler.later(1 + i * 3, () -> {
                if (!valid(p, w)) return;
                Vec3d from = muzzle(p);
                Vec3d dir = p.getRotationVec(1f).rotateY((float) Math.toRadians(deg)).add(0, 0.35, 0).normalize();
                fire(p, w, from, dir, Mode.HOMING, 7f, 1.7f, 0.2f, 0, 0, 140);
                shotFx(w, p, from, 1.3f);
            });
        }
    }

    /** Resistance + deflects every projectile that comes near, firing a homing nail back for each one. */
    private static void tuskGuard(ServerPlayerEntity p, ServerWorld w) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 55, 2));
        sfx(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.7f, 1.4f);
        guardPulse(p, w, 25);
    }

    private static void guardPulse(ServerPlayerEntity p, ServerWorld w, int left) {
        if (left <= 0 || !valid(p, w)) return;
        Vec3d c = p.getPos().add(0, 1, 0);
        for (int i = 0; i < 14; i++) {
            double a = left * 0.5 + i * Math.PI * 2 / 14;
            w.spawnParticles(GOLD, c.x + Math.cos(a) * 1.7, c.y + Math.sin(left * 0.4 + i) * 0.8, c.z + Math.sin(a) * 1.7, 1, 0, 0, 0, 0);
        }
        List<ProjectileEntity> list = w.getEntitiesByClass(ProjectileEntity.class, p.getBoundingBox().expand(5),
                pr -> pr.getOwner() != p);
        for (ProjectileEntity pr : list) {
            Vec3d v = pr.getVelocity();
            pr.setVelocity(v.multiply(-1.1));
            pr.setOwner(p);
            pr.velocityModified = true;
            w.spawnParticles(ParticleTypes.END_ROD, pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
            sfx(w, p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);
            fire(p, w, pr.getPos(), v.multiply(-1), Mode.HOMING, 5f, 2.0f, 0f, 0, 0, 60);
        }
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(2.5),
                e -> e != p && e instanceof Monster)) {
            e.takeKnockback(0.4, p.getX() - e.getX(), p.getZ() - e.getZ());
        }
        Scheduler.later(2, () -> guardPulse(p, w, left - 1));
    }

    /** Ground slam: damages and launches everyone close by, then a ring of nails shoots outwards. */
    private static void rotationShockwave(ServerPlayerEntity p, ServerWorld w) {
        Vec3d c = p.getPos().add(0, 1.0, 0);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(4.5),
                e -> e != p && e.isAlive())) {
            e.timeUntilRegen = 0;
            e.damage(w.getDamageSources().playerAttack(p), 8f);
            e.takeKnockback(1.1, p.getX() - e.getX(), p.getZ() - e.getZ());
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), p);
        }
        for (int i = 0; i < 20; i++) {
            double a = i * Math.PI * 2 / 20;
            Vec3d dir = new Vec3d(Math.cos(a), 0.05, Math.sin(a));
            fire(p, w, c.add(dir.multiply(0.8)), dir, Mode.SPIN, 5f, 1.9f, 0f, 0, 0, 16);
        }
        for (double r : new double[]{1.0, 2.5, 4.0}) {
            for (int i = 0; i < 30; i++) {
                double a = i * Math.PI * 2 / 30;
                w.spawnParticles(GOLD, c.x + Math.cos(a) * r, p.getY() + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
            }
        }
        w.spawnParticles(ParticleTypes.EXPLOSION, c.x, p.getY() + 0.3, c.z, 1, 0, 0, 0, 0);
        sfx(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.6f);
    }

    // =====================================================================================
    // ACT 4 - Infinite rotation
    // =====================================================================================

    /** The golden nail: homes in, hits hard, then ten pulses of infinite rotation keep tearing the target. */
    private static void infiniteRotation(ServerPlayerEntity p, ServerWorld w) {
        Vec3d from = muzzle(p);
        Vec3d aim = aimPoint(p, w, 64);
        fire(p, w, from, aim.subtract(from), Mode.INFINITE, 14f, 2.6f, 0f, 0, 0, 160);
        w.spawnParticles(ParticleTypes.PORTAL, from.x, from.y, from.z, 40, 0.3, 0.3, 0.3, 0.8);
        sfx(w, p, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.7f);
        sfx(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
    }

    /** Blink through a wormhole: hurts everything on the path, then twelve homing nails burst out on arrival. */
    private static void wormholeDash(ServerPlayerEntity p, ServerWorld w) {
        Vec3d start = p.getPos();
        Vec3d look = p.getRotationVec(1f);
        Vec3d dest = start;
        for (double d = 18; d >= 2; d -= 1) {
            Vec3d off = look.multiply(d);
            if (w.isSpaceEmpty(p, p.getBoundingBox().offset(off))) {
                dest = start.add(off);
                break;
            }
        }
        Vec3d line = dest.subtract(start);
        int steps = Math.max(1, (int) (line.length() * 2));
        Set<Integer> hit = new HashSet<>();
        for (int i = 0; i <= steps; i++) {
            Vec3d pt = start.add(line.multiply(i / (double) steps)).add(0, 1, 0);
            w.spawnParticles(GOLD, pt.x, pt.y, pt.z, 2, 0.25, 0.4, 0.25, 0.01);
            w.spawnParticles(ParticleTypes.PORTAL, pt.x, pt.y, pt.z, 3, 0.25, 0.4, 0.25, 0.3);
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(pt, pt).expand(1.6),
                    e -> e != p && e.isAlive())) {
                if (hit.add(e.getId())) {
                    e.timeUntilRegen = 0;
                    e.damage(w.getDamageSources().playerAttack(p), 10f);
                    e.takeKnockback(0.8, p.getX() - e.getX(), p.getZ() - e.getZ());
                }
            }
        }
        sfx(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        p.networkHandler.requestTeleport(dest.x, dest.y, dest.z, p.getYaw(), p.getPitch());
        p.fallDistance = 0;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 4));
        final Vec3d arrive = dest;
        Scheduler.later(2, () -> {
            if (!valid(p, w)) return;
            Vec3d c = arrive.add(0, 1.2, 0);
            w.spawnParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 50, 0.4, 0.6, 0.4, 0.5);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI * 2 / 12;
                fire(p, w, c, new Vec3d(Math.cos(a), 0.25, Math.sin(a)), Mode.HOMING, 5f, 1.9f, 0.1f, 0, 0, 80);
            }
            sfx(w, p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.7f);
        });
    }

    /** Sixteen wormholes open in a ring around the target; each one fires a nail a moment later. */
    private static void goldenEncirclement(ServerPlayerEntity p, ServerWorld w) {
        LivingEntity target = aimedEntity(p, w, 48);
        Vec3d aim = aimPoint(p, w, 32);
        Vec3d center = target != null ? target.getPos().add(0, target.getHeight() * 0.5, 0) : aim;
        for (int i = 0; i < 16; i++) {
            final int k = i;
            double a = k * Math.PI * 2 / 16;
            final Vec3d pos = center.add(Math.cos(a) * 7, ((k % 4) - 1.5) * 0.9, Math.sin(a) * 7);
            Scheduler.later(1 + k, () -> {
                w.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 30, 0.3, 0.3, 0.3, 0.5);
                w.spawnParticles(GOLD, pos.x, pos.y, pos.z, 12, 0.35, 0.35, 0.35, 0.02);
                sfx(w, p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f + k * 0.05f);
            });
            Scheduler.later(26 + k, () -> {
                if (!valid(p, w)) return;
                Vec3d aimAt = (target != null && target.isAlive())
                        ? target.getPos().add(0, target.getHeight() * 0.5, 0) : center;
                fire(p, w, pos, aimAt.subtract(pos), Mode.SPIN, 9f, 2.8f, 0f, 0, 0, 30);
                w.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 4, 0.1, 0.1, 0.1, 0.05);
            });
        }
    }

    /** A black hole of rotation: drags everyone in, grinds them, then explodes into 24 hunting nails. */
    private static void infiniteVoid(ServerPlayerEntity p, ServerWorld w) {
        Vec3d center = aimPoint(p, w, 24).add(0, 1.0, 0);
        sfx(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.6f);
        for (int k = 0; k < 30; k++) {
            final int tick = k;
            Scheduler.later(1 + k * 2, () -> {
                if (w.getServer() == null) return;
                w.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 30, 3.5, 3.5, 3.5, 0.6);
                w.spawnParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 3, 0.25, 0.25, 0.25, 0.01);
                for (int i = 0; i < 10; i++) {
                    double a = tick * 0.6 + i * Math.PI * 2 / 10;
                    double r = 1.5 + (i % 3) * 0.7;
                    w.spawnParticles(GOLD, center.x + Math.cos(a) * r, center.y + Math.sin(a * 2) * 0.4, center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(8),
                        e -> e != p && e.isAlive() && !e.isSpectator())) {
                    Vec3d to = center.subtract(e.getPos().add(0, e.getHeight() * 0.5, 0));
                    if (to.lengthSquared() < 0.25) continue;
                    Vec3d pull = to.normalize().multiply(0.14);
                    e.addVelocity(pull.x, pull.y + 0.02, pull.z);
                    e.velocityModified = true;
                    e.fallDistance = 0;
                    if (tick % 5 == 0) {
                        e.timeUntilRegen = 0;
                        DamageSource src = w.getDamageSources().indirectMagic(p, p);
                        e.damage(src, 2f);
                    }
                }
            });
        }
        Scheduler.later(64, () -> {
            w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            sfx(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.2f);
            if (!valid(p, w)) return;
            for (int i = 0; i < 24; i++) {
                double y = 1 - 2 * (i + 0.5) / 24;
                double r = Math.sqrt(1 - y * y);
                double th = i * 2.399963;
                fire(p, w, center, new Vec3d(Math.cos(th) * r, y, Math.sin(th) * r), Mode.HOMING, 6f, 1.5f, 0f, 0, 0, 100);
            }
        });
    }

    // =====================================================================================
    // helpers
    // =====================================================================================

    private static boolean valid(ServerPlayerEntity p, ServerWorld w) {
        return p.isAlive() && !p.isRemoved() && p.getWorld() == w;
    }

    static Optional<TuskStandEntity> findStand(ServerPlayerEntity p) {
        List<TuskStandEntity> l = p.getWorld().getEntitiesByClass(TuskStandEntity.class,
                p.getBoundingBox().expand(16), s -> p.getUuid().equals(s.getOwnerUuid()));
        return l.stream().findFirst();
    }

    /** Nails leave from the stand's hand, falling back to the player's eyes. */
    private static Vec3d muzzle(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f);
        return findStand(p)
                .map(s -> s.getPos().add(0, 1.3, 0).add(look.multiply(0.7)))
                .orElse(p.getEyePos().add(look.multiply(0.7)));
    }

    private static TuskNailEntity fire(ServerPlayerEntity p, ServerWorld w, Vec3d from, Vec3d dir, Mode mode,
                                       float damage, float speed, float spread, int pierce, int bounces, int life) {
        TuskNailEntity nail = new TuskNailEntity(w, p, mode, damage).configure(pierce, bounces, life);
        Vec3d d = dir.lengthSquared() < 1e-6 ? p.getRotationVec(1f) : dir.normalize();
        nail.setPosition(from);
        nail.setVelocity(d.x, d.y, d.z, speed, spread);
        w.spawnEntity(nail);
        return nail;
    }

    private static void shotFx(ServerWorld w, ServerPlayerEntity p, Vec3d at, float pitch) {
        w.spawnParticles(GOLD, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
        sfx(w, p, SoundEvents.ENTITY_ARROW_SHOOT, 0.7f, pitch);
    }

    private static void sfx(ServerWorld w, ServerPlayerEntity p, SoundEvent sound, float volume, float pitch) {
        w.playSound(null, p.getBlockPos(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    /** The point the player is looking at (entity or block), or the far end of the ray if nothing is hit. */
    private static Vec3d aimPoint(ServerPlayerEntity p, ServerWorld w, double range) {
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1f).multiply(range));
        HitResult block = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, p));
        Vec3d blockEnd = block.getType() == HitResult.Type.MISS ? end : block.getPos();
        EntityHitResult ent = ProjectileUtil.raycast(p, eye, blockEnd, new Box(eye, blockEnd).expand(1.0),
                e -> !e.isSpectator() && e.canHit(), range * range);
        return ent != null ? ent.getPos() : blockEnd;
    }

    private static LivingEntity aimedEntity(ServerPlayerEntity p, ServerWorld w, double range) {
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1f).multiply(range));
        HitResult block = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, p));
        Vec3d blockEnd = block.getType() == HitResult.Type.MISS ? end : block.getPos();
        EntityHitResult ent = ProjectileUtil.raycast(p, eye, blockEnd, new Box(eye, blockEnd).expand(1.0),
                e -> !e.isSpectator() && e.canHit(), range * range);
        return ent != null && ent.getEntity() instanceof LivingEntity le ? le : null;
    }
}
