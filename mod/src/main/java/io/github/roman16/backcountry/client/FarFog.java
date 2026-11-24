package io.github.roman16.backcountry.client;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiFogRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;
import net.minecraft.util.Mth;

final class FarFog extends DhApiBeforeFogRenderEvent {
    private FarFog() {}

    static void bind() {
        DhApi.events.bind(DhApiBeforeFogRenderEvent.class, new FarFog());
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<EventParam> event) {
        float strength = Blizzard.strength();
        if (strength <= 0) {
            return;
        }
        DhApiFogRenderParam original = event.value.getOriginalFogRenderParam();
        DhApiMutableFogRenderParam fog = event.value.getFogRenderParam();
        fog.setFarFogStartPercent(Mth.lerp(strength, original.getFarFogStartPercent(), 0));
        fog.setFarFogEndPercent(Mth.lerp(strength, original.getFarFogEndPercent(), 0));
        fog.setFarFogMinThickness(Mth.lerp(strength, original.getFarFogMinThickness(), 1));
        fog.setFarFogMaxThickness(Mth.lerp(strength, original.getFarFogMaxThickness(), 1));
    }
}
