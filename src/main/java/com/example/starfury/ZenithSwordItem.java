package com.example.starfury;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/**
 * Zenith (Terraria).
 * Left click: normal sword hit, 25 damage.
 * Hold right click: channels a flurry of spectral blades and deals guaranteed damage to all enemies within
 * a 3-block wide by 25-block long corridor originating from the player's position, ticking 5 times per second.
 * Flying swords are aggressive cosmetic animations.
 */
public class ZenithSwordItem extends SwordItem {
    /** 4 ticks between damage ticks and blade launches (20 ticks / 4 = 5 hits per second). */
    public static final int LAUNCH_INTERVAL = 4;
    public static final double CORRIDOR_RANGE = 25.0D;
    public static final double CORRIDOR_WIDTH = 3.0D;
    public static final float CORRIDOR_DAMAGE = 25.0F;

    /** Effectively "forever": the ability runs until the player lets go of right click. */
    private static final int USE_DURATION = 72000;

    public ZenithSwordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Called every tick while right click is held (both sides; gameplay only runs on the server). */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int used = this.getUseDuration(stack) - remainingUseTicks;
        if (used % LAUNCH_INTERVAL != 0) {
            return;
        }

        // 1. Guaranteed corridor damage (3 blocks wide x 25 blocks long, 5 times per second)
        performCorridorDamage(serverLevel, user);

        // 2. Launch dual rapid spectral blades (pure visuals, extremely fast and aggressive)
        int pulseIndex = used / LAUNCH_INTERVAL;
        ZenithSwordEntity.launch(serverLevel, user, pulseIndex * 2, 1.0D);
        ZenithSwordEntity.launch(serverLevel, user, pulseIndex * 2 + 1, -1.0D);

        // 3. Fierce weapon sound effects
        float pitch = 1.35F + serverLevel.random.nextFloat() * 0.45F;
        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, pitch);
        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6F, pitch + 0.25F);
        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.5F, pitch + 0.1F);

        user.swing(user.getUsedItemHand(), true);
    }

    /**
     * Deals damage to every enemy inside the 3-block wide by 25-block long corridor in front of the player.
     * Hits are guaranteed without requiring blade contact.
     */
    private static void performCorridorDamage(ServerLevel level, LivingEntity user) {
        Vec3 eyePos = user.getEyePosition();
        Vec3 look = user.getViewVector(1.0F).normalize();
        double halfWidth = CORRIDOR_WIDTH / 2.0D; // 1.5 blocks

        AABB queryBox = user.getBoundingBox().inflate(CORRIDOR_RANGE + 2.0D);
        List<Entity> potentialTargets = level.getEntities(user, queryBox, e -> canHarm(user, e));

        double hx = look.x;
        double hz = look.z;
        double hLen = Math.sqrt(hx * hx + hz * hz);
        boolean hasHorizontal = hLen > 1.0E-4D;
        if (hasHorizontal) {
            hx /= hLen;
            hz /= hLen;
        }

        for (Entity target : potentialTargets) {
            if (!isInDamageCorridor(user, eyePos, look, hx, hz, hasHorizontal, CORRIDOR_RANGE, halfWidth, target)) {
                continue;
            }

            // Target is inside the corridor -> guaranteed hit
            target.invulnerableTime = 0;
            if (target instanceof PartEntity<?> part) {
                part.getParent().invulnerableTime = 0;
            }

            DamageSource source;
            if (user instanceof Player player) {
                source = level.damageSources().playerAttack(player);
            } else {
                source = level.damageSources().mobAttack(user);
            }

            if (target.hurt(source, CORRIDOR_DAMAGE)) {
                if (target instanceof LivingEntity living) {
                    living.knockback(0.18D, -look.x, -look.z);
                }
                double ty = target.getY(0.5D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), ty, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), ty, target.getZ(), 8, 0.25D, 0.35D, 0.25D, 0.4D);
                level.playSound(null, target.getX(), ty, target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.55F, 1.3F + level.random.nextFloat() * 0.4F);
            }
        }
    }

    private static boolean isInDamageCorridor(LivingEntity user, Vec3 eyePos, Vec3 look,
                                             double hx, double hz, boolean hasHorizontal,
                                             double range, double halfWidth, Entity target) {
        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();
        double radius = Math.max(target.getBbWidth(), 0.6D) * 0.5D;

        // 1. Horizontal corridor check originating from where the player is standing
        if (hasHorizontal) {
            double dx = target.getX() - user.getX();
            double dz = target.getZ() - user.getZ();
            double forward = dx * hx + dz * hz;
            double lateral = Math.abs(-dx * hz + dz * hx);
            double dy = target.getY() - user.getY();

            if (forward >= -0.5D && forward <= range + 0.5D
                    && lateral <= (halfWidth + radius)
                    && dy >= -3.5D && dy <= 4.5D) {
                return true;
            }
        }

        // 2. 3D ray corridor check (for looking upwards/downwards at airborne or elevated targets)
        Vec3 toTarget = center.subtract(eyePos);
        double proj = toTarget.dot(look);
        if (proj >= -0.5D && proj <= range + 0.5D) {
            double clampedProj = Mth.clamp(proj, 0.0D, range);
            Vec3 closestOnRay = eyePos.add(look.scale(clampedProj));
            double distSq = center.distanceToSqr(closestOnRay);
            double maxDist = halfWidth + Math.max(radius, target.getBbHeight() * 0.5D);
            if (distSq <= maxDist * maxDist) {
                return true;
            }
        }

        return false;
    }

    private static boolean canHarm(LivingEntity owner, Entity e) {
        if (e == owner || !e.isAlive() || e.isSpectator()) {
            return false;
        }
        if (!(e instanceof LivingEntity) && !(e instanceof PartEntity<?>)) {
            return false;
        }
        if (owner.isAlliedTo(e)) {
            return false;
        }
        return !(e instanceof TamableAnimal pet && pet.isOwnedBy(owner));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Clean up any old leftover Night Vision potion effects so the player gets real torch lighting
        if (!level.isClientSide && entity instanceof Player player) {
            if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                MobEffectInstance eff = player.getEffect(MobEffects.NIGHT_VISION);
                if (eff != null && eff.getDuration() <= 220 && !eff.isVisible()) {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                }
            }
        }
    }
}

