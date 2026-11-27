package io.github.roman16.backcountry.coast;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.weather.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Shore {
    private static final int TICKS_A_DAY = 24000;
    private static final int CHECK_TICKS = 1000;
    private static final int GOOSE_EGGS_LEAST = 2;
    private static final int GOOSE_EGGS_MOST = 4;

    private Shore() {}

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % CHECK_TICKS != 0) {
            return;
        }
        Balance.Coast coast = Balance.get().coast();
        int day = Calendar.day(level);
        boolean gale = Weather.day(level, day - 1).windKmh() >= coast.galeKmh() || Weather.day(level, day).windKmh() >= coast.galeKmh();
        double share = (double) CHECK_TICKS / TICKS_A_DAY;
        for (ServerPlayer player : level.players()) {
            if (!Metabolism.lives(player)) {
                continue;
            }
            if (level.random.nextDouble() < coast.driftwoodPerDay() * (gale ? coast.galeShare() : 1) * share) {
                place(level, player, Kind.DRIFTWOOD);
            }
            if (level.random.nextDouble() < coast.musselsPerDay() * share) {
                place(level, player, Kind.MUSSELS);
            }
            if (Calendar.date(day).getMonthValue() == coast.nestMonth() && level.random.nextDouble() < coast.nestsPerDay() * share) {
                place(level, player, Kind.NEST);
            }
        }
    }

    private enum Kind {
        DRIFTWOOD,
        MUSSELS,
        NEST
    }

    private static void place(ServerLevel level, ServerPlayer player, Kind kind) {
        Balance.Coast coast = Balance.get().coast();
        BlockState placed = switch (kind) {
            case DRIFTWOOD -> Things.DRIFTWOOD.get().defaultBlockState().setValue(DriftwoodBlock.AXIS, level.random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
            case MUSSELS -> Things.MUSSEL_BED.get().defaultBlockState();
            case NEST -> Things.GOOSE_NEST.get().defaultBlockState().setValue(GooseNestBlock.EGGS,
                    GOOSE_EGGS_LEAST + level.random.nextInt(GOOSE_EGGS_MOST - GOOSE_EGGS_LEAST + 1));
        };
        if (count(level, player, placed) >= coast.most()) {
            return;
        }
        for (int attempt = 0; attempt < coast.tries(); attempt++) {
            int x = player.getBlockX() + level.random.nextInt(2 * coast.reachBlocks() + 1) - coast.reachBlocks();
            int z = player.getBlockZ() + level.random.nextInt(2 * coast.reachBlocks() + 1) - coast.reachBlocks();
            if (!level.isLoaded(new BlockPos(x, 0, z)) || player.distanceToSqr(x, player.getY(), z) < coast.unseenBlocks() * coast.unseenBlocks()) {
                continue;
            }
            BlockPos at = switch (kind) {
                case DRIFTWOOD -> driftwood(level, x, z);
                case MUSSELS -> mussels(level, x, z);
                case NEST -> nest(level, x, z);
            };
            if (at != null) {
                level.setBlockAndUpdate(at, placed);
                return;
            }
        }
    }

    private static int count(ServerLevel level, ServerPlayer player, BlockState placed) {
        int reach = Balance.get().coast().reachBlocks();
        int count = 0;
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int x = player.getBlockX() - reach; x <= player.getBlockX() + reach; x++) {
            for (int z = player.getBlockZ() - reach; z <= player.getBlockZ() + reach; z++) {
                column.set(x, 0, z);
                if (!level.isLoaded(column)) {
                    continue;
                }
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                BlockPos floor = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, column);
                if (level.getBlockState(top).is(placed.getBlock()) || level.getBlockState(top.below()).is(placed.getBlock()) || level.getBlockState(floor).is(placed.getBlock())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static BlockPos driftwood(ServerLevel level, int x, int z) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        BlockState ground = level.getBlockState(top.below());
        boolean beach = level.getBiome(top).is(BiomeTags.IS_BEACH);
        return beach && ground.is(BlockTags.SAND) && level.getBlockState(top).isAir() && nearWater(level, top.below(), 4) ? top : null;
    }

    private static BlockPos mussels(ServerLevel level, int x, int z) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        BlockPos floor = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, new BlockPos(x, 0, z));
        boolean shallow = surface.getY() - floor.getY() <= 2 && surface.getY() > floor.getY();
        boolean salt = level.getBiome(floor).is(Things.SALT_WATER) || level.getBiome(floor).is(BiomeTags.IS_BEACH);
        BlockState rock = level.getBlockState(floor.below());
        boolean stony = rock.is(BlockTags.BASE_STONE_OVERWORLD) || rock.is(Blocks.GRAVEL) || rock.is(Blocks.COBBLESTONE);
        return shallow && salt && stony && level.getFluidState(floor).is(FluidTags.WATER) && level.getBlockState(floor).is(Blocks.WATER) ? floor : null;
    }

    private static BlockPos nest(ServerLevel level, int x, int z) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        BlockState ground = level.getBlockState(top.below());
        boolean lake = !level.getBiome(top).is(Things.SALT_WATER) && nearWater(level, top.below(), 2);
        return lake && (ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK)) && level.getBlockState(top).canBeReplaced() && level.getFluidState(top).isEmpty() ? top : null;
    }

    private static boolean nearWater(ServerLevel level, BlockPos pos, int reach) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int step = 1; step <= reach; step++) {
                if (level.getFluidState(pos.relative(direction, step)).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
