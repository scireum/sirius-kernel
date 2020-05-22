/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.di.std.Part;
import sirius.kernel.nls.NLS;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * A flexible parser for dates in various formats.
 * <p>
 * It can parse formats like DD.MM.YYYY, DD-MM-YYYY, MM/DD/YYYY or ISO dates like YYYY-MM-DDTHH:MM:SS along with some
 * modifiers as decribed below.
 * <p>
 * A valid expression is defined by the following grammar:
 * <ul>
 * <li><code><b>ROOT</b> ::= (MODIFIER ",")* (":")? ("now" | DATE)? (("+" | "-") NUMBER (UNIT)?)</code></li>
 * <li><code><b>UNIT</b> ::= ("day" | "days" | "week" | "weeks" | "month" | "months" | "year" | "years")</code></li>
 * <li><code><b>NUMBER</b> ::=(0-9)+</code></li>
 * <li><code><b>DATE</b> ::= DAY_MONTH_YEAR_DATE | ENGLISH_DATE | ISO_DATE | YM_EXPRESSION</code></li>
 * <li><code><b>DAY_MONTH_YEAR_DATE</b> ::= NUMBER ("." | "-") NUMBER (("." | "-") NUMBER)? (NUMBER (":" NUMBER (":" NUMBER)?)?)?</code></li>
 * <li><code><b>ENGLISH_DATE</b> ::=  NUMBER "/" NUMBER ("/" NUMBER)? (NUMBER (":" NUMBER (":" NUMBER)?)?)? ("am" |
 * "pm")?)?</code></li>
 * <li><code><b>ISO_DATE</b> ::=  NUMBER "-" NUMBER "-" NUMBER ("T")? (NUMBER ":" NUMBER ":" NUMBER)?</code></li>
 * <li><code><b>YM_EXPRESSION</b> ::= NUMBER</code></li>
 * <li><code><b>MODIFIER</b> ::= ("start" | "end") ("of")? ("day" | "week" | "month" | "year")</code></li>
 * </ul>
 * <p>
 * <b>Examples</b>
 * <ul>
 * <li>{@code now} - actual date</li>
 * <li>{@code 1.1} - first of january of current year</li>
 * <li>{@code +1} or {@code now + 1 day} - tomorrow</li>
 * <li>{@code start of week: now - 1 year} - start of the week of day one year ago</li>
 * </ul>
 */
public class AdvancedDateParser {

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String NEGATIVE_DELTA = "-";
    private static final String POSITIVE_DELTA = "+";
    private static final String MODIFIER_END = ":";
    private static final String MODIFIER_SEPARATOR = ",";
    private static final String PM = "pm";
    private static final String AM = "am";
    private static final String TIME_SEPARATOR = ":";
    private static final String ENGLISH_DATE_SEPARATOR = "/";
    private static final String GERMAN_DATE_SEPARATOR = ".";
    private static final String DASH_DATE_SEPARATOR = "-";

    private final String lang;
    private boolean parseBritishDate = false;
    private Tokenizer tokenizer;
    private boolean startOfDay = false;
    private boolean startOfWeek = false;
    private boolean startOfMonth = false;
    private boolean startOfYear = false;
    private boolean endOfDay = false;
    private boolean endOfWeek = false;
    private boolean endOfMonth = false;
    private boolean endOfYear = false;

    @Part
    private static TimeProvider timeProvider;

    /**
     * Creates a new parser for the given language to use.
     *
     * @param lang contains the two letter language code to obtain the translations for the available modifiers etc.
     */
    public AdvancedDateParser(String lang) {
        this.lang = lang;
    }

    /**
     * Creates a new parser for the given language to use.
     *
     * @param lang             contains the two letter language code to obtain the translations for the available
     *                         modifiers etc.
     * @param parseBritishDate determines if british dates (DD/MM/YYYY) instead of american (MM/DD/YYYY) dates should
     *                         be parsed.
     */
    public AdvancedDateParser(String lang, boolean parseBritishDate) {
        this.lang = lang;
        this.parseBritishDate = parseBritishDate;
    }

    /**
     * Used to tokenize the input supplied by the user
     */
    static class Tokenizer {

        enum TokenType {END_OF_INPUT, NUMBER, IDENTIFIER, SPECIAL}

        private final String input;
        private StringBuilder nextToken;
        private TokenType type;
        private int tokenStart = 0;
        private int position = 0;

