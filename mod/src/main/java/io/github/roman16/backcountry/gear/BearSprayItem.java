package io.github.roman16.backcountry.gear;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public class BearSprayItem extends Item {
    private static final int TICKS_A_SECOND = 20;
    private static final int FLEE_EVERY_TICKS = 20;
    private static final int FLEE_BLOCKS = 16;
    private static final int FLEE_HEIGHT = 7;
    private static final double FLEE_SPEED = 1.4;
    private static final int COOLDOWN_TICKS = 10;
    private static final int CLOUD_STEPS = 8;

    public BearSprayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.6F);
        if (level instanceof ServerLevel server) {
            spray(server, player);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void spray(ServerLevel level, Player player) {
        Balance.BearSpray spray = Balance.get().bearSpray();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (int step = 1; step <= CLOUD_STEPS; step++) {
            Vec3 at = eye.add(look.scale(spray.reachBlocks() * step / CLOUD_STEPS));
            level.sendParticles(ParticleTypes.SPIT, at.x, at.y, at.z, 2, 0.1 * step / 2, 0.1 * step / 2, 0.1 * step / 2, 0.02);
        }
        double cosine = Math.cos(Math.toRadians(spray.coneDegrees() / 2));
        long until = level.getGameTime() + (long) spray.fleeSeconds() * TICKS_A_SECOND;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(spray.reachBlocks()), entity -> entity != player)) {
            Vec3 toward = target.getBoundingBox().getCenter().subtract(eye);
            if (toward.length() <= spray.reachBlocks() + target.getBbWidth() / 2 && toward.normalize().dot(look) >= cosine && player.hasLineOfSight(target)) {
                target.setData(Things.SPRAYED_UNTIL.get(), until);
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
        }
        var wall = level.clip(new ClipContext(eye, eye.add(look.scale(spray.pointBlankBlocks())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() == HitResult.Type.BLOCK) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, spray.blindSeconds() * TICKS_A_SECOND));
        }
    }

    private static boolean sprayed(LivingEntity entity) {
        return entity.getData(Things.SPRAYED_UNTIL.get()) > entity.level().getGameTime();
    }

    @SubscribeEvent
    static void flee(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob) || mob.level().isClientSide || !mob.hasData(Things.SPRAYED_UNTIL.get()) || !sprayed(mob)) {
            return;
        }
        mob.setTarget(null);
        if (mob.tickCount % FLEE_EVERY_TICKS == 0) {
            Player nearest = mob.level().getNearestPlayer(mob, FLEE_BLOCKS);
            Vec3 away = nearest == null ? null : DefaultRandomPos.getPosAway(mob, FLEE_BLOCKS, FLEE_HEIGHT, nearest.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
            }
        }
    }

    @SubscribeEvent
    static void noAttack(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasData(Things.SPRAYED_UNTIL.get()) && sprayed(attacker)) {
            event.setCanceled(true);
        }
    }
}
