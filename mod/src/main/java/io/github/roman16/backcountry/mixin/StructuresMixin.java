package io.github.roman16.backcountry.mixin;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGeneratorStructureState.class)
abstract class StructuresMixin {
    @Shadow
    @Final
    @Mutable
    private List<Holder<StructureSet>> possibleStructureSets;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void backcountry$wildernessOnly(CallbackInfo info) {
        possibleStructureSets = List.of();
    }
}
