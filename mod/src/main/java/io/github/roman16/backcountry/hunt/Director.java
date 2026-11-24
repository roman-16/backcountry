package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.survivor.Team;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Director {
    private static final int TICKS_A_SECOND = 20;
    private static final double SECONDS_A_DAY = 1200;
    private static final double EYE_ABOVE = 1;

    private Director() {}

    @SubscribeEvent
    static void natural(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL || type == MobSpawnType.REINFORCEMENT) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    static void traders(EntityJoinLevelEvent event) {
        EntityType<?> type = event.getEntity().getType();
        if (type == EntityType.WANDERING_TRADER || type == EntityType.TRADER_LLAMA) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        Balance.Director director = Balance.get().director();
        if (level.getGameTime() % ((long) director.checkSeconds() * TICKS_A_SECOND) != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (Metabolism.lives(player)) {
                for (Map.Entry<ResourceKey<EntityType<?>>, Animal> entry : BuiltInRegistries.ENTITY_TYPE.getDataMap(Things.ANIMAL).entrySet()) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
                    if (level.random.nextDouble() < chance(level, player, type, entry.getValue())) {
                        spawn(level, player, type, entry.getValue());
                    }
                }
            }
        }
    }

    public static double chance(ServerLevel level, ServerPlayer player, EntityType<?> type, Animal animal) {
        Balance.Director director = Balance.get().director();
        int day = Calendar.day(level);
        if (!animal.inSeason(Seasons.month(level))) {
            return 0;
        }
        Team team = Team.get(level.getServer());
        if (day - team.lastTaken(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()) < animal.restDays()) {
            return 0;
        }
        double chance = animal.perDay() * director.checkSeconds() / SECONDS_A_DAY;
        if (level.isRainingAt(player.blockPosition()) || level.isRaining()) {
            chance *= director.rainShare();
        }
        if (animal.season().map(season -> Hunting.season(level, season)).orElse(false)) {
            chance *= director.eventShare();
        }
        if (animal.scavenger() && scented(level, player)) {
            chance *= director.scentShare();
        }
        return chance;
    }

    private static boolean scented(ServerLevel level, ServerPlayer player) {
        double reach = Balance.get().rations().reachBlocks();
        for (BlockPos pos : Team.get(level.getServer()).caches()) {
            if (pos.distToCenterSqr(player.position()) <= reach * reach && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CacheEntity cache && cache.scented()) {
                return true;
            }
        }
        return false;
    }

    public static int spawn(ServerLevel level, ServerPlayer player, EntityType<?> type, Animal animal) {
        Balance.Director director = Balance.get().director();
        AABB around = player.getBoundingBox().inflate(director.countBlocks());
        if (level.getEntities(type, around, Entity::isAlive).size() >= animal.cap()) {
            return 0;
        }
        Optional<BlockPos> spot = spot(level, player, animal);
        if (spot.isEmpty()) {
            return 0;
        }
        List<Integer> group = animal.group();
        int count = group.get(0) + level.random.nextInt(group.get(1) - group.get(0) + 1);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            BlockPos at = spot.get().offset(level.random.nextInt(3) - 1, 0, level.random.nextInt(3) - 1);
            at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
            Entity entity = type.spawn(level, at, MobSpawnType.EVENT);
            if (entity != null) {
                if (entity instanceof Rabbit rabbit) {
                    rabbit.setVariant(Hunting.white(level) ? Rabbit.Variant.WHITE : Rabbit.Variant.BROWN);
                }
                spawned++;
            }
        }
        return spawned;
    }

    private static Optional<BlockPos> spot(ServerLevel level, ServerPlayer player, Animal animal) {
        Balance.Director director = Balance.get().director();
        for (int attempt = 0; attempt < director.tries(); attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = director.nearBlocks() + level.random.nextDouble() * (director.farBlocks() - director.nearBlocks());
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            Holder<Biome> biome = level.getBiome(ground);
            boolean wet = level.getFluidState(ground).is(FluidTags.WATER) || level.getFluidState(ground.below()).is(FluidTags.WATER);
            if (!biome.is(animal.habitat()) || (animal.nearWater() ? !wet && !nearWater(level, ground) : wet)) {
                continue;
            }
            Vec3 eyes = player.getEyePosition();
            Vec3 target = Vec3.atCenterOf(ground).add(0, EYE_ABOVE, 0);
            if (level.clip(new ClipContext(eyes, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) {
                continue;
            }
            return Optional.of(ground);
        }
        return Optional.empty();
    }

    private static boolean nearWater(ServerLevel level, BlockPos ground) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int step = 1; step <= 2; step++) {
                BlockPos next = ground.relative(direction, step);
                if (level.getFluidState(next).is(FluidTags.WATER) || level.getFluidState(next.below()).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
