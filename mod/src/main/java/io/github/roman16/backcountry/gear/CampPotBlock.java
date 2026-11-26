package io.github.roman16.backcountry.gear;

import com.mojang.serialization.MapCodec;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CampPotBlock extends BaseEntityBlock {
    public static final MapCodec<CampPotBlock> CODEC = simpleCodec(CampPotBlock::new);
    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 7, 13);
    private static final double SURFACE = 0.3;
    private static final int WISP_CHANCE = 4;
    private static final int BUBBLES = 2;

    public enum Content implements StringRepresentable {
        EMPTY,
        WATER,
        HEATING,
        BOILING;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public CampPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CONTENT, Content.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + SURFACE;
        double z = pos.getZ() + 0.5;
        switch (state.getValue(CONTENT)) {
            case HEATING -> {
                if (random.nextInt(WISP_CHANCE) == 0) {
                    level.addParticle(ParticleTypes.WHITE_SMOKE, x + random.nextGaussian() * 0.1, y, z + random.nextGaussian() * 0.1, 0, 0.02, 0);
                }
            }
            case BOILING -> {
                for (int bubble = 0; bubble < BUBBLES; bubble++) {
                    level.addParticle(ParticleTypes.BUBBLE_POP, x + random.nextGaussian() * 0.12, y, z + random.nextGaussian() * 0.12, 0, 0.02, 0);
                }
                level.addParticle(ParticleTypes.CLOUD, x + random.nextGaussian() * 0.1, y + 0.1, z + random.nextGaussian() * 0.1, 0, 0.06, 0);
            }
            default -> {}
        }
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
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof CampfireBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CampPotEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Things.CAMP_POT_ENTITY.get(), CampPotEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CampPotEntity pot) {
            ItemStack item = pot.item();
            pot.pickedUp();
            level.removeBlock(pos, false);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Things.SPRUCE_NEEDLES.get())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof CampPotEntity pot) {
                if (pot.steep()) {
                    stack.consume(1, player);
                    player.displayClientMessage(Component.translatable("message.backcountry.needles_in"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.backcountry.needles_need_water"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(stack.getItem() instanceof WaterVesselItem vessel) || stack.getItem() instanceof CampPotItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CampPotEntity pot) {
            Water held = WaterVesselItem.water(stack);
            float room = vessel.capacity() - held.litres();
            float poured = Math.min(room, pot.water().litres());
            if (poured > 0 && !held.takes(pot.water())) {
                player.displayClientMessage(Component.translatable("message.backcountry.not_boiled_yet"), true);
            } else if (poured > 0) {
                stack.set(Things.WATER.get(), held.add(pot.water(), poured, vessel.capacity()));
                if (pot.water().boiled() && stack.is(Things.WATER_BOTTLE.get()) && player instanceof ServerPlayer server) {
                    Guide.did(server, Deed.BOTTLE_FROM_POT);
                }
                pot.hold(pot.water().take(poured));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CampPotEntity pot && !pot.isPickedUp()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pot.item());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
