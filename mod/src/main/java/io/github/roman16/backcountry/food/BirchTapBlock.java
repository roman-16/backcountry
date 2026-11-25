package io.github.roman16.backcountry.food;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.gear.Water;
import io.github.roman16.backcountry.gear.WaterVesselItem;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BirchTapBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BirchTapBlock> CODEC = simpleCodec(BirchTapBlock::new);
    public static final int FULL = 8;
    public static final IntegerProperty SAP = IntegerProperty.create("sap", 0, FULL);
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(6, 6, 10, 10, 10, 16),
            Direction.SOUTH, Block.box(6, 6, 0, 10, 10, 6),
            Direction.WEST, Block.box(10, 6, 6, 16, 10, 10),
            Direction.EAST, Block.box(0, 6, 6, 6, 10, 10));

    public BirchTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SAP, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SAP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(BlockTags.BIRCH_LOGS);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(SAP) < FULL;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Balance.Forage forage = Balance.get().forage();
        if (running(level) && random.nextDouble() < forage.sapChance()) {
            level.setBlock(pos, state.setValue(SAP, state.getValue(SAP) + 1), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean running(Level level) {
        var months = Balance.get().forage().sapMonths();
        return Seasons.within(level, months.get(0).intValue(), months.get(1).intValue());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof WaterVesselItem vessel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        int sap = state.getValue(SAP);
        if (sap == 0) {
            player.displayClientMessage(Component.translatable(running(level) ? "message.backcountry.no_sap_yet" : "message.backcountry.sap_stopped"), true);
            return ItemInteractionResult.CONSUME;
        }
        Water held = WaterVesselItem.water(stack);
        float litres = (float) (sap * Balance.get().forage().sapLPerStep());
        float room = vessel.capacity() - held.litres();
        float poured = Math.min(room, litres);
        if (poured <= 0) {
            return ItemInteractionResult.CONSUME;
        }
        stack.set(Things.WATER.get(), held.add(new Water(Water.Drink.BIRCH_SAP, false, poured), poured, vessel.capacity()));
        int left = (int) Math.round((litres - poured) / Balance.get().forage().sapLPerStep());
        level.setBlock(pos, state.setValue(SAP, left), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1.2F);
        return ItemInteractionResult.CONSUME;
    }
}
