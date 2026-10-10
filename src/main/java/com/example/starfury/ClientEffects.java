package com.example.starfury;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only visuals: star impact explosion and camera shake. Never referenced directly on a server. */
@Mod.EventBusSubscriber(modid = StarfuryMod.MODID, value = Dist.CLIENT)
public class ClientEffects {
    private static final double SHAKE_RANGE = 28.0D;
    private static final float MAX_SHAKE = 1.6F;

    private static float shakePower = 0.0F;

    /** Called on the client when a fallen star hits something. */
    public static void impact(Level level, double x, double y, double z) {
        RandomSource rnd = level.random;
        double cy = y + 0.2D;

        // 1) blinding flash
        level.addParticle(ParticleTypes.FLASH, x, cy, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.EXPLOSION, x, cy, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.EXPLOSION, x + 0.6D, cy, z - 0.4D, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.EXPLOSION, x - 0.5D, cy + 0.3D, z + 0.5D, 0.0D, 0.0D, 0.0D);

        // 2) spherical burst of golden sparks
        for (int i = 0; i < 90; i++) {
            double dx = rnd.nextGaussian();
            double dy = rnd.nextGaussian();
            double dz = rnd.nextGaussian();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-4D) {
                continue;
            }
            double speed = 0.2D + rnd.nextDouble() * 0.6D;
            ParticleOptions type = switch (i % 3) {
                case 0 -> ParticleTypes.END_ROD;
                case 1 -> StarParticles.GOLD_DUST;
                default -> ParticleTypes.FIREWORK;
            };
            level.addParticle(type, x, cy, z, dx / len * speed, dy / len * speed + 0.05D, dz / len * speed);
        }

        // 3) expanding shockwave rings along the ground
        ring(level, x, y + 0.15D, z, 48, 0.65D, ParticleTypes.END_ROD);
        ring(level, x, y + 0.15D, z, 36, 0.40D, StarParticles.GOLD_DUST);
        ring(level, x, y + 0.15D, z, 24, 0.22D, ParticleTypes.FIREWORK);

        // 4) pillar of light shooting upward
        for (int i = 0; i < 30; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    x + (rnd.nextDouble() - 0.5D) * 0.5D, y + i * 0.2D, z + (rnd.nextDouble() - 0.5D) * 0.5D,
                    0.0D, 0.06D + rnd.nextDouble() * 0.14D, 0.0D);
        }

        // 5) camera shake, stronger the closer the player is
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double dist = Math.sqrt(player.distanceToSqr(x, y, z));
            float power = (float) Math.max(0.0D, 1.0D - dist / SHAKE_RANGE) * 0.9F;
            shakePower = Math.min(MAX_SHAKE, shakePower + power);
        }
    }

    private static void ring(Level level, double x, double y, double z, int points, double speed, ParticleOptions type) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0D) * i / points;
            level.addParticle(type, x, y, z, Math.cos(a) * speed, 0.02D, Math.sin(a) * speed);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && shakePower > 0.0F) {
            shakePower *= 0.88F;
            if (shakePower < 0.01F) {
                shakePower = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakePower <= 0.0F) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float t = player.tickCount + (float) event.getPartialTick();
        event.setYaw(event.getYaw() + Mth.sin(t * 2.3F) * shakePower * 2.5F);
        event.setPitch(event.getPitch() + Mth.cos(t * 2.9F) * shakePower * 2.0F);
        event.setRoll(event.getRoll() + Mth.sin(t * 3.7F) * shakePower * 3.0F);
    }
}
