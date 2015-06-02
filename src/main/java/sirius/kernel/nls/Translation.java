/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import com.google.common.collect.Maps;
import sirius.kernel.commons.Strings;

import java.util.Map;

/**
 * Describes a translated property.
 * <p>
 * Used by {@link Babelfish} to manage all translations available.
 */
public class Translation {
    private boolean autocreated;
    private String file;
    private String key;
    private boolean accessed;
    private Map<String, String> translationTable = Maps.newTreeMap();

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
     * Sets the name of the file in which the property is defined.
     *
     * @param file the name of the file in which the property is defined
     */
    protected void setFile(String file) {
        this.file = file;
    }

    /**
     * Returns the name of the file in which the property is defined.
     *
     * @return the name of the file in which the property is defined
     */
    public String getFile() {
        return file;
    }

    /**
     * Adds a translation for the given language.
     *
     * @param lang   a two-letter language code for which the given translation should be used
     * @param value  the translation for the given language
     * @param silent determines if a warning is issued if an existing translation is overridden. Setting
     *               silent to <tt>true</tt> will prevent such warnings and is used to load customer specific
     *               translations which purpose is ti override existing translations.
     */
    public void addTranslation(String lang, String value, boolean silent) {
        if (!silent && translationTable.containsKey(lang) && !Strings.areEqual(translationTable.get(lang), value)) {
            Babelfish.LOG.WARN("Overriding translation for '%s' in language %s: '%s' --> '%s'",
                               key,
                               lang,
                               translationTable.get(lang),
                               value);
        }
        translationTable.put(lang, value);
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
     * Determines whether the translation was already accessed
     *
     * @return <tt>true</tt> if the translation was already accessed, <tt>false</tt> otherwise
     */
    public boolean isAccessed() {
        return accessed;
    }

    /**
     * Returns the translation for the given language
     *
     * @param lang the language as two-letter code
     * @return a translation in the requested language or the key if no translation was found
     */
    public String translate(String lang) {
        this.accessed = true;
        String result = translationTable.get(lang);
        if (result == null) {
            result = translationTable.get(NLS.getDefaultLanguage());
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

    /**
     * Checks if the given filter is either contained in the key or in one of the translations
     *
     * @param effectiveFilter the filter to be applied. <tt>null</tt> indicates that no filtering should take place
     * @return <tt>true</tt> if the given filter was <tt>null</tt> or if it either occurred in the key
     * or in one of the translations.
     */
    protected boolean containsText(String effectiveFilter) {
        if (effectiveFilter == null) {
            return true;
        }
        if (key.toLowerCase().contains(effectiveFilter)) {
            return true;
        }
        for (String string : translationTable.values()) {
            if (string.toLowerCase().contains(effectiveFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes all translations into the given map
     *
     * @param propMap the target map with maps two-letter language codes to <tt>StoredProperties</tt>
     */
    protected void writeTo(Map<String, SortedProperties> propMap) {
        for (Map.Entry<String, String> entry : translationTable.entrySet()) {
            if (Strings.isFilled(entry.getValue())) {
                SortedProperties props = propMap.get(entry.getKey());
                if (props == null) {
                    props = new SortedProperties();
                    propMap.put(entry.getKey(), props);
                }
                props.setProperty(key, entry.getValue());
            }
        }
    }
}
