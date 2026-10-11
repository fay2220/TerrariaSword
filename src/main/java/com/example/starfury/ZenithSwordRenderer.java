package com.example.starfury;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a Zenith blade: the sword sprite (full bright), a coloured glow halo, fading afterimages behind it and an
 * additive light ribbon along its recent path.
 */
public class ZenithSwordRenderer extends EntityRenderer<ZenithSwordEntity> {
    private static final float SCALE = 2.1F;
    private static final int AFTERIMAGES = 5;
    private static final float RIBBON_WIDTH = 0.40F;

    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS);

    private final ItemRenderer itemRenderer;
    private ItemStack[] stacks;

    public ZenithSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    private ItemStack stackFor(int variant) {
        if (this.stacks == null) {
            this.stacks = new ItemStack[] {
                    new ItemStack(ModItems.ZENITH_SWORD.get()),
                    new ItemStack(ModItems.STARFURY.get()),
                    new ItemStack(ModItems.ZENITH_PHANTOM_EMERALD.get()),
                    new ItemStack(ModItems.ZENITH_PHANTOM_EMBER.get()),
                    new ItemStack(ModItems.ZENITH_PHANTOM_ROSE.get()),
                    new ItemStack(ModItems.ZENITH_PHANTOM_AZURE.get()),
            };
        }
        return this.stacks[Mth.clamp(variant, 0, this.stacks.length - 1)];
    }

    @Override
    public void render(ZenithSwordEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        int variant = entity.getVariant();
        float[] color = ZenithSwordEntity.VARIANT_COLORS[variant];
        ItemStack stack = stackFor(variant);

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        Vec3 pos = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        Vec3 motion = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);

        renderRibbon(entity, pos, poseStack, buffers, color);

        BakedModel model = this.itemRenderer.getModel(stack, entity.level(), null, entity.getId());
        VertexConsumer glow = buffers.getBuffer(GLOW);

        // Furious spinning animation around the cutting plane
        float spin = (entity.tickCount + partialTick) * 60.0F + entity.getId() * 45.0F;

        // afterimages: fading, slightly smaller copies trailing behind the high-speed blade
        for (int i = AFTERIMAGES; i >= 1; i--) {
            float fade = 1.0F - i / (float) (AFTERIMAGES + 1);
            poseStack.pushPose();
            Vec3 back = motion.scale(-0.16D * i);
            poseStack.translate(back.x, back.y, back.z);
            orient(poseStack, yaw, pitch);
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin - i * 28.0F));
            float s = SCALE * (1.0F - 0.05F * i);
            poseStack.scale(s, s, s);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            renderQuads(poseStack, glow, model, color[0], color[1], color[2], 0.45F * fade);
            poseStack.popPose();
        }

        // the blade itself
        poseStack.pushPose();
        orient(poseStack, yaw, pitch);
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        poseStack.scale(SCALE, SCALE, SCALE);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
        // glow halo: a bigger, tinted, see-through copy around it
        poseStack.scale(1.14F, 1.14F, 1.6F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderQuads(poseStack, buffers.getBuffer(GLOW), model, color[3], color[4], color[5], 0.35F);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    /**
     * Item sprites point to the top-right. Turn that diagonal into +Z, then point +Z along the flight direction
     * (same yaw/pitch convention vanilla projectiles use).
     */
    private static void orient(PoseStack poseStack, float yaw, float pitch) {
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
    }

    private static void renderQuads(PoseStack poseStack, VertexConsumer consumer, BakedModel model,
                                    float r, float g, float b, float alpha) {
        RandomSource rand = RandomSource.create(42L);
        PoseStack.Pose pose = poseStack.last();
        for (Direction dir : Direction.values()) {
            rand.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, dir, rand)) {
                consumer.putBulkData(pose, quad, r, g, b, alpha, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
            }
        }
        rand.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, null, rand)) {
            consumer.putBulkData(pose, quad, r, g, b, alpha, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
        }
    }

    /** Additive ribbon through the blade's recent positions, facing the camera, fading toward the tail. */
    private void renderRibbon(ZenithSwordEntity entity, Vec3 pos, PoseStack poseStack, MultiBufferSource buffers,
                              float[] c) {
        if (entity.trail.size() < 2) {
            return;
        }
        List<Vec3> points = new ArrayList<>(entity.trail.size() + 1);
        points.add(pos);
        for (Vec3 p : entity.trail) {
            if (points.get(points.size() - 1).distanceToSqr(p) > 1.0E-4D) {
                points.add(p);
            }
        }
        if (points.size() < 2) {
            return;
        }

        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = poseStack.last().pose();
        int n = points.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            float fa = 1.0F - i / (float) (n - 1);
            float fb = 1.0F - (i + 1) / (float) (n - 1);
            Vec3 wa = widthVector(a, b, cam).scale(RIBBON_WIDTH * fa);
            Vec3 wb = widthVector(a, b, cam).scale(RIBBON_WIDTH * fb);
            Vec3 la = a.subtract(pos);
            Vec3 lb = b.subtract(pos);
            float ra = Mth.lerp(fa, c[3], c[0]);
            float ga = Mth.lerp(fa, c[4], c[1]);
            float ba = Mth.lerp(fa, c[5], c[2]);
            float aa = 0.55F * fa;
            float ab = 0.55F * fb;
            // both windings so it is visible from either side
            quad(vc, m, la.add(wa), la.subtract(wa), lb.subtract(wb), lb.add(wb), ra, ga, ba, aa, ab);
            quad(vc, m, lb.add(wb), lb.subtract(wb), la.subtract(wa), la.add(wa), ra, ga, ba, ab, aa);
        }
    }

    private static Vec3 widthVector(Vec3 a, Vec3 b, Vec3 cam) {
        Vec3 seg = b.subtract(a);
        Vec3 view = a.subtract(cam);
        Vec3 w = seg.cross(view);
        double len = w.length();
        return len < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : w.scale(1.0D / len);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                             float r, float g, float b, float a12, float a34) {
        vc.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a12).endVertex();
        vc.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a12).endVertex();
        vc.vertex(m, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a34).endVertex();
        vc.vertex(m, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a34).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ZenithSwordEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
