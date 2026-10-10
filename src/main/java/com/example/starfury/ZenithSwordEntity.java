package com.example.starfury;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fml.DistExecutor;
import org.joml.Vector3f;

/**
 * One spectral blade of the Zenith.
 *
 * Server: flies along a looping path (out to the turn point, back to the player's hand) in {@link #FLIGHT_TICKS}
 * ticks, anchored to the player's current position. Stops at walls (never goes through blocks) and then flies
 * straight home. Hits every mob it passes once for {@link #DAMAGE}.
 *
 * Client: only visuals (trail particles; sonic boom effects through entity events -> {@link ZenithClientEffects}).
 */
public class ZenithSwordEntity extends Projectile {
    /** Damage per hit. With 5 blades per second each target can be hit about 5 times per second. */
    public static final float DAMAGE = 20.0F;
    /** Maximum distance (blocks) from the player to the turn point. */
    public static final double RANGE = 25.0D;
    /** Ticks for a full out-and-back trip (20 ticks = 1 second). */
    public static final int FLIGHT_TICKS = 20;
    /** Extra reach around the blade for hit detection (blocks). */
    private static final double HIT_RADIUS = 0.7D;
    /** Speed (blocks/tick) when a blade that bumped into a wall flies straight back. */
    private static final double RETURN_SPEED = 2.6D;
    private static final double MIN_LENGTH = 1.5D;
    private static final int MAX_AGE = 80;
    /** Max random angle (degrees) between the player's view and each blade's direction. */
    private static final double SPREAD_DEGREES = 9.0D;

    /** Colours (start, end) used by trails/particles per variant: 0 zenith, 1 starfury, 2-5 phantom blades. */
    public static final float[][] VARIANT_COLORS = {
            {0.55F, 0.45F, 1.00F, 0.45F, 0.95F, 0.85F}, // zenith: violet -> mint
            {1.00F, 0.30F, 0.55F, 1.00F, 0.85F, 0.30F}, // starfury: pink -> gold
            {0.35F, 1.00F, 0.45F, 0.85F, 1.00F, 0.55F}, // emerald
            {1.00F, 0.55F, 0.15F, 1.00F, 0.90F, 0.45F}, // ember
            {1.00F, 0.40F, 0.85F, 0.75F, 0.55F, 1.00F}, // rose
            {0.30F, 0.80F, 1.00F, 0.85F, 0.95F, 1.00F}, // azure
    };
    public static final int VARIANT_COUNT = VARIANT_COLORS.length;

