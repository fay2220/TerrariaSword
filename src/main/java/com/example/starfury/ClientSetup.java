package com.example.starfury;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StarfuryMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Renders the projectile as a camera-facing, fullbright 2D star sprite (scale 1.6).
        event.registerEntityRenderer(ModEntities.FALLEN_STAR.get(),
                context -> new ThrownItemRenderer<>(context, 1.6F, true));
    }
}
