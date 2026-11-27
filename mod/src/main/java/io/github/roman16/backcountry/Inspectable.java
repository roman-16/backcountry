package io.github.roman16.backcountry;

import java.util.List;
import net.minecraft.network.chat.Component;

public interface Inspectable {
    List<Component> inspect();
}
