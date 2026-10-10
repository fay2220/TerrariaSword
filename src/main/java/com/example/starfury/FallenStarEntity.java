package com.example.starfury;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Star that falls from the sky when the Starfury is right-clicked. */
public class FallenStarEntity extends ThrowableItemProjectile {
    public static final float DAMAGE = 5.0F;
    private static final int MAX_AGE = 100; // ticks

    public FallenStarEntity(EntityType<? extends FallenStarEntity> type, Level level) {
        super(type, level);
    }

    public FallenStarEntity(Level level, LivingEntity owner) {
        super(ModEntities.FALLEN_STAR.get(), owner, level);
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

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        } else if (this.tickCount > MAX_AGE) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living) {
            // Let every star in the volley deal its full damage (skip i-frames).
            living.invulnerableTime = 0;
        }
        target.hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(), 12, 0.2D, 0.2D, 0.2D, 0.08D);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
            this.discard();
        }
    }
}