        Tokenizer(String inputString) {
            this.input = inputString;
        }

        /**
         * Reads the next token in the input.
         */
        void nextToken() {
            nextToken = new StringBuilder();
            while (endOfInput()) {
                if (isDigit()) {
                    readNumber();
                    return;
                }
                if (isLetter()) {
                    readIdentifier();
                    return;
                }
                if (!isWhitespace()) {
                    readSpecialChars();
                    return;
                }
                position++;
            }
            type = TokenType.END_OF_INPUT;
        }

        private boolean isWhitespace() {
            return Character.isWhitespace(input.charAt(position));
        }

        private boolean isLetter() {
            return Character.isLetter(input.charAt(position));
        }

        private boolean endOfInput() {
            return position < input.length();
        }

        private boolean isDigit() {
            return Character.isDigit(input.charAt(position));
        }

        private void readSpecialChars() {
            tokenStart = position;
            type = TokenType.SPECIAL;
            nextToken.append(input.charAt(position));
            position++;
        }

        private void readIdentifier() {
            tokenStart = position;
            type = TokenType.IDENTIFIER;

            while (endOfInput() && isLetter()) {
                nextToken.append(input.charAt(position));
                position++;
            }
        }

        private void readNumber() {
            tokenStart = position;
            type = TokenType.NUMBER;
            while (endOfInput() && isDigit()) {
                nextToken.append(input.charAt(position));
                position++;
            }
        }

        @Override
        public String toString() {
            return NLS.fmtr("AdvancedDateParser.tokenizerMessage")
                      .set("nextToken", nextToken)
                      .set("tokenStart", tokenStart)
                      .set("tokenEnd", position)
                      .format();
        }

        String getToken() {
            return nextToken.toString();
        }

        TokenType getType() {
            return type;
        }

        int getTokenStart() {
            return tokenStart;
        }

        int getPosition() {
            return position;
        }
    }

    /**
     * Parses the given input and returns a <tt>DateSelection</tt> as result.
     * <p>
     * Note that an <tt>AdvancedDateParser</tt> is stateful an is therefore neither reusable nor thread-safe.
     *
     * @param input the text to parse
     * @return a <tt>DateSelection</tt> containing the effective date along with the parsed expression
     * @throws ParseException if the input cannot be parsed as it does not conform to the given grammar
     */
    public DateSelection parse(String input) throws ParseException {
        startOfDay = false;
        startOfWeek = false;
        startOfMonth = false;
        startOfYear = false;
        endOfDay = false;
        endOfWeek = false;
        endOfMonth = false;
        endOfYear = false;

        if (Strings.isEmpty(input)) {
            return null;
        }

        input = eliminateTextInBrackets(input);
        tokenizer = new Tokenizer(input.toLowerCase());
        parseModifiers();
        // ignore ":" after modifiers
        if ((tokenizer.getType() == Tokenizer.TokenType.SPECIAL) && in(MODIFIER_END)) {
            tokenizer.nextToken();
        }

        LocalDateTime result = parseFixPoint();
        result = parseDeltas(result);
        result = applyModifiers(result);
        return new DateSelection(result, input);
    }

    private LocalDateTime parseDeltas(LocalDateTime result) throws ParseException {
        while (tokenizer.getType() != Tokenizer.TokenType.END_OF_INPUT) {
            result = parseDelta(result, tokenizer);
            tokenizer.nextToken();
        }

        return result;
    }

    private void parseModifiers() throws ParseException {
        do {
            tokenizer.nextToken();
            // ignore "," between modifiers
            if ((tokenizer.getType() == Tokenizer.TokenType.SPECIAL) && in(MODIFIER_SEPARATOR)) {
                tokenizer.nextToken();
            }
        } while (parseModifier());
    }

    /*
     * The text representation of a DateSelection contains the effective value in brackets. If we re-parse the string,
     * we simply cut this block.
     */
    private String eliminateTextInBrackets(String input) {
        int first = input.indexOf('[');
        int last = input.lastIndexOf(']');
        if (first < 0) {
            return input.trim();
        }
        String result = input.substring(0, first);

        if ((last > -1) && (last < input.length() - 1)) {
            result += input.substring(last + 1);
        }
        return result.trim();
    }

    private LocalDateTime applyModifiers(LocalDateTime result) {
        result = applyDateModifiers(result);
        result = applyTimeModifiers(result);

        return result;
    }

