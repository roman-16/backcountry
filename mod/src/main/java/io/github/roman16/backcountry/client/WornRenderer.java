package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class WornRenderer implements ICurioRenderer {
    private static final float BACK_SCALE = 0.625F;
    private static final float CHEST_SCALE = 0.375F;
    private static final float HAND_SCALE = 0.3125F;
    private final Place place;

    public enum Place {
        BACK,
        CHEST,
        HANDS
    }

    public WornRenderer(Place place) {
        this.place = place;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose, RenderLayerParent<T, M> parent,
            MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        LivingEntity entity = slot.entity();
        switch (place) {
            case BACK -> draw(stack, entity, pose, buffers, light, humanoid.body, 0, 0.375F, 0.25F, BACK_SCALE);
            case CHEST -> draw(stack, entity, pose, buffers, light, humanoid.body, 0, 0.3125F, -0.1875F, CHEST_SCALE);
            case HANDS -> {
                draw(stack, entity, pose, buffers, light, humanoid.rightArm, -0.0625F, 0.625F, 0, HAND_SCALE);
                draw(stack, entity, pose, buffers, light, humanoid.leftArm, 0.0625F, 0.625F, 0, HAND_SCALE);
            }
        }
    }

    private static void draw(ItemStack stack, LivingEntity entity, PoseStack pose, MultiBufferSource buffers, int light, ModelPart part, float x, float y, float z, float scale) {
        pose.pushPose();
        ICurioRenderer.translateIfSneaking(pose, entity);
        ICurioRenderer.rotateIfSneaking(pose, entity);
        part.translateAndRotate(pose);
        pose.translate(x, y, z);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }
}
