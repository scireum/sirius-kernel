/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

/**
 * A flexible parser which can parse dates like DD.MM.YYYY or YYYY/DD/MM along with some computations.
 * <p>
 * A valid expression is defined by the following grammar:
 * <ul>
 * <li><code><b>ROOT</b> ::= (MODIFIER ",")* (":")? ("now" | DATE)? (("+" | "-") NUMBER (UNIT)?)</code></li>
 * <li><code><b>UNIT</b> ::= ("day" | "days" | "week" | "weeks" | "month" | "months" | "year" | "years")</code></li>
 * <li><code><b>NUMBER</b> ::=(0-9)+</code></li>
 * <li><code><b>DATE</b> ::= GERMAN_DATE | ENGLISH_DATE | YM_EXPRESSION</code></li>
 * <li><code><b>GERMAN_DATE</b> ::= NUMBER "." NUMBER ("." NUMBER)? (NUMBER (":" NUMBER (":" NUMBER)?)?)?</code></li>
 * <li><code><b>ENGLISH_DATE</b> ::=  NUMBER "/" NUMBER ("/" NUMBER)? (NUMBER (":" NUMBER (":" NUMBER)?)?)? ("am" |
 * "pm")?)?</code></li>
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

    private String lang;

    private static final String NEGATIVE_DELTA = "-";
    private static final String POSITIVE_DELTA = "+";
    private static final String MODIFIER_END = ":";
    private static final String MODIFIER_SEPARATOR = ",";
    private static final String PM = "pm";
    private static final String AM = "am";
    private static final String TIME_SEPARATOR = ":";
    private static final String ENGLISH_DATE_SEPARATOR = "/";
    private static final String GERMAN_DATE_SEPARATOR = ".";

    private Tokenizer tokenizer;
    private boolean startOfDay = false;
    private boolean startOfWeek = false;
    private boolean startOfMonth = false;
    private boolean startOfYear = false;
    private boolean endOfDay = false;
    private boolean endOfWeek = false;
    private boolean endOfMonth = false;
    private boolean endOfYear = false;

    /**
     * Creates a new parser for the given language to use.
     *
     * @param lang contains the two letter language code to obtain the translations for the available modifiers etc.
     */
    public AdvancedDateParser(String lang) {
        this.lang = lang;
    }

    /**
     * Used to tokenize the input supplied by the user
     */
    static class Tokenizer {

        private static final int END_OF_INPUT = 1;
        private static final int NUMBER = 2;
        private static final int IDENTIFIER = 3;
        private static final int SPECIAL = 4;
        private final String input;
        private StringBuilder nextToken;
        private int type;
        private int tokenStart = 0;
        private int position = 0;

        Tokenizer(String inputString) {
            this.input = inputString;
        }

        /*
         * Reads the next token in the input
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
            type = END_OF_INPUT;
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
            type = SPECIAL;
            nextToken.append(input.charAt(position));
            position++;
        }

        private void readIdentifier() {
            tokenStart = position;
            type = IDENTIFIER;
            //noinspection UnnecessaryParentheses
            while (endOfInput() && isLetter()) {
                nextToken.append(input.charAt(position));
                position++;
            }
        }

        private void readNumber() {
            tokenStart = position;
            type = NUMBER;
            //noinspection UnnecessaryParentheses
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

        /*
         * Returns the current token
         */
        String getToken() {
            return nextToken.toString();
        }

        /*
         * Returns the type of the current token
         */
        int getType() {
            return type;
        }

        /*
         * Returns the start pos of the current toke
         */
        int getTokenStart() {
            return tokenStart;
        }

        /*
         * Returns the position of the next character checked by the tokenizer.
         */
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
        //noinspection UnnecessaryParentheses
        if ((tokenizer.getType() == Tokenizer.SPECIAL) && in(MODIFIER_END)) {
            tokenizer.nextToken();
        }
        Calendar result = parseFixPoint();
        parseDeltas(result);
        applyModifiers(result);
        return new DateSelection(result, input);
    }

    private void parseDeltas(Calendar result) throws ParseException {
        while (tokenizer.getType() != Tokenizer.END_OF_INPUT) {
            parseDelta(result, tokenizer);
            tokenizer.nextToken();
        }
    }

    private void parseModifiers() throws ParseException {
        do {
            tokenizer.nextToken();
            // ignore "," between modifiers
            //noinspection UnnecessaryParentheses,UnnecessaryParentheses
            if ((tokenizer.getType() == Tokenizer.SPECIAL) && in(MODIFIER_SEPARATOR)) {
                tokenizer.nextToken();
            }
        } while (parseModifier());
    }

    /*
     * The text representation of a DateSelection contains the effective value in brackets. If we re-parse the string,
     * we simply cut this block.
     */
    private String eliminateTextInBrackets(String input) {
        int first = input.indexOf("[");
        int last = input.lastIndexOf("]");
        if (first < 0) {
            return input.trim();
        }
        String result = input.substring(0, first);
        //noinspection UnnecessaryParentheses
        if ((last > -1) && (last < input.length() - 1)) {
            result += input.substring(last + 1, input.length());
        }
        return result.trim();
    }

    /*
     * Applies the parsed modifiers to the previously calculated result.
     */
    private void applyModifiers(Calendar result) {
        forceConversion(result);
        applyDateModifiers(result);
        applyTimeModifiers(result);
    }

    private void applyTimeModifiers(Calendar result) {
        if (startOfDay) {
            result.set(Calendar.MILLISECOND, 0);
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MINUTE, 0);
            result.set(Calendar.HOUR_OF_DAY, 0);
            forceConversion(result);
        }
        if (endOfDay) {
            result.set(Calendar.MILLISECOND, result.getMaximum(Calendar.MILLISECOND));
            result.set(Calendar.SECOND, result.getMaximum(Calendar.SECOND));
            result.set(Calendar.MINUTE, result.getMaximum(Calendar.MINUTE));
            result.set(Calendar.HOUR_OF_DAY, result.getMaximum(Calendar.HOUR_OF_DAY));
            forceConversion(result);
        }
    }

    private void applyDateModifiers(Calendar result) {
        if (startOfYear) {
            result.set(Calendar.DAY_OF_MONTH, 1);
            result.set(Calendar.MONTH, Calendar.JANUARY);
            forceConversion(result);
        }
        if (endOfYear) {
            result.set(Calendar.MONTH, Calendar.DECEMBER);
            result.set(Calendar.DAY_OF_MONTH, result.getMaximum(Calendar.DAY_OF_MONTH));
            forceConversion(result);
        }
        if (startOfMonth) {
            result.set(Calendar.DAY_OF_MONTH, 1);
            forceConversion(result);
        }
        if (endOfMonth) {
            result.set(Calendar.DAY_OF_MONTH, result.getActualMaximum(Calendar.DAY_OF_MONTH));
            forceConversion(result);
        }
        if (startOfWeek) {
            result.set(Calendar.DAY_OF_WEEK, result.getFirstDayOfWeek());
            forceConversion(result);
        }
        if (endOfWeek) {
            result.set(Calendar.DAY_OF_WEEK, result.getActualMaximum(Calendar.DAY_OF_WEEK));
            forceConversion(result);
        }
    }

    private void forceConversion(Calendar result) {
        result.getTime();
    }

    private String[] getI18n(String key, String... extraKeys) {
        return join(getI18nText(key).split(MODIFIER_SEPARATOR), extraKeys);
    }

    private String getI18nText(String key) {
        return NLS.get(key, lang);
    }

    private boolean parseModifier() throws ParseException {
        if (tokenizer.getType() != Tokenizer.IDENTIFIER) {
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

    private void parseDelta(Calendar fixPoint, Tokenizer tokenizer) throws ParseException {
        int amount = parseDeltaAmount(tokenizer);
        tokenizer.nextToken();
        if (tokenizer.getType() == Tokenizer.END_OF_INPUT) {
            fixPoint.add(Calendar.DAY_OF_MONTH, amount);
            return;
        }
        applyDelta(fixPoint, amount);
    }

    private void applyDelta(Calendar fixPoint, int amount) throws ParseException {
        expectKeyword(join(seconds(), minutes(), hours(), days(), weeks(), months(), years()));
        if (in(seconds())) {
            fixPoint.add(Calendar.SECOND, amount);
            return;
        }
        if (in(minutes())) {
            fixPoint.add(Calendar.MINUTE, amount);
            return;
        }
        if (in(hours())) {
            fixPoint.add(Calendar.HOUR, amount);
            return;
        }
        if (in(days())) {
            fixPoint.add(Calendar.DAY_OF_MONTH, amount);
            return;
        }
        if (in(weeks())) {
            fixPoint.add(Calendar.WEEK_OF_YEAR, amount);
            return;
        }
        if (in(months())) {
            fixPoint.add(Calendar.MONTH, amount);
            return;
        }
        if (in(years())) {
            fixPoint.add(Calendar.YEAR, amount);
        }
    }

    private int parseDeltaAmount(Tokenizer tokenizer) throws ParseException {
        expectKeyword(POSITIVE_DELTA, NEGATIVE_DELTA);
        boolean add = true;
        if (POSITIVE_DELTA.equals(tokenizer.getToken())) {
            add = true;
        } else if (NEGATIVE_DELTA.equals(tokenizer.getToken())) {
            add = false;
        }
        tokenizer.nextToken();
        expectNumber();
        int amount = Integer.parseInt(tokenizer.getToken());
        if (!add) {
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
        return values.toArray(new String[values.size()]);
    }

    private Calendar parseFixPoint() throws ParseException {
        if (tokenizer.getType() == Tokenizer.NUMBER) {
            return parseDate(tokenizer);
        }
        if (tokenizer.getType() == Tokenizer.SPECIAL) {
            return now();
        }
        if (tokenizer.getType() == Tokenizer.END_OF_INPUT) {
            return now();
        }
        if (in(calendarWeek())) {
            tokenizer.nextToken();
            expectNumber();
            Calendar result = now();
            result.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(tokenizer.getToken()));
            forceConversion(result);
            tokenizer.nextToken();
            return result;
        }
        expectKeyword(nowToken());
        tokenizer.nextToken();
        return now();
    }

    private String[] nowToken() {
        return getI18n("AdvancedDateParser.now", "now");
    }

    private Calendar now() {
        return Calendar.getInstance();
    }

    private void expectNumber() throws ParseException {
        if (tokenizer.getType() != Tokenizer.NUMBER) {
            throw new ParseException(NLS.fmtr("AdvancedDateParser.errInvalidToken")
                                        .set("token", tokenizer.toString())
                                        .format(), tokenizer.getTokenStart());
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

    private Calendar parseDate(Tokenizer tokenizer) throws ParseException {
        expectNumber();
        int firstNumber = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        if (!GERMAN_DATE_SEPARATOR.equals(tokenizer.getToken())
            && !ENGLISH_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            return parseYMExpression(firstNumber);
        }
        expectKeyword(GERMAN_DATE_SEPARATOR, ENGLISH_DATE_SEPARATOR);
        if (GERMAN_DATE_SEPARATOR.equals(tokenizer.getToken())) {
            return parseGermanDate(firstNumber);
        } else {
            return parseEnglishDate(firstNumber);
        }
    }

    /*
     * Parses YM expressions: 200903 will be March 2009, 0903 will be converted
     * into the same. 9910 is October 1999.
     */
    private Calendar parseYMExpression(int number) {
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
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.YEAR, year);
        return cal;
    }

    private Calendar parseEnglishDate(int month) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int day = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int year = now().get(Calendar.YEAR);
        if (in(ENGLISH_DATE_SEPARATOR)) {
            tokenizer.nextToken();
            if (tokenizer.getType() == Tokenizer.NUMBER) {
                year = Integer.parseInt(tokenizer.getToken());
                year = fixYear(year);
                tokenizer.nextToken();
            }
        }
        if (tokenizer.getType() == Tokenizer.NUMBER) {
            return parseTime(buildCalendar(day, month, year));
        }
        return buildCalendar(day, month, year);
    }

    private Calendar parseTime(Calendar result) throws ParseException {
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
            result.set(Calendar.HOUR, hour);
            result.set(Calendar.AM_PM, Calendar.AM);
            tokenizer.nextToken();
        } else if (in(PM)) {
            result.set(Calendar.HOUR, hour);
            result.set(Calendar.AM_PM, Calendar.PM);
            result.set(Calendar.HOUR, hour);
            tokenizer.nextToken();
        } else {
            result.set(Calendar.HOUR_OF_DAY, hour);
        }
        result.set(Calendar.MINUTE, minute);
        result.set(Calendar.SECOND, second);
        return result;
    }

    private Calendar buildCalendar(int day, int month, int year) {
        Calendar result = now();
        result.set(Calendar.MILLISECOND, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.HOUR_OF_DAY, 0);
        result.set(Calendar.YEAR, year);
        result.set(Calendar.MONTH, month - 1);
        result.set(Calendar.DAY_OF_MONTH, day);
        forceConversion(result);
        return result;
    }

    private Calendar parseGermanDate(int day) throws ParseException {
        tokenizer.nextToken();
        expectNumber();
        int month = Integer.parseInt(tokenizer.getToken());
        tokenizer.nextToken();
        int year = now().get(Calendar.YEAR);
        if (in(GERMAN_DATE_SEPARATOR)) {
            tokenizer.nextToken();
            if (tokenizer.getType() == Tokenizer.NUMBER) {
                year = Integer.parseInt(tokenizer.getToken());
                year = fixYear(year);
                tokenizer.nextToken();
            }
        }
        if (tokenizer.getType() == Tokenizer.NUMBER) {
            return parseTime(buildCalendar(day, month, year));
        }
        return buildCalendar(day, month, year);
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
     * Combines the parsed text along with the effective date (as <tt>Calendar</tt>).
     * <p>
     * The string representation of this contains the effective date in angular brackets. As there are ignored by
     * the parser, the resulting string can be re-parsed to refresh modifiers and computations.
     */
    public static class DateSelection {

        private Temporal date;
        private String dateString;

        /**
         * Creates a new <tt>DateSelection</tt> for the given calendar and input string.
         *
         * @param calendar   the effective date to be used
         * @param dateString the input string which yielded the given calendar
         */
        DateSelection(Calendar calendar, String dateString) {
            super();
            this.date = LocalDateTime.of(calendar.get(Calendar.YEAR),
                                         calendar.get(Calendar.MONTH) + 1,
                                         calendar.get(Calendar.DAY_OF_MONTH),
                                         calendar.get(Calendar.HOUR_OF_DAY),
                                         calendar.get(Calendar.MINUTE));
            this.dateString = dateString;
        }

        /**
         * Returns the effective date as <tt>Temporal</tt>
         *
         * @return the effective date. This might be <tt>null</tt> if parsing the expression failed.
         */
        public Temporal getTemporal() {
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
