package com.example.starfury;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Star that falls from the sky when the Starfury is right-clicked. Rendered as an animated 3D model via GeckoLib. */
public class FallenStarEntity extends ThrowableItemProjectile implements GeoEntity {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.fallen_star.spin");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** Damage dealt by each star to every mob caught in the impact. */
    public static final float DAMAGE = 5.0F;
    /** Impact radius in blocks. Set to 0 for a single-target hit only. */
    public static final double SPLASH_RADIUS = 2.5D;
    private static final int MAX_AGE = 100; // ticks
    private static final byte EVENT_IMPACT = 77;

    public FallenStarEntity(EntityType<? extends FallenStarEntity> type, Level level) {
        super(type, level);
    }

    public FallenStarEntity(Level level, net.minecraft.world.entity.LivingEntity owner) {
        super(ModEntities.FALLEN_STAR.get(), owner, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", 0, state -> state.setAndContinue(SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FALLEN_STAR.get();
    }

    /** Stars fly in a straight line; no gravity drop. */
    @Override
    protected float getGravity() {
        return 0.0F;
    }

    /** A star never explodes on the player who summoned it. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != this.getOwner();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnTrail();
        } else if (this.tickCount > MAX_AGE) {
            this.discard();
        }
    }

    /** Comet tail: dense gold/white sparks along the path travelled this tick. */
    private void spawnTrail() {
        RandomSource rnd = this.random;
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        int steps = Math.max(2, Mth.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 3.0D));

        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            double x = this.xo + dx * f;
            double y = this.yo + dy * f;
            double z = this.zo + dz * f;

            this.level().addParticle(StarParticles.TAIL_DUST, x, y, z,
                    rnd.nextGaussian() * 0.02D, rnd.nextGaussian() * 0.02D, rnd.nextGaussian() * 0.02D);
            if (rnd.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, x, y, z,
                        rnd.nextGaussian() * 0.03D, rnd.nextGaussian() * 0.03D - 0.01D, rnd.nextGaussian() * 0.03D);
            }
            if (rnd.nextInt(7) == 0) {
                this.level().addParticle(ParticleTypes.FIREWORK, x, y, z,
                        rnd.nextGaussian() * 0.05D, rnd.nextGaussian() * 0.05D, rnd.nextGaussian() * 0.05D);
            }
        }
        // bright core flare around the star itself
        this.level().addParticle(StarParticles.GOLD_DUST, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
    }

    /** Server side: all gameplay (damage, knockback, sound) happens here. */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Vec3 center = result.getLocation();
        this.setPos(center.x, center.y, center.z);
        Entity owner = this.getOwner();

        // every living thing inside the blast radius takes full star damage
        List<LivingEntity> victims = new ArrayList<>();
        if (SPLASH_RADIUS > 0.0D) {
            double r2 = SPLASH_RADIUS * SPLASH_RADIUS;
            AABB box = new AABB(center, center).inflate(SPLASH_RADIUS);
            victims.addAll(level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != owner && e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) <= r2));
        }
        if (result instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity direct
                && direct != owner && !victims.contains(direct)) {
            victims.add(direct);
        }

        for (LivingEntity victim : victims) {
            victim.invulnerableTime = 0; // each star in a volley counts fully
            victim.hurt(this.damageSources().thrown(this, owner), DAMAGE);

            Vec3 away = victim.position().subtract(center);
            double len = Math.max(0.3D, away.horizontalDistance());
            double falloff = 1.0D - Math.min(1.0D, victim.position().distanceTo(center) / (SPLASH_RADIUS + 1.0D));
            victim.push(away.x / len * 0.55D * falloff, 0.3D + 0.2D * falloff, away.z / len * 0.55D * falloff);
            victim.hurtMarked = true;

            level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY(0.5D), victim.getZ(),
                    14, 0.3D, 0.4D, 0.3D, 0.25D);
        }

        // client-side spectacle (particles + screen shake) is triggered by this entity event
        level.broadcastEntityEvent(this, EVENT_IMPACT);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0F, 0.9F + level.random.nextFloat() * 0.3F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.5F);

        this.discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_IMPACT) {
            final Level lvl = this.level();
            final double x = this.getX();
            final double y = this.getY();
            final double z = this.getZ();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffects.impact(lvl, x, y, z));
        } else {
            super.handleEntityEvent(id);
        }
    }
}
