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
import sirius.kernel.async.CallContext;
import sirius.kernel.async.ExecutionPoint;
import sirius.kernel.commons.AdvancedDateParser;
import sirius.kernel.commons.Amount;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.NumberFormat;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.di.Injector;
import sirius.kernel.health.Exceptions;
import sirius.kernel.timer.Timers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Native Language Support used by the framework.
 * <p>
 * This class provides a translation engine ({@link #get(String)}, {@link #safeGet(String, String, String)},
 * {@link #fmtr(String)}). It also provides access to the current language via {@link #getCurrentLang()} and to the
 * default language ({@link #getDefaultLanguage()}. Most of the methods come in two versions, one which accepts a
 * <tt>lang</tt> parameter and another which uses the currently active language.
 * <p>
 * Additionally this class provides conversion methods to and from <tt>String</tt>. The most prominent ones are
 * {@link #toUserString(Object)} and {@link #toMachineString(Object)} along with their equivalent parse methods.
 * Although some conversions, especially <tt>toMachineString</tt> or <tt>formatSize</tt> are not language dependent,
 * those are kept in this class, to keep all conversion methods together.
 * <p>
 * <tt>Babelfish</tt> is used as translation engine and responsible for loading all provided .properties files.
 * <p>
 * <b>Configuration</b>
 * <ul>
 * <li><b>nls.defaultLanguage:</b> Sets the two-letter code used as default language</li>
 * <li><b>nls.language:</b> Sets an array of two-letter codes which enumerate all supported languages</li>
 * </ul>
 *
 * @see Babelfish
 */

@SuppressWarnings("squid:S1192")
@Explain("String literales here have different semantics and are therefore duplicated.")
public class NLS {

    private static final Babelfish blubb = new Babelfish();

    private static String defaultLanguage;
    private static Set<String> supportedLanguages;

    private static final DateTimeFormatter MACHINE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter MACHINE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final DateTimeFormatter MACHINE_PARSE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm[:ss]", Locale.ENGLISH);
    private static final DateTimeFormatter MACHINE_FORMAT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
    private static final Map<String, DateTimeFormatter> fullDateTimeFormatters = new TreeMap<>();
    private static final Map<String, DateTimeFormatter> dateTimeFormatters = new TreeMap<>();
    private static final Map<String, DateTimeFormatter> dateFormatters = new TreeMap<>();
    private static final Map<String, DateTimeFormatter> shortDateFormatters = new TreeMap<>();
    private static final Map<String, DateTimeFormatter> timeFormatters = new TreeMap<>();
    private static final Map<String, DateTimeFormatter> parseTimeFormatters = new TreeMap<>();
    private static final Map<String, DateTimeFormatter> fullTimeFormatters = new TreeMap<>();

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private static final String[] UNITS = {"Bytes", "kB", "MB", "GB", "TB", "PB"};

    private NLS() {
    }

    /**
     * Returns the currently active language as two-letter code.
     *
     * @return a two-letter code of the currently active language, as defined in
     * {@link sirius.kernel.async.CallContext#getLang()}
     */
    @Nonnull
    public static String getCurrentLang() {
        return CallContext.getCurrent().getLang();
    }

    /**
     * Returns the two-letter code of the default language. Provided via the config in {@code nls.defaultLanguage}
     * <p>
     * If this is set to "auto" the default language will be the system language.
     *
     * @return the language code of the default language
     */
    @Nonnull
    public static String getDefaultLanguage() {
        if (defaultLanguage != null) {
            return defaultLanguage;
        }

        return determineDefaultLanguage();
    }

    private static String determineDefaultLanguage() {
        if (defaultLanguage == null && Sirius.getSettings() != null) {
            defaultLanguage = Sirius.getSettings().getString("nls.defaultLanguage").toLowerCase();
            if ("auto".equals(defaultLanguage)) {
                defaultLanguage = getSystemLanguage();
            }
        }
        // Returns the default language or (for very early access we default to en)
        return defaultLanguage == null ? "en" : defaultLanguage;
    }

    /**
     * Returns the two-letter code of the fall back language. Provided via the {@link CallContext}. If the value is
     * empty, {@link NLS#getDefaultLanguage} is returned.
     *
     * @return the language code of the fallback language
     */
    @Nonnull
    public static String getFallbackLanguage() {
        String fallback = CallContext.getCurrent().getFallbackLang();
        if (Strings.isEmpty(fallback)) {
            return getDefaultLanguage();
        }

        return fallback;
    }

    /**
     * Overrides the default language as defined in the configuration ({@code nls.defaultLanguage}).
     * <p>
     * This can be used to enforce the system language. However, setting nls.defaultLanguage to 'auto' is
     * the recommended approach.
     *
     * @param lang the two letter language code to use as default language.
     * @see #getSystemLanguage()
     */
    public static void setDefaultLanguage(String lang) {
        defaultLanguage = lang;
    }

    /**
     * Returns the two-letter code of the system language.
     * <p>
     * By default, SIRIUS initializes with the language set in {@code nls.defaultLanguage} so a switchover
     * to the system language has to be performed manually.
     *
     * @return the language code of the underlying operating system. If the language is not supported (not listed
     * in {@code nls.languages}), <tt>null</tt> will be returned as
     * {@link sirius.kernel.async.CallContext#setLang(String)} doesn't change the current language if <tt>null</tt> is
     * passed in.
     */
    @Nullable
    public static String getSystemLanguage() {
        return makeLang(Locale.getDefault().getLanguage().toLowerCase());
    }

    /**
     * Returns a list of two-letter codes enumerating all supported languages. Provided via the config in
     * {@code nls.languages}
     *
     * @return a list of supported language codes
     */
    public static Set<String> getSupportedLanguages() {
        if (supportedLanguages == null && Sirius.getSettings() != null) {
            try {
                supportedLanguages = Sirius.getSettings()
                                           .getStringList("nls.languages")
                                           .stream()
                                           .map(String::toLowerCase)
                                           .collect(Collectors.toCollection(LinkedHashSet::new));
            } catch (Exception e) {
                Exceptions.handle(e);
            }
        }
        // Returns the default language or (for very early access we default to en)
        return supportedLanguages == null ?
               Collections.singleton("en") :
               Collections.unmodifiableSet(supportedLanguages);
    }

    /**
     * Determines if the given language code is supported or not.
     *
     * @param twoLetterLanguageCode the language as two-letter code
     * @return <tt>true</tt> if the language is listed in <tt>nls.languages</tt>, <tt>false</tt> otherwise.
     */
    public static boolean isSupportedLanguage(String twoLetterLanguageCode) {
        return getSupportedLanguages().contains(twoLetterLanguageCode);
    }

    /**
     * Checks if the given language is supproted. Returns the default language otherwise.
     * <p>
     * Note that if the given lang is empty or <tt>null</tt>, this method will also return <tt>null</tt> as a call
     * to {@link sirius.kernel.async.CallContext#setLang(String)} with <tt>null</tt> as parameter won't change
     * the language at all.
     *
     * @param lang the language to check
     * @return <tt>lang</tt> if it was a supported language or the defaultLanguage otherwise, unless an empty string
     * was passed in, in which case <tt>null</tt> is returned.
     */
    @Nullable
    public static String makeLang(@Nullable String lang) {
        if (Strings.isEmpty(lang)) {
            return null;
        }
        String langAsLowerCase = lang.toLowerCase();
        if (getSupportedLanguages().contains(langAsLowerCase)) {
            return langAsLowerCase;
        } else {
            return getDefaultLanguage();
        }
    }

    /**
     * Initializes the engine based on the given classpath
     *
     * @param classpath the classpath used to discover all .properties files
     */
    public static void init(Classpath classpath) {
        blubb.init(classpath);
    }

    /**
     * Start the monitoring of resources in development environments.
     *
     * @param classpath the classpath used to resolve .properties files
     */
    public static void startMonitoring(Classpath classpath) {
        Timers timer = Injector.context().getPart(Timers.class);
        for (String name : blubb.getLoadedResourceBundles()) {
            URL resource = classpath.getLoader().getResource(name);
            if ("file".equals(resource.getProtocol()) && !resource.getFile().contains("!")) {
                timer.addWatchedResource(resource, () -> blubb.reloadBundle(name));
            }
        }
    }

    /**
     * Provides direct access to the translation engine to supply new properties or inspect current ones.
     *
     * @return the internally used translation engine
     */
    public static Babelfish getTranslationEngine() {
        return blubb;
    }

    /**
     * Returns a translated text for the given <tt>property</tt> and the currently active language.
     * <p>
     * If no translation is found, the translation for the default language is used. If still no translation is
     * found, the property itself is returned.
     *
     * @param property the key for which a translation is requested
     * @return a translated string for the current language (or for the default language if no translation was found)
     * or the property itself if no translation for neither of both languages is available.
     */
    public static String get(@Nonnull String property) {
        return blubb.get(property, null, true).translate(getCurrentLang(), getFallbackLanguage());
    }

    /**
     * Returns a translated text for the given property in the given language.
     * <p>
     * The same fallback rules as for {@link #get(String)} apply.
     *
     * @param property the key for which a translation is requested
     * @param lang     a two-letter language code for which the translation is requested
     * @return a translated string in the requested language or a fallback value if no translation was found
     */
    @SuppressWarnings("squid:S2637")
    @Explain("Strings.isEmpty checks for null on lang")
    public static String get(@Nonnull String property, @Nullable String lang) {
        return blubb.get(property, null, true)
                    .translate(Strings.isEmpty(lang) ? getCurrentLang() : lang, getFallbackLanguage());
    }

    /**
     * Returns one of two or three versions of a translation based on the given numeric for the current language.
     *
     * @param property the property to fetch
     * @param numeric  the numeric used to determine which version to use
     * @return the version of the given property in the current language based on the given numeric
     * @see #get(String, int, String)
     */
    public static String get(@Nonnull String property, int numeric) {
        return get(property, numeric, null);
    }

    /**
     * Returns one of two or three versions of a translation based on the given numeric.
     * <p>
     * The property has to be defined like:
     * <pre>
     * <tt>property.key=Version for 0|Version for 1|Version for many</tt>
     * </pre>
     * <p>
     * Alternatively, only two versions can be given:
     * <pre>
     * <tt>property.key=Version for 1|Version for 0 or many</tt>
     * </pre>
     * <p>
     * Based on the given <tt>numeric</tt> the right version will be chosen.
     *
     * @param property the property to fetch
     * @param numeric  the numeric used to determine which version to use
     * @param lang     the language to translate for
     * @return the version of the given property in the given language based on the given numeric
     */
    public static String get(@Nonnull String property, int numeric, @Nullable String lang) {
        String value = get(property, lang);
        String[] versions = value.split("\\|");

        if (versions.length == 1) {
            Babelfish.LOG.WARN(
                    "A numeric translation was accessed which doesn't provide any versions: %s, Lang: %s, Value: %s\n%s",
                    property,
                    lang,
                    value,
                    ExecutionPoint.snapshot());

            return value;
        }

        if (numeric == 0) {
            if (versions.length == 3) {
                return versions[0].trim();
            } else {
                return Formatter.create(versions[1].trim()).set("count", 0).format();
            }
        } else if (numeric == 1) {
            if (versions.length == 3) {
                return versions[1].trim();
            } else {
                return versions[0].trim();
            }
        } else {
            if (versions.length == 3) {
                return Formatter.create(versions[2].trim()).set("count", numeric).format();
            } else {
                return Formatter.create(versions[1].trim()).set("count", numeric).format();
            }
        }
    }

    /**
     * Returns a translated text for the given <tt>property</tt> in the given language
     * or <tt>null</tt> if no translation was found.
     * <p>
     * The same fallback rules as for {@link #get(String, String)} apply. However, if no translation
     *
     * @param property the key for which a translation is requested
     * @param lang     a two-letter language code for which the translation is requested
     * @return a translated text in the requested language (or in the default language if no translation for the given
     * language was found). Returns an empty optional if no translation for this property exists at all.
     */
    @SuppressWarnings("squid:S2637")
    @Explain("Strings.isEmpty checks for null on lang")
    public static Optional<String> getIfExists(String property, @Nullable String lang) {
        if (Strings.isEmpty(property)) {
            return Optional.empty();
        }

        Translation translation = blubb.get(property, null, false);
        if (translation == null) {
            return Optional.empty();
        }
        return Optional.of(translation.translate(Strings.isEmpty(lang) ? getCurrentLang() : lang,
                                                 getFallbackLanguage()));
    }

    /**
     * Returns a translated text for the given <tt>property</tt> or for the given <tt>fallback</tt>, if no translation
     * for <tt>property</tt> was found.
     *
     * @param property the primary key for which a translation is requested
     * @param fallback the fallback key for which a translation is requested
     * @param lang     a two-letter language code for which the translation is requested
     * @return a translated text in the requested language for the given property, or for the given fallback. If either
     * of both doesn't provide a translation for the given language, the translation for the default
     * language is returned. If neither of both keys exist <tt>property</tt> will be returned.
     */
    @SuppressWarnings("squid:S2637")
    @Explain("Strings.isEmpty checks for null on lang")
    public static String safeGet(@Nonnull String property, @Nonnull String fallback, @Nullable String lang) {
        return blubb.get(property, fallback, true)
                    .translate(Strings.isEmpty(lang) ? getCurrentLang() : lang, getFallbackLanguage());
    }

    /**
     * Returns a translated text for the given <tt>property</tt> or for the given <tt>fallback</tt>, if no translation
     * for <tt>property</tt> was found.
     *
     * @param property the primary key for which a translation is requested
     * @param fallback the fallback key for which a translation is requested
     * @return a translated text in the current language for the given property, or for the given fallback. If either
     * of both doesn't provide a translation for the given language, the translation for the default
     * language is returned. If neither of both keys exist <tt>property</tt> will be returned.
     */
    public static String safeGet(@Nonnull String property, @Nonnull String fallback) {
        return blubb.get(property, fallback, true).translate(getCurrentLang(), getFallbackLanguage());
    }

    /**
     * Translates the given string if it starts with a $ sign.
     *
     * @param keyOrString the string to translate if it starts with a $ sign
     * @return the translated string or the original string if it doesn't start with a $ sign or if no matching
     * translation was found
     */
    public static String smartGet(@Nonnull String keyOrString) {
        return smartGet(keyOrString, null);
    }

    /**
     * Translates the given string if it starts with a $ sign.
     *
     * @param keyOrString the string to translate if it starts with a $ sign
     * @param lang        a two-letter language code for which the translation is requested
     * @return the translated string or the original string if it doesn't start with a $ sign or if no matching
     * translation was found
     */
    @SuppressWarnings("squid:S2583")
    @Explain("Duplicate null check as predicate is not enforced by the compiler")
    public static String smartGet(@Nonnull String keyOrString, @Nullable String lang) {
        if (keyOrString == null) {
            return keyOrString;
        }

        if (keyOrString.length() > 2 && keyOrString.charAt(0) == '$' && keyOrString.charAt(1) != '{') {
            return NLS.getIfExists(keyOrString.substring(1), lang).orElseGet(() -> keyOrString.substring(1));
        }

        return keyOrString;
    }

    /**
     * Creates a formatted using the pattern supplied by the translation value for the given <tt>property</tt>.
     *
     * @param property the property to used to retrieve a translated pattern
     * @return a <tt>Formatter</tt> initialized with the translated text of the given property
     */
    public static Formatter fmtr(@Nonnull String property) {
        return Formatter.create(get(property), getCurrentLang());
    }

    /**
     * Creates a formatted using the pattern supplied by the translation value for the given <tt>property</tt>.
     * smasmassm
     *
     * @param property the property to used to retrieve a translated pattern
     * @param lang     a two-letter language code for which the translation is requested
     * @return a <tt>Formatter</tt> initialized with the translated text of the given property
     */
    public static Formatter fmtr(@Nonnull String property, @Nullable String lang) {
        return Formatter.create(get(property, lang), getCurrentLang());
    }

    /**
     * Marks a string as deliberately not translated.
     * <p>
     * Can be used to signal that a string needs no internationalization as it is only used on rare cases etc.
     *
     * @param s the text which will be used as output
     * @return the given value for s
     */
    public static String nonNLS(String s) {
        return s;
    }

    /**
     * Provides access to commonly used keys.
     */
    public enum CommonKeys {

        YES, NO, OK, CANCEL, NAME, EDIT, DELETE, SEARCH, SEARCHKEY, REFRESH, CLOSE, DESCRIPTION, SAVE, NEW, BACK,
        FILTER, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, JANUARY, FEBRUARY, MARCH, APRIL, MAY,
        JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

        /**
         * Returns the fully qualified key to retrieve the translation
         *
         * @return the fully qualified key which can be supplied to <tt>NLS.get</tt>
         */
        public String key() {
            return "NLS." + name().toLowerCase();
        }

        /**
         * Returns the translation for this key in the current language.
         *
         * @return the translation for this key
         */
        public String translated() {
            return get(key());
        }
    }

    /**
     * Converts a given integer ({@code Calendar.Monday...Calendar.Sunday}) into textual their representation.
     *
     * @param day the weekday to be translated. Use constants {@link Calendar#MONDAY} etc.
     * @return the name of the given weekday in the current language
     * or {@code ""} if an invalid index was given
     */
    public static String getDayOfWeek(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return CommonKeys.MONDAY.translated();
            case Calendar.TUESDAY:
                return CommonKeys.TUESDAY.translated();
            case Calendar.WEDNESDAY:
                return CommonKeys.WEDNESDAY.translated();
            case Calendar.THURSDAY:
                return CommonKeys.THURSDAY.translated();
            case Calendar.FRIDAY:
                return CommonKeys.FRIDAY.translated();
            case Calendar.SATURDAY:
                return CommonKeys.SATURDAY.translated();
            case Calendar.SUNDAY:
                return CommonKeys.SUNDAY.translated();
            default:
                return "";
        }
    }

    /**
     * Returns a two letter abbreviation of the name of the given day, like {@code "Mo"}.
     *
     * @param day the weekday to be translated. Use constants {@link Calendar#MONDAY} etc.
     * @return returns the first two letters of the name
     * or {@code ""} if the given index was invalid.
     */
    public static String getDayOfWeekShort(int day) {
        return Value.of(getDayOfWeek(day)).substring(0, 2);
    }

    /**
     * Returns the name of the given month in the current language
     *
     * @param month the month which name is requested (1..12)
     * @return the name of the given month translated in the current language
     * or <tt>""</tt> if an invalid index was given
     */
    public static String getMonthName(int month) {
        switch (month) {
            case 1:
                return CommonKeys.JANUARY.translated();
            case 2:
                return CommonKeys.FEBRUARY.translated();
            case 3:
                return CommonKeys.MARCH.translated();
            case 4:
                return CommonKeys.APRIL.translated();
            case 5:
                return CommonKeys.MAY.translated();
            case 6:
                return CommonKeys.JUNE.translated();
            case 7:
                return CommonKeys.JULY.translated();
            case 8:
                return CommonKeys.AUGUST.translated();
            case 9:
                return CommonKeys.SEPTEMBER.translated();
            case 10:
                return CommonKeys.OCTOBER.translated();
            case 11:
                return CommonKeys.NOVEMBER.translated();
            case 12:
                return CommonKeys.DECEMBER.translated();
            default:
                return "";
        }
    }

    /**
     * Returns a three letter abbreviation of the name of the given month, like <tt>"Jan"</tt>.
     *
     * @param month the month to be translated (January is 1, December is 12).
     * @return returns the first three letters of the name
     * or <tt>""</tt> if the given index was invalid.
     */
    public static String getMonthNameShort(int month) {
        return getMonthNameShort(month, "");
    }

    /**
     * Returns a three letter abbreviation of the name of the given month, like <tt>"Jan"</tt>.
     * If the name is short and has at most 4 characters, the name of the given month is returned instead.
     * The given symbol is only appended if the month was abbreviated so for example you get <tt>"Jan."</tt>
     * but with <tt>"May"</tt> the symbol String is not appended.
     *
     * @param month  the month to be translated (January is 1, December is 12).
     * @param symbol the symbol to append in case of abbreviation
     * @return returns the first three letters of the name, the name of the month if short enough
     * or <tt>""</tt> if the given index was invalid.
     */
    public static String getMonthNameShort(int month, String symbol) {
        String result = getMonthName(month);
        if (result.length() > 4) {
            result = result.substring(0, 3) + symbol;
        }
        return result;
    }

    /**
     * Returns the date format for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getDateFormat(String lang) {
        return dateFormatters.computeIfAbsent(lang, l -> DateTimeFormatter.ofPattern(get("NLS.patternDate", l)));
    }

    /**
     * Returns the short date format (two digit year like 24.10.14) for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getShortDateFormat(String lang) {
        return shortDateFormatters.computeIfAbsent(lang,
                                                   l -> DateTimeFormatter.ofPattern(get("NLS.patternShortDate", l)));
    }

    /**
     * Returns the full time format (with seconds) for the given language.
     * <p>
     * This should be used to format dates (times). Use {@link #getTimeParseFormat(String)} to parse strings
     * as it is more reluctant (or use {@link #parseUserString(Class, String)}).
     * <p>
     * The pattern in this case will conform to the PHP 5 patterns as these are used by some JavaScript
     * libraries like jQuery timepicker. (See http://php.net/manual/en/function.date.php).
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getTimeFormatWithSeconds(String lang) {
        return fullTimeFormatters.computeIfAbsent(lang,
                                                  l -> DateTimeFormatter.ofPattern(get("NLS.patternFullTime", l)));
    }

    /**
     * Returns the time format (without seconds) for the given language.
     * <p>
     * This should be used to format dates (times). Use {@link #getTimeParseFormat(String)} to parse strings
     * as it is more reluctant (or use {@link #parseUserString(Class, String)}).
     * </p>
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getTimeFormat(String lang) {
        return timeFormatters.computeIfAbsent(lang, l -> DateTimeFormatter.ofPattern(get("NLS.patternTime", l)));
    }

    /**
     * Returns the time format which is intended to parse time value in the given language.
     * <p>
     * In contrast to {@link #getTimeFormat(String)} and {@link #getTimeFormatWithSeconds(String)}
     * this is used to parse dates and is more reluctant when it comes to formatting (parses '9:00' whereas
     * <tt>getTimeFormat(String)</tt> only accepts '09:00').
     * </p>
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getTimeParseFormat(String lang) {
        return parseTimeFormatters.computeIfAbsent(lang,
                                                   l -> DateTimeFormatter.ofPattern(get("NLS.patternParseTime", l)));
    }

    /**
     * Returns the date and time format (with seconds) for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getDateTimeFormat(String lang) {
        return fullDateTimeFormatters.computeIfAbsent(lang,
                                                      l -> DateTimeFormatter.ofPattern(get("NLS.patternDateTime", l)));
    }

    /**
     * Returns the date and time format (without seconds) for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static DateTimeFormatter getDateTimeFormatWithoutSeconds(String lang) {
        return dateTimeFormatters.computeIfAbsent(lang,
                                                  l -> DateTimeFormatter.ofPattern(get(
                                                          "NLS.patternDateTime.withoutSeconds",
                                                          l)));
    }

    /**
     * Returns the format for the current language to format decimal numbers
     *
     * @return a format initialized with the pattern described by the current language
     */
    public static java.text.NumberFormat getDecimalFormat() {
        return getDecimalFormat(getCurrentLang());
    }

    /**
     * Returns the format for the given language to format decimal numbers
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static java.text.NumberFormat getDecimalFormat(String lang) {
        return new DecimalFormat(get("NLS.patternDecimal", lang), getDecimalFormatSymbols(lang));
    }

    /**
     * Returns the decimal format symbols for the current language
     *
     * @return the decimal format symbols like thousands separator or decimal separator
     * as described by the current language
     */
    public static DecimalFormatSymbols getDecimalFormatSymbols() {
        return getDecimalFormatSymbols(getCurrentLang());
    }

    /**
     * Returns the decimal format symbols for the given language
     *
     * @param lang the two-letter code of the language for which the decimal format symbols should be returned
     * @return the decimal format symbols like thousands separator or decimal separator
     * as described by the given language
     */
    public static DecimalFormatSymbols getDecimalFormatSymbols(String lang) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator(get("NLS.groupingSeparator", lang).charAt(0));
        sym.setDecimalSeparator(get("NLS.decimalSeparator", lang).charAt(0));
        return sym;
    }

    /**
     * Creates a new decimal format symbols instance which is independent of the current language or locale and
     * constantly set to use '.' as decimal separator with no grouping separator.
     * <p>
     * This is commonly used to exchange numbers between machines.
     *
     * @return a decimal format symbols instance used for formatting numbers as "machine" strings
     */
    public static DecimalFormatSymbols getMachineFormatSymbols() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setDecimalSeparator('.');
        return sym;
    }

    /**
     * Formats the given data in a language independent format.
     *
     * @param data the input data which should be converted to string
     * @return string representation of the given object, which can be parsed by
     * {@link #parseMachineString(Class, String)} independently of the language settings
     */
    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
    @Explain("The high complexity as acceptable as it is basically just a list of if statements")
    public static String toMachineString(Object data) {
        if (data == null) {
            return "";
        }
        if (data instanceof String) {
            return ((String) data).trim();
        }
        if (data instanceof Boolean) {
            return data.toString();
        }
        if (data instanceof Temporal) {
            // Convert Instant to LocalDateTime to permit a "normal" time format
            if (data instanceof Instant) {
                data = LocalDateTime.ofInstant((Instant) data, ZoneId.systemDefault());
            }
            Temporal temporal = (Temporal) data;
            if (ChronoUnit.HOURS.isSupportedBy(temporal)) {
                if (!ChronoField.DAY_OF_MONTH.isSupportedBy(temporal)) {
                    return MACHINE_FORMAT_TIME_FORMAT.format(temporal);
                } else {
                    return MACHINE_DATE_TIME_FORMAT.format(temporal);
                }
            } else {
                return MACHINE_DATE_FORMAT.format(temporal);
            }
        }
        if (data instanceof Integer) {
            return String.valueOf(data);
        }
        if (data instanceof Long) {
            return String.valueOf(data);
        }
        if (data instanceof Amount) {
            return ((Amount) data).toString(NumberFormat.MACHINE_TWO_DECIMAL_PLACES).asString();
        }
        if (data instanceof BigDecimal) {
            return ((BigDecimal) data).toPlainString();
        }
        if (data instanceof Double) {
            return String.valueOf(data);
        }
        if (data.getClass().isEnum()) {
            return ((Enum<?>) data).name();
        }
        if (data instanceof Float) {
            return String.valueOf(data);
        }
        if (data instanceof Throwable) {
            return writeThreadStrace((Throwable) data);
        }
        return String.valueOf(data);
    }

    private static String writeThreadStrace(Throwable data) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        data.printStackTrace(pw);
        String result = writer.toString();
        pw.close();
        return result;
    }

    /**
     * Formats the given data according to the format rules of the current language
     *
     * @param object the object to be converted to a string
     * @return a string representation of the given object, formatted by the language settings of the current language
     */
    public static String toUserString(Object object) {
        return toUserString(object, getCurrentLang());
    }

    /**
     * Formats the given data according to the format rules of the given language
     *
     * @param data the object to be converted to a string
     * @param lang a two-letter language code for which the translation is requested
     * @return a string representation of the given object, formatted by the language settings of the current language
     */
    @SuppressWarnings("squid:S3776")
    @Explain("The high complexity as acceptable as it is basically just a list of if statements")
    public static String toUserString(Object data, String lang) {
        if (data == null) {
            return "";
        }
        if (data instanceof String) {
            return ((String) data).trim();
        }
        if (data instanceof Boolean) {
            if (((Boolean) data).booleanValue()) {
                return NLS.get(CommonKeys.YES.key(), lang);
            } else {
                return NLS.get(CommonKeys.NO.key(), lang);
            }
        }
        if (data instanceof Temporal) {
            // Convert Instant to LocalDateTime to permit a "normal" time format
            if (data instanceof Instant) {
                data = LocalDateTime.ofInstant((Instant) data, ZoneId.systemDefault());
            }
            Temporal temporal = (Temporal) data;
            if (ChronoUnit.HOURS.isSupportedBy(temporal)) {
                if (!ChronoField.DAY_OF_MONTH.isSupportedBy(temporal)) {
                    return getTimeFormatWithSeconds(lang).format(temporal);
                } else {
                    return getDateTimeFormat(lang).format(temporal);
                }
            } else {
                return getDateFormat(lang).format(temporal);
            }
        }
        if (data instanceof Integer) {
            return String.valueOf(data);
        }
        if (data instanceof Long) {
            return String.valueOf(data);
        }
        if (data instanceof BigDecimal) {
            return getDecimalFormat(lang).format(((BigDecimal) data).doubleValue());
        }
        if (data instanceof Double) {
            return getDecimalFormat(lang).format(data);
        }
        if (data instanceof Float) {
            return getDecimalFormat(lang).format(data);
        }
        if (data instanceof Throwable) {
            return writeThreadStrace((Throwable) data);
        }
        return String.valueOf(data);
    }

    /**
     * Converts dates to a "human" (e.g. "today", "yesterday") format.
     * <p>
     * The following texts are supported:
     * <ul>
     * <li>If the given date contains a time and is less than 30 min ago: "some minutes ago"</li>
     * <li>If the given date contains a time and is less than 60 min ago: "N minutes ago"</li>
     * <li>If the given date contains a time and is less than 2 hours ago: "one hour ago"</li>
     * <li>If the given date contains a time and is less than 6 hours ago: "N hours ago"</li>
     * <li>If the given date is today: "today"</li>
     * <li>If the given date is tomorrow: "tomorrow"</li>
     * <li>If the given date is yesterday: "yesterday"</li>
     * <li>For everything else we use the date format (note that in this case the time is omitted</li>
     * </ul>
     *
     * @param date the date to be formatted
     * @return a date string which a human would use in common sentences
     */
    public static String toSpokenDate(Temporal date) {
        if (date == null) {
            return "";
        }

        if (ChronoUnit.HOURS.isSupportedBy(date)) {
            return formatSpokenDateWithTime(date);
        } else {
            return formatSpokenDate(date);
        }
    }

    private static String formatSpokenDate(Temporal date) {
        // Check if we have a date which is not "today"....
        LocalDate givenDate = LocalDate.from(date);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.equals(givenDate)) {
            // Handle tomorrow
            return NLS.get("NLS.tomorrow");
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        if (yesterday.equals(givenDate)) {
            // Handle yesterday
            return NLS.get("NLS.yesterday");
        }

        if (tomorrow.isBefore(givenDate) || yesterday.isAfter(givenDate)) {
            // Handle dates in the future or the past
            return getDateFormat(getCurrentLang()).format(givenDate);
        } else {
            return NLS.get("NLS.today");
        }
    }

    private static String formatSpokenDateWithTime(Temporal date) {
        // We have a time, perform some nice formatting...
        LocalDateTime givenDateTime = LocalDateTime.from(date);
        if (givenDateTime.isAfter(LocalDateTime.now())) {
            return formatSpokenFutureDateWithTime(date, givenDateTime);
        }
        if (givenDateTime.isAfter(LocalDateTime.now().minusHours(12))) {
            return formatSpokenRecentDateWithTime(givenDateTime);
        }

        if (!ChronoField.DAY_OF_MONTH.isSupportedBy(date)) {
            // We don't have a date and the time difference is quite big -> simply format the time...
            return getTimeFormat(getCurrentLang()).format(date);
        }

        return formatSpokenDate(date);
    }

    private static String formatSpokenFutureDateWithTime(Temporal date, LocalDateTime givenDateTime) {
        if (givenDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            return NLS.get("NLS.nextHour");
        }
        if (givenDateTime.isBefore(LocalDateTime.now().plusHours(4))) {
            return NLS.fmtr("NLS.inNHours")
                      .set("hours", Duration.between(LocalDateTime.now(), givenDateTime).toHours())
                      .format();
        }
        if (ChronoField.DAY_OF_MONTH.isSupportedBy(date) && !LocalDate.now().equals(LocalDate.from(date))) {
            return formatSpokenDate(date);
        }

        return getTimeFormat(getCurrentLang()).format(date);
    }

    private static String formatSpokenRecentDateWithTime(LocalDateTime givenDateTime) {
        if (givenDateTime.isAfter(LocalDateTime.now().minusMinutes(30))) {
            return NLS.get("NLS.someMinutesAgo");
        }
        if (givenDateTime.isAfter(LocalDateTime.now().minusMinutes(59))) {
            return NLS.fmtr("NLS.nMinutesAgo")
                      .set("minutes", Duration.between(givenDateTime, LocalDateTime.now()).toMinutes())
                      .format();
        }
        if (givenDateTime.isAfter(LocalDateTime.now().minusHours(2))) {
            return NLS.get("NLS.oneHourAgo");
        }
        return NLS.fmtr("NLS.nHoursAgo")
                  .set("hours", Duration.between(givenDateTime, LocalDateTime.now()).toHours())
                  .format();
    }

    /**
     * Parses the given string by expecting a machine independent format.
     * <p>
     * This can parse all strings generated by <tt>toMachineString</tt>
     *
     * @param clazz the expected class of the value to be parsed
     * @param value the string to be parsed
     * @param <V>   the target type be be parsed
     * @return an instance of <tt>clazz</tt> representing the parsed string or <tt>null</tt> if value was empty.
     * @throws IllegalArgumentException if the given input was not well formed or if instances of <tt>clazz</tt>
     *                                  cannot be created. The thrown exception has a translated error message which
     *                                  can be directly presented to the user.
     */
    @SuppressWarnings("unchecked")
    public static <V> V parseMachineString(Class<V> clazz, String value) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        if (String.class.equals(clazz)) {
            return (V) value;
        }
        return parseBasicTypesFromMachineString(clazz, value);
    }

    @SuppressWarnings({"unchecked", "squid:S3776"})
    @Explain("The high complexity as acceptable as it is basically just a list of if statements")
    private static <V> V parseBasicTypesFromMachineString(Class<V> clazz, String value) {
        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            try {
                return (V) Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            try {
                return (V) Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            try {
                Double result = Double.valueOf(value);
                return (result == null) ? null : (V) Float.valueOf(result.floatValue());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            try {
                return (V) Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (BigDecimal.class.equals(clazz)) {
            try {
                return (V) new BigDecimal(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            return (V) Boolean.valueOf(Boolean.parseBoolean(value));
        }

        return parseDatesFromMachineString(clazz, value);
    }

    @SuppressWarnings("unchecked")
    private static <V> V parseDatesFromMachineString(Class<V> clazz, String value) {
        if (LocalDate.class.equals(clazz)) {
            try {
                return (V) LocalDate.from(MACHINE_DATE_FORMAT.parse(value));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                                             .set("format", "yyyy-MM-dd")
                                                                             .format(), e);
            }
        }
        if (LocalDateTime.class.equals(clazz)) {
            try {
                return (V) LocalDateTime.from(MACHINE_DATE_TIME_FORMAT.parse(value));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                                             .set("format", "yyyy-MM-dd HH:mm:ss")
                                                                             .format(), e);
            }
        }
        if (ZonedDateTime.class.equals(clazz)) {
            try {
                return (V) ZonedDateTime.from(MACHINE_DATE_TIME_FORMAT.parse(value));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                                             .set("format", "yyyy-MM-dd HH:mm:ss")
                                                                             .format(), e);
            }
        }
        if (LocalTime.class.equals(clazz)) {
            try {
                return (V) LocalTime.from(MACHINE_PARSE_TIME_FORMAT.parse(value));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                                             .set("format", "H:mm[:ss]")
                                                                             .format(), e);
            }
        }

        throw new IllegalArgumentException(fmtr("NLS.parseError").set("type", clazz).format());
    }

    /**
     * Parses the given string by expecting a format as defined by the given language.
     *
     * @param clazz the expected class of the value to be parsed
     * @param value the string to be parsed
     * @param lang  the two-letter code of the language which format should be used
     * @param <V>   the target type be be parsed
     * @return an instance of <tt>clazz</tt> representing the parsed string or <tt>null</tt> if value was empty.
     * @throws IllegalArgumentException if the given input was not well formed or if instances of <tt>clazz</tt>
     *                                  cannot be created. The thrown exception has a translated error message which
     *                                  can be directly presented to the user.
     */
    @SuppressWarnings("unchecked")
    public static <V> V parseUserString(Class<V> clazz, String value, String lang) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        if (String.class.equals(clazz)) {
            return (V) value;
        }
        return parseBasicTypesFromUserString(clazz, value, lang);
    }

    @SuppressWarnings({"unchecked", "squid:S3776"})
    @Explain("The high complexity as acceptable as it is basically just a list of if statements")
    private static <V> V parseBasicTypesFromUserString(Class<V> clazz, String value, String lang) {
        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            try {
                return (V) Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            try {
                return (V) Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            return (V) Float.valueOf((float) parseDecimalNumberFromUser(value, lang).doubleValue());
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            return (V) parseDecimalNumberFromUser(value, lang);
        }
        if (Amount.class.equals(clazz)) {
            return (V) Amount.of(parseDecimalNumberFromUser(value, lang));
        }
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            if (NLS.get(CommonKeys.YES.key(), lang).equalsIgnoreCase(value)) {
                return (V) Boolean.TRUE;
            }
            if (NLS.get(CommonKeys.NO.key(), lang).equalsIgnoreCase(value)) {
                return (V) Boolean.FALSE;
            }
            return (V) Boolean.valueOf(Boolean.parseBoolean(value));
        }
        return parseDatesFromUserString(clazz, value, lang);
    }

    private static Double parseDecimalNumberFromUser(String value, String lang) {
        try {
            Double result = tryParseMachineFormat(value);
            if (result != null) {
                return result;
            }

            return getDecimalFormat(lang).parse(value).doubleValue();
        } catch (ParseException e) {
            Exceptions.ignore(e);
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
        }
    }

    /**
     * If there is exactly one "." in the pattern and no "," and we have less then 3 digits behind the "." we treat this
     * as english decimal format and not as german grouping separator.
     *
     * @param value the parsed value or <tt>null</tt> if the format doesn't match
     * @return <tt>true</tt> if the format being used is a english / technical one and not a german one where "." is the
     * thousand separator
     */
    private static Double tryParseMachineFormat(String value) {
        if (!".".equals(NLS.get("NLS.groupingSeparator"))) {
            return null;
        }
        if (!value.contains(".") || value.contains(",")) {
            return null;
        }
        if (value.indexOf('.') == value.lastIndexOf('.') && value.indexOf('.') > value.length() - 4) {
            try {
                return Double.valueOf(value);
            } catch (Exception e) {
                Exceptions.ignore(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <V> V parseDatesFromUserString(Class<V> clazz, String value, String lang) {
        if (LocalDate.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) parser.parse(value).asDateTime().toLocalDate();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (LocalDateTime.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) parser.parse(value).asDateTime();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (ZonedDateTime.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) ZonedDateTime.from(parser.parse(value).asDateTime());
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (LocalTime.class.equals(clazz)) {
            try {
                return (V) LocalTime.from(NLS.getTimeParseFormat(lang).parse(value.toUpperCase()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidTime").set("format", get("NLS.patternParseTime"))
                                                                             .set("value", value)
                                                                             .format(), e);
            }
        }
        if (AdvancedDateParser.DateSelection.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) parser.parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }

        throw new IllegalArgumentException(fmtr("NLS.parseError").set("type", clazz).format());
    }

    /**
     * Parses the given string by expecting a format as defined by the current language.
     *
     * @param clazz  the expected class of the value to be parsed
     * @param string the string to be parsed
     * @param <V>    the target type be be parsed
     * @return an instance of <tt>clazz</tt> representing the parsed string or <tt>null</tt> if value was empty.
     * @throws IllegalArgumentException if the given input was not well formed or if instances of <tt>clazz</tt>
     *                                  cannot be created. The thrown exception has a translated error message which
     *                                  can be directly presented to the user.
     */
    public static <V> V parseUserString(Class<V> clazz, String string) {
        return parseUserString(clazz, string, getCurrentLang());
    }

    /**
     * Converts a given time range in milliseconds to a human readable format using the current language
     *
     * @param duration       the duration in milliseconds
     * @param includeSeconds determines whether to include seconds or to ignore everything below minutes
     * @param includeMillis  determines whether to include milli seconds or to ignore everything below seconds
     * @return a string representation of the given duration in days, hours, minutes and,
     * if enabled, seconds and milliseconds
     */
    public static String convertDuration(long duration, boolean includeSeconds, boolean includeMillis) {
        StringBuilder result = new StringBuilder();
        if (duration > DAY) {
            appendDurationValue(result, "NLS.day", "NLS.days", duration / DAY);
            duration = duration % DAY;
        }
        if (duration > HOUR) {
            appendDurationValue(result, "NLS.hour", "NLS.hours", duration / HOUR);
            duration = duration % HOUR;
        }
        if (duration > MINUTE || (!includeSeconds && duration > 0)) {
            appendDurationValue(result, "NLS.minute", "NLS.minutes", duration / MINUTE);
            duration = duration % MINUTE;
        }
        if (includeSeconds) {
            if (duration > SECOND || (!includeMillis && duration > 0)) {
                appendDurationValue(result, "NLS.second", "NLS.seconds", duration / SECOND);
                duration = duration % SECOND;
            }
            if (includeMillis && duration > 0) {
                appendDurationValue(result, "NLS.millisecond", "NLS.milliseconds", duration);
            }
        }

        return result.toString();
    }

    private static void appendDurationValue(StringBuilder result, String oneKey, String manyKey, long value) {
        if (result.length() > 0) {
            result.append(", ");
        }
        if (value == 1) {
            result.append(Strings.apply(NLS.get(oneKey), 1));
        } else {
            result.append(Strings.apply(NLS.get(manyKey), value));
        }
    }

    /**
     * Converts the given duration in milliseconds including seconds and milliseconds
     * <p>
     * This is a boilerplate method for {@link #convertDuration(long, boolean, boolean)} with
     * <tt>includeSeconds</tt> and <tt>includeMillis</tt> set to <tt>true</tt>.
     *
     * @param duration the duration in milliseconds
     * @return a string representation of the given duration in days, hours, minutes, seconds and milliseconds
     */
    public static String convertDuration(long duration) {
        return convertDuration(duration, true, true);
    }

    /**
     * Outputs integer numbers without decimals, but fractional numbers with two digits.
     * <p>
     * Discards fractional parts which absolute value is less or equal to {@code 0.00001}.
     *
     * @param number the number to be rounded
     * @return a string representation using the current languages decimal format.
     * Rounds fractional parts less or equal to <tt>0.00001</tt>
     */
    public static String smartRound(double number) {
        if (Math.abs(Math.floor(number) - number) > 0.000001D) {
            return NLS.toUserString(number);
        } else {
            return String.valueOf(Math.round(number));
        }
    }

    /**
     * Converts a file or byte size.
     * <p>
     * Supports sizes up to petabyte. Uses conventional SI-prefixed abbreviations like kB, MB.
     *
     * @param size the size to format in bytes
     * @return an english representation (using dot as decimal separator) along with one of the known abbreviations:
     * <tt>Bytes, KB, MB, GB, TB, PB</tt>.
     */
    public static String formatSize(long size) {
        int index = 0;
        double sizeAsFloat = size;
        while (sizeAsFloat > 1000 && index < UNITS.length - 1) {
            sizeAsFloat = sizeAsFloat / 1000;
            index++;
        }
        return Amount.of(sizeAsFloat).toSmartRoundedString(NumberFormat.MACHINE_TWO_DECIMAL_PLACES)
               + " "
               + UNITS[index];
    }
}
