package io.github.roman16.backcountry.mixin;

import com.mojang.authlib.GameProfile;
import io.github.roman16.backcountry.client.Binoculars;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin extends AbstractClientPlayer {
    LocalPlayerMixin(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Override
    public boolean isScoping() {
        return super.isScoping() || Binoculars.looking();
    }
}
