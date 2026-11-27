package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.fire.ChimneyBlock;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.fire.FireBlock;
import io.github.roman16.backcountry.gear.TarpBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class Rooms {
    private static final int SMOKE_REACH = 8;
    private static final double TICKS_AN_HOUR = 1000;
    private static final Map<Level, Map<BlockPos, Seen>> SEEN = new WeakHashMap<>();
    private static final Map<Level, Map<BlockPos, Warmth>> AIR = new WeakHashMap<>();
    private static final Room OPEN = new Room(Kind.OPEN, 0, 0, 0, 0, List.of(), false, BlockPos.ZERO, Set.of());

    private Rooms() {}

    public enum Kind {
        OPEN,
        CABIN,
        SNOW_SHELTER,
        CELLAR;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Room(Kind kind, int volume, double insulation, double earthShare, double snowShare, List<BlockPos> fires, boolean smoky, BlockPos anchor,
            Set<BlockPos> cells) {
        public boolean sealed() {
            return kind != Kind.OPEN;
        }
    }

    private record Seen(Room room, long tick) {}

    private record Warmth(double airC, long tick) {}

    public static Room at(Level level, BlockPos pos) {
        long now = level.getGameTime();
        Map<BlockPos, Seen> seen = SEEN.computeIfAbsent(level, key -> new HashMap<>());
        Seen known = seen.get(pos);
        if (known != null && now - known.tick() < Balance.get().rooms().recheckTicks()) {
            return known.room();
        }
        Room room = measure(level, pos);
        if (seen.size() > Balance.get().rooms().remembered()) {
            seen.clear();
        }
        seen.put(pos.immutable(), new Seen(room, now));
        return room;
    }

    private static BlockPos start(Level level, BlockPos pos) {
        if (!wall(level, pos, level.getBlockState(pos))) {
            return pos;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (!wall(level, next, level.getBlockState(next))) {
                return next;
            }
        }
        return null;
    }

    private static Room measure(Level level, BlockPos pos) {
        Balance.Rooms rules = Balance.get().rooms();
        BlockPos first = start(level, pos);
        if (first == null) {
            return OPEN;
        }
        Set<BlockPos> cells = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(first.immutable());
        cells.add(first.immutable());
        int minX = first.getX();
        int maxX = minX;
        int minY = first.getY();
        int maxY = minY;
        int minZ = first.getZ();
        int maxZ = minZ;
        List<BlockPos> fires = new ArrayList<>();
        Map<String, Integer> faces = new HashMap<>();
        int faceCount = 0;
        BlockPos anchor = first.immutable();
        while (!open.isEmpty()) {
            BlockPos cell = open.poll();
            boolean fire = level.getBlockState(cell).getBlock() instanceof FireBlock;
            if (level.canSeeSky(cell) && !smokeColumn(level, cell) && !(fire && vented(level, cell))) {
                return OPEN;
            }
            if (fire) {
                fires.add(cell);
            }
            if (cell.getY() < anchor.getY() || cell.getY() == anchor.getY() && (cell.getX() < anchor.getX() || cell.getX() == anchor.getX() && cell.getZ() < anchor.getZ())) {
                anchor = cell;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = cell.relative(direction).immutable();
                if (cells.contains(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (wall(level, next, state)) {
                    faces.merge(material(state), 1, Integer::sum);
                    faceCount++;
                    continue;
                }
                if (smokeColumn(level, next)) {
                    continue;
                }
                minX = Math.min(minX, next.getX());
                maxX = Math.max(maxX, next.getX());
                minY = Math.min(minY, next.getY());
                maxY = Math.max(maxY, next.getY());
                minZ = Math.min(minZ, next.getZ());
                maxZ = Math.max(maxZ, next.getZ());
                if (maxX - minX >= rules.acrossBlocks() || maxZ - minZ >= rules.acrossBlocks() || maxY - minY >= rules.highBlocks()) {
                    return OPEN;
                }
                cells.add(next);
                open.add(next);
            }
        }
        double insulation = 0;
        for (Map.Entry<String, Integer> face : faces.entrySet()) {
            insulation += rules.insulation().getOrDefault(face.getKey(), rules.insulation().get("other")) * face.getValue();
        }
        insulation /= Math.max(1, faceCount);
        double earth = faces.getOrDefault("ground", 0) / (double) Math.max(1, faceCount);
        double snow = faces.getOrDefault("snow", 0) / (double) Math.max(1, faceCount);
        boolean smoky = fires.stream().anyMatch(fire -> lit(level, fire) && !vented(level, fire));
        Kind kind = earth >= rules.cellarShare() ? Kind.CELLAR : snow >= rules.snowShare() ? Kind.SNOW_SHELTER : Kind.CABIN;
        return new Room(kind, cells.size(), insulation, earth, snow, List.copyOf(fires), smoky, anchor, Set.copyOf(cells));
    }

    private static boolean wall(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FireBlock) {
            return false;
        }
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof FenceGateBlock) {
            return !state.getValue(BlockStateProperties.OPEN);
        }
        return state.getBlock() instanceof SlabBlock || state.getBlock() instanceof StairBlock || state.getBlock() instanceof IronBarsBlock
                || state.getBlock() instanceof TarpBlock || state.getBlock() instanceof ChimneyBlock || state.is(Blocks.SNOW) && state.getValue(BlockStateProperties.LAYERS) >= 8
                || state.isCollisionShapeFullBlock(level, pos);
    }

    private static String material(BlockState state) {
        if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.ICE)) {
            return "snow";
        }
        if (state.is(BlockTags.LOGS)) {
            return "log";
        }
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)) {
            return "planks";
        }
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return "ground";
        }
        if (state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)) {
            return "glass";
        }
        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Things.STONE_CHIMNEY.get()) || state.is(BlockTags.STONE_BRICKS)) {
            return "stone";
        }
        return "other";
    }

    private static boolean smokeColumn(Level level, BlockPos cell) {
        BlockPos below = cell.below();
        for (int step = 0; step < SMOKE_REACH; step++) {
            BlockState state = level.getBlockState(below);
            if (state.getBlock() instanceof FireBlock) {
                return vented(level, below);
            }
            if (wall(level, below, state)) {
                return false;
            }
            below = below.below();
        }
        return false;
    }

    private static boolean vented(Level level, BlockPos fire) {
        BlockPos up = fire.above();
        for (int step = 0; step < Balance.get().rooms().chimneyBlocks(); step++) {
            BlockState state = level.getBlockState(up);
            if (state.is(Things.STONE_CHIMNEY.get())) {
                up = up.above();
                continue;
            }
            if (wall(level, up, state)) {
                return false;
            }
            if (level.canSeeSky(up)) {
                return true;
            }
            up = up.above();
        }
        return false;
    }

    private static boolean lit(Level level, BlockPos fire) {
        return level.getBlockState(fire).hasProperty(BlockStateProperties.LIT) && level.getBlockState(fire).getValue(BlockStateProperties.LIT);
    }

    private static double heatC(Level level, Room room) {
        double heat = 0;
        for (BlockPos fire : room.fires()) {
            if (level.getBlockEntity(fire) instanceof CampfireBlockEntity campfire) {
                heat += Fire.of(campfire).heatC();
            }
        }
        return heat;
    }

    private static int occupants(Level level, Room room) {
        int inside = 0;
        for (Player player : level.players()) {
            if (!player.isSpectator() && room.cells().contains(player.blockPosition())) {
                inside++;
            }
        }
        return inside;
    }

    public static double targetC(Level level, Room room, double outsideC) {
        Balance.Rooms rules = Balance.get().rooms();
        double base = outsideC + (rules.groundC() - outsideC) * room.earthShare();
        double fire = heatC(level, room) * rules.fireGain() * Math.min(1, rules.fireVolume() / room.volume());
        double bodies = occupants(level, room) * rules.bodyGainC() * Math.min(1, rules.bodyVolume() / room.volume());
        return base + (fire + bodies) * room.insulation();
    }

    public static double airC(Level level, BlockPos pos) {
        double outside = Climate.airC(level, pos, Calendar.day(level));
        Room room = at(level, pos);
        if (!room.sealed()) {
            return outside;
        }
        return airC(level, room, outside);
    }

    public static double airC(Level level, Room room, double outsideC) {
        double target = targetC(level, room, outsideC);
        long now = level.getGameTime();
        Map<BlockPos, Warmth> air = AIR.computeIfAbsent(level, key -> new HashMap<>());
        Warmth known = air.get(room.anchor());
        double current = target;
        if (known != null) {
            double hours = Math.max(0, now - known.tick()) / TICKS_AN_HOUR;
            current = known.airC() + (target - known.airC()) * (1 - Math.exp(-hours / Balance.get().rooms().warmHours()));
        }
        if (air.size() > Balance.get().rooms().remembered()) {
            air.clear();
        }
        air.put(room.anchor(), new Warmth(current, now));
        return current;
    }
}
