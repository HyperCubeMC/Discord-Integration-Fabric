package de.erdbeerbaerlp.dcintegration.fabric.mixin;

import com.google.common.collect.ImmutableMap;
import de.erdbeerbaerlp.dcintegration.fabric.util.TranslationUtil;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Adapted from https://github.com/LoganDark/fabric-languagehack/blob/master/src/main/java/net/logandark/languagehack/mixin/MixinLanguage.java under MIT
@Mixin(Language.class)
public abstract class MixinLanguage {
    @Redirect(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;",
                    remap = false
            )
    )
    private static ImmutableMap<String, String> translationutil$activate(ImmutableMap.Builder<String, String> builder) {
        // Just kind of let vanilla's builder fall into the garbage collector.
        //
        // We don't need it; Fabric provides a `minecraft` mod that will tell us
        // to load its localizations anyway.

        return TranslationUtil.activate();
    }
}