    private static final byte EVENT_APEX = 78;
    private static final byte EVENT_WALL = 79;
    private static final byte FLAG_LAUNCH_BOOM = 1; // every blade: small vapor puff in front of the player
    private static final byte FLAG_BIG_BOOM = 2;    // sonic boom: vapor cone, shockwave line, speed lines, shake
    private static final byte FLAG_FLASH = 4;       // first boom of a hold: white impact flash
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(ZenithSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(ZenithSwordEntity.class, EntityDataSerializers.BYTE);

    // ---- server-side flight state ----
    private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 side = new Vec3(1.0D, 0.0D, 0.0D);
    private double length = RANGE;
    private double width = 3.0D;
    private int age;
    private boolean returning;
    private boolean apexDone;
    private final Set<Integer> alreadyHit = new HashSet<>();

    // ---- client-side visual state (plain common classes only) ----
    private boolean clientStarted;
    /** Recent positions, newest first. Read by the renderer to draw the light ribbon. */
    public final Deque<Vec3> trail = new ArrayDeque<>();
    public static final int TRAIL_LENGTH = 6;

    public ZenithSwordEntity(EntityType<? extends ZenithSwordEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VARIANT, 0);
        this.entityData.define(DATA_FLAGS, (byte) 0);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(DATA_VARIANT), 0, VARIANT_COUNT - 1);
    }

    // ------------------------------------------------------------------------------------------------
    // Launch
    // ------------------------------------------------------------------------------------------------

    /** Spawns one blade from {@code owner}'s hand toward where they look. Server only. */
    public static void launch(ServerLevel level, LivingEntity owner, int index, boolean bigBoom, boolean firstBoom) {
        RandomSource rnd = level.random;
        Vec3 look = owner.getViewVector(1.0F);

        // small random deviation so the blades fan out around the crosshair
        float yawOff = (float) ((rnd.nextDouble() - 0.5D) * 2.0D * SPREAD_DEGREES);
        float pitchOff = (float) ((rnd.nextDouble() - 0.5D) * 2.0D * SPREAD_DEGREES);
        Vec3 dir = look.yRot(yawOff * DEG_TO_RAD).xRot(pitchOff * DEG_TO_RAD).normalize();
        if (index == 0) {
            dir = look; // the very first blade goes exactly where the player is aiming
        }

        // orthonormal basis around the flight direction, then a random plane for the loop
        Vec3 ref = Math.abs(dir.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = dir.cross(ref).normalize();
        Vec3 up = right.cross(dir).normalize();
        double a = rnd.nextDouble() * Math.PI * 2.0D;
        Vec3 sideVec = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));

        // never aim further than the first wall in that direction
        Vec3 eye = owner.getEyePosition();
        BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(dir.scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        double len = RANGE;
        if (wall.getType() != HitResult.Type.MISS) {
            len = Math.max(MIN_LENGTH, wall.getLocation().distanceTo(eye) - 0.6D);
        }

        ZenithSwordEntity blade = new ZenithSwordEntity(ModEntities.ZENITH_BLADE.get(), level);
        blade.setOwner(owner);
        blade.direction = dir;
        blade.side = sideVec;
        blade.length = len;
        blade.width = len * (0.12D + rnd.nextDouble() * 0.16D);
        blade.entityData.set(DATA_VARIANT, index == 0 ? 0 : rnd.nextInt(VARIANT_COUNT));
        int flags = FLAG_LAUNCH_BOOM;
        if (bigBoom) {
            flags |= FLAG_BIG_BOOM;
        }
        if (firstBoom) {
            flags |= FLAG_FLASH;
        }
        blade.entityData.set(DATA_FLAGS, (byte) flags);

        Vec3 start = anchor(owner);
        blade.setPos(start.x, start.y, start.z);
        blade.setRotationFrom(dir);
        level.addFreshEntity(blade);
    }

    /** Where the blades start and come back to: roughly the player's sword hand. */
    private static Vec3 anchor(LivingEntity owner) {
        Vec3 look = owner.getViewVector(1.0F);
        double h = Math.sqrt(look.x * look.x + look.z * look.z);
        Vec3 eye = owner.getEyePosition().add(0.0D, -0.45D, 0.0D);
        if (h < 1.0E-4D) {
            return eye;
        }
        // a little to the right, so the blades do not start inside the camera
        return eye.add(-look.z / h * 0.35D, 0.0D, look.x / h * 0.35D);
    }

    private void setRotationFrom(Vec3 motion) {
        double h = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
        this.setXRot((float) (Mth.atan2(motion.y, h) * (180.0D / Math.PI)));
    }

    // ------------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
            return;
        }

        Entity ownerEntity = this.getOwner();
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }

        this.age++;
        Vec3 origin = anchor(owner);
        Vec3 prev = this.position();
        Vec3 next;
        boolean finished = false;

        if (!this.returning) {
            double t = Math.min(1.0D, this.age / (double) FLIGHT_TICKS);
            // out-and-back along "direction", swinging sideways: the blade draws a loop like in Terraria
            double along = this.length * Math.sin(Math.PI * t);
            double sideways = this.width * Math.sin(Math.PI * 2.0D * t);
            next = origin.add(this.direction.scale(along)).add(this.side.scale(sideways));

            // blades cannot pass through blocks: stop at the wall and fly straight back
            BlockHitResult hit = this.level().clip(new ClipContext(prev, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 back = prev.subtract(next);
                back = back.lengthSqr() > 1.0E-6D ? back.normalize().scale(0.25D) : Vec3.ZERO;
                next = hit.getLocation().add(back);
                this.returning = true;
                this.apexDone = true;
                hitWall(next);
            } else if (!this.apexDone && t >= 0.5D) {
                this.apexDone = true;
                this.setPos(next.x, next.y, next.z); // event uses the current position
                this.level().broadcastEntityEvent(this, EVENT_APEX);
            }
            if (t >= 1.0D) {
                finished = true;
            }
        } else {
            Vec3 toHome = origin.subtract(prev);
            double dist = toHome.length();
            if (dist <= RETURN_SPEED + 0.5D) {
                next = origin;
                finished = true;
            } else {
                next = prev.add(toHome.scale(RETURN_SPEED / dist));
            }
        }

        hitEntities(owner, prev, next);

        Vec3 motion = next.subtract(prev);
        this.setDeltaMovement(motion);
        if (motion.lengthSqr() > 1.0E-6D) {
            this.setRotationFrom(motion);
        }
        this.setPos(next.x, next.y, next.z);

        if (finished || this.age > MAX_AGE) {
            this.discard();
        }
    }

    private void hitEntities(LivingEntity owner, Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(HIT_RADIUS);
        ServerLevel level = (ServerLevel) this.level();
        for (Entity target : level.getEntities(this, sweep, e -> canHarm(owner, e))) {
            if (this.alreadyHit.contains(target.getId())) {
                continue;
            }
            AABB box = target.getBoundingBox().inflate(HIT_RADIUS);
            if (!box.contains(from) && !box.contains(to) && box.clip(from, to).isEmpty()) {
                continue;
            }
            this.alreadyHit.add(target.getId());

            // every blade counts fully, even if another one hit a moment ago (allows ~5 hits/second)
            target.invulnerableTime = 0;
            if (target instanceof PartEntity<?> part) {
                part.getParent().invulnerableTime = 0;
            }
            if (!target.hurt(this.damageSources().thrown(this, owner), DAMAGE)) {
                continue;
            }

            Vec3 push = to.subtract(from);
            if (target instanceof LivingEntity living && push.horizontalDistanceSqr() > 1.0E-4D) {
                living.knockback(0.2D, -push.x, -push.z);
            }
            double y = target.getY(0.5D);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), y, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), y, target.getZ(), 10, 0.3D, 0.4D, 0.3D, 0.5D);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), y, target.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.4D);
            level.playSound(null, target.getX(), y, target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS, 0.5F, 1.3F + this.random.nextFloat() * 0.4F);
        }
    }

    private static boolean canHarm(LivingEntity owner, Entity e) {
        if (e == owner || !e.isAlive() || e.isSpectator()) {
            return false;
        }
        if (!(e instanceof LivingEntity) && !(e instanceof PartEntity<?>)) {
            return false; // no boats, item frames, dropped items...
        }
        if (owner.isAlliedTo(e)) {
            return false;
        }
        return !(e instanceof TamableAnimal pet && pet.isOwnedBy(owner));
    }

    private void hitWall(Vec3 at) {
        this.setPos(at.x, at.y, at.z);
        this.level().broadcastEntityEvent(this, EVENT_WALL);
        this.level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 1.6F + this.random.nextFloat() * 0.3F);
    }

    // ------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------

    private void clientTick() {
        if (!this.clientStarted) {
            this.clientStarted = true;
            byte flags = this.entityData.get(DATA_FLAGS);
            if ((flags & FLAG_LAUNCH_BOOM) != 0) {
                final boolean big = (flags & FLAG_BIG_BOOM) != 0;
                final boolean flash = (flags & FLAG_FLASH) != 0;
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ZenithClientEffects.launchBoom(this, big, flash));
            }
        }

        Vec3 now = this.position();
        if (this.trail.isEmpty() || this.trail.peekFirst().distanceToSqr(now) > 1.0E-4D) {
            this.trail.addFirst(now);
            while (this.trail.size() > TRAIL_LENGTH) {
                this.trail.removeLast();
            }
        }
        spawnTrailParticles();
    }

    private void spawnTrailParticles() {
        RandomSource rnd = this.random;
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1.0E-3D) {
            return;
        }
        float[] c = VARIANT_COLORS[getVariant()];
        DustColorTransitionOptions dust = new DustColorTransitionOptions(
                new Vector3f(c[0], c[1], c[2]), new Vector3f(c[3], c[4], c[5]), 1.1F);
        int steps = Math.max(2, Mth.ceil(dist * 1.5D));
        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            double x = this.xo + dx * f;
            double y = this.yo + dy * f;
            double z = this.zo + dz * f;
            this.level().addParticle(dust, x, y, z, 0.0D, 0.0D, 0.0D);
            if (rnd.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, x, y, z,
                        rnd.nextGaussian() * 0.02D, rnd.nextGaussian() * 0.02D, rnd.nextGaussian() * 0.02D);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_APEX || id == EVENT_WALL) {
            final boolean wall = id == EVENT_WALL;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ZenithClientEffects.bladeBoom(this, wall));
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Visible from far away (default would cut off at ~32 blocks for such a small hitbox). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) {
        return distanceSq < 128.0D * 128.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
