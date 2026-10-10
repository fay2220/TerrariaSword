package com.example.starfury;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the star with GeckoLib. Asset paths (derived from "fallen_star"):
 *   assets/starfury/geo/entity/fallen_star.geo.json
 *   assets/starfury/animations/entity/fallen_star.animation.json
 *   assets/starfury/textures/entity/fallen_star.png
 */
public class FallenStarRenderer extends GeoEntityRenderer<FallenStarEntity> {
    public FallenStarRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(new ResourceLocation(StarfuryMod.MODID, "fallen_star")));
        this.shadowRadius = 0.0F;
    }

    /** Stars glow: always rendered at full brightness, even at night or in caves. */
    @Override
    protected int getBlockLightLevel(FallenStarEntity entity, BlockPos pos) {
        return 15;
    }
}
