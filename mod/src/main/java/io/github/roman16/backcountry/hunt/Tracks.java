package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.fire.Fire;
import io.github.roman16.backcountry.guide.Deed;
import io.github.roman16.backcountry.guide.Guide;
import io.github.roman16.backcountry.weather.Day;
import io.github.roman16.backcountry.weather.Weather;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Tracks {
    public static final double TICKS_AN_HOUR = 1000;
    private static final int TICKS_A_SECOND = 20;
    private static final int STEP_TICKS = 10;
    private static final double TICKS_A_DAY = 24000;
    private static final double LEAP = 8;
    private static final double MEANDER = Math.PI / 8;
    private static final double QUARTER_TURN = 45;
    private static final double FLEE_SPEED = 1.25;
    private static final long FLEE_TICKS = 600;

    private Tracks() {}

    public static final class Steps {
        private boolean started;
        private double x;
        private double z;
        private double walked;
    }

    public enum Age {
        FRESH,
        HOURS,
        HALF_DAY,
        DAY;

        public static Age of(long ticks) {
            List<Double> limits = Balance.get().tracking().ageHours();
            double hours = ticks / TICKS_AN_HOUR;
            for (int i = 0; i < limits.size(); i++) {
                if (hours < limits.get(i)) {
                    return values()[i];
                }
            }
            return DAY;
        }

        public String key() {
            return "sign.backcountry.age." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    @SubscribeEvent
    static void walk(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob animal) || !(animal.level() instanceof ServerLevel level) || animal.tickCount % STEP_TICKS != 0) {
            return;
        }
        Optional<Animal> data = Animal.of(animal.getType());
        if (data.isEmpty()) {
            return;
        }
        data.get().track().ifPresent(track -> step(level, animal, track));
        if (data.get().scents() && animal.tickCount % TICKS_A_SECOND == 0) {
            sniff(level, animal);
        }
    }

    private static void step(ServerLevel level, PathfinderMob animal, Track track) {
        Balance.Tracking tracking = Balance.get().tracking();
        Steps steps = animal.getData(Things.STEPS);
        if (!steps.started) {
            steps.started = true;
            steps.x = animal.getX();
            steps.z = animal.getZ();
            return;
        }
        double dx = animal.getX() - steps.x;
        double dz = animal.getZ() - steps.z;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved < tracking.printBlocks()) {
            return;
        }
        steps.x = animal.getX();
        steps.z = animal.getZ();
        if (moved > LEAP) {
            return;
        }
        long now = level.getDayTime();
        Direction facing = Direction.fromYRot(Math.toDegrees(Math.atan2(-dx, dz)));
        mark(level, animal.blockPosition(), track, facing, now, false);
        steps.walked += moved;
        if (steps.walked >= tracking.droppingsBlocks()) {
            steps.walked = 0;
            mark(level, animal.blockPosition(), track, facing, now, true);
        }
    }

    public static void mark(ServerLevel level, BlockPos feet, Track track, Direction facing, long laid, boolean droppings) {
        BlockPos at = feet;
        BlockState here = level.getBlockState(at);
        if (here.isAir() && level.getBlockState(at.below()).is(Blocks.SNOW)) {
            at = at.below();
            here = level.getBlockState(at);
        }
        int snow;
        if (here.is(Blocks.SNOW)) {
            snow = here.getValue(SnowLayerBlock.LAYERS);
        } else if (here.getBlock() instanceof TrackBlock sign) {
            if (sign.droppings() && !droppings) {
                return;
            }
            snow = here.getValue(TrackBlock.SNOW);
        } else if (here.isAir()) {
            snow = 0;
        } else {
            return;
        }
        BlockPos below = at.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)) {
            return;
        }
        if (snow == 0 && !droppings && (track.snowOnly() ? !ground.is(Blocks.SNOW_BLOCK) : !ground.is(Things.TAKES_PRINTS))) {
            return;
        }
        Block block = droppings ? Things.DROPPINGS.get() : Things.TRACKS.get();
        level.setBlock(at, block.defaultBlockState().setValue(TrackBlock.KIND, track).setValue(TrackBlock.FACING, facing).setValue(TrackBlock.SNOW, snow), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(at) instanceof TrackEntity sign) {
            sign.laid(laid);
        }
    }

    public static void weather(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof TrackEntity sign)) {
            return;
        }
        Balance.Tracking tracking = Balance.get().tracking();
        boolean falling = Fire.rainedOn(level, pos);
        boolean snowing = falling && Climate.snows(level, level.getBiome(pos).value(), pos);
        sign.weather(falling && !snowing ? tracking.rainFade() - 1 : 0);
        if (snowing || sign.age() >= tracking.signDays() * TICKS_A_DAY) {
            fade(level, pos, state);
        }
    }

    public static void fade(Level level, BlockPos pos, BlockState state) {
        int snow = state.getValue(TrackBlock.SNOW);
        level.setBlockAndUpdate(pos, snow > 0 ? Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, snow) : Blocks.AIR.defaultBlockState());
    }

    public static void backTrail(ServerLevel level, Entity animal, Track track) {
        Balance.Tracking tracking = Balance.get().tracking();
        long now = level.getDayTime();
        double ticksABlock = tracking.backTrailHours() * TICKS_AN_HOUR / tracking.backTrailBlocks();
        double heading = level.random.nextDouble() * Math.PI * 2;
        double x = animal.getX();
        double z = animal.getZ();
        double walked = 0;
        for (double back = tracking.printBlocks(); back <= tracking.backTrailBlocks(); back += tracking.printBlocks()) {
            heading += (level.random.nextDouble() - 0.5) * 2 * MEANDER;
            x += Math.cos(heading) * tracking.printBlocks();
            z += Math.sin(heading) * tracking.printBlocks();
            BlockPos column = BlockPos.containing(x, animal.getY(), z);
            if (!level.isLoaded(column)) {
                return;
            }
            BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            Direction facing = Direction.fromYRot(Math.toDegrees(Math.atan2(Math.cos(heading), -Math.sin(heading))));
            long laid = now - (long) (back * ticksABlock);
            mark(level, feet, track, facing, laid, false);
            walked += tracking.printBlocks();
            if (walked >= tracking.droppingsBlocks()) {
                walked = 0;
                mark(level, feet, track, facing, laid, true);
            }
        }
    }

    public static boolean carries(Day day, Vec3 from, Vec3 to) {
        Balance.Tracking tracking = Balance.get().tracking();
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (day.windKmh() < tracking.calmKmh()) {
            return distance <= tracking.calmScentBlocks();
        }
        if (distance > tracking.scentBlocks()) {
            return false;
        }
        Vec3 downwind = downwind(day);
        double along = (dx * downwind.x + dz * downwind.z) / Math.max(distance, 1e-6);
        return along >= Math.cos(Math.toRadians(tracking.scentDegrees() / 2));
    }

    public static Vec3 downwind(Day day) {
        double from = Math.toRadians(day.windFrom() * QUARTER_TURN);
        return new Vec3(-Math.sin(from), 0, Math.cos(from));
    }

    public static Day today(Level level) {
        return Weather.day(level, Calendar.day(level));
    }

    private static void sniff(ServerLevel level, PathfinderMob animal) {
        if (animal.getData(Things.FLEEING_UNTIL) > level.getGameTime() || animal.getData(Things.ANSWERING)) {
            return;
        }
        Day day = today(level);
        double reach = Balance.get().tracking().scentBlocks();
        for (ServerPlayer survivor : level.getEntitiesOfClass(ServerPlayer.class, animal.getBoundingBox().inflate(reach), Metabolism::lives)) {
            if (carries(day, survivor.position(), animal.position())) {
                Vec3 away = animal.position().subtract(survivor.position()).multiply(1, 0, 1).normalize().scale(Balance.get().tracking().fleeBlocks());
                animal.getNavigation().moveTo(animal.getX() + away.x, animal.getY(), animal.getZ() + away.z, FLEE_SPEED);
                animal.setData(Things.FLEEING_UNTIL, level.getGameTime() + FLEE_TICKS);
                return;
            }
        }
    }

    @SubscribeEvent
    static void read(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % TICKS_A_SECOND != 0 || !Metabolism.lives(player)) {
            return;
        }
        Balance.Tracking tracking = Balance.get().tracking();
        ServerLevel level = player.serverLevel();
        if (player.isCrouching()) {
            HitResult hit = player.pick(player.blockInteractionRange(), 0, false);
            if (hit instanceof BlockHitResult block && level.getBlockEntity(block.getBlockPos()) instanceof TrackEntity sign
                    && sign.age() < tracking.readHours() * TICKS_AN_HOUR) {
                Guide.did(player, Deed.READ_SIGN);
            }
        }
        AABB near = player.getBoundingBox().inflate(tracking.stalkBlocks());
        boolean unnoticed = !level.getEntitiesOfClass(PathfinderMob.class, near, animal -> Animal.of(animal.getType()).map(Animal::scents).orElse(false)
                && animal.getData(Things.FLEEING_UNTIL) <= level.getGameTime() && animal.distanceTo(player) <= tracking.stalkBlocks()).isEmpty();
        if (unnoticed) {
            Guide.did(player, Deed.STALKED);
        }
    }
}