    private LocalDateTime applyTimeModifiers(LocalDateTime result) {
        if (startOfDay) {
            result = result.toLocalDate().atStartOfDay();
        }
        if (endOfDay) {
            result = result.toLocalDate().plusDays(1).atStartOfDay().minusSeconds(1);
        }

        return result;
    }

    private LocalDateTime applyDateModifiers(LocalDateTime result) {
        if (startOfYear) {
            result = result.withDayOfMonth(1).withMonth(1);
        }
        if (endOfYear) {
            result = result.withMonth(12).withDayOfMonth(31);
        }
        if (startOfMonth) {
            result = result.withDayOfMonth(1);
        }
        if (endOfMonth) {
            result = result.with(TemporalAdjusters.lastDayOfMonth());
        }
        if (startOfWeek) {
            result = result.with(WeekFields.ISO.dayOfWeek(), 1);
        }
        if (endOfWeek) {
            result = result.with(WeekFields.ISO.dayOfWeek(), 7);
        }

        return result;
    }

    private String[] getI18n(String key, String... extraKeys) {
        return join(getI18nText(key).split(MODIFIER_SEPARATOR), extraKeys);
    }

    private String getI18nText(String key) {
        return NLS.get(key, lang);
    }

    private boolean parseModifier() throws ParseException {
        if (tokenizer.getType() != Tokenizer.TokenType.IDENTIFIER) {
            return false;
        }
        if (in(start())) {
            parseStartModifier();
            return true;
        }
        if (in(end())) {
            parseEndModifier();
            return true;
        }
        return false;
    }

    private String[] end() {
        return getI18n("AdvancedDateParser.end", "end");
    }

    private String[] calendarWeek() {
        return getI18n("AdvancedDateParser.calendarWeek", "week");
    }

    private void parseStartModifier() throws ParseException {
        tokenizer.nextToken();
        expectKeyword(join(of(), day(), week(), month(), year()));
        if (in(of())) {
            tokenizer.nextToken();
            expectKeyword(join(day(), week(), month(), year()));
        }
        if (in(day())) {
            startOfDay = true;
        }
        if (in(week())) {
            startOfWeek = true;
        }
        if (in(month())) {
            startOfMonth = true;
        }
        if (in(year())) {
            startOfYear = true;
        }
    }

    private void parseEndModifier() throws ParseException {
        tokenizer.nextToken();
        expectKeyword(join(of(), day(), week(), month(), year()));
        if (in(of())) {
            tokenizer.nextToken();
            expectKeyword(join(day(), week(), month(), year()));
        }
        if (in(day())) {
            endOfDay = true;
        }
        if (in(week())) {
            endOfWeek = true;
        }
        if (in(month())) {
            endOfMonth = true;
        }
        if (in(year())) {
            endOfYear = true;
        }
    }

    private String[] of() {
        return getI18n("AdvancedDateParser.of", "of");
    }

    private String[] day() {
        return getI18n("AdvancedDateParser.day", "day");
    }

    private String[] week() {
        return getI18n("AdvancedDateParser.week", "week");
    }

    private String[] month() {
        return getI18n("AdvancedDateParser.month", "month");
    }

    private String[] year() {
        return getI18n("AdvancedDateParser.year", "year");
    }

    private String[] start() {
        return getI18n("AdvancedDateParser.start", "start");
    }

    private LocalDateTime parseDelta(LocalDateTime fixPoint, Tokenizer tokenizer) throws ParseException {
        int amount = parseDeltaAmount(tokenizer);
        tokenizer.nextToken();
        if (tokenizer.getType() == Tokenizer.TokenType.END_OF_INPUT) {
            fixPoint = fixPoint.plusDays(amount);
            return fixPoint;
        }
        return applyDelta(fixPoint, amount);
    }

    private LocalDateTime applyDelta(LocalDateTime fixPoint, int amount) throws ParseException {
        expectKeyword(join(seconds(), minutes(), hours(), days(), weeks(), months(), years()));
        if (in(seconds())) {
            return fixPoint.plusSeconds(amount);
        }
        if (in(minutes())) {
            return fixPoint.plusMinutes(amount);
        }
        if (in(hours())) {
            return fixPoint.plusHours(amount);
        }
        if (in(days())) {
            return fixPoint.plusDays(amount);
        }
        if (in(weeks())) {
            return fixPoint.plusWeeks(amount);
        }
        if (in(months())) {
            return fixPoint.plusMonths(amount);
        }
        if (in(years())) {
            return fixPoint.plusYears(amount);
        }

        return fixPoint;
    }

