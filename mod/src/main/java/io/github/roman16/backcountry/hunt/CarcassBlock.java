package io.github.roman16.backcountry.hunt;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Activity;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.haul.Sled;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CarcassBlock extends BaseEntityBlock {
    public static final MapCodec<CarcassBlock> CODEC = simpleCodec(CarcassBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 3, 15, 6, 13);
    private static final int LEAST_CLICKS = 4;
    private static final int BAR = 10;

    public CarcassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarcassEntity(pos, state);
    }

    public static int clicks(CarcassEntity carcass, Body body) {
        double kg = carcass.data().map(Animal::kg).orElse(1.0);
        return Math.max(LEAST_CLICKS, (int) Math.round(kg * Balance.get().hunting().dressClicksPerKg() * Skill.BUTCHERING.effort(body)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CarcassEntity carcass)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.is(Things.KNIVES)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.backcountry.needs_knife"), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        int done = carcass.dress();
        int needed = clicks(carcass, Metabolism.body(player));
        Metabolism.effortMinutes(player, Activity.BUTCHER, Balance.get().effort().dressMinutes());
        if (player instanceof ServerPlayer server) {
            Skill.BUTCHERING.use(server, 1);
        }
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.6F, 0.8F + level.random.nextFloat() * 0.4F);
        int filled = Math.min(BAR, done * BAR / needed);
        player.displayClientMessage(Component.translatable("message.backcountry.dressing", carcass.animal().getDescription(), "▮".repeat(filled) + "▯".repeat(BAR - filled)), true);
        if (done >= needed) {
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            carcass.data().ifPresent(animal -> animal.cuts().forEach(cut -> popResource(level, pos, cut.stack())));
            carcass.arrows().forEach(arrow -> popResource(level, pos, arrow.copy()));
            level.removeBlock(pos, false);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CarcassEntity carcass)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        Optional<Animal> animal = carcass.data();
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.backcountry.carcass_hint"), true);
        } else if (animal.map(data -> data.kg() > Balance.get().hunting().carryKg()).orElse(false)) {
            boolean loaded = player instanceof ServerPlayer puller && Sled.pulledBy(puller).stream().anyMatch(sled -> sled.load(carcass.item()));
            if (loaded) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            player.displayClientMessage(Component.translatable(loaded ? "message.backcountry.loaded_on_sled" : "message.backcountry.too_heavy"), true);
        } else {
            player.setItemInHand(InteractionHand.MAIN_HAND, carcass.item());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        return InteractionResult.SUCCESS;
    }
}
