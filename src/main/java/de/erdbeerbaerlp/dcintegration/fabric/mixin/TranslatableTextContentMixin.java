package de.erdbeerbaerlp.dcintegration.fabric.mixin;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(TranslatableTextContent.class)
public interface TranslatableTextContentMixin {
    @Invoker("updateTranslations")
    void translationutil$updateTranslations();

    @Accessor("translations")
    List<StringVisitable> translationutil$getTranslations();
}