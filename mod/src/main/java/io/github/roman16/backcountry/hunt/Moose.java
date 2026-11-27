package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.food.Seasons;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Moose {
    public static final ResourceLocation MOOSE = ResourceLocation.fromNamespaceAndPath("alexsmobs", "moose");
    private static final String ANTLERED = "Antlered";
    private static final int CHECK_TICKS = 20;
    private static final int SNORT_TICKS = 20;
    private static final double CHARGE_SPEED = 1.6;
    private static final double WALK_AWAY_SPEED = 1.0;
    private static final int WALK_AWAY_BLOCKS = 16;
    private static final int WALK_AWAY_TRIES = 3;
    private static final Map<Mob, Player> BLUFFS = new WeakHashMap<>();

    private Moose() {}

    public static boolean is(EntityType<?> type) {
        return MOOSE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static boolean bull(Mob moose) {
        CompoundTag tag = new CompoundTag();
        moose.saveWithoutId(tag);
        return tag.getBoolean(ANTLERED);
    }

    private static void antlers(Mob moose, boolean antlered) {
        CompoundTag tag = new CompoundTag();
        moose.saveWithoutId(tag);
        tag.putBoolean(ANTLERED, antlered);
        moose.load(tag);
    }

    public static void calves(ServerLevel level, Mob moose) {
        Balance.MooseCharges rules = Balance.get().moose();
        if (!Hunting.months(Seasons.month(level), rules.calfMonths()) || level.random.nextDouble() >= rules.cowShare()) {
            return;
        }
        antlers(moose, false);
        if (moose.getType().create(level) instanceof AgeableMob calf) {
            calf.setAge(AgeableMob.BABY_START_AGE);
            calf.moveTo(moose.getX() + level.random.nextDouble() - 0.5, moose.getY(), moose.getZ() + level.random.nextDouble() - 0.5, moose.getYRot(), 0);
            level.addFreshEntity(calf);
        }
    }

    @SubscribeEvent
    static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob moose) || !is(moose.getType())) {
            return;
        }
        moose.targetSelector.addGoal(1, new Temper(moose));
        moose.goalSelector.addGoal(1, new Bluff(moose));
    }

    private static boolean calfNear(Mob moose, ServerPlayer player, double reach) {
        if (moose.isBaby() || bull(moose)) {
            return false;
        }
        AABB around = moose.getBoundingBox().inflate(reach);
        return moose.level().getEntities(moose.getType(), around, other -> other instanceof Mob calf && calf.isBaby() && calf.isAlive()).stream()
                .anyMatch(calf -> calf.distanceTo(player) <= reach);
    }

    private static boolean riled(Mob moose, ServerPlayer player, ServerLevel level) {
        Balance.MooseCharges rules = Balance.get().moose();
        int month = Seasons.month(level);
        if (Hunting.months(month, rules.calfMonths()) && calfNear(moose, player, rules.chargeBlocks())) {
            return true;
        }
        return Hunting.season(level, Animal.Season.RUT) && bull(moose) && moose.distanceTo(player) <= rules.chargeBlocks()
                && level.random.nextDouble() < rules.chargeChance();
    }

    public static boolean behindATree(Mob moose, Player target) {
        Vec3 from = moose.getEyePosition();
        Vec3 to = target.getEyePosition();
        BlockHitResult hit = moose.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, moose));
        return hit.getType() == HitResult.Type.BLOCK && moose.level().getBlockState(hit.getBlockPos()).is(BlockTags.LOGS)
                && hit.getLocation().distanceTo(to) <= Balance.get().moose().treeBlocks();
    }

    private static void walkAway(PathfinderMob moose, Player from) {
        BLUFFS.remove(moose);
        moose.setTarget(null);
        Vec3 away = DefaultRandomPos.getPosAway(moose, WALK_AWAY_BLOCKS, WALK_AWAY_TRIES, from.position());
        if (away != null) {
            moose.getNavigation().moveTo(away.x, away.y, away.z, WALK_AWAY_SPEED);
        }
    }

    private static final class Temper extends TargetGoal {
        Temper(Mob moose) {
            super(moose, true);
        }

        @Override
        public boolean canUse() {
            if (mob.isBaby() || mob.getTarget() != null || mob.tickCount % CHECK_TICKS != 0 || !(mob.level() instanceof ServerLevel level)
                    || mob.level().getGameTime() < mob.getData(Things.FLEEING_UNTIL)) {
                return false;
            }
            double reach = Balance.get().moose().chargeBlocks();
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(reach))) {
                if (Metabolism.lives(player) && mob.hasLineOfSight(player) && riled(mob, player, level)) {
                    targetMob = player;
                    if (level.random.nextDouble() < Balance.get().moose().bluffShare()) {
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
        private final PathfinderMob moose;
        private int snorted;

        Bluff(PathfinderMob moose) {
            this.moose = moose;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player target = moose.getTarget() instanceof Player player ? player : null;
            return target != null && target.isAlive() && (BLUFFS.get(moose) == target || behindATree(moose, target));
        }

        @Override
        public void start() {
            snorted = 0;
        }

        @Override
        public void tick() {
            if (!(moose.getTarget() instanceof Player target)) {
                return;
            }
            if (behindATree(moose, target)) {
                walkAway(moose, target);
                return;
            }
            if (moose.getLastHurtByMob() == target) {
                BLUFFS.remove(moose);
                return;
            }
            moose.getLookControl().setLookAt(target);
            if (moose.distanceTo(target) > Balance.get().moose().bluffStopBlocks() && snorted == 0) {
                moose.getNavigation().moveTo(target, CHARGE_SPEED);
                return;
            }
            moose.getNavigation().stop();
            if (snorted++ == 0) {
                moose.level().playSound(null, moose, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6F, 1.4F);
            }
            if (snorted >= SNORT_TICKS) {
                walkAway(moose, target);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return moose.getTarget() != null && (BLUFFS.containsKey(moose) || moose.getTarget() instanceof Player player && behindATree(moose, player));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
