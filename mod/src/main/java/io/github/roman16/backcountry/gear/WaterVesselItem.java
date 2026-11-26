package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WaterVesselItem extends Item {
    private static final double POUR_REACH = 0.75;
    private static final int POUR_PARTICLES = 16;
    private static final double POUR_SPREAD = 0.25;
    private static final int DRINK_TICKS = 32;
    private final double capacity;

    public WaterVesselItem(Properties properties, double capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public static Water water(ItemStack stack) {
        return stack.getOrDefault(Things.WATER.get(), Water.EMPTY);
    }

    public float capacity() {
        return (float) capacity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && !water(stack).empty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.backcountry.poured_out", litres(water(stack))), true);
                stack.set(Things.WATER.get(), Water.EMPTY);
                if (level instanceof ServerLevel server) {
                    Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(POUR_REACH));
                    server.sendParticles(ParticleTypes.SPLASH, front.x, front.y, front.z, POUR_PARTICLES, POUR_SPREAD, POUR_SPREAD, POUR_SPREAD, 0);
                }
            }
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.2F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos at = hit.getBlockPos();
            boolean aimedAtWater = level.getFluidState(at).is(FluidTags.WATER);
            boolean salt = aimedAtWater && level.getBiome(at).is(Things.SALT_WATER);
            if (salt && water(stack).empty()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.backcountry.salt_water"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
            Water drawn = Water.drawn(Water.Source.at(level, at), capacity());
            if (aimedAtWater && !salt && water(stack).litres() < capacity && water(stack).takes(drawn)) {
                stack.set(Things.WATER.get(), water(stack).add(drawn, capacity(), capacity()));
                if (player instanceof ServerPlayer server && stack.is(Things.CAMP_POT.get())) {
                    Guide.did(server, Deed.FILLED_POT);
                }
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        if (water(stack).empty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!Metabolism.thirsty(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.backcountry.not_thirsty"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            Water held = water(stack);
            float sip = (float) Math.min(held.litres(), Balance.get().water().sipL());
            Metabolism.drink(player, sip, held);
            stack.set(Things.WATER.get(), held.take(sip));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(describe(water(stack)).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent describe(Water held) {
        if (held.empty()) {
            return Component.translatable("tooltip.backcountry.empty");
        }
        String kind = switch (held.drink()) {
            case SPRUCE_TEA -> "tooltip.backcountry.spruce_tea";
            case BIRCH_SAP -> held.boiled() ? "tooltip.backcountry.boiled_sap" : "tooltip.backcountry.birch_sap";
            case WATER -> held.boiled() ? "tooltip.backcountry.boiled_water"
                    : held.source() == Water.Source.STREAM ? "tooltip.backcountry.stream_water" : "tooltip.backcountry.lake_water";
        };
        return Component.translatable(kind, litres(held));
    }

    public static String litres(Water held) {
        return String.format(Locale.ROOT, "%.2f", held.litres());
    }
}
