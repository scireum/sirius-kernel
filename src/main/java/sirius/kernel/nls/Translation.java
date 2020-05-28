/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Describes a translated property.
 * <p>
 * Used by {@link Babelfish} to manage all translations available.
 */
public class Translation {
    private boolean autocreated;
    private String key;
    private Map<String, String> translationTable = new TreeMap<>();

    /**
     * Creates a new translation, containing all native language values for the given key.
     *
     * @param key the key for which the translations are held
     */
    protected Translation(String key) {
        this.key = key;
    }

    /**
     * Returns <tt>true</tt> if the translation was auto created as it was missing in the .properties files.
     *
     * @return <tt>true</tt> if the translation is auto created, <tt>false</tt> otherwise
     */
    public boolean isAutocreated() {
        return autocreated;
    }

    /**
     * Sets the the autocreated flag.
     *
     * @param autocreated determines if the translation was automatically created by the system (true).
     */
    protected void setAutocreated(boolean autocreated) {
        this.autocreated = autocreated;
    }

    /**
     * Adds a translation for the given language.
     *
     * @param lang  a two-letter language code for which the given translation should be used
     * @param value the translation for the given language
     * @return the previous translation stored for the given language or <tt>null</tt> if there was none present.
     */
    public String addTranslation(String lang, String value) {
        return translationTable.put(lang, value);
    }

    /**
     * Returns the key for which the translation can be found
     *
     * @return the name of the key used to register the translation
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the translation for the given language
     *
     * @param lang     the language as two-letter code
     * @param fallback the fallback language as two-letter code
     * @return a translation in the requested language or the key if no translation was found
     */
    public String translate(@Nonnull String lang, @Nullable String fallback) {
        String result = translationTable.get(lang);
        if (result == null && fallback != null) {
            result = translationTable.get(fallback);
        }
        if (result == null) {
            return key;
        }
        return result;
    }

    /**
     * Returns the translation for the given language
     *
     * @param lang the language as two-letter code
     * @return a translation in the requested language or <tt>null</tt> if no translation was found
     */
    public String translateWithoutFallback(String lang) {
        return translationTable.get(lang);
    }

    /**
     * Determines if a translation for the given language is available
     *
     * @param lang the language as two-letter code
     * @return <tt>true</tt> if a translation for the given language exists, <tt>false</tt> otherwise
     */
    public boolean hasTranslation(String lang) {
        return translationTable.containsKey(lang);
    }
}
