package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.Backcountry;
import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.gear.Worn;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class Untamed {
    private static final ResourceLocation ORCAS_MIGHT = ResourceLocation.fromNamespaceAndPath("alexsmobs", "orcas_might");
    private static final ResourceLocation PORCUPINE = ResourceLocation.fromNamespaceAndPath("animalgarden_porcupine", "porcupine");

    private Untamed() {}

    @SubscribeEvent
    static void bred(BabyEntitySpawnEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void tamed(AnimalTameEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void milked(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Goat && event.getItemStack().is(Items.BUCKET)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    static void pricked(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (PORCUPINE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType())) && player.getMainHandItem().isEmpty() && Worn.HANDS.get(player).isEmpty()
                && !player.level().isClientSide) {
            player.hurt(player.damageSources().cactus(), (float) Balance.get().hunting().quillDamage());
            player.displayClientMessage(Component.translatable("message.backcountry.quills"), true);
        }
    }

    @SubscribeEvent
    static void blessed(MobEffectEvent.Applicable event) {
        if (ORCAS_MIGHT.equals(BuiltInRegistries.MOB_EFFECT.getKey(event.getEffectInstance().getEffect().value()))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
