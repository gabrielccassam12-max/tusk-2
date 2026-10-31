package com.tuskmod.entity;

import com.tuskmod.ModRegistry;
import com.tuskmod.item.TuskItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tusk itself. Not a living entity: it has no hitbox interactions, just follows its owner
 * (right shoulder, slightly behind) while the Tusk item is held and plays poses triggered by attacks.
 */
public class TuskStandEntity extends Entity {
    private static final TrackedData<Optional<UUID>> OWNER =
            DataTracker.registerData(TuskStandEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> ACT =
            DataTracker.registerData(TuskStandEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ANIM =
            DataTracker.registerData(TuskStandEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Incremented on every trigger so the client notices even when the same pose repeats. */
    private static final TrackedData<Integer> TRIGGER =
            DataTracker.registerData(TuskStandEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Client only: entity age at which the current pose started. */
    public int animStartAge = -1000;

    public TuskStandEntity(EntityType<? extends TuskStandEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER, Optional.empty());
        builder.add(ACT, 1);
        builder.add(ANIM, 0);
        builder.add(TRIGGER, 0);
    }

    // ------------------------------------------------------------ accessors
    public UUID getOwnerUuid() {
        return dataTracker.get(OWNER).orElse(null);
    }

    public void setOwner(UUID uuid) {
        dataTracker.set(OWNER, Optional.ofNullable(uuid));
    }

    public int getAct() {
        return dataTracker.get(ACT);
    }

    public void setAct(int act) {
        dataTracker.set(ACT, act);
    }

    public int getAnim() {
        return dataTracker.get(ANIM);
    }

    public void playAnim(int anim) {
        dataTracker.set(ANIM, anim);
        dataTracker.set(TRIGGER, dataTracker.get(TRIGGER) + 1);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (data.equals(TRIGGER) && age > 2) {
            animStartAge = age;
        }
    }

    private PlayerEntity getOwnerPlayer() {
        UUID id = getOwnerUuid();
        return id == null ? null : getWorld().getPlayerByUuid(id);
    }

    // ------------------------------------------------------------ behaviour
    @Override
    public void tick() {
        super.tick();
        PlayerEntity owner = getOwnerPlayer();
        World w = getWorld();

        if (owner == null) {
            if (!w.isClient) discard();
            return;
        }
        if (!w.isClient) {
            ItemStack held = TuskItem.heldTusk(owner);
            if (held.isEmpty() || !owner.isAlive() || owner.isSpectator()) {
                discard();
                return;
            }
            int act = TuskItem.getAct(held);
            if (act != getAct()) setAct(act);
        }
        follow(owner);
    }

    private void follow(PlayerEntity owner) {
        double yaw = Math.toRadians(owner.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);  // owner's right
        double x = owner.getX() + rx * 0.95 - fx * 0.55;
        double y = owner.getY() + 0.1;
        double z = owner.getZ() + rz * 0.95 - fz * 0.55;
        setPosition(x, y, z);
        setYaw(owner.getYaw());
        setPitch(owner.getPitch());
    }

    /** Spawns the stand for this player unless one already exists nearby. */
    public static void ensure(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        List<TuskStandEntity> list = world.getEntitiesByClass(TuskStandEntity.class,
                player.getBoundingBox().expand(32), s -> player.getUuid().equals(s.getOwnerUuid()));
        if (!list.isEmpty()) {
            for (int i = 1; i < list.size(); i++) list.get(i).discard(); // never keep duplicates
            return;
        }
        ItemStack held = TuskItem.heldTusk(player);
        if (held.isEmpty()) return;
        TuskStandEntity stand = new TuskStandEntity(ModRegistry.STAND, world);
        stand.setOwner(player.getUuid());
        stand.setAct(TuskItem.getAct(held));
        stand.follow(player);
        stand.prevX = stand.getX();
        stand.prevY = stand.getY();
        stand.prevZ = stand.getZ();
        world.spawnEntity(stand);
    }

    // ------------------------------------------------------------ it is a spirit: untouchable
    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
