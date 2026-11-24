package io.github.roman16.backcountry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.neoforged.fml.ModList;

public final class IslandShape implements DensityFunction {
    public static final double LAND = -0.19;
    public static final MapCodec<IslandShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(IslandShape::input),
            NoiseHolder.CODEC.fieldOf("warp").forGetter(IslandShape::warp),
            NoiseHolder.CODEC.fieldOf("detail").forGetter(IslandShape::detail)).apply(instance, IslandShape::new));
    private static final KeyDispatchDataCodec<IslandShape> DISPATCH = KeyDispatchDataCodec.of(CODEC);
    private static final ResourceLocation TECTONIC = ResourceLocation.fromNamespaceAndPath("tectonic", "noise/raw_continents");
    private static final ResourceLocation VANILLA = ResourceLocation.withDefaultNamespace("overworld/continents");
    private static final ResourceKey<NormalNoise.NoiseParameters> WARP = ResourceKey.create(Registries.NOISE, Backcountry.id("island_warp"));
    private static final ResourceKey<NormalNoise.NoiseParameters> DETAIL = ResourceKey.create(Registries.NOISE, Backcountry.id("island_detail"));
    private static final HolderOwner<NormalNoise.NoiseParameters> NOISES = new HolderOwner<>() {};
    private static final int CAP_BLOCKS = 512;
    private static final int MEASURE_BLOCKS = 96;
    private static final int FINGERPRINTS = 8;
    private static final int KEPT = 4;
    private static final int TRIES = 24;
    private static final Map<List<Double>, Sized> MEASURED = new LinkedHashMap<>(KEPT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<List<Double>, Sized> eldest) {
            return size() > KEPT;
        }
    };
    private final DensityFunction input;
    private final NoiseHolder warp;
    private final NoiseHolder detail;
    private final Balance.World layout;
    private volatile Sized sized;

    public IslandShape(DensityFunction input, NoiseHolder warp, NoiseHolder detail) {
        this.input = input;
        this.warp = warp;
        this.detail = detail;
        this.layout = Balance.get().world();
    }

    public record Sized(double scale, double inner, double outer, double warp, double detail) {}

    public DensityFunction input() {
        return input;
    }

    public NoiseHolder warp() {
        return warp;
    }

    public NoiseHolder detail() {
        return detail;
    }

    public static ResourceLocation continents() {
        return ModList.get().isLoaded("tectonic") ? TECTONIC : VANILLA;
    }

    public static DensityFunction around(DensityFunction mappedInput, Visitor visitor) {
        IslandShape shape = new IslandShape(mappedInput,
                visitor.visitNoise(new NoiseHolder(Holder.Reference.createStandAlone(NOISES, WARP))),
                visitor.visitNoise(new NoiseHolder(Holder.Reference.createStandAlone(NOISES, DETAIL))));
        return DensityFunctions.flatCache(shape);
    }

    public Sized sized() {
        Sized known = sized;
        if (known == null) {
            List<Double> fingerprint = new ArrayList<>(FINGERPRINTS * 3);
            for (int index = 0; index < FINGERPRINTS; index++) {
                double angle = 2 * Math.PI * index / FINGERPRINTS;
                double x = Math.cos(angle) * layout.edgeBlocks() / 2;
                double z = Math.sin(angle) * layout.edgeBlocks() / 2;
                fingerprint.add(input.compute(new SinglePointContext((int) x, 0, (int) z)));
                fingerprint.add(warpAt(x, z));
                fingerprint.add(detailAt(x, z));
            }
            synchronized (MEASURED) {
                known = MEASURED.computeIfAbsent(fingerprint, key -> measure());
            }
            sized = known;
        }
        return known;
    }

    private Sized measure() {
        int edge = layout.edgeBlocks();
        List<double[]> points = new ArrayList<>();
        for (int x = -edge; x < edge; x += MEASURE_BLOCKS) {
            for (int z = -edge; z < edge; z += MEASURE_BLOCKS) {
                double cx = x + MEASURE_BLOCKS / 2.0;
                double cz = z + MEASURE_BLOCKS / 2.0;
                points.add(new double[] {input.compute(new SinglePointContext((int) cx, 0, (int) cz)), Math.sqrt(cx * cx + cz * cz), warpAt(cx, cz), detailAt(cx, cz)});
            }
        }
        double radius = layout.acrossBlocks() / 2.0;
        double target = Math.PI * radius * radius / layout.dryShare();
        double low = 0.25;
        double high = 4.0;
        for (int attempt = 0; attempt < TRIES; attempt++) {
            double middle = (low + high) / 2;
            Sized trial = sizedBy(middle);
            int land = 0;
            for (double[] point : points) {
                if (shaped(point[0], point[1], point[2], point[3], trial) > layout.landContinentalness()) {
                    land++;
                }
            }
            if ((double) land * MEASURE_BLOCKS * MEASURE_BLOCKS < target) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return sizedBy((low + high) / 2);
    }

    private Sized sizedBy(double scale) {
        double radius = layout.acrossBlocks() / 2.0 * scale;
        return new Sized(scale, radius * layout.innerShare(), radius * layout.outerShare(), radius * layout.warpShare(), radius * layout.detailShare());
    }

    private double warpAt(double x, double z) {
        double wavelength = layout.warpWavelengthBlocks();
        return warp.getValue(x / wavelength, 0, z / wavelength);
    }

    private double detailAt(double x, double z) {
        double wavelength = layout.detailWavelengthBlocks();
        return detail.getValue(x / wavelength, 0, z / wavelength);
    }

    private double shaped(double continents, double real, double warpValue, double detailValue, Sized size) {
        double distance = real - size.warp() * warpValue - size.detail() * detailValue;
        double bias;
        if (distance <= size.inner()) {
            bias = layout.lift();
        } else if (distance >= size.outer()) {
            bias = -layout.sink();
        } else {
            bias = layout.lift() - (layout.lift() + layout.sink()) * (distance - size.inner()) / (size.outer() - size.inner());
        }
        double cap = Math.min(cap(distance, size.outer()), cap(real, layout.edgeBlocks()));
        return Math.min(cap, Math.max(layout.seaFloor(), continents + bias));
    }

    private double cap(double distance, double end) {
        if (distance >= end) {
            return layout.seaFloor();
        }
        if (distance <= end - CAP_BLOCKS) {
            return layout.highest();
        }
        return layout.seaFloor() + (layout.highest() - layout.seaFloor()) * (end - distance) / CAP_BLOCKS;
    }

    @Override
    public double compute(FunctionContext context) {
        double x = context.blockX();
        double z = context.blockZ();
        return shaped(input.compute(context), Math.sqrt(x * x + z * z), warpAt(x, z), detailAt(x, z), sized());
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new IslandShape(input.mapAll(visitor), visitor.visitNoise(warp), visitor.visitNoise(detail)));
    }

    @Override
    public double minValue() {
        return layout.seaFloor();
    }

    @Override
    public double maxValue() {
        return layout.highest();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DISPATCH;
    }
}
