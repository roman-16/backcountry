package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.food.CacheEntity;
import io.github.roman16.backcountry.food.Seasons;
import io.github.roman16.backcountry.survivor.Team;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Bears {
    public static final ResourceLocation GRIZZLY = ResourceLocation.fromNamespaceAndPath("alexsmobs", "grizzly_bear");
    public static final ResourceLocation BLACK_BEAR = ResourceLocation.fromNamespaceAndPath("cnc", "black_bear");
    private static final long NOWHERE = Long.MIN_VALUE;
    private static final int CHECK_TICKS = 20;
    private static final int ROAR_TICKS = 20;
    private static final double CHARGE_SPEED = 1.6;
    private static final double WALK_AWAY_SPEED = 1.0;
    private static final int WALK_AWAY_BLOCKS = 16;
    private static final int GROAN_SECONDS = 3;
    private static final int TICKS_AN_HOUR = 1000;
    private static final Map<Mob, Player> BLUFFS = new WeakHashMap<>();

    private Bears() {}

    public static boolean is(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return GRIZZLY.equals(id) || BLACK_BEAR.equals(id);
    }

    private static boolean grizzly(Mob bear) {
        return GRIZZLY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(bear.getType()));
    }

    public static boolean salmonRun(ServerLevel level) {
        List<Double> months = Balance.get().bears().salmonMonths();
        return Seasons.within(level, months.get(0).intValue(), months.get(1).intValue());
    }

    public static void fishing(Mob bear, BlockPos at) {
        bear.setData(Things.FISHING_AT, at.asLong());
    }

    public static void cubs(ServerLevel level, Mob sow) {
        Balance.Bears bears = Balance.get().bears();
        if (level.random.nextDouble() >= bears.cubShare()) {
            return;
        }
        int count = 1 + level.random.nextInt(bears.mostCubs());
        for (int i = 0; i < count; i++) {
            if (sow.getType().create(level) instanceof AgeableMob cub) {
                cub.setAge(AgeableMob.BABY_START_AGE);
                cub.moveTo(sow.getX() + level.random.nextDouble() - 0.5, sow.getY(), sow.getZ() + level.random.nextDouble() - 0.5, sow.getYRot(), 0);
                level.addFreshEntity(cub);
            }
        }
    }

    @SubscribeEvent
    static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob bear) || !is(bear.getType())) {
            return;
        }
        bear.targetSelector.removeAllGoals(goal -> goal.getClass().getSimpleName().equals("AttackPlayerGoal")
                || goal instanceof NearestAttackableTargetGoal<?> && goal.getClass() == NearestAttackableTargetGoal.class);
        bear.targetSelector.addGoal(1, new Temper(bear));
        bear.goalSelector.addGoal(1, new Bluff(bear));
        if (!grizzly(bear)) {
            bear.goalSelector.addGoal(2, new AvoidEntityGoal<>(bear, Player.class, (float) Balance.get().bears().fleeBlocks(), 1.0, 1.4,
                    player -> player instanceof ServerPlayer survivor && Metabolism.lives(survivor) && !bear.isBaby() && reason(bear, survivor).isEmpty()));
        }
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob bear) || bear.level().isClientSide || bear.tickCount % TICKS_AN_HOUR != 0 || !is(bear.getType())) {
            return;
        }
        long fishing = bear.getData(Things.FISHING_AT);
        if (fishing == NOWHERE || !(bear.level() instanceof ServerLevel level) || !salmonRun(level)) {
            return;
        }
        if (level.random.nextDouble() < 1 / Balance.get().bears().leftoverHours()) {
            ItemStack salmon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("riverfishing", "sockeye_salmon")));
            CompoundTag data = new CompoundTag();
            data.putString("Species", "riverfishing:sockeye_salmon");
            data.putInt("WeightG", Balance.get().bears().leftoverGrams());
            salmon.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            salmon.set(DataComponents.CUSTOM_NAME, Component.translatable("item.backcountry.half_eaten_salmon"));
            bear.spawnAtLocation(salmon);
        }
    }

    public static Optional<String> reason(Mob bear, ServerPlayer player) {
        Balance.Bears bears = Balance.get().bears();
        double distance = bear.distanceTo(player);
        if (distance <= bears.cubBlocks() && cubNear(bear, player, bears.cubBlocks())) {
            return Optional.of("cubs");
        }
        if (distance <= bears.defendBlocks() * 2 && defending(bear, player, bears.defendBlocks())) {
            return Optional.of("food");
        }
        return Optional.empty();
    }

    private static boolean cubNear(Mob bear, ServerPlayer player, double reach) {
        if (bear.isBaby()) {
            return false;
        }
        AABB around = bear.getBoundingBox().inflate(reach);
        return bear.level().getEntities(bear.getType(), around, other -> other instanceof Mob cub && cub.isBaby() && cub.isAlive())
                .stream().anyMatch(cub -> cub.distanceTo(player) <= reach);
    }

    private static boolean defending(Mob bear, ServerPlayer player, double reach) {
        long fishing = bear.getData(Things.FISHING_AT);
        if (fishing != NOWHERE && BlockPos.of(fishing).distToCenterSqr(player.position()) <= reach * reach
                && BlockPos.of(fishing).distToCenterSqr(bear.position()) <= reach * reach * 4) {
            return true;
        }
        BlockPos at = bear.blockPosition();
        int r = (int) Math.ceil(reach);
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 2, r))) {
            if (bear.level().getBlockState(pos).is(Things.CARCASS.get()) && pos.distToCenterSqr(player.position()) <= reach * reach) {
                return true;
            }
        }
        if (bear.level() instanceof ServerLevel level) {
            for (BlockPos cache : Team.get(level.getServer()).caches()) {
                if (cache.distToCenterSqr(bear.position()) <= reach * reach && cache.distToCenterSqr(player.position()) <= reach * reach && level.isLoaded(cache)
                        && level.getBlockEntity(cache) instanceof CacheEntity stores && stores.scented()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean riled(Mob bear, ServerPlayer player) {
        if (!grizzly(bear) || !(bear.level() instanceof ServerLevel level)) {
            return false;
        }
        Balance.Bears bears = Balance.get().bears();
        double distance = bear.distanceTo(player);
        int month = Seasons.month(level);
        boolean hungry = Seasons.within(month, bears.springMonths().get(0).intValue(), bears.springMonths().get(1).intValue())
                || Seasons.within(month, bears.fallMonths().get(0).intValue(), bears.fallMonths().get(1).intValue());
        if (hungry && distance <= bears.chargeBlocks()) {
            return level.random.nextDouble() < bears.chargeChance();
        }
        return distance <= bears.surpriseBlocks();
    }

    private static final class Temper extends TargetGoal {
        Temper(Mob bear) {
            super(bear, true);
        }

        @Override
        public boolean canUse() {
            if (mob.isBaby() || mob.getTarget() != null || mob.tickCount % CHECK_TICKS != 0 || !(mob.level() instanceof ServerLevel level)) {
                return false;
            }
            double reach = Balance.get().bears().chargeBlocks();
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(reach))) {
                if (!Metabolism.lives(player) || !mob.hasLineOfSight(player)) {
                    continue;
                }
                if (reason(mob, player).isPresent() || riled(mob, player)) {
                    targetMob = player;
                    if (level.random.nextDouble() < Balance.get().bears().bluffShare()) {
                        BLUFFS.put(mob, player);
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }
    }

    private static final class Bluff extends Goal {
        private final PathfinderMob bear;
        private int roared;

        Bluff(PathfinderMob bear) {
            this.bear = bear;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player target = BLUFFS.get(bear);
            return target != null && bear.getTarget() == target && target.isAlive();
        }

        @Override
        public void start() {
            roared = 0;
        }

        @Override
        public void tick() {
            Player target = BLUFFS.get(bear);
            if (target == null) {
                return;
            }
            if (bear.getLastHurtByMob() == target || target.isSprinting() && target.getDeltaMovement().dot(target.position().subtract(bear.position())) > 0) {
                BLUFFS.remove(bear);
                return;
            }
            bear.getLookControl().setLookAt(target);
            if (bear.distanceTo(target) > Balance.get().bears().bluffStopBlocks() && roared == 0) {
                bear.getNavigation().moveTo(target, CHARGE_SPEED);
                return;
            }
            bear.getNavigation().stop();
            if (roared++ == 0) {
                bear.level().playSound(null, bear, SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 1, 0.8F);
            }
            if (roared >= ROAR_TICKS) {
                BLUFFS.remove(bear);
                bear.setTarget(null);
                Vec3 away = DefaultRandomPos.getPosAway(bear, WALK_AWAY_BLOCKS, GROAN_SECONDS, target.position());
                if (away != null) {
                    bear.getNavigation().moveTo(away.x, away.y, away.z, WALK_AWAY_SPEED);
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return BLUFFS.containsKey(bear) && bear.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    public static boolean bluffing(Mob bear) {
        return BLUFFS.containsKey(bear);
    }
}
