package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Untamed {
    private static final ResourceLocation ORCAS_MIGHT = ResourceLocation.fromNamespaceAndPath("alexsmobs", "orcas_might");

    private Untamed() {}

    @SubscribeEvent
    static void bred(BabyEntitySpawnEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void blessed(MobEffectEvent.Applicable event) {
        if (ORCAS_MIGHT.equals(BuiltInRegistries.MOB_EFFECT.getKey(event.getEffectInstance().getEffect().value()))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
