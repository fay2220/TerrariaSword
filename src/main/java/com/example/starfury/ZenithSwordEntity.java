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
    /** Damage per hit (kept for backward compatibility; actual damage is dealt by ZenithSwordItem). */
    public static final float DAMAGE = 25.0F;
    /** Maximum distance (blocks) from the player to the turn point. */
    public static final double RANGE = 25.0D;
    /** Ticks for a full out-and-back trip (8 ticks = 0.4 seconds, fast and fierce). */
    public static final int FLIGHT_TICKS = 8;
    /** Extra reach around the blade for hit detection (blocks). */
    private static final double HIT_RADIUS = 1.5D;
    /** Speed (blocks/tick) when a blade that bumped into a wall flies straight back. */
    private static final double RETURN_SPEED = 5.0D;
    private static final double MIN_LENGTH = 1.5D;
    private static final int MAX_AGE = 20;
    /** Max random angle (degrees) between the player's view and each blade's direction. */
    private static final double SPREAD_DEGREES = 8.0D;

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
    private static final byte FLAG_BIG_BOOM = 2;    // sonic boom
    private static final byte FLAG_FLASH = 4;       // flash
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
    public static final int TRAIL_LENGTH = 10;

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

    /** Spawns one blade from {@code owner}'s hand with a directional swoop. Pure visual animation. */
    public static void launch(ServerLevel level, LivingEntity owner, int index, double sideSign) {
        RandomSource rnd = level.random;
        Vec3 look = owner.getViewVector(1.0F);

        float yawOff = (float) ((rnd.nextDouble() - 0.5D) * 2.0D * SPREAD_DEGREES);
        float pitchOff = (float) ((rnd.nextDouble() - 0.5D) * 2.0D * SPREAD_DEGREES);
        Vec3 dir = look.yRot(yawOff * DEG_TO_RAD).xRot(pitchOff * DEG_TO_RAD).normalize();

        Vec3 ref = Math.abs(dir.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = dir.cross(ref).normalize();
        Vec3 up = right.cross(dir).normalize();

        double angle = (rnd.nextDouble() * 0.45D + 0.25D) * Math.PI;
        if (sideSign < 0) {
            angle += Math.PI;
        }
        Vec3 sideVec = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));

        // Zenith penetrates all blocks: full reach up to 25 blocks
        double len = RANGE * (0.92D + rnd.nextDouble() * 0.08D);

        ZenithSwordEntity blade = new ZenithSwordEntity(ModEntities.ZENITH_BLADE.get(), level);
        blade.setOwner(owner);
        blade.direction = dir;
        blade.side = sideVec;
        blade.length = len;
        blade.width = len * (0.16D + rnd.nextDouble() * 0.16D);
        blade.entityData.set(DATA_VARIANT, index % VARIANT_COUNT);
        blade.entityData.set(DATA_FLAGS, FLAG_LAUNCH_BOOM);

        Vec3 start = anchor(owner);
        blade.setPos(start.x, start.y, start.z);
        blade.setRotationFrom(dir);
        level.addFreshEntity(blade);
    }

    public static void launch(ServerLevel level, LivingEntity owner, int index, boolean bigBoom, boolean firstBoom) {
        launch(level, owner, index, 1.0D);
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

        double t = Math.min(1.0D, this.age / (double) FLIGHT_TICKS);
        // Fast, aggressive out-and-back loop uninhibited by blocks
        double along = this.length * Math.sin(Math.PI * t);
        double sideways = this.width * Math.sin(Math.PI * 2.0D * t);
        next = origin.add(this.direction.scale(along)).add(this.side.scale(sideways));

        if (!this.apexDone && t >= 0.5D) {
            this.apexDone = true;
            this.setPos(next.x, next.y, next.z);
            this.level().broadcastEntityEvent(this, EVENT_APEX);
        }
        if (t >= 1.0D) {
            finished = true;
        }

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
