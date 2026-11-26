package io.github.roman16.backcountry.client;

import io.github.roman16.backcountry.Backcountry;
import me.pieking1215.invmove.InvMove;
import me.pieking1215.invmove.module.ModuleImpl;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

final class InvMoveLink extends ModuleImpl {
    private static final String JEI = "jei";

    private InvMoveLink() {}

    static void register() {
        InvMove.registerModule(new InvMoveLink());
    }

    @Override
    public String getId() {
        return Backcountry.MODID;
    }

    @Override
    public Movement shouldAllowMovement(Screen screen) {
        return screen instanceof BowDrillScreen || screen instanceof SummaryScreen || ModList.get().isLoaded(JEI) && JeiLink.typing() ? Movement.FORCE_DISABLE : Movement.PASS;
    }
}
