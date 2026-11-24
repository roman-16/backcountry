package io.github.roman16.backcountry.hunt;

import io.github.roman16.backcountry.body.Metabolism;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MooseCallItem extends Item {
    private static final ResourceLocation MOOSE = ResourceLocation.fromNamespaceAndPath("alexsmobs", "moose");
    private static final int COOLDOWN_TICKS = 2400;

    public MooseCallItem(Properties properties) {
        super(properties.stacksTo(1).durability(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(1).value(), SoundSource.PLAYERS, 1, 0.6F);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer survivor && Metabolism.lives(survivor)) {
            EntityType<?> moose = BuiltInRegistries.ENTITY_TYPE.get(MOOSE);
            boolean answered = Hunting.season(level, Animal.Season.RUT) && Animal.of(moose).map(animal -> Director.spawn(server, survivor, moose, animal) > 0).orElse(false);
            survivor.displayClientMessage(Component.translatable(answered ? "message.backcountry.moose_answers" : "message.backcountry.no_answer"), true);
            stack.hurtAndBreak(1, survivor, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
