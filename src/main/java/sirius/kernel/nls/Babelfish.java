/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import sirius.kernel.Classpath;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Internal translation engine used by {@link NLS}.
 * <p>
 * Loads all .properties files in the given {@link Classpath} which match the pattern
 * {@code name_lang.properties}. Where <tt>name</tt> is any string (must not contain "_") and <tt>lang</tt>
 * is a two letter language code.
 *
 * @see NLS
 */
public class Babelfish {

    /**
     * Logs WARN messages if translations are inconsistent (override each other).
     */
    protected static final Log LOG = Log.get("babelfish");

    /**
     * Since the translationMap is not thread-safe, modifying the map requires to acquire this lock
     */
    private final Lock translationsWriteLock = new ReentrantLock();

    /**
     * Contains all known translations
     */
    private Map<String, Translation> translationMap = new TreeMap<>();

    /**
     * Describes the pattern for .properties files of interest.
     */
    private static final Pattern PROPERTIES_FILE = Pattern.compile("(.*?)_([a-z]{2}).properties");

    /**
     * Contains a list of all loaded resource bundles. Once the framework is booted, this is passed to
     * the TimerService.addWatchedResource to reload changes from the development environment.
     */
    private final List<String> loadedResourceBundles = new ArrayList<>();

    private static final ResourceBundle.Control CONTROL = new NonCachingUTF8Control();

    /**
     * Enumerates all translations which key starts with the given prefix
     *
     * @param key the prefix with which the key must start
     * @return a stream of all translations matching the given filter
     */
    public Stream<Translation> getEntriesStartingWith(@Nonnull String key) {
        return translationMap.values().stream().filter(e -> e.getKey().startsWith(key));
    }

    /**
     * Enumerates all translations which have not been accessed yet.
     *
     * @return a list of all unused translations.
     */
    public Stream<Translation> getUnusedTranslations() {
        return translationMap.values().stream().filter(e -> !e.isUsed());
    }

    /**
     * Enumerates all translations which have been autocreated (for which a proper translation value was missing).
     *
     * @return a list of all translations which miss an actual value
     */
    public Stream<Translation> getAutocreatedTranslations() {
        return translationMap.values().stream().filter(Translation::isAutocreated);
    }

    /**
     * Retrieves the <tt>Translation</tt> for the given property.
     * <p>
     * If no matching entry is found, the given <tt>fallback</tt> is used. If still no match was found, either
     * a new one is create (if <tt>create</tt> is <tt>true</tt>) or <tt>false</tt> otherwise.
     *
     * @param property the property key for which a translation is required
     * @param fallback a fallback key, if no translation is found. May be <tt>null</tt>.
     * @param create   determines if a new <tt>Translation</tt> is created if non-existent
     * @return a <tt>Translation</tt> for the given property or fallback.
     * Returns <tt>null</tt> if no translation was found and <tt>create</tt> is false.
     */
    @SuppressWarnings("squid:S2583")
    @Explain("Double check for @Nonnull property as it is not enforced by the compiler")
    protected Translation get(@Nonnull String property, @Nullable String fallback, boolean create) {
        if (property == null) {
            throw new IllegalArgumentException("property");
        }

        Translation entry = translationMap.get(property);
        if (entry != null) {
            return entry;
        }

        return getWithFallback(property, fallback, create);
    }

    private Translation getWithFallback(@Nonnull String property, @Nullable String fallback, boolean create) {
        Translation entry = fallback != null ? translationMap.get(fallback) : null;
        if (entry == null && create) {
            entry = autocreateMissingEntry(property);
        }

        return entry;
    }

    private Translation autocreateMissingEntry(@Nonnull String property) {
        LOG.INFO("Non-existent translation: %s", property);
        Translation entry = new Translation(property);
        entry.setAutocreated(true);

        inLock(newTranslations -> {
            newTranslations.put(entry.getKey(), entry);
        });

        return entry;
    }

    /**
     * Returns a list of all loaded resource bundles.
     *
     * @return a list of all loaded resource bundles (.properties files)
     */
    public List<String> getLoadedResourceBundles() {
        return Collections.unmodifiableList(loadedResourceBundles);
    }

    private void inLock(Consumer<Map<String, Translation>> propertiesModifier) {
        translationsWriteLock.lock();
        try {
            Map<String, Translation> copy = new TreeMap<>(translationMap);
            propertiesModifier.accept(copy);
            translationMap = copy;
        } finally {
            translationsWriteLock.unlock();
        }
    }

