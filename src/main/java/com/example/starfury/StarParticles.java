package com.example.starfury;

import net.minecraft.core.particles.DustColorTransitionOptions;
import org.joml.Vector3f;

/** Shared particle options (common-side safe: no client-only classes here). */
public final class StarParticles {
    private StarParticles() {}

    /** Gold -> pale white dust, used for trails and bursts. */
    public static final DustColorTransitionOptions GOLD_DUST = new DustColorTransitionOptions(
            new Vector3f(1.0F, 0.82F, 0.2F), new Vector3f(1.0F, 1.0F, 0.9F), 1.7F);

    /** Smaller, hotter dust for the comet tail. */
    public static final DustColorTransitionOptions TAIL_DUST = new DustColorTransitionOptions(
            new Vector3f(1.0F, 0.6F, 0.1F), new Vector3f(1.0F, 0.95F, 0.6F), 1.2F);
}
