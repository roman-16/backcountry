package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.body.Metabolism;
import io.github.roman16.backcountry.mixin.TargetTypeAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Wolves {
    public static final ResourceLocation TIMBER_WOLF = ResourceLocation.fromNamespaceAndPath("cnc", "timber_wolf");
    private static final double WALK = 1.0;
    private static final double TROT = 1.25;

    private Wolves() {}

    @SubscribeEvent
    static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob wolf) || !BuiltInRegistries.ENTITY_TYPE.getKey(wolf.getType()).equals(TIMBER_WOLF)) {
            return;
        }
        wolf.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?> hunt && Player.class.isAssignableFrom(((TargetTypeAccessor) hunt).backcountry$targetType()));
        wolf.goalSelector.addGoal(1, new AvoidEntityGoal<>(wolf, Player.class, (float) Balance.get().hunting().wolfKeepOffBlocks(), WALK, TROT,
                player -> player instanceof ServerPlayer survivor && Metabolism.lives(survivor) && wolf.getTarget() != survivor));
    }
}
