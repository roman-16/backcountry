package io.github.roman16.backcountry.client;

import net.irisshaders.iris.api.v0.IrisApi;
import net.neoforged.fml.ModList;

public final class Shaders {
    private static final String IRIS = "iris";

    private Shaders() {}

    public static boolean drawing() {
        return ModList.get().isLoaded(IRIS) && IrisApi.getInstance().isShaderPackInUse();
    }
}
