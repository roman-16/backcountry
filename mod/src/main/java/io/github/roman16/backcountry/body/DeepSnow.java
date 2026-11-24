package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class DeepSnow {
    private static final ResourceLocation WADING = Backcountry.id("deep_snow");
    private static final int STEP_TICKS = 10;

    private DeepSnow() {}

    public static boolean deep(Level level, BlockPos feet) {
        int deep = Balance.get().snow().deepLayers();
        BlockState at = level.getBlockState(feet);
        BlockState below = level.getBlockState(feet.below());
        return at.getBlock() instanceof SnowLayerBlock && at.getValue(SnowLayerBlock.LAYERS) >= deep || below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW)
                || below.getBlock() instanceof SnowLayerBlock && below.getValue(SnowLayerBlock.LAYERS) == SnowLayerBlock.MAX_HEIGHT;
    }

    @SubscribeEvent
    static void wade(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % STEP_TICKS != 0) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        Balance.Snow snow = Balance.get().snow();
        boolean wading = Metabolism.lives(player) && player.onGround() && deep(player.level(), player.blockPosition())
                && !player.getItemBySlot(EquipmentSlot.FEET).is(Things.SNOWSHOES.get());
        if (speed == null) {
            return;
        }
        if (!wading) {
            speed.removeModifier(WADING);
            Metabolism.body(player).snowFrom = null;
            return;
        }
        speed.addOrUpdateTransientModifier(new AttributeModifier(WADING, -snow.slowShare(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        Body body = Metabolism.body(player);
        if (body.snowFrom != null) {
            body.snowBlocks += Math.sqrt(Math.pow(player.getX() - body.snowFrom.x, 2) + Math.pow(player.getZ() - body.snowFrom.z, 2));
        }
        body.snowFrom = player.position();
        if (body.snowBlocks >= snow.frostbiteBlocks()) {
            body.snowBlocks = 0;
            body.afflict(Condition.FROSTNIP);
        }
    }
}
