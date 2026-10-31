package com.tuskmod.entity;

import com.tuskmod.ModRegistry;
import com.tuskmod.TuskMod;
import com.tuskmod.ability.Scheduler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/** The golden, rotating nail. One class, eight behaviours (see {@link Mode}). */
public class TuskNailEntity extends ProjectileEntity {
    public enum Mode { NORMAL, SPIN, DRILL, RICOCHET, HOMING, WORMHOLE, INFINITE, RAIN }

    private static final TrackedData<Integer> MODE =
            DataTracker.registerData(TuskNailEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.0f, 0.78f, 0.15f), 0.9f);

    private float damage = 3f;
    private int pierce = 0;
    private int bounces = 0;
    private int maxAge = 100;
    private final Set<Integer> alreadyHit = new HashSet<>();
    private LivingEntity homingTarget;

    public TuskNailEntity(EntityType<? extends TuskNailEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public TuskNailEntity(World world, Entity owner, Mode mode, float damage) {
        this(ModRegistry.NAIL, world);
        setOwner(owner);
        setMode(mode);
        this.damage = damage;
    }

    public TuskNailEntity configure(int pierce, int bounces, int maxAge) {
        this.pierce = pierce;
        this.bounces = bounces;
        this.maxAge = maxAge;
        return this;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(MODE, 0);
    }

    public Mode getMode() {
        int i = dataTracker.get(MODE);
        Mode[] all = Mode.values();
        return all[MathHelper.clamp(i, 0, all.length - 1)];
    }

    private void setMode(Mode mode) {
        dataTracker.set(MODE, mode.ordinal());
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}

    @Override
    protected boolean canHit(Entity entity) {
        if (entity instanceof TuskStandEntity || entity == getOwner()) return false;
        if (alreadyHit.contains(entity.getId())) return false;
        return super.canHit(entity);
    }

    // ------------------------------------------------------------ movement
    @Override
    public void tick() {
        super.tick();
        Mode mode = getMode();
        World w = getWorld();

        if (!w.isClient) {
            if (age > maxAge) {
                discard();
                return;
            }
            if (mode == Mode.HOMING || mode == Mode.INFINITE) steer(mode == Mode.INFINITE ? 0.22 : 0.12);

            HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
            if (hit.getType() != HitResult.Type.MISS) {
                onCollision(hit);
                if (isRemoved()) return;
            }
        }

        Vec3d v = getVelocity();
        setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
        double h = v.horizontalLength();
        setYaw((float) (MathHelper.atan2(v.x, v.z) * 57.29578));
        setPitch((float) (MathHelper.atan2(v.y, h) * 57.29578));

        if (w.isClient) trail(mode, v);
    }

    private void trail(Mode mode, Vec3d v) {
        World w = getWorld();
        w.addParticle(GOLD, getX(), getY(), getZ(), 0, 0, 0);
        if (mode == Mode.SPIN || mode == Mode.WORMHOLE || mode == Mode.INFINITE || mode == Mode.HOMING) {
            Vec3d dir = v.lengthSquared() < 1e-6 ? new Vec3d(0, 1, 0) : v.normalize();
            Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
            if (side.lengthSquared() < 1e-4) side = new Vec3d(1, 0, 0);
            side = side.normalize();
            Vec3d up = side.crossProduct(dir).normalize();
            double a = age * 0.9;
            double r = mode == Mode.INFINITE ? 0.45 : 0.28;
            Vec3d o = side.multiply(Math.cos(a) * r).add(up.multiply(Math.sin(a) * r));
            w.addParticle(ParticleTypes.END_ROD, getX() + o.x, getY() + o.y, getZ() + o.z, 0, 0, 0);
            if (mode == Mode.INFINITE || mode == Mode.WORMHOLE) {
                w.addParticle(ParticleTypes.PORTAL, getX() - o.x, getY() - o.y, getZ() - o.z, 0, 0, 0);
            }
        }
    }

    private void steer(double turn) {
        if (homingTarget == null || !homingTarget.isAlive() || homingTarget.squaredDistanceTo(this) > 48 * 48) {
            homingTarget = findTarget();
        }
        if (homingTarget == null) return;
        Vec3d v = getVelocity();
        double speed = v.length();
        if (speed < 1e-4) return;
        Vec3d desired = homingTarget.getPos().add(0, homingTarget.getHeight() * 0.5, 0).subtract(getPos()).normalize();
        Vec3d nv = v.normalize().multiply(1 - turn).add(desired.multiply(turn)).normalize().multiply(speed);
        setVelocity(nv);
    }

    private LivingEntity findTarget() {
        Entity owner = getOwner();
        Vec3d dir = getVelocity().normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Box box = getBoundingBox().expand(24);
        for (LivingEntity e : getWorld().getEntitiesByClass(LivingEntity.class, box, le -> le.isAlive() && le != owner
                && !le.isSpectator() && !(le instanceof ArmorStandEntity))) {
            if (e instanceof TameableEntity t && owner instanceof LivingEntity lo && t.isOwner(lo)) continue;
            Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(getPos());
            double d = to.length();
            if (d < 0.01) continue;
            double dot = to.normalize().dotProduct(dir);
            if (dot < 0.0) continue;
            double score = d * (2.2 - dot) * (e instanceof Monster ? 0.6 : 1.0);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ impacts
    @Override
    protected void onEntityHit(EntityHitResult result) {
        super.onEntityHit(result);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        Entity target = result.getEntity();
        Entity owner = getOwner();
        alreadyHit.add(target.getId());

        if (target instanceof LivingEntity le) le.timeUntilRegen = 0;
        DamageSource src = sw.getDamageSources().thrown(this, owner);
        boolean hurt = target.damage(src, damage);

        if (hurt && target instanceof LivingEntity le) {
            switch (getMode()) {
                case SPIN -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), owner);
                case HOMING -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0), owner);
                case WORMHOLE -> {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2), owner);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1), owner);
                    sw.spawnParticles(ParticleTypes.PORTAL, le.getX(), le.getBodyY(0.5), le.getZ(), 40, 0.4, 0.6, 0.4, 0.8);
                }
                case INFINITE -> infiniteRotation(sw, le, owner);
                default -> {}
            }
        }
        impactFx(sw, result.getPos());
        if (pierce > 0) {
            pierce--;
        } else {
            discard();
        }
    }

    /** The signature move: ten pulses of golden rotation after the nail lands. */
    private static void infiniteRotation(ServerWorld sw, LivingEntity le, Entity owner) {
        le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 110, 1), owner);
        for (int i = 1; i <= 10; i++) {
            final int k = i;
            Scheduler.later(i * 10, () -> {
                if (!le.isAlive() || le.isRemoved() || le.getWorld() != sw) return;
                le.timeUntilRegen = 0;
                DamageSource src = owner != null
                        ? sw.getDamageSources().indirectMagic(owner, owner)
                        : sw.getDamageSources().magic();
                le.damage(src, 3.0f);
                for (int j = 0; j < 12; j++) {
                    double a = (k * 0.7) + j * (Math.PI * 2 / 12);
                    sw.spawnParticles(GOLD, le.getX() + Math.cos(a) * 0.9, le.getBodyY(0.1 + 0.08 * j), le.getZ() + Math.sin(a) * 0.9,
                            1, 0, 0, 0, 0);
                }
                sw.spawnParticles(ParticleTypes.PORTAL, le.getX(), le.getBodyY(0.5), le.getZ(), 20, 0.3, 0.5, 0.3, 0.5);
                sw.playSound(null, le.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 0.6f + k * 0.08f);
            });
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult result) {
        super.onBlockHit(result);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        Mode mode = getMode();
        BlockPos pos = result.getBlockPos();
        BlockState state = sw.getBlockState(pos);

        switch (mode) {
            case RICOCHET -> {
                if (bounces > 0) {
                    bounce(sw, result);
                    return;
                }
            }
            case DRILL -> {
                float hardness = state.getHardness(sw, pos);
                if (TuskMod.BREAK_BLOCKS && hardness >= 0 && hardness <= 0.6f) {
                    sw.breakBlock(pos, true, getOwner());
                    return; // keep drilling
                }
            }
            case WORMHOLE -> carve(sw, result, 6, false);
            case INFINITE -> carve(sw, result, 9, true);
            default -> {}
        }
        impactFx(sw, result.getPos());
        discard();
    }

    private void bounce(ServerWorld sw, BlockHitResult r) {
        Vec3d v = getVelocity();
        Vec3d n = Vec3d.of(r.getSide().getVector());
        Vec3d reflected = v.subtract(n.multiply(2 * v.dotProduct(n)));
        setVelocity(reflected.multiply(0.95));
        setPosition(r.getPos().add(n.multiply(0.06)));
        bounces--;
        damage *= 1.15f;
        alreadyHit.clear();
        sw.spawnParticles(GOLD, r.getPos().x, r.getPos().y, r.getPos().z, 8, 0.1, 0.1, 0.1, 0.05);
        sw.playSound(null, getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.9f, 1.8f);
    }

    /** Drills a hole into the surface that was hit: the "wormhole". */
    private void carve(ServerWorld sw, BlockHitResult r, int depth, boolean wide) {
        if (!TuskMod.BREAK_BLOCKS) return;
        Direction into = r.getSide().getOpposite();
        Entity owner = getOwner();
        for (int i = 0; i < depth; i++) {
            BlockPos bp = r.getBlockPos().offset(into, i);
            BlockState s = sw.getBlockState(bp);
            if (s.isAir()) continue;
            float h = s.getHardness(sw, bp);
            if (h < 0 || h > 25f) break;
            sw.breakBlock(bp, true, owner);
            if (wide) {
                for (Direction d : Direction.values()) {
                    if (d.getAxis() == into.getAxis()) continue;
                    BlockPos np = bp.offset(d);
                    BlockState ns = sw.getBlockState(np);
                    float nh = ns.getHardness(sw, np);
                    if (!ns.isAir() && nh >= 0 && nh <= 25f) sw.breakBlock(np, true, owner);
                }
            }
            sw.spawnParticles(ParticleTypes.PORTAL, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.4);
        }
        sw.playSound(null, r.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5f, 2.0f);
    }

    private void impactFx(ServerWorld sw, Vec3d p) {
        sw.spawnParticles(GOLD, p.x, p.y, p.z, 10, 0.15, 0.15, 0.15, 0.05);
        sw.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.03);
        sw.playSound(null, BlockPos.ofFloored(p), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.8f, 1.5f);
    }
}
