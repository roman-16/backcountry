package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Activity;
import io.github.roman16.backcountry.body.Body;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.body.Skill;
import io.github.roman16.backcountry.food.Seasons;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Hunting {
    private static final double TICKS_AN_HOUR = 1000;
    private static final int TICKS_A_SECOND = 20;
    private static final int FULL_DRAW_TICKS = 20;
    private static final int CLIMB = 3;

    private Hunting() {}

    public static boolean months(int month, List<Double> range) {
        int first = range.get(0).intValue();
        int last = range.get(1).intValue();
        return first <= last ? month >= first && month <= last : month >= first || month <= last;
    }

    public static boolean season(Level level, Animal.Season season) {
        return months(Seasons.month(level), range(season));
    }

    private static List<Double> range(Animal.Season season) {
        Balance.Seasons seasons = Balance.get().seasons();
        return season == Animal.Season.RUT ? seasons.rutMonths() : seasons.feedingMonths();
    }

    public static boolean begins(int day, Animal.Season season) {
        return months(Calendar.date(day).getMonthValue(), range(season)) && !months(Calendar.date(day - 1).getMonthValue(), range(season));
    }

    public static boolean white(Level level) {
        return months(Seasons.month(level), Balance.get().director().whiteMonths());
    }

    public static double strength(Player player) {
        Balance.Hunting hunting = Balance.get().hunting();
        Body body = Metabolism.body(player);
        double extraction = Balance.get().body().extractionBmi();
        double built = (body.bmi() - extraction) / (hunting.strongBmi() - extraction);
        double rested = body.energy / hunting.strongEnergy();
        return Math.max(0, Math.min(1, built)) * Math.max(0, Math.min(1, rested));
    }

    @SubscribeEvent
    static void scraped(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().is(Things.SCRAPED_HIDE.get()) && !event.getEntity().level().isClientSide) {
            Metabolism.effortMinutes(event.getEntity(), Activity.SCRAPE, Balance.get().effort().scrapeMinutes() * Skill.BUTCHERING.effort(Metabolism.body(event.getEntity())));
            if (event.getEntity() instanceof ServerPlayer player) {
                Skill.BUTCHERING.use(player, 1);
            }
        }
    }

    @SubscribeEvent
    static void loosed(ArrowLooseEvent event) {
        if (event.getBow().getItem() instanceof HuntingBowItem) {
            Body body = Metabolism.body(event.getEntity());
            int charge = (int) Math.round(event.getCharge() * Skill.ARCHERY.speed(body));
            event.setCharge(charge);
            if (charge >= FULL_DRAW_TICKS && event.getEntity() instanceof ServerPlayer player) {
                Skill.ARCHERY.use(player, 1);
            }
        }
        if (event.getBow().getItem() instanceof HuntingBowItem bow && bow.heavy()) {
            int drawn = (int) Math.floor(FULL_DRAW_TICKS * strength(event.getEntity()));
            event.setCharge(Math.min(event.getCharge(), Math.max(1, drawn)));
        }
    }

    private static double recovered(double chance, AbstractArrow arrow) {
        if (!(arrow.getOwner() instanceof Player archer)) {
            return chance;
        }
        return chance + (1 - chance) * (1 - Skill.ARCHERY.effort(Metabolism.body(archer)));
    }

    @SubscribeEvent
    static void struck(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow && event.getRayTraceResult().getType() == HitResult.Type.BLOCK
                && arrow.getPickupItemStackOrigin().getItem() instanceof HuntArrowItem kind && arrow.level().random.nextDouble() >= recovered(kind.fromBlocks(), arrow)) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
    }

    @SubscribeEvent
    static void hit(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Optional<Animal> animal = Animal.of(target.getType());
        if (animal.isEmpty() || target.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        Optional<UUID> hunter = source.getEntity() instanceof Player player ? Optional.of(player.getUUID()) : Optional.empty();
        Balance.Hunting hunting = Balance.get().hunting();
        if (direct instanceof AbstractArrow arrow) {
            ItemStack shot = arrow.getPickupItemStackOrigin();
            if (shot.getItem() instanceof HuntArrowItem kind && target.getRandom().nextDouble() < recovered(kind.fromAnimals(), arrow)) {
                target.setData(Things.WOUND, target.getData(Things.WOUND).pierced(shot));
            }
        }
        if (animal.get().kg() < hunting.organKg() || !(source.getEntity() instanceof Player)) {
            return;
        }
        boolean vital = false;
        if (direct instanceof AbstractArrow arrow && !(arrow.getPickupItemStackOrigin().getItem() instanceof HuntArrowItem kind && kind.blunt())) {
            double height = (arrow.getY() - target.getY()) / target.getBbHeight();
            vital = height >= animal.get().vital().get(0) && height <= animal.get().vital().get(1);
        }
        if (vital) {
            event.setAmount(Math.max(event.getAmount(), target.getHealth() + target.getAbsorptionAmount() + 1));
            return;
        }
        event.setAmount(event.getAmount() * (float) hunting.woundShare());
        long until = target.level().getGameTime() + (long) (hunting.bleedHours() * TICKS_AN_HOUR);
        target.setData(Things.WOUND, target.getData(Things.WOUND).bleed(until, hunter));
        if (source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.backcountry.wounded"), true);
        }
    }

    @SubscribeEvent
    static void bleed(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity animal) || animal.level().isClientSide || !animal.hasData(Things.WOUND) || animal.tickCount % TICKS_A_SECOND != 0) {
            return;
        }
        Wound wound = animal.getData(Things.WOUND);
        if (!wound.bleeding() || !animal.isAlive()) {
            return;
        }
        ServerLevel level = (ServerLevel) animal.level();
        if (level.random.nextDouble() < Balance.get().hunting().bloodChance()) {
            BlockPos feet = animal.blockPosition();
            if (level.getBlockState(feet).canBeReplaced() && level.getFluidState(feet).isEmpty() && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                    && !level.getBlockState(feet).is(Things.BLOOD.get())) {
                level.setBlockAndUpdate(feet, Things.BLOOD.get().defaultBlockState());
            }
        }
        if (level.getGameTime() >= wound.bleedsOutAt()) {
            Entity hunter = wound.hunter().map(level::getEntity).orElse(null);
            DamageSource bledOut = hunter instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().generic();
            animal.hurt(bledOut, Float.MAX_VALUE);
        }
    }

    @SubscribeEvent
    static void died(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (Animal.of(dead.getType()).isEmpty() || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        event.setCanceled(true);
        BlockPos at = dead.blockPosition();
        for (int climb = 0; climb < CLIMB && !(level.getBlockState(at).canBeReplaced()); climb++) {
            at = at.above();
        }
        BlockState carcass = Things.CARCASS.get().defaultBlockState();
        if (level.getBlockState(at).canBeReplaced() && level.setBlockAndUpdate(at, carcass) && level.getBlockEntity(at) instanceof CarcassEntity entity) {
            entity.hold(dead.getType(), dead.getData(Things.WOUND).arrows(), 0);
        }
    }
}
