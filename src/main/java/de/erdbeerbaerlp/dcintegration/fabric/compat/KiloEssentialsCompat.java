package de.erdbeerbaerlp.dcintegration.fabric.compat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.fabric.api.FabricDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.fabric.util.FabricMessageUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.kilocraft.essentials.api.KiloEssentials;
import org.kilocraft.essentials.api.ModConstants;
import org.kilocraft.essentials.api.text.ComponentText;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.events.ChatEvents;
import org.kilocraft.essentials.util.EssentialPermission;
import org.kilocraft.essentials.util.registry.RegistryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class KiloEssentialsCompat {
    private static ServerPlayerEntity sender;
    private static final List<String> flagged = new ArrayList<>();

    private static final String ITEM_REGEX = "\\[item\\]";

    public static void registerCompatHook() {
        ChatEvents.CHAT_MESSAGE.register((player, message, channel) -> {
            if (PlayerLinkController.getSettings(null, player.getUuid()).hideFromDiscord) {
                return;
            }
            sender = player;

            boolean shouldCensor = KiloConfig.main().chat().shouldCensor && !KiloEssentials.hasPermissionNode(sender.getCommandSource(), EssentialPermission.CHAT_BYPASS_CENSOR);
            TextComponent textComponent = Component.text().append(parse(message, shouldCensor)).build();
            Text text = ComponentText.toText(textComponent);

            if (discord_instance.callEvent((e) -> {
                if (e instanceof FabricDiscordEventHandler) {
                    return ((FabricDiscordEventHandler) e).onMcChatMessage(text, player);
                }
                return false;
            })) {
                return;
            }
            // Don't escape markdown, it's fine to allow it through
            String rawText = text.getString();
            final MessageEmbed embed = FabricMessageUtils.genItemStackEmbedIfAvailable(text);
            if (discord_instance != null) {
                TextChannel textChannel = discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID);
                if (textChannel == null) {
                    return;
                }
                discord_instance.sendMessage(FabricMessageUtils.formatPlayerName(player), player.getUuid().toString(), new DiscordMessage(embed, rawText, true), textChannel);
            }
        });
    }

    private static Component parse(final String input, boolean censor) {
        TextComponent.Builder builder = Component.text();
        if (input == null || input.equals("")) return builder.build();
        Matcher itemMatcher = Pattern.compile(ITEM_REGEX).matcher(input);
        if (censor) {
            for (String censored : ModConstants.getCensored()) {
                Matcher censoredMatcher = Pattern.compile("(?i)" + censored).matcher(input);
                if (censoredMatcher.find()) {
                    parseMatcher(builder, input, true, censoredMatcher, KiloEssentialsCompat::censoredComponent);
                    return builder.build();
                }
            }
        }
        if (itemMatcher.find() && hasPermission(EssentialPermission.CHAT_SHOW_ITEM)) {
            parseMatcher(builder, input, censor, itemMatcher, s -> itemComponent());
        } else {
            builder.append(ComponentText.of(input));
        }
        return builder.build();
    }

    private static void parseMatcher(TextComponent.Builder builder, String input, boolean censor, Matcher matcher, Function<String, Component> function) {
        String prefix = input.substring(0, Math.max(0, matcher.start()));
        String matched = matcher.group(0);
        String suffix = input.substring(Math.min(matcher.end(), input.length()));
        builder.append(parse(prefix, censor));
        builder.append(function.apply(matched));
        builder.append(parse(suffix, censor));
    }

    private static Component itemComponent() {
        TextComponent.Builder builder = Component.text();
        ItemStack itemStack = sender.getMainHandStack();
        NbtCompound tag = itemStack.getNbt();
        builder.append(Component.text("["))
                .append(ComponentText.toComponent(itemStack.getName()))
                .append(Component.text("]"));
        builder.style(
                style -> style
                        .hoverEvent(
                                HoverEvent.showItem(
                                        Key.key(RegistryUtils.toIdentifier(itemStack.getItem())), 1,
                                        BinaryTagHolder.of(tag == null ? new NbtCompound().toString() : tag.toString())
                                )
                        )
        );
        return builder.build();
    }

    private static Component censoredComponent(String input) {
        String censored = "*".repeat(input.length());
        flagged.add(input);
        return Component.text(censored).style(style ->
                style.hoverEvent(HoverEvent.showText(Component.text(input).color(NamedTextColor.GRAY)))
        );
    }

    private static boolean hasPermission(EssentialPermission permission) {
        return KiloEssentials.hasPermissionNode(sender.getCommandSource(), permission);
    }
}
