package io.github.roman16.backcountry;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public record Balance(DropOff dropOff, Mining mining, World world) {
    private static final Balance LOADED = read();

    public record DropOff(int highestLakeY, int inlandBlocks, int lakeCells, int searchBlocks, int shoreRiseBlocks, int stepBlocks) {}

    public record Mining(int openBlocks, double speed) {}

    public record World(int acrossBlocks, double detailShare, int detailWavelengthBlocks, double dryShare, int edgeBlocks, double highest, double innerShare, double landContinentalness, double lift,
            double outerShare, double seaFloor, double sink, double warpShare, int warpWavelengthBlocks) {}

    public static Balance get() {
        return LOADED;
    }

    private static Balance read() {
        try (InputStream stream = Balance.class.getResourceAsStream("/backcountry/balance.toml")) {
            if (stream == null) {
                throw new IllegalStateException("backcountry/balance.toml is missing from the mod jar");
            }
            return load(new TomlParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Balance load(Config toml) {
        Config dropOff = toml.get("drop_off");
        Config mining = toml.get("mining");
        Config world = toml.get("world");
        return new Balance(
                new DropOff(dropOff.getInt("highest_lake_y"), dropOff.getInt("inland_blocks"), dropOff.getInt("lake_cells"), dropOff.getInt("search_blocks"),
                        dropOff.getInt("shore_rise_blocks"), dropOff.getInt("step_blocks")),
                new Mining(mining.getInt("open_blocks"), number(mining, "speed")),
                new World(
                world.getInt("across_blocks"),
                number(world, "detail_share"),
                world.getInt("detail_wavelength_blocks"),
                number(world, "dry_share"),
                world.getInt("edge_blocks"),
                number(world, "highest"),
                number(world, "inner_share"),
                number(world, "land_continentalness"),
                number(world, "lift"),
                number(world, "outer_share"),
                number(world, "sea_floor"),
                number(world, "sink"),
                number(world, "warp_share"),
                world.getInt("warp_wavelength_blocks")));
    }

    private static double number(Config table, String key) {
        return table.<Number>get(key).doubleValue();
    }
}
