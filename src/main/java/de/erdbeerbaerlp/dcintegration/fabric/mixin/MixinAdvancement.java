package de.erdbeerbaerlp.dcintegration.fabric.mixin;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.fabric.util.FabricMessageUtils;
import de.erdbeerbaerlp.dcintegration.fabric.util.TranslationUtil;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

@Mixin(PlayerAdvancementTracker.class)
public class MixinAdvancement {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/PlayerAdvancementTracker;updateDisplay(Lnet/minecraft/advancement/Advancement;)V"))
    public void advancement(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir){
        if (discord_instance == null) return;
        if (PlayerLinkController.getSettings(null, owner.getUuid()).hideFromDiscord) return;
            if (advancement != null && advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceToChat()) {
                Text advancementTitle = advancement.getDisplay().getTitle();
                Text advancementDescription = advancement.getDisplay().getDescription();

                discord_instance.sendMessage(Configuration.instance().localization.advancementMessage.replace("%player%",
                                Formatting.strip(FabricMessageUtils.formatPlayerName(owner)))
                        .replace("%name%",
                                Formatting.strip(advancementTitle instanceof TranslatableText ? TranslationUtil.translate((TranslatableText) advancementTitle).getString() : advancementTitle.getString()))
                        .replace("%desc%",
                                Formatting.strip(advancementDescription instanceof TranslatableText ? TranslationUtil.translate((TranslatableText) advancementDescription).getString() : advancementDescription.getString()))
                        .replace("\\n", "\n"));

            }
    }
}
