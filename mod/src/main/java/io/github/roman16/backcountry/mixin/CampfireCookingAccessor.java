package io.github.roman16.backcountry.mixin;

import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CampfireBlockEntity.class)
public interface CampfireCookingAccessor {
    @Accessor("cookingProgress")
    int[] backcountry$progress();

    @Accessor("cookingTime")
    int[] backcountry$time();
}
