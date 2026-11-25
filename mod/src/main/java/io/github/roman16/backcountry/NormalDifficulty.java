package io.github.roman16.backcountry;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Backcountry.MODID)
public final class NormalDifficulty {
    private static final String DIFFICULTY_COMMAND = "difficulty";

    private NormalDifficulty() {}

    @SubscribeEvent
    static void started(ServerStartedEvent event) {
        event.getServer().setDifficulty(Difficulty.NORMAL, true);
        event.getServer().setDifficultyLocked(true);
    }

    @SubscribeEvent
    static void command(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
        if (context.getNodes().size() > 1 && context.getNodes().getFirst().getNode() instanceof LiteralCommandNode<CommandSourceStack> literal
                && literal.getLiteral().equals(DIFFICULTY_COMMAND)) {
            event.setCanceled(true);
            context.getSource().sendFailure(Component.translatable("message.backcountry.difficulty_normal"));
        }
    }
}
