package de.erdbeerbaerlp.dcintegration.fabric.util;

import net.fabricmc.loader.api.FabricLoader;

public class CompatibilityUtils {
    /**
     * @return true if mod with id 'kiloessentials' is present.
     */
    public static boolean kiloEssentialsLoaded() {
        return FabricLoader.getInstance().isModLoaded("kilo_essentials");
    }

    /**
     * @return true if mod with id 'styledchat' is present.
     */
    public static boolean styledChatLoaded() {
       return FabricLoader.getInstance().isModLoaded("styledchat");
    }
}
