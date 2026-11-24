package io.github.roman16.backcountry;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int INLAND_DIRECTIONS = 16;
    private static final int REGION_CELLS = 256;

    private DropOff() {}

    @SubscribeEvent
    static void spawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator) {
            find(level, generator, level.getChunkSource().randomState()).ifPresent(found -> {
                LOGGER.info("The drop-off is at {} by {} of {} cells", found.shore().toShortString(), found.lake() ? "a lake" : "a bank", found.waterCells());
                event.getSettings().setSpawn(found.shore(), 0);
                event.setCanceled(true);
            });
        }
    }

    public record Found(BlockPos shore, int waterCells, boolean lake) {}

    public static Optional<Found> find(LevelHeightAccessor heights, NoiseBasedChunkGenerator generator, RandomState state) {
        return new Search(heights, generator, state).run();
    }

    private static final class Search {
        private final LevelHeightAccessor heights;
        private final NoiseBasedChunkGenerator generator;
        private final RandomState state;
        private final DensityFunction continents;
        private final Balance.DropOff rules = Balance.get().dropOff();
        private final Map<Long, int[]> columns = new HashMap<>();

        Search(LevelHeightAccessor heights, NoiseBasedChunkGenerator generator, RandomState state) {
            this.heights = heights;
            this.generator = generator;
            this.state = state;
            this.continents = state.router().continents();
        }

        Optional<Found> run() {
            int reach = rules.searchBlocks() / rules.stepBlocks();
            List<int[]> cells = new ArrayList<>();
            for (int i = -reach; i <= reach; i++) {
                for (int j = -reach; j <= reach; j++) {
                    if (i * i + j * j <= reach * reach) {
                        cells.add(new int[] {i, j});
                    }
                }
            }
            cells.sort(Comparator.comparingInt(cell -> cell[0] * cell[0] + cell[1] * cell[1]));
            Optional<Found> bank = Optional.empty();
            for (int[] cell : cells) {
                if (!inland(cell[0], cell[1]) || !water(cell[0], cell[1])) {
                    continue;
                }
                Region region = region(cell[0], cell[1]);
                Optional<BlockPos> shore = shore(region.cells());
                if (!region.sea() && region.cells().size() >= rules.lakeCells() && shore.isPresent()) {
                    return shore.map(at -> new Found(at, region.cells().size(), true));
                }
                if (bank.isEmpty()) {
                    bank = shore.map(at -> new Found(at, region.cells().size(), false));
                }
            }
            return bank;
        }

        private record Region(Set<Long> cells, boolean sea) {}

        private boolean sea(int i, int j) {
            return continents.compute(new DensityFunction.SinglePointContext(i * rules.stepBlocks(), 0, j * rules.stepBlocks())) <= IslandShape.LAND;
        }

        private boolean inland(int i, int j) {
            double x = (double) i * rules.stepBlocks();
            double z = (double) j * rules.stepBlocks();
            for (int ring = 1; ring <= 2; ring++) {
                double distance = rules.inlandBlocks() * ring / 2.0;
                for (int direction = 0; direction < INLAND_DIRECTIONS; direction++) {
                    double angle = 2 * Math.PI * direction / INLAND_DIRECTIONS;
                    int px = (int) Math.round(x + Math.cos(angle) * distance);
                    int pz = (int) Math.round(z + Math.sin(angle) * distance);
                    if (continents.compute(new DensityFunction.SinglePointContext(px, 0, pz)) <= IslandShape.LAND) {
                        return false;
                    }
                }
            }
            return true;
        }

        private int[] column(int i, int j) {
            return columns.computeIfAbsent(key(i, j), key -> {
                int x = i * rules.stepBlocks();
                int z = j * rules.stepBlocks();
                return new int[] {
                    generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, state),
                    generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, state)
                };
            });
        }

        private boolean water(int i, int j) {
            int[] column = column(i, j);
            return column[1] > column[0] && column[1] <= rules.highestLakeY();
        }

        private Region region(int i, int j) {
            Set<Long> region = new HashSet<>();
            boolean sea = false;
            Deque<int[]> open = new ArrayDeque<>();
            open.add(new int[] {i, j});
            region.add(key(i, j));
            while (!open.isEmpty() && region.size() < REGION_CELLS) {
                int[] cell = open.poll();
                for (int[] step : NEIGHBOURS) {
                    int ni = cell[0] + step[0];
                    int nj = cell[1] + step[1];
                    if (!region.contains(key(ni, nj)) && water(ni, nj)) {
                        region.add(key(ni, nj));
                        open.add(new int[] {ni, nj});
                        sea |= sea(ni, nj);
                    }
                }
            }
            return new Region(region, sea);
        }

        private Optional<BlockPos> shore(Set<Long> region) {
            BlockPos best = null;
            long bestDistance = Long.MAX_VALUE;
            for (long member : region) {
                int i = (int) (member >> 32);
                int j = (int) member;
                for (int[] step : NEIGHBOURS) {
                    int ni = i + step[0];
                    int nj = j + step[1];
                    if (region.contains(key(ni, nj)) || water(ni, nj) || column(ni, nj)[1] > column(i, j)[1] + rules.shoreRiseBlocks()) {
                        continue;
                    }
                    long distance = (long) ni * ni + (long) nj * nj;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new BlockPos(ni * rules.stepBlocks(), column(ni, nj)[1], nj * rules.stepBlocks());
                    }
                }
            }
            return Optional.ofNullable(best);
        }

        private static long key(int i, int j) {
            return ((long) i << 32) | (j & 0xffffffffL);
        }
    }
}
