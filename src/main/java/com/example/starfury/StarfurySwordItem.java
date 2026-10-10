package com.example.starfury;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class StarfurySwordItem extends SwordItem {
    private static final double RANGE = 60.0D;          // how far the crosshair can target
    private static final int COOLDOWN_TICKS = 20;       // right-click cooldown (20 ticks = 1 s)
    private static final double SPAWN_HEIGHT_MIN = 20.0D;
    private static final double SPAWN_HEIGHT_VAR = 14.0D; // varied heights make the stars land one after another
    private static final double SPAWN_SPREAD = 10.0D;   // horizontal spread of spawn points
    private static final double LANDING_SPREAD = 1.0D;  // spread of landing points around crosshair

    public StarfurySwordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    /** Enchanted-looking shimmer on the sword. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            Vec3 target = findTarget(level, player);
            summonStars(level, player, target);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 1.1F);

            if (level instanceof ServerLevel serverLevel) {
                // glowing target zone on the ground where the stars will land
                serverLevel.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 0.1D, target.z,
                        45, 1.3D, 0.05D, 1.3D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, target.x, target.y + 0.5D, target.z,
                        40, 1.2D, 0.6D, 1.2D, 0.8D);
                // sparks swirling around the player's hand
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY(0.7D), player.getZ(),
                        20, 0.5D, 0.6D, 0.5D, 0.3D);
            }
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The point under the player's crosshair: a mob if one is aimed at, otherwise the block/sky point. */
    private static Vec3 findTarget(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double maxDistSq = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eye)
                : RANGE * RANGE;

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
                e -> !e.isSpectator() && e.isPickable(), maxDistSq);

        if (entityHit != null) {
            Entity e = entityHit.getEntity();
            return e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D); // aim at the body center
        }
        return blockHit.getLocation();
    }

    private static void summonStars(Level level, Player player, Vec3 target) {
        RandomSource rnd = level.random;
        int count = 3 + rnd.nextInt(3); // 3-5 stars

        for (int i = 0; i < count; i++) {
            Vec3 aim = target.add(
                    (rnd.nextDouble() - 0.5D) * LANDING_SPREAD,
                    0.0D,
                    (rnd.nextDouble() - 0.5D) * LANDING_SPREAD);

            Vec3 desired = aim.add(
                    (rnd.nextDouble() - 0.5D) * SPAWN_SPREAD,
                    SPAWN_HEIGHT_MIN + rnd.nextDouble() * SPAWN_HEIGHT_VAR,
                    (rnd.nextDouble() - 0.5D) * SPAWN_SPREAD);

            // If a ceiling/wall is in the way (caves, indoors), spawn just in front of it.
            BlockHitResult clip = level.clip(new ClipContext(aim, desired,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 spawn = desired;
            if (clip.getType() != HitResult.Type.MISS) {
                Vec3 back = aim.subtract(clip.getLocation()).normalize().scale(0.4D);
                spawn = clip.getLocation().add(back);
            }

            Vec3 dir = aim.subtract(spawn);
            if (dir.lengthSqr() < 0.25D) {
                dir = new Vec3(0.0D, -1.0D, 0.0D);
            }

            FallenStarEntity star = new FallenStarEntity(level, player);
            star.setPos(spawn.x, spawn.y, spawn.z);
            star.setDeltaMovement(dir.normalize().scale(1.5D + rnd.nextDouble() * 0.5D));
            level.addFreshEntity(star);

            // flash in the sky where each star appears
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLASH, spawn.x, spawn.y, spawn.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.END_ROD, spawn.x, spawn.y, spawn.z, 18, 0.4D, 0.4D, 0.4D, 0.12D);
            }
        }
    }
}
