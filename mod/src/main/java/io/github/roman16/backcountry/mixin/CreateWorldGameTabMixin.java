package io.github.roman16.backcountry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
abstract class CreateWorldGameTabMixin extends GridLayoutTab {
    private static final String WITH_VALUES = "Lnet/minecraft/client/gui/components/CycleButton$Builder;withValues([Ljava/lang/Object;)Lnet/minecraft/client/gui/components/CycleButton$Builder;";

    CreateWorldGameTabMixin(Component title) {
        super(title);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", ordinal = 0, target = WITH_VALUES))
    private Object[] backcountry$withoutHardcore(Object[] modes) {
        return new Object[] {WorldCreationUiState.SelectedGameMode.SURVIVAL, WorldCreationUiState.SelectedGameMode.CREATIVE};
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", ordinal = 1, target = WITH_VALUES))
    private Object[] backcountry$normalOnly(Object[] difficulties) {
        return new Object[] {Difficulty.NORMAL};
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backcountry$lockDifficulty(CallbackInfo info) {
        if (!(Minecraft.getInstance().screen instanceof CreateWorldScreen screen)) {
            return;
        }
        layout.visitWidgets(widget -> {
            if (widget instanceof CycleButton<?> button && button.getValue() instanceof Difficulty) {
                screen.getUiState().addListener(state -> button.active = false);
            }
        });
    }
}
