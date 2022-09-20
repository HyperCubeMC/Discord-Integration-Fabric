package de.erdbeerbaerlp.dcintegration.fabric.util;

import com.google.common.collect.ImmutableMap;
import de.erdbeerbaerlp.dcintegration.fabric.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.fabric.mixin.TranslatableTextContentMixin;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.*;
import net.minecraft.util.Language;
import org.apache.logging.log4j.core.util.Closer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

// Adapted from https://github.com/LoganDark/fabric-languagehack/blob/master/src/main/java/net/logandark/languagehack/LanguageHacker.java under MIT
public class TranslationUtil {
    private static final AtomicBoolean ACTIVATED = new AtomicBoolean();

    private static String[] getChain() {
        return new String[]{"en_us"};
    }

    private static List<Path> findLangDirs(Path assets) throws IOException {
        List<Path> lang_dirs = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(assets)) {
            stream.forEach(path -> {
                Path lang_path = path.resolve("lang");

                if (Files.isDirectory(lang_path)) {
                    lang_dirs.add(lang_path);
                }
            });
        }

        return lang_dirs;
    }

    private static Map<String, String> loadTranslations(ModContainer container, String[] chain) {
        String modid = container.getMetadata().getId();
        Map<String, String> translations = new HashMap<>();

        List<Path> lang_dirs;

        Optional<Path> assetsPath = container.findPath("assets");

        if (assetsPath.isPresent()) {
            try {
                lang_dirs = findLangDirs(assetsPath.get());
                DiscordIntegration.LOGGER.debug("{}: Found {} lang directories to check", modid, lang_dirs.size());
            } catch (IOException e) {
                DiscordIntegration.LOGGER.warn("{}: Error scanning lang directories", modid);
                return translations;
            }

            for (Path dir : lang_dirs) {
                InputStream inputStream = null;

                for (String lang : chain) {
                    Path lang_path = dir.resolve(lang + ".json");

                    try {
                        inputStream = Files.newInputStream(lang_path);
                        DiscordIntegration.LOGGER.debug("{}: Found {} translations ({})", modid, lang, container.getRootPaths().get(0).relativize(lang_path));
                        break;
                    } catch (IOException e) {
                        if (!(e instanceof NoSuchFileException)) {
                            DiscordIntegration.LOGGER.error("{}: Error loading {} translations: {}", modid, lang, e);
                        }
                    }
                }

                if (inputStream == null) {
                    DiscordIntegration.LOGGER.error("{}: Couldn't load any translations, skipping", modid);
                    continue;
                }

                try {
                    Language.load(inputStream, translations::put);
                } finally {
                    Closer.closeSilently(inputStream);
                }
            }
        }

        if (translations.size() > 0) {
            DiscordIntegration.LOGGER.info("{}: Successfully loaded {} translations", modid, translations.size());
        } else {
            DiscordIntegration.LOGGER.debug("{}: Did not load any translations", modid);
        }

        return translations;
    }

    public static ImmutableMap<String, String> activate() {
        if (!ACTIVATED.compareAndSet(false, true)) {
            throw new IllegalStateException("Can't activate TranslationUtil twice!");
        }

        // The chain of languages to try. They will be tried in order and if a
        // language file cannot be loaded, the next one will be tried, until
        // there are no more to try.
        String[] chain = getChain();

        DiscordIntegration.LOGGER.info("Activating translation util! Language chain: {}", Arrays.deepToString(chain));

        // Don't use an ImmutableMap.Builder for this, ImmutableMap builders
        // throw an exception if there are any duplicate keys
        Map<String, String> translations = new HashMap<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            translations.putAll(loadTranslations(mod, chain));
        }

        return ImmutableMap.copyOf(translations);
    }

    public static Text translate(Text translatableText) {
        if (!(translatableText.getContent() instanceof TranslatableTextContent)) throw new IllegalArgumentException("translatableText contents must be of type TranslatableTextContent");

        ((TranslatableTextContentMixin) translatableText.getContent()).translationutil$updateTranslations();
        MutableText literalText = Text.literal("");
        literalText.setStyle(translatableText.getStyle());

        List<StringVisitable> translations = ((TranslatableTextContentMixin) translatableText.getContent()).translationutil$getTranslations();
        for (StringVisitable translation : translations) {
            Text text = translation instanceof Text
                    ? ((Text) translation).copy()
                    : Text.literal(translation.getString());

            literalText.append(text);
        }

        for (Text sibling : translatableText.getSiblings()) {
            literalText.append(sibling.copy());
        }

        return literalText;
    }
}