    private int parseDeltaAmount(Tokenizer tokenizer) throws ParseException {
        expectKeyword(POSITIVE_DELTA, NEGATIVE_DELTA);
        boolean subtract = NEGATIVE_DELTA.equals(tokenizer.getToken());
        tokenizer.nextToken();
        expectNumber();
        int amount = Integer.parseInt(tokenizer.getToken());
        if (subtract) {
            amount *= -1;
        }
        return amount;
    }

    private String[] years() {
        return getI18n("AdvancedDateParser.years", "year", "years");
    }

    private String[] months() {
        return getI18n("AdvancedDateParser.months", "month", "months");
    }

    private String[] weeks() {
        return getI18n("AdvancedDateParser.weeks", "week", "weeks");
    }

    private String[] days() {
        return getI18n("AdvancedDateParser.days", "day", "days");
    }

    private String[] hours() {
        return getI18n("AdvancedDateParser.hours", "hour", "hours");
    }

    private String[] minutes() {
        return getI18n("AdvancedDateParser.minutes", "minute", "minutes");
    }

    private String[] seconds() {
        return getI18n("AdvancedDateParser.seconds", "second", "seconds");
    }

    private String[] join(String[]... arrays) {
        Set<String> values = new TreeSet<>();
        for (String[] array : arrays) {
            values.addAll(Arrays.asList(array));
        }
        return values.toArray(EMPTY_STRING_ARRAY);
    }

    private LocalDateTime parseFixPoint() throws ParseException {
        if (tokenizer.getType() == Tokenizer.TokenType.NUMBER) {
            return parseDate(tokenizer);
        }
        if (tokenizer.getType() == Tokenizer.TokenType.SPECIAL) {
            return now();
        }
        if (tokenizer.getType() == Tokenizer.TokenType.END_OF_INPUT) {
            return now();
        }
        if (in(calendarWeek())) {
            tokenizer.nextToken();
            expectNumber();
            LocalDateTime result = now();
            result = result.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, Integer.parseInt(tokenizer.getToken()));
            tokenizer.nextToken();
            return result;
        }

