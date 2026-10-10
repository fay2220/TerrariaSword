package com.example.starfury;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only visuals for the Zenith: sonic boom (vapor cone, shockwave rings, warden-style boom line), impact frame
 * (white/black flash + anime speed lines), FOV punch and screen shake. Also keeps the player at full walking speed
 * while holding right click. Never referenced directly from server code (only through DistExecutor).
 */
@Mod.EventBusSubscriber(modid = StarfuryMod.MODID, value = Dist.CLIENT)
public class ZenithClientEffects {
    /** Set to false to turn off the white/black full-screen flash (speed lines stay). */
    public static final boolean ENABLE_SCREEN_FLASH = false;
    private static final int FLASH_TICKS = 4;
    private static final int SPEED_LINE_TICKS = 10;
    private static final int SPEED_LINES = 42;
    private static final float FOV_PUNCH = 0.10F;
    private static final double BOOM_LINE_LENGTH = 14.0D;

    private static int flashTicks;
    private static int speedLineTicks;
    private static long speedLineSeed;
    private static float fovPunch;

    // ------------------------------------------------------------------------------------------------
    // Effects triggered by ZenithSwordEntity
    // ------------------------------------------------------------------------------------------------

    /** A blade just left the player's hand. Every blade: small vapor puff. Big boom: the full sonic boom. */
    public static void launchBoom(ZenithSwordEntity blade, boolean big, boolean flash) {
        Entity owner = blade.getOwner();
        if (owner == null) {
            return;
        }
        Level level = blade.level();
        RandomSource rnd = level.random;
        Vec3 look = owner.getViewVector(1.0F);
        Vec3 eye = owner.getEyePosition();
        Vec3[] basis = basis(look);

        // small puff for every blade
        // ring(level, eye.add(look.scale(1.3D)), look, basis, 0.35D, 8, ParticleTypes.CLOUD, 0.06D, 0.12D);

        if (!big) {
            return;
        }

        // vapor cone: stacked rings of cloud that widen with distance, like a jet breaking the sound barrier
        for (int i = 0; i < 4; i++) {
            // double dist = 1.6D + i * 0.8D;
            // double radius = 0.6D + i * 0.45D;
            // ring(level, eye.add(look.scale(dist)), look, basis, radius, 22 + i * 6, ParticleTypes.CLOUD,
            //         0.10D + i * 0.03D, 0.30D);
        }
        // fast expanding shockwave rings
        // ring(level, eye.add(look.scale(2.2D)), look, basis, 0.25D, 48, ParticleTypes.END_ROD, 0.55D, 0.05D);
        // ring(level, eye.add(look.scale(3.0D)), look, basis, 0.25D, 36, ParticleTypes.ELECTRIC_SPARK, 0.80D, 0.05D);
        // level.addParticle(ParticleTypes.FLASH, eye.x + look.x * 2.5D, eye.y + look.y * 2.5D, eye.z + look.z * 2.5D,
        //         0.0D, 0.0D, 0.0D);

        // sonic boom line toward where the player faces (stops at the first wall)
        Vec3 end = eye.add(look.scale(BOOM_LINE_LENGTH));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        double maxDist = wall.getType() == HitResult.Type.MISS ? BOOM_LINE_LENGTH : wall.getLocation().distanceTo(eye);
        for (double d = 2.0D; d < maxDist; d += 1.6D) {
            Vec3 p = eye.add(look.scale(d));
            // level.addParticle(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
            if (rnd.nextBoolean()) {
                // level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z,
                //         look.x * 0.4D + rnd.nextGaussian() * 0.05D, look.y * 0.4D + rnd.nextGaussian() * 0.05D,
                //         look.z * 0.4D + rnd.nextGaussian() * 0.05D);
            }
        }

        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) {
            return;
        }
        if (me == owner) {
            // speedLineTicks = SPEED_LINE_TICKS;
            // speedLineSeed = rnd.nextLong();
            // fovPunch = FOV_PUNCH;
            // ClientEffects.addShake(flash ? 0.9F : 0.45F);
            // if (flash && ENABLE_SCREEN_FLASH) {
            //     flashTicks = FLASH_TICKS;
            // }
        } else {
            // double dist = Math.sqrt(me.distanceToSqr(owner));
            // ClientEffects.addShake((float) Math.max(0.0D, 1.0D - dist / 24.0D) * 0.5F);
        }
    }

    /** A blade reached its turn point (sonic crack out in front) or hit a wall (sparks). */
    public static void bladeBoom(ZenithSwordEntity blade, boolean wall) {
        Level level = blade.level();
        RandomSource rnd = level.random;
        Vec3 pos = blade.position();

        if (wall) {
            // for (int i = 0; i < 18; i++) {
            //     level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
            //             rnd.nextGaussian() * 0.3D, rnd.nextGaussian() * 0.3D, rnd.nextGaussian() * 0.3D);
            // }
            // for (int i = 0; i < 10; i++) {
            //     level.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z,
            //             rnd.nextGaussian() * 0.4D, rnd.nextGaussian() * 0.4D, rnd.nextGaussian() * 0.4D);
            // }
            return;
        }

        Entity owner = blade.getOwner();
        Vec3 dir = owner != null ? pos.subtract(owner.getEyePosition()) : new Vec3(0.0D, 0.0D, 1.0D);
        dir = dir.lengthSqr() > 1.0E-6D ? dir.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3[] basis = basis(dir);

        // level.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        // ring(level, pos, dir, basis, 0.3D, 16, ParticleTypes.CLOUD, 0.22D, 0.0D);
        // ring(level, pos, dir, basis, 0.2D, 20, ParticleTypes.END_ROD, 0.35D, 0.0D);
        // for (int i = 0; i < 8; i++) {
        //     level.addParticle(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z,
        //             rnd.nextGaussian() * 0.15D, rnd.nextGaussian() * 0.15D, rnd.nextGaussian() * 0.15D);
        // }
        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                0.35F, 1.6F + rnd.nextFloat() * 0.3F, false);
    }

    /** Two unit vectors perpendicular to {@code dir} (and to each other). */
    private static Vec3[] basis(Vec3 dir) {
        Vec3 ref = Math.abs(dir.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = dir.cross(ref).normalize();
        Vec3 up = right.cross(dir).normalize();
        return new Vec3[] {right, up};
    }

    /** Ring of particles in the plane perpendicular to {@code dir}, flying outward (and a little forward). */
    private static void ring(Level level, Vec3 center, Vec3 dir, Vec3[] basis, double radius, int points,
                             ParticleOptions type, double outSpeed, double forwardSpeed) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0D * i / points;
            Vec3 radial = basis[0].scale(Math.cos(a)).add(basis[1].scale(Math.sin(a)));
            Vec3 p = center.add(radial.scale(radius));
            Vec3 v = radial.scale(outSpeed).add(dir.scale(forwardSpeed));
            level.addParticle(type, p.x, p.y, p.z, v.x, v.y, v.z);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (speedLineTicks > 0) {
            speedLineTicks--;
        }
        fovPunch *= 0.72F;
        if (fovPunch < 0.002F) {
            fovPunch = 0.0F;
        }
    }

    /** Short zoom-in "punch" on each sonic boom. */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (fovPunch > 0.0F && event.usedConfiguredFov()) {
            event.setFOV(event.getFOV() * (1.0D - fovPunch));
        }
    }

    /**
     * Using an item normally slows the player to 20% speed. The Zenith is used while running around (like in
     * Terraria), so undo that slowdown. This event fires right before vanilla multiplies the input by 0.2.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.isUsingItem() && !player.isPassenger()
                && player.getUseItem().getItem() instanceof ZenithSwordItem) {
            Input input = event.getInput();
            input.leftImpulse *= 5.0F;
            input.forwardImpulse *= 5.0F;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Impact frame overlay (registered in ClientSetup)
    // ------------------------------------------------------------------------------------------------

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (speedLineTicks > 0) {
            renderSpeedLines(graphics, partialTick, width, height);
        }
        if (flashTicks > 0) {
            int frame = FLASH_TICKS - flashTicks; // 0,1,2,3
            int color = switch (frame) {
                case 0 -> 0xD8FFFFFF;  // white impact frame
                case 1 -> 0x8C000000;  // black inverse frame
                case 2 -> 0x5AF0E8FF;  // pale violet afterglow
                default -> 0x26FFFFFF;
            };
            graphics.fill(0, 0, width, height, color);
        }
    }

    private static void renderSpeedLines(GuiGraphics graphics, float partialTick, int width, int height) {
        float life = Mth.clamp((speedLineTicks - partialTick) / SPEED_LINE_TICKS, 0.0F, 1.0F);
        int alpha = (int) (life * 150.0F);
        if (alpha <= 2) {
            return;
        }
        float maxR = (float) Math.sqrt(width * width + height * height) * 0.5F;
        // reseeded every tick so the lines flicker like hand-drawn anime speed lines
        RandomSource rnd = RandomSource.create(speedLineSeed + speedLineTicks);

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
        for (int i = 0; i < SPEED_LINES; i++) {
            float angle = 360.0F * i / SPEED_LINES + rnd.nextFloat() * 6.0F;
            int inner = (int) (maxR * (0.42F + rnd.nextFloat() * 0.25F) + (1.0F - life) * maxR * 0.2F);
            int outer = (int) maxR + 4;
            int half = 1 + rnd.nextInt(2);
            int color = (alpha << 24) | (rnd.nextInt(4) == 0 ? 0xD8C8FF : 0xFFFFFF);
            graphics.pose().pushPose();
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            graphics.fill(inner, -half, outer, half, color);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }
}
