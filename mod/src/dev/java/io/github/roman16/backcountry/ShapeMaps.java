package io.github.roman16.backcountry;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class ShapeMaps {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEA_LEVEL = 63;
    private static final int STEP_BLOCKS = 24;
    private static final int DEEPEST_BLOCKS = 96;

    private ShapeMaps() {}

    @SubscribeEvent
    static void started(ServerStartedEvent event) {
        String seeds = System.getProperty(Backcountry.MODID + ".shape", "");
        if (seeds.isBlank()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Path folder = FMLPaths.GAMEDIR.get().resolve("../shape").normalize();
        try {
            Files.createDirectories(folder);
            for (String seed : seeds.split("[ ,]+")) {
                draw(server.overworld(), Long.parseLong(seed), folder);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            server.halt(false);
        }
    }

    private static void draw(ServerLevel level, long seed, Path folder) throws IOException {
        long started = System.nanoTime();
        NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        RandomState state = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
        int edge = Balance.get().world().edgeBlocks();
        int size = 2 * edge / STEP_BLOCKS;
        int[] floor = new int[size * size];
        int[] surface = new int[size * size];
        float[] warmth = new float[size * size];
        Map<String, AtomicLong> biomes = new ConcurrentHashMap<>();
        IntStream.range(0, size).parallel().forEach(row -> {
            for (int column = 0; column < size; column++) {
                int x = -edge + column * STEP_BLOCKS + STEP_BLOCKS / 2;
                int z = -edge + row * STEP_BLOCKS + STEP_BLOCKS / 2;
                int index = row * size + column;
                floor[index] = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state);
                surface[index] = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, state);
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(floor[index], SEA_LEVEL)),
                        QuartPos.fromBlock(z), state.sampler());
                warmth[index] = biome.value().getBaseTemperature();
                if (floor[index] >= SEA_LEVEL) {
                    biomes.computeIfAbsent(biome.unwrapKey().map(key -> key.location().toString()).orElse("?"), key -> new AtomicLong()).incrementAndGet();
                }
            }
        });
        BufferedImage image = picture(floor, surface, warmth, size);
        Optional<DropOff.Found> dropOff = DropOff.find(level, generator, state);
        dropOff.ifPresent(found -> mark(image, (found.shore().getX() + edge) / STEP_BLOCKS, (found.shore().getZ() + edge) / STEP_BLOCKS));
        ImageIO.write(image, "png", folder.resolve(seed + ".png").toFile());
        long land = Arrays.stream(floor).filter(height -> height >= SEA_LEVEL).count();
        double area = land * (double) STEP_BLOCKS * STEP_BLOCKS;
        double farthest = 0;
        for (int index = 0; index < floor.length; index++) {
            if (floor[index] >= SEA_LEVEL) {
                double x = -edge + (index % size) * STEP_BLOCKS + STEP_BLOCKS / 2.0;
                double z = -edge + (index / size) * STEP_BLOCKS + STEP_BLOCKS / 2.0;
                farthest = Math.max(farthest, Math.sqrt(x * x + z * z));
            }
        }
        long total = biomes.values().stream().mapToLong(AtomicLong::get).sum();
        String common = biomes.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get())).limit(8)
                .map(entry -> String.format("%s %.0f%%", entry.getKey(), 100.0 * entry.getValue().get() / total)).collect(Collectors.joining(", "));
        LOGGER.info("shape {}: scale {}, land {} km² (a circle {} blocks across), farthest land {} blocks out, drop-off {}, {} s; land biomes: {}", seed,
                String.format("%.3f", scale(state)), String.format("%.1f", area / 1e6), Math.round(2 * Math.sqrt(area / Math.PI)), Math.round(farthest),
                dropOff.map(found -> found.shore().getX() + " " + found.shore().getY() + " " + found.shore().getZ() + " facing " + found.seaward().getName() + ", "
                        + Math.round(Math.hypot(found.shore().getX(), found.shore().getZ())) + " blocks out").orElse("none"),
                (System.nanoTime() - started) / 1_000_000_000, common);
    }

    private static double scale(RandomState state) {
        List<IslandShape> found = new ArrayList<>();
        state.router().continents().mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (function instanceof IslandShape shape) {
                    found.add(shape);
                }
                if (function instanceof DensityFunctions.HolderHolder holder && holder.function().value() instanceof IslandShape shape) {
                    found.add(shape);
                }
                return function;
            }
        });
        return found.isEmpty() ? Double.NaN : found.getFirst().sized().scale();
    }

    private static BufferedImage picture(int[] floor, int[] surface, float[] warmth, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int index = row * size + column;
                int ground = floor[index];
                int rgb;
                if (ground < SEA_LEVEL) {
                    double depth = Math.min(1, (SEA_LEVEL - ground) / (double) DEEPEST_BLOCKS);
                    rgb = mix(0x3e70a8, 0x0a1634, Math.sqrt(depth));
                } else if (surface[index] > ground) {
                    rgb = 0x4f8fc8;
                } else {
                    int west = floor[row * size + Math.max(0, column - 1)];
                    int north = floor[Math.max(0, row - 1) * size + column];
                    double shade = Math.max(0.55, Math.min(1.35, 1 + ((west - ground) + (north - ground)) * 0.04));
                    rgb = shaded(land(ground - SEA_LEVEL, warmth[index]), shade);
                }
                image.setRGB(column, row, rgb);
            }
        }
        return image;
    }

    private static void mark(BufferedImage image, int column, int row) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = column + dx;
                int y = row + dz;
                if (dx * dx + dz * dz <= 9 && x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                    image.setRGB(x, y, dx * dx + dz * dz >= 6 ? 0xffffff : 0xe03020);
                }
            }
        }
    }

    private static int land(int height, float warmth) {
        int rgb;
        if (height < 3) {
            rgb = 0xc4ba8c;
        } else if (height < 64) {
            rgb = mix(0x5c8046, 0x3f5f36, height / 64.0);
        } else if (height < 128) {
            rgb = mix(0x3f5f36, 0x7a6e5a, (height - 64) / 64.0);
        } else if (height < 192) {
            rgb = mix(0x7a6e5a, 0x9a9a9a, (height - 128) / 64.0);
        } else {
            rgb = 0xeeeef2;
        }
        return warmth < 0.15f ? mix(rgb, 0xe8ecf0, 0.45) : rgb;
    }

    private static int mix(int from, int to, double share) {
        double t = Math.max(0, Math.min(1, share));
        int red = (int) Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int green = (int) Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int blue = (int) Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return red << 16 | green << 8 | blue;
    }

    private static int shaded(int rgb, double shade) {
        int red = (int) Math.min(255, ((rgb >> 16) & 255) * shade);
        int green = (int) Math.min(255, ((rgb >> 8) & 255) * shade);
        int blue = (int) Math.min(255, (rgb & 255) * shade);
        return red << 16 | green << 8 | blue;
    }
}