        expectKeyword(nowToken());
        tokenizer.nextToken();
        LocalDateTime result = now();
        if (tokenizer.getType() == Tokenizer.TokenType.NUMBER) {
            result = parseTime(result);
        }
        return result;
    }

    private String[] nowToken() {
        return getI18n("AdvancedDateParser.now", "now");
    }

    private LocalDateTime now() {
        return timeProvider.localDateTimeNow();
    }

    private void expectNumber() throws ParseException {
        if (tokenizer.getType() != Tokenizer.TokenType.NUMBER) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidToken")
                                        .set("token", tokenizer.toString())
                                        .format(), tokenizer.getTokenStart());
        }
    }

    private void ensureValidSecond(int second) throws ParseException {
        if (!ChronoField.SECOND_OF_MINUTE.range().isValidIntValue(second)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidSecond")
                                        .set("secondOfMinute", second)
                                        .format(), 0);
        }
    }

    private void ensureValidMinute(int minute) throws ParseException {
        if (!ChronoField.MINUTE_OF_HOUR.range().isValidIntValue(minute)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidMinute")
                                        .set("minuteOfHour", minute)
                                        .format(), 0);
        }
    }

    private void ensureValidHour(int hour) throws ParseException {
        if (!ChronoField.HOUR_OF_DAY.range().isValidIntValue(hour)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidHour").set("hourOfDay", hour).format(), 0);
        }
    }

    private void ensureValidAmPmHour(int hour) throws ParseException {
        if (!ChronoField.HOUR_OF_AMPM.range().isValidIntValue(hour)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidHour").set("hourOfDay", hour).format(), 0);
        }
    }

    private void ensureValidDayOfMonth(int year, int month, int day) throws ParseException {
        if (!YearMonth.of(year, month).isValidDay(day)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidDay").set("dayOfMonth", day).format(), 0);
        }
    }

    private void ensureValidMonth(int month) throws ParseException {
        if (!ChronoField.MONTH_OF_YEAR.range().isValidIntValue(month)) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidMonth").set("monthOfYear", month).format(),
                                     0);
        }
    }

    private void ensureValidYear(int year) throws ParseException {
        if (year < 1900 || year > 2100) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidYear").set("year", year).format(), 0);
        }
    }

    private boolean in(String... keywords) {
        for (String keyword : keywords) {
            if (keyword.equals(tokenizer.getToken())) {
                return true;
            }
        }
        return false;
    }

    private void expectKeyword(String... keywords) throws ParseException {
        if (!in(keywords)) {
            StringBuilder allKeywords = new StringBuilder();
            Monoflop mf = Monoflop.create();
            for (String keyword : keywords) {
                if (mf.successiveCall()) {
                    allKeywords.append(", ");
                }
                allKeywords.append("'");
                allKeywords.append(keyword);
                allKeywords.append("'");
            }
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errUnexpectedKeyword")
                                        .set("token", tokenizer.toString())
                                        .set("keywords", allKeywords)
                                        .format(), tokenizer.getTokenStart());
        }
    }

    private LocalDateTime parseDate(Tokenizer tokenizer) throws ParseException {
        expectNumber();
        int firstNumber = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        if (!GERMAN_DATE_SEPARATOR.equals(tokenizer.getToken())
            && !DASH_DATE_SEPARATOR.equals(tokenizer.getToken())
            && !ENGLISH_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            return parseYMExpression(firstNumber);
        }
        expectKeyword(GERMAN_DATE_SEPARATOR, ENGLISH_DATE_SEPARATOR, DASH_DATE_SEPARATOR);
        if (GERMAN_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            return parseDayMonthYearDate(firstNumber);
        } else if (DASH_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            if (firstNumber > 31) {
                return parseISODate(firstNumber);
            } else {
                return parseDayMonthYearDate(firstNumber);
            }
        } else {
            return parseEnglishDate(firstNumber);
        }
    }

    /*
     * Parses YM expressions: 200903 will be March 2009, 0903 will be converted
     * into the same. 9910 is October 1999.
     */
    private LocalDateTime parseYMExpression(int number) throws ParseException {
        // Convert short format like 0801 or 9904 into the equivalent long
        // format.
        if (number < 6000) {
            // everything below 6000 is considered to be in the 21th century:
            // therefore 6001 is January 1960, 5901 is January 2059.
            number += 200000;
        }
        if (number < 9999) {
            // handle short form of 19th century
            number += 190000;
        }
        int year = number / 100;
        int month = number % 100;

        ensureValidYear(year);
        ensureValidMonth(month);
        return LocalDate.of(year, month, 1).atStartOfDay();
    }

    @SuppressWarnings("java:S2234")
    @Explain("We intentionally flip the parameters here, as the british and the american format differ this way...")
    private LocalDateTime parseEnglishDate(int month) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int day = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int year = now().getYear();
        if (in(ENGLISH_DATE_SEPARATOR)) {
            tokenizer.nextToken();
            if (tokenizer.getType() == Tokenizer.TokenType.NUMBER) {
                year = Integer.parseInt(tokenizer.getToken());
                year = fixYear(year);
                tokenizer.nextToken();
            }
        }

        if (parseBritishDate) {
            // The empire uses a format DD/MM/YYYY instead of the yankee version (MM/DD/YYYY), therefore
            // we have to flip month and day here...
            return buildDateAndParseTime(month, day, year);
        } else {
            return buildDateAndParseTime(day, month, year);
        }
    }

    private LocalDateTime buildDateAndParseTime(int day, int month, int year) throws ParseException {
        ensureValidYear(year);
        ensureValidMonth(month);
        ensureValidDayOfMonth(year, month, day);

        LocalDateTime result = LocalDate.of(year, month, day).atStartOfDay();
        if (tokenizer.getType() == Tokenizer.TokenType.NUMBER) {
            result = parseTime(result);
        }
        return result;
    }

    private LocalDateTime parseTime(LocalDateTime result) throws ParseException {
        int hour = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int minute = 0;
        if (in(TIME_SEPARATOR)) {
            tokenizer.nextToken();
            expectNumber();
            minute = Integer.parseInt(tokenizer.getToken());
            tokenizer.nextToken();
        }
        int second = 0;
        if (in(TIME_SEPARATOR)) {
            tokenizer.nextToken();
            expectNumber();
            second = Integer.parseInt(tokenizer.getToken());
            tokenizer.nextToken();
        }
        if (in(AM)) {
            ensureValidAmPmHour(hour);
            result = result.with(ChronoField.AMPM_OF_DAY, 0).with(ChronoField.HOUR_OF_AMPM, hour);
            tokenizer.nextToken();
        } else if (in(PM)) {
            ensureValidAmPmHour(hour);
            result = result.with(ChronoField.AMPM_OF_DAY, 1).with(ChronoField.HOUR_OF_AMPM, hour);
            tokenizer.nextToken();
        } else {
            ensureValidHour(hour);
            result = result.withHour(hour);
        }

        ensureValidMinute(minute);
        result = result.withMinute(minute);
        ensureValidSecond(second);
        result = result.withSecond(second);
        return result;
    }

    private LocalDateTime parseDayMonthYearDate(int day) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int month = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();

        int year = now().getYear();

        if (in(GERMAN_DATE_SEPARATOR, DASH_DATE_SEPARATOR)) {
            tokenizer.nextToken();
            if (tokenizer.getType() == Tokenizer.TokenType.NUMBER) {
                year = Integer.parseInt(tokenizer.getToken());
                year = fixYear(year);
                tokenizer.nextToken();
            }
        }

        return buildDateAndParseTime(day, month, year);
    }

    private LocalDateTime parseISODate(int year) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int month = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        expectKeyword(DASH_DATE_SEPARATOR);
        tokenizer.nextToken();
        expectNumber();
        int day = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();

        if (in("t")) {
            tokenizer.nextToken();
        }

        return buildDateAndParseTime(day, month, year);
    }

    private int fixYear(int year) {
        if (year < 50) {
            year += 2000;
        } else if (year < 100) {
            year += 1900;
        }
        return year;
    }

    /**
     * Combines the parsed text along with the effective date (as <tt>LocalDateTime</tt>).
     * <p>
     * The string representation of this contains the effective date in angular brackets. As there are ignored by
     * the parser, the resulting string can be re-parsed to refresh modifiers and computations.
     */
    public static class DateSelection {

        private final LocalDateTime date;
        private final String dateString;

        /**
         * Creates a new <tt>DateSelection</tt> for the given calendar and input string.
         *
         * @param date       the effective date to be used
         * @param dateString the input string which yielded the given calendar
         */
        DateSelection(LocalDateTime date, String dateString) {
            super();
            this.date = date;
            this.dateString = dateString;
        }

        /**
         * Returns the effective date as <tt>Temporal</tt>
         *
         * @return the effective date. This might be <tt>null</tt> if parsing the expression failed.
         * @deprecated use {@link #asDateTime()} which returns the proper type (<tt>LocalDateTime</tt>).
         */
        @Deprecated
        public Temporal getTemporal() {
            return date;
        }

        /**
         * Returns the effective date as <tt>LocalDateTime</tt>
         *
         * @return the effective date. This might be <tt>null</tt> if parsing the expression failed.
         */
        public LocalDateTime asDateTime() {
            return date;
        }

        /**
         * Returns the text input which was used to compute the effective date.
         *
         * @return the string from which the <tt>calendar</tt> was parsed
         */
        public String getDateString() {
            return dateString;
        }

        @Override
        public String toString() {
            return asDateString();
        }

        /**
         * Returns a string representation of this <tt>DateSelection</tt> without any information about the time
         * of day for the parsed date.
         *
         * @return the input string used to create this <tt>DateSelection</tt> appended with the effective
         * date surrounded by angular brackets
         */
        public String asDateString() {
            if (dateString == null) {
                return getDateString(false);
            }
            return dateString + " [" + getDateString(false) + "]";
        }

        /**
         * Returns a string representation of this <tt>DateSelection</tt> including the time information.
         *
         * @return the input string used to create this <tt>DateSelection</tt> appended with the effective
         * date surrounded by angular brackets
         */
        public String asDateTimeString() {
            if (dateString == null) {
                return getDateString(true);
            }
            return dateString + " [" + getDateString(true) + "]";
        }

        private String getDateString(boolean dateTime) {
            if (date == null) {
                return "";
            }
            if (dateTime) {
                return NLS.toUserString(date);
            } else {
                return NLS.toUserString(LocalDate.from(date));
            }
        }

        /**
         * Returns the effective date as string
         *
         * @return the effective date formatted as string without any time information
         */
        public String getDate() {
            return getDateString(false);
        }

        /**
         * Returns the effective date as string
         *
         * @return the effective date formatted as string with time information
         */
        public String getDateTime() {
            return getDateString(true);
        }
    }
}
