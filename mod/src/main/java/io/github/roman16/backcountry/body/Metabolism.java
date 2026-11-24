package io.github.roman16.backcountry.body;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.net.Messages;
import io.github.roman16.backcountry.survivor.Ending;
import io.github.roman16.backcountry.survivor.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Metabolism {
    public static final ResourceKey<DamageType> FROSTBITE = ResourceKey.create(Registries.DAMAGE_TYPE, Backcountry.id("frostbite"));
    public static final ResourceKey<DamageType> INFECTION = ResourceKey.create(Registries.DAMAGE_TYPE, Backcountry.id("infection"));
    public static final ResourceKey<DamageType> THIRST = ResourceKey.create(Registries.DAMAGE_TYPE, Backcountry.id("thirst"));
    public static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double TICKS_A_DAY = 24000;
    private static final int TICKS_A_SECOND = 20;
    private static final int TIRED_FOOD = 6;
    private static final int HELD_FOOD = 17;
    private static final float HEART = 2;
    private static final double TELEPORT_BLOCKS = 16;
    private static final double SINK_PER_TICK = 0.04;
    private static final ResourceLocation SLOW_HANDS = Backcountry.id("tired_hands");
    private static final ResourceLocation STILL = Backcountry.id("blacked_out");
    private static final ResourceLocation STILL_JUMP = Backcountry.id("blacked_out_jump");
    private static final ResourceLocation STUMBLE = Backcountry.id("stumble");

    private Metabolism() {}

    public static Body body(Player player) {
        return player.getData(Things.BODY);
    }

    public static boolean lives(ServerPlayer player) {
        Body body = body(player);
        GameType mode = player.gameMode.getGameModeForPlayer();
        return body.arrived && body.ended == null && player.isAlive() && (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE);
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !lives(player)) {
            return;
        }
        Body body = body(player);
        move(player, body);
        hold(player, body);
        if (player.tickCount % STEP_TICKS == 0) {
            step(player, body, STEP_TICKS, player.isSleeping());
            if (lives(player)) {
                Messages.view(player, body);
            }
        }
    }

    @SubscribeEvent
    static void sleptThrough(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long skipped = event.getNewTime() - level.getDayTime();
        if (skipped <= 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() && lives(player)) {
                step(player, body(player), skipped, true);
            }
        }
    }

    public static void step(ServerPlayer player, Body body, long ticks, boolean asleep) {
        Balance balance = Balance.get();
        double hours = ticks / TICKS_AN_HOUR;
        double days = ticks / TICKS_A_DAY;
        int day = Calendar.day(player.level());
        if (day != body.lastDay) {
            body.lastDay = day;
            body.weights.add((float) body.weightKg);
            body.newDay();
        }
        Warmth.sense(player, body, day);
        body.loadKg = Load.kg(player, body);
        double over = Math.max(0, body.loadKg - balance.load().capacityKg());
        double shiver = body.coreC < balance.warmth().shiverCoreC() ? balance.warmth().shiverKcalPerHourC() * (balance.warmth().shiverCoreC() - body.coreC) * hours : 0;
        double work = body.workKcal + over * balance.work().overloadKcalPerKgHour() * hours + shiver;
        body.workKcal = 0;
        double illness = (body.has(Condition.INFECTED_WOUND) ? balance.conditions().feverBurn() : 0) + (body.has(Condition.SCURVY) ? balance.conditions().scurvyBurn() : 0);
        double burn = balance.body().restingKcalPerKgDay() * body.weightKg * days * (1 + illness) + work;
        double absorbed = digest(body, hours);
        body.weightKg += (absorbed - burn) / balance.body().kcalPerKg();
        body.kcalBurnedToday += burn;
        body.vitaminCMg = Math.max(0, body.vitaminCMg - balance.body().vitaminCMgDay() * days);
        Balance.Energy energy = balance.energy();
        body.energy -= work / energy.workKcalToEmpty();
        if (asleep) {
            body.energy += hours / energy.sleepHoursToFull();
            body.awakeTicks = 0;
        } else {
            body.energy -= hours / energy.awakeHoursToEmpty();
            body.awakeTicks += ticks;
        }
        body.energy += absorbed / energy.digestKcalToFull();
        body.energy = Math.max(0, Math.min(body.has(Condition.EXHAUSTED) ? balance.conditions().exhaustedEnergy() : 1, body.energy));
        double thirst = balance.water().needLDay() * days * (body.has(Condition.GUT_INFECTION) ? balance.conditions().gutWater() : 1);
        if (body.coreC > balance.warmth().sweatCoreC()) {
            thirst += balance.warmth().sweatLPerHour() * hours;
        }
        body.waterL = Math.max(0, body.waterL - thirst);
        Warmth.core(body, hours);
        Warmth.wet(player, body, hours);
        Afflictions.advance(player, body, days, hours);
        heal(player, body, hours, asleep);
        if (!asleep) {
            limits(player, body, ticks);
        }
        if (body.loadKg > balance.load().capacityKg() && player.tickCount - body.loadWarnedAt >= balance.load().warnSeconds() * TICKS_A_SECOND) {
            body.loadWarnedAt = player.tickCount;
            player.displayClientMessage(Component.translatable("message.backcountry.pack_too_full"), true);
        }
        Team.get(player.server).note(player, body);
        Alerts.check(player, body, day);
        if (player.isAlive() && body.bmi() <= balance.body().extractionBmi()) {
            Ending.extract(player);
        }
    }

    private static double digest(Body body, double hours) {
        if (body.stomachKcal <= 0) {
            return 0;
        }
        Balance balance = Balance.get();
        double digested = Math.min(body.stomachKcal, balance.body().digestKcalPerHour() * hours);
        double share = digested / body.stomachKcal;
        double vitamin = body.stomachVitaminC * share;
        body.vitaminCMg = Math.min(balance.body().vitaminCMgDay() * balance.body().vitaminCStoreDays(), body.vitaminCMg + vitamin);
        body.stomachVitaminC -= vitamin;
        body.stomachFat -= body.stomachFat * share;
        body.stomachProtein -= body.stomachProtein * share;
        body.stomachCarbs -= body.stomachCarbs * share;
        body.stomachKcal -= digested;
        double absorb = (body.has(Condition.GUT_INFECTION) ? balance.conditions().gutAbsorb() : 1) * (body.has(Condition.PARASITES) ? balance.conditions().parasitesAbsorb() : 1);
        return digested * absorb;
    }

    private static void heal(ServerPlayer player, Body body, double hours, boolean asleep) {
        Balance.Healing healing = Balance.get().healing();
        boolean fit = body.energy > healing.above() && body.waterShare() > healing.above() && !body.has(Condition.INFECTED_WOUND) && !body.has(Condition.BURNS);
        if (!fit || player.getHealth() >= player.getMaxHealth()) {
            body.healHours = 0;
            return;
        }
        double rate = (asleep ? healing.asleepFactor() : 1) * (body.has(Condition.SCURVY) ? healing.scurvyFactor() : 1) / healing.hoursPerHeart();
        body.healHours += hours * rate;
        while (body.healHours >= 1 && player.getHealth() < player.getMaxHealth()) {
            player.heal(HEART);
            body.healHours -= 1;
        }
    }

    private static void limits(ServerPlayer player, Body body, long ticks) {
        Balance balance = Balance.get();
        Balance.Energy energy = balance.energy();
        if (body.blackoutTicks == 0 && body.energy <= 0) {
            blackOut(player, body);
        }
        if (body.waterL <= 0) {
            body.thirstSeconds += ticks / (double) TICKS_A_SECOND;
            if (body.thirstSeconds >= balance.water().knockoutSeconds()) {
                body.thirstSeconds = 0;
                blackOut(player, body);
                player.hurt(player.damageSources().source(THIRST), balance.water().thirstDamage());
            }
        } else {
            body.thirstSeconds = 0;
        }
        if (body.energy < energy.stumbleBelow() && body.stumbleTicks == 0 && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0
                && player.getRandom().nextDouble() < energy.stumbleShare()) {
            body.stumbleTicks = energy.stumbleSeconds() * TICKS_A_SECOND;
            player.displayClientMessage(Component.translatable("message.backcountry.stumble"), true);
        }
        boolean tired = body.energy < energy.sprintBelow();
        if (tired && player.isSprinting()) {
            player.displayClientMessage(Component.translatable("message.backcountry.too_tired_to_sprint"), true);
        }
    }

    private static void blackOut(ServerPlayer player, Body body) {
        body.blackoutTicks = Balance.get().energy().blackoutSeconds() * TICKS_A_SECOND;
        player.displayClientMessage(Component.translatable("message.backcountry.blacked_out"), true);
        if (player.isSleeping()) {
            player.stopSleeping();
        }
    }

    private static void move(ServerPlayer player, Body body) {
        Vec3 now = player.position();
        if (body.lastPosition != null) {
            double blocks = Math.sqrt(Math.pow(now.x - body.lastPosition.x, 2) + Math.pow(now.z - body.lastPosition.z, 2));
            if (blocks < TELEPORT_BLOCKS && player.getVehicle() == null) {
                Balance.Work work = Balance.get().work();
                if (player.isInWater()) {
                    body.workKcal += blocks * work.swimKcalPerBlock();
                } else if (player.onGround()) {
                    body.workKcal += blocks * (player.isSprinting() ? work.sprintKcalPerBlock() : work.walkKcalPerBlock());
                    if (body.wetness >= Balance.get().conditions().blistersWetness()) {
                        body.wetBlocks += blocks;
                    }
                }
            }
        }
        body.lastPosition = now;
    }

    private static void hold(ServerPlayer player, Body body) {
        Balance.Energy energy = Balance.get().energy();
        boolean tired = body.energy < energy.sprintBelow() || body.has(Condition.BLISTERS) || body.has(Condition.FROSTBITE) || body.has(Condition.SEVERE_FROSTBITE);
        player.getFoodData().setFoodLevel(tired ? TIRED_FOOD : HELD_FOOD);
        player.getFoodData().setSaturation(0);
        player.getFoodData().setExhaustion(0);
        if (tired && player.isSprinting()) {
            player.setSprinting(false);
        }
        if (body.blackoutTicks > 0) {
            body.blackoutTicks--;
            if (body.blackoutTicks == 0) {
                body.energy = Math.max(body.energy, energy.wakeEnergy());
            }
        }
        if (body.stumbleTicks > 0) {
            body.stumbleTicks--;
        }
        boolean sinking = player.isInWater() && !player.onGround() && (body.blackoutTicks > 0 || body.energy < energy.stumbleBelow());
        if (sinking) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.min(motion.y, -SINK_PER_TICK), motion.z);
            player.hurtMarked = true;
        }
        modify(player.getAttribute(Attributes.BLOCK_BREAK_SPEED), SLOW_HANDS, body.energy < energy.slowBelow() ? -energy.slowSpeed() : 0);
        modify(player.getAttribute(Attributes.MOVEMENT_SPEED), STILL, body.blackoutTicks > 0 ? -1 : 0);
        modify(player.getAttribute(Attributes.JUMP_STRENGTH), STILL_JUMP, body.blackoutTicks > 0 ? -1 : 0);
        modify(player.getAttribute(Attributes.MOVEMENT_SPEED), STUMBLE, body.stumbleTicks > 0 ? -energy.slowSpeed() : 0);
    }

    public static void release(ServerPlayer player) {
        for (var attribute : List.of(Attributes.BLOCK_BREAK_SPEED, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                for (ResourceLocation id : List.of(SLOW_HANDS, STILL, STILL_JUMP, STUMBLE)) {
                    instance.removeModifier(id);
                }
            }
        }
    }

    private static void modify(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) {
            return;
        }
        if (amount == 0) {
            attribute.removeModifier(id);
        } else {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static boolean eat(ServerPlayer player, Nutrition food) {
        Body body = body(player);
        Balance balance = Balance.get();
        if (body.stomachKcal + food.kcal() > balance.body().stomachKcal() && body.stomachKcal > 0) {
            return false;
        }
        body.stomachKcal += food.kcal();
        body.stomachFat += food.fat();
        body.stomachProtein += food.protein();
        body.stomachCarbs += food.carbs();
        body.stomachVitaminC += food.vitaminC();
        body.kcalEaten += food.kcal();
        body.kcalEatenToday += food.kcal();
        body.fatToday += food.fat();
        body.proteinToday += food.protein();
        body.carbsToday += food.carbs();
        Team.get(player.server).ate(food.kcal());
        if (food.raw() && player.getRandom().nextDouble() < balance.conditions().parasitesChance()) {
            body.afflict(Condition.PARASITES);
        }
        if (food.poisoning() > 0 && player.getRandom().nextDouble() < food.poisoning()) {
            poison(body);
        }
        return true;
    }

    public static void poison(Body body) {
        body.afflict(Condition.FOOD_POISONING);
        body.stomachKcal = 0;
        body.stomachFat = 0;
        body.stomachProtein = 0;
        body.stomachCarbs = 0;
        body.stomachVitaminC = 0;
        body.waterL = Math.max(0, body.waterL - Balance.get().conditions().vomitL());
    }

    public static boolean thirsty(Player player) {
        Body body = body(player);
        return body.waterL < Balance.get().water().needLDay() - Balance.get().water().sipL() / 2;
    }

    public static void drink(ServerPlayer player, double litres, boolean boiled) {
        Body body = body(player);
        body.waterL = Math.min(Balance.get().water().needLDay(), body.waterL + litres);
        if (!boiled && player.getRandom().nextDouble() < Balance.get().conditions().gutInfectionChance()) {
            body.afflict(Condition.GUT_INFECTION);
        }
    }

    private static void work(Player player, double kcal) {
        if (player instanceof ServerPlayer server && lives(server)) {
            body(server).workKcal += kcal;
        }
    }

    @SubscribeEvent
    static void broke(BlockEvent.BreakEvent event) {
        Balance.Work work = Balance.get().work();
        var state = event.getState();
        work(event.getPlayer(), state.is(BlockTags.LOGS) ? work.chopKcal()
                : state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES) ? work.mineKcal() : work.digKcal());
    }

    @SubscribeEvent
    static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            work(player, Balance.get().work().placeKcal());
        }
    }

    @SubscribeEvent
    static void attacked(AttackEntityEvent event) {
        work(event.getEntity(), Balance.get().work().attackKcal());
    }

    @SubscribeEvent
    static void jumped(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            work(player, Balance.get().work().jumpKcal());
        }
    }

    @SubscribeEvent
    static void hurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !lives(player) || event.getNewDamage() <= 0) {
            return;
        }
        Body body = body(player);
        body.lastHurt = player.level().getGameTime();
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE)) {
            body.afflict(Condition.BURNS);
        } else if (hit(source) && player.getRandom().nextDouble() < Balance.get().conditions().infectionChance()) {
            body.afflict(Condition.INFECTED_WOUND);
        }
    }

    private static boolean hit(DamageSource source) {
        return source.getEntity() != null || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.FALLING_BLOCK);
    }

    @SubscribeEvent
    static void killed(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer hunter && lives(hunter) && !(event.getEntity() instanceof Player)
                && event.getEntity().getType().getCategory() != MobCategory.MONSTER && event.getEntity().getType().getCategory() != MobCategory.MISC) {
            String key = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
            body(hunter).animals.merge(key, 1, Integer::sum);
            Team.get(hunter.server).took(key);
        }
    }

    @SubscribeEvent
    static void startEating(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || event.getItem().getFoodProperties(player) == null) {
            return;
        }
        if (Nutrition.of(event.getItem()).isEmpty()) {
            event.setCanceled(true);
            return;
        }
        if (!player.level().isClientSide && player instanceof ServerPlayer server && lives(server)) {
            Body body = body(player);
            double kcal = Nutrition.of(event.getItem()).get().kcal();
            if (body.stomachKcal > 0 && body.stomachKcal + kcal > Balance.get().body().stomachKcal()) {
                player.displayClientMessage(Component.translatable("message.backcountry.too_full"), true);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    static void finishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !lives(player)) {
            return;
        }
        if (event.getItem().is(Items.POTION) && event.getItem().getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
            drink(player, Balance.get().water().sipL(), false);
            return;
        }
        Nutrition.of(event.getItem()).ifPresent(food -> eat(player, food));
        hold(player, body(player));
    }

    @SubscribeEvent
    static void stillHands(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && body(player).blackoutTicks > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void stillItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && body(player).blackoutTicks > 0) {
            event.setCanceled(true);
        }
    }

    public static List<Condition> shown(Body body) {
        List<Condition> shown = new ArrayList<>();
        for (Map.Entry<Condition, Body.Affliction> entry : body.conditions.entrySet()) {
            shown.add(entry.getKey());
        }
        shown.sort(null);
        return shown;
    }
}