    /**
     * Initializes the translation engine by using the given classpath.
     * <p>
     * Scans for all .properties files matching <tt>PROPERTIES_FILE</tt> and loads their contents.
     *
     * @param classpath the classpath used to discover all relevant properties files
     */
    protected void init(final Classpath classpath) {
        inLock(newTranslations -> {
            // "" is silently returned as "". This is the most memory efficient way
            // to ensure that this happens without any additional logging etc.
            newTranslations.put("", new Translation(""));

            loadProvidedProperties(classpath, newTranslations);
        });
    }

    private void loadProvidedProperties(Classpath classpath, Map<String, Translation> newTranslations) {
        // Load core translations.
        // Files loaded later in the process will overwrite translations added by earlier files.
        // The order is as follows:
        // 1. Load the regular files.
        // 2. Load the "product"-prefix files.
        // 3. Load the customizations files.

        List<Matcher> customizations = new ArrayList<>();
        List<Matcher> productFiles = new ArrayList<>();
        classpath.find(PROPERTIES_FILE).forEach(value -> {
            if (Sirius.isCustomizationResource(value.group())) {
                customizations.add(value);
            } else if (value.group().startsWith("product")) {
                productFiles.add(value);
            } else {
                loadMatchedResource(value, newTranslations);
            }
        });

        productFiles.forEach(value -> loadMatchedResource(value, newTranslations));

        // Sort configurations according to system config
        customizations.sort((a, b) -> {
            String confA = Sirius.getCustomizationName(a.group());
            String confB = Sirius.getCustomizationName(b.group());
            return Sirius.compareCustomizations(confA, confB);
        });

        // Apply translations in correct order
        customizations.forEach(value -> loadMatchedResource(value, newTranslations));
    }

    private void loadMatchedResource(Matcher value, Map<String, Translation> newTranslations) {
        LOG.FINE("Loading: %s", value.group());
        String baseName = value.group(1);
        String lang = value.group(2);
        importProperties(baseName, lang, newTranslations);
        loadedResourceBundles.add(value.group());
    }

    /**
     * Reloads the given properties file
     */
    protected void reloadBundle(String name) {
        LOG.FINE("Reloading: %s", name);
        Matcher m = PROPERTIES_FILE.matcher(name);
        if (m.matches()) {
            String baseName = m.group(1);
            String lang = m.group(2);
            inLock(newTranslations -> {
                importProperties(baseName, lang, newTranslations);
            });
        }
    }

    private void importProperties(String baseName, String lang, Map<String, Translation> newTranslations) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName + "_" + lang, CONTROL);
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            importProperty(newTranslations, lang, baseName, key, value);
        }
    }

    private void importProperty(Map<String, Translation> modifiableTranslationsCopy,
                                String lang,
                                String file,
                                String key,
                                String value) {
        Translation entry = modifiableTranslationsCopy.computeIfAbsent(key, Translation::new);
        entry.setAutocreated(false);

        String previous = entry.addTranslation(lang, value);
        if (shouldLogOverrideWarning(file, value, previous)) {
            Babelfish.LOG.WARN("Overriding translation for '%s' (%s) in language %s: '%s' --> '%s'",
                               key,
                               file,
                               lang,
                               previous,
                               value);
        }
    }

    private boolean shouldLogOverrideWarning(String file, String value, String previous) {
        if (previous == null) {
            return false;
        }
        if (Strings.areEqual(previous, value)) {
            return false;
        }
        if (Sirius.isCustomizationResource(file)) {
            return false;
        }
        return !"product".equals(file);
    }

    /**
     * Throws an exception if any unknown translations where encountered so far.
     * <p>
     * This is called by {@link sirius.kernel.Sirius#stop()} if the framework was initialized as test
     * environment. The idea is to break unit-tests if unknown translations are encountered.
     *
     * @throws sirius.kernel.health.HandledException if unknown translations where detected.
     */
    public void reportMissingTranslations() {
        String missing = translationMap.values()
                                       .stream()
                                       .filter(Translation::isAutocreated)
                                       .map(Translation::getKey)
                                       .collect(Collectors.joining(", "));
        if (Strings.isFilled(missing)) {
            throw Exceptions.handle().withSystemErrorMessage("Missing translations found: %s", missing).handle();
        }
    }
}
