package io.github.roman16.backcountry.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NearestAttackableTargetGoal.class)
public interface TargetTypeAccessor {
    @Accessor("targetType")
    Class<?> backcountry$targetType();
}
