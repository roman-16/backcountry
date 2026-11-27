package io.github.roman16.backcountry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.roman16.backcountry.food.DryingRackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DryingRackRenderer implements BlockEntityRenderer<DryingRackEntity> {
    private static final float SCALE = 0.375F;
    private static final float HANG_Y = 0.62F;
    private static final float SPACING = 0.22F;

    public DryingRackRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DryingRackEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < DryingRackEntity.SLOTS; slot++) {
            ItemStack piece = rack.items().get(slot);
            if (piece.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5F - 1.5F * SPACING + slot * SPACING, HANG_Y, 0.5F);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.scale(SCALE, SCALE, SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(piece, ItemDisplayContext.FIXED, light, overlay, pose, buffers, rack.getLevel(), slot);
            pose.popPose();
        }
    }
}
