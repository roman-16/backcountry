package io.github.roman16.backcountry;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class DropOff {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DropOff() {}

    @SubscribeEvent
    static void spawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator) {
            find(level, generator, level.getChunkSource().randomState()).ifPresent(found -> {
                LOGGER.info("The drop-off is at {} facing {}", found.shore().toShortString(), found.seaward().getName());
                event.getSettings().setSpawn(found.shore(), found.seaward().toYRot());
                event.setCanceled(true);
            });
        }
    }

    public record Found(BlockPos shore, Direction seaward) {}

    public static Optional<Found> find(LevelHeightAccessor heights, NoiseBasedChunkGenerator generator, RandomState state) {
        return new Search(heights, generator, state).run();
    }

    private record Shore(int i, int j, Direction seaward) {}

    private static final class Search {
        private final LevelHeightAccessor heights;
        private final NoiseBasedChunkGenerator generator;
        private final RandomState state;
        private final Balance.DropOff rules = Balance.get().dropOff();
        private final int reach;
        private final int size;
        private final boolean[] sea;

        Search(LevelHeightAccessor heights, NoiseBasedChunkGenerator generator, RandomState state) {
            this.heights = heights;
            this.generator = generator;
            this.state = state;
            this.reach = Balance.get().world().edgeBlocks() / rules.stepBlocks();
            this.size = 2 * reach + 1;
            this.sea = new boolean[size * size];
            DensityFunction continents = state.router().continents();
            for (int i = -reach; i <= reach; i++) {
                for (int j = -reach; j <= reach; j++) {
                    sea[index(i, j)] = i * i + j * j > reach * reach
                            || continents.compute(new DensityFunction.SinglePointContext(i * rules.stepBlocks(), 0, j * rules.stepBlocks())) <= IslandShape.LAND;
                }
            }
        }

        Optional<Found> run() {
            Optional<Integer> middle = landNearestTheCentre();
            if (middle.isEmpty()) {
                return Optional.empty();
            }
            boolean[] ocean = flood(index(-reach, -reach), true);
            boolean[] island = flood(middle.get(), false);
            List<Shore> shores = new ArrayList<>();
            for (int i = -reach; i <= reach; i++) {
                for (int j = -reach; j <= reach; j++) {
                    if (!island[index(i, j)]) {
                        continue;
                    }
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        int ni = i + side.getStepX();
                        int nj = j + side.getStepZ();
                        if (inside(ni, nj) && ocean[index(ni, nj)]) {
                            shores.add(new Shore(i, j, side));
                        }
                    }
                }
            }
            if (shores.isEmpty()) {
                return Optional.empty();
            }
            RandomSource random = state.getOrCreateRandomFactory(Backcountry.id("drop_off")).at(0, 0, 0);
            for (int last = shores.size() - 1; last > 0; last--) {
                int pick = random.nextInt(last + 1);
                Shore held = shores.get(last);
                shores.set(last, shores.get(pick));
                shores.set(pick, held);
            }
            for (Shore shore : shores) {
                Optional<Found> found = waterline(shore);
                if (found.isPresent()) {
                    return found;
                }
            }
            Shore first = shores.getFirst();
            int x = first.i() * rules.stepBlocks();
            int z = first.j() * rules.stepBlocks();
            return Optional.of(new Found(new BlockPos(x, column(x, z)[1], z), first.seaward()));
        }

        private Optional<Integer> landNearestTheCentre() {
            Optional<Integer> nearest = Optional.empty();
            long nearestDistance = Long.MAX_VALUE;
            for (int i = -reach; i <= reach; i++) {
                for (int j = -reach; j <= reach; j++) {
                    long distance = (long) i * i + (long) j * j;
                    if (!sea[index(i, j)] && distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = Optional.of(index(i, j));
                    }
                }
            }
            return nearest;
        }

        private boolean[] flood(int start, boolean wanted) {
            boolean[] reached = new boolean[size * size];
            int[] open = new int[size * size];
            int head = 0;
            int tail = 0;
            reached[start] = true;
            open[tail++] = start;
            while (head < tail) {
                int cell = open[head++];
                int i = cell / size - reach;
                int j = cell % size - reach;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    int ni = i + side.getStepX();
                    int nj = j + side.getStepZ();
                    if (inside(ni, nj) && !reached[index(ni, nj)] && sea[index(ni, nj)] == wanted) {
                        reached[index(ni, nj)] = true;
                        open[tail++] = index(ni, nj);
                    }
                }
            }
            return reached;
        }

        private Optional<Found> waterline(Shore shore) {
            int x = shore.i() * rules.stepBlocks();
            int z = shore.j() * rules.stepBlocks();
            int[] dry = column(x, z);
            if (wet(dry)) {
                return Optional.empty();
            }
            for (int step = 1; step <= rules.stepBlocks(); step++) {
                int[] ahead = column(x + shore.seaward().getStepX() * step, z + shore.seaward().getStepZ() * step);
                if (wet(ahead)) {
                    if (ahead[1] != generator.getSeaLevel() || dry[1] > ahead[1] + rules.shoreRiseBlocks()) {
                        return Optional.empty();
                    }
                    int back = step - 1;
                    return Optional.of(new Found(new BlockPos(x + shore.seaward().getStepX() * back, dry[1], z + shore.seaward().getStepZ() * back), shore.seaward()));
                }
                dry = ahead;
            }
            return Optional.empty();
        }

        private int[] column(int x, int z) {
            return new int[] {
                generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, state),
                generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, state)
            };
        }

        private static boolean wet(int[] column) {
            return column[1] > column[0];
        }

        private boolean inside(int i, int j) {
            return Math.abs(i) <= reach && Math.abs(j) <= reach;
        }

        private int index(int i, int j) {
            return (i + reach) * size + j + reach;
        }
    }
}
