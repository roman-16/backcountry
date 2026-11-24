package io.github.roman16.backcountry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.teamtea.eclipticseasons.api.constant.climate.ISnowTerm;
import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import com.teamtea.eclipticseasons.api.data.weather.special_effect.WeatherEffect;
import com.teamtea.eclipticseasons.common.core.biome.WeatherManager;
import io.github.roman16.backcountry.body.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WeatherManager.class, remap = false)
abstract class PrecipitationMixin {
    @WrapOperation(method = {"getRainOrSnow",
            "getPrecipitationAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/biome/Biome;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"},
            at = @At(value = "INVOKE",
                    target = "Lcom/teamtea/eclipticseasons/api/constant/climate/ISnowTerm;maySnow(Lcom/teamtea/eclipticseasons/api/constant/solar/SolarTerm;Lnet/minecraft/world/level/biome/Biome;Lnet/minecraft/core/BlockPos;Z)Z"))
    private static boolean backcountry$snowsWhereTheAirFreezes(ISnowTerm term, SolarTerm solarTerm, Biome biome, BlockPos pos, boolean server, Operation<Boolean> original,
            @Local(argsOnly = true) Level level) {
        return level == null ? original.call(term, solarTerm, biome, pos, server) : Climate.snows(level, biome, pos);
    }

    @WrapOperation(method = {"getRainOrSnow",
            "getPrecipitationAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/biome/Biome;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"},
            at = @At(value = "INVOKE",
                    target = "Lcom/teamtea/eclipticseasons/api/data/weather/special_effect/WeatherEffect;shouldChangePrecipitation(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/biome/Biome;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/biome/Biome$Precipitation;)Z"))
    private static boolean backcountry$keepsTheAirsChoice(WeatherEffect effect, Level level, Biome biome, BlockPos pos, boolean precipitation, Biome.Precipitation original,
            Operation<Boolean> call) {
        return false;
    }
}
