package com.example.starfury;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StarfuryMod.MODID);

    public static final RegistryObject<EntityType<FallenStarEntity>> FALLEN_STAR =
            ENTITY_TYPES.register("fallen_star", () ->
                    EntityType.Builder.<FallenStarEntity>of(FallenStarEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(StarfuryMod.MODID, "fallen_star").toString()));

    /** One flying blade of the Zenith. Lives about 1 second, never saved to disk. */
    public static final RegistryObject<EntityType<ZenithSwordEntity>> ZENITH_BLADE =
            ENTITY_TYPES.register("zenith_blade", () ->
                    EntityType.Builder.<ZenithSwordEntity>of(ZenithSwordEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .noSave()
                            .fireImmune()
                            .build(new ResourceLocation(StarfuryMod.MODID, "zenith_blade").toString()));
}
