/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Provides a generic wrapper for a value which is read from an untyped context like HTTP parameters.
 * <p>
 * It supports elegant {@code null} handling and type conversions.
 */
public class Value {

    /**
     * Represents an empty value which contains <tt>null</tt> as data.
     */
    public static final Value EMPTY = new Value();

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private Object data;

    /**
     * Use {@code Value.of} to create a new instance.
     */
    private Value() {
        super();
    }

    /**
     * Creates a new wrapper for the given data.
     *
     * @param data the object wrap
     * @return the newly created value which wraps the given data object
     */
    @Nonnull
    public static Value of(@Nullable Object data) {
        if (data == null) {
            return EMPTY;
        }

        if (data instanceof Value value) {
            return value;
        }

        Value val = new Value();
        val.data = data;
        return val;
    }

    /**
     * Returns the n-th (index-th) element of the given collection.
     *
     * @param index      the zero based index of the element to fetch
     * @param collection the collection to pick the element from
     * @return the element at <tt>index</tt> wrapped as <tt>Value</tt>
     * or an empty value if the collection is <tt>null</tt> or if the index is outside the collections bounds
     */
    @Nonnull
    public static Value indexOf(int index, @Nullable Collection<?> collection) {
        if (collection == null || index < 0 || index >= collection.size()) {
            return Value.of(null);
        }
        if (collection instanceof List) {
            return Value.of(((List<?>) collection).get(index));
        }
        return Value.of(collection.stream().skip(index).findFirst().orElse(null));
    }

    /**
     * Returns the n-th (index-th) element of the given array.
     *
     * @param index the zero based index of the element to fetch
     * @param array the array to pick the element from
     * @return the element at <tt>index</tt> wrapped as <tt>Value</tt>
     * or an empty value if the array is <tt>null</tt> or if the index is outside the arrays bounds
     */
    @Nonnull
    public static Value indexOf(int index, @Nullable Object[] array) {
        if (array == null || index < 0 || index >= array.length) {
            return Value.of(null);
        }
        return Value.of(array[index]);
    }

    /**
     * Determines if the wrapped value is <tt>null</tt>
     *
     * @return <tt>true</tt> if the wrapped value is null, <tt>false</tt> otherwise
     */
    public boolean isNull() {
        return data == null;
    }

    /**
     * Determines if the wrapped value is an empty string.
     *
     * @return <tt>true</tt> if the wrapped value is an empty string, <tt>false</tt> otherwise
     */
    public boolean isEmptyString() {
        return Strings.isEmpty(data);
    }

    /**
     * Checks if the given value is filled and contains the given needle in its
     * string representation
     *
     * @param needle the substring to search
     * @return <tt>true</tt> if the given substring <tt>needle</tt> was found in the wrapped objects string
     * representation, <tt>false</tt> otherwise
     */
    public boolean contains(String needle) {
        return asString("").contains(needle);
    }

    /**
     * Determines if the wrapped value is not null.
     *
     * @return <tt>true</tt> if the wrapped value is neither <tt>null</tt> nor ""
     */
    public boolean isFilled() {
        return !isEmptyString();
    }

    /**
     * Calls the given consumer with <tt>this</tt> if the value is filled.
     * <p>
     * Note that if the consumer throws an exception use {@link #ifPresent(Callback)}
     * to tunnel exceptions back to the caller.
     *
     * @param consumer the consumer to call with this object if it is filled
     * @return the value itself for fluent method calls
     */
    @Nonnull
    public Value ifFilled(@Nonnull Consumer<Value> consumer) {
        if (isFilled()) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Calls the given callback with <tt>this</tt> if the value is filled.
     * <p>
     * Note that if there are no exceptions declared by the consumer, use {@link #ifFilled(Consumer)}.
     *
     * @param callback the callback to call with this object if it is filled
     * @return the value itself for fluent method calls
     * @throws Exception if the callback itself throws an exception
     */
    @Nonnull
    public Value ifPresent(@Nonnull Callback<Value> callback) throws Exception {
        if (isFilled()) {
            callback.invoke(this);
        }
        return this;
    }

    /**
     * Calls the given <tt>consumer</tt> with the result of the given <tt>extractor</tt> if the value is filled.
     * <p>
     * Note that if the extractor or the consumer throws an exception use {@link #ifPresent(Processor, Callback)}
     * to tunnel exceptions back to the caller.
     *
     * @param extractor the extractor to call with this object if it is filled
     * @param consumer  the consumer to call with this object if it is filled
     */
    public <T> void ifFilled(@Nonnull Function<Value, T> extractor, @Nonnull Consumer<T> consumer) {
        if (isFilled()) {
            consumer.accept(extractor.apply(this));
        }
    }

    /**
     * Calls the given <tt>consumer</tt> with the result of the given <tt>extractor</tt> if the value is filled.
     * <p>
     * Note that if there are no exceptions declared by the extractor or the callback, use {@link #ifFilled(Consumer)}.
     *
     * @param extractor the extractor to call with this object if it is filled
     * @param callback  the callback to call with this object if it is filled
     * @throws Exception if either the extractor or the callback itself throws an exception
     */
    public <T> void ifPresent(@Nonnull Processor<Value, T> extractor, @Nonnull Callback<T> callback) throws Exception {
        if (isFilled()) {
            callback.invoke(extractor.apply(this));
        }
    }

    /**
     * Returns a new {@link Value} which will be empty its value equals one of the given ignored values.
     *
     * @param ignoredValues the list of values which will be replaced by an empty value
     * @return a <tt>Value</tt> which is empty if the currently wrapped value equals to one of the given values.
     * Otherwise, the current value is returned.
     */
    @Nonnull
    public Value ignore(@Nonnull String... ignoredValues) {
        if (isEmptyString()) {
            return this;
        }
        for (String val : ignoredValues) {
            if (data.equals(val)) {
                return Value.EMPTY;
            }
        }
        return this;
    }

    /**
     * Returns a new <tt>Value</tt> which will wrap the given value, if the current value is empty.
     * Otherwise, the current value will be returned.
     *
     * @param replacement the value which is used as replacement if this value is empty
     * @return a new Value wrapping the given value or the current value if this is not empty.
     */
    @Nonnull
    public Value replaceEmptyWith(@Nullable Object replacement) {
        if (isFilled()) {
            return this;
        }
        return Value.of(replacement);
    }

    /**
     * Returns a new <tt>Value</tt> which will wrap the value produced by the given supplier, if the current
     * value is empty. Otherwise, the current value will be returned.
     *
     * @param supplier the supplier used to compute a replacement value if this value is empty
     * @return a new Value wrapping the produced value of the given supplier or the current value if this is not empty.
     */
    @Nonnull
    public Value replaceIfEmpty(@Nonnull Supplier<?> supplier) {
        if (isFilled()) {
            return this;
        }
        return Value.of(supplier.get());
    }

    /**
     * Returns an optional wrapping the value computed by the given mapper. If this value is <tt>empty</tt> the mapper will not
     * be called, but an empty optional will be returned.
     *
     * @param mapper the function used to convert the value into the desired object
     * @param <R>    the type of the desired result
     * @return an Optional object wrapping the result of the computation or an empty Optional, if the value wasn't filled
     */
    @Nonnull
    public <R> Optional<R> map(@Nonnull Function<Value, R> mapper) {
        if (isFilled()) {
            return Optional.ofNullable(mapper.apply(this));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an optional value computed by the given mapper. If this value is <tt>empty</tt> the mapper will not
     * be called, but an empty optional will be returned.
     * <p>
     * This method is similar to {@link #map(Function)}, but the provided mapper is one whose result is already an {@link Optional},
     * and if invoked, {@code flatMap} does not wrap it with an additional {@link Optional}.
     *
     * @param mapper the function used to convert the value into the desired optional object
     * @param <R>    the type of the desired result
     * @return the Optional object from the result of the computation or an empty Optional, if the value wasn't filled
     */
    @Nonnull
    public <R> Optional<R> flatMap(@Nonnull Function<Value, Optional<R>> mapper) {
        if (isFilled()) {
            return mapper.apply(this);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Boilerplate for {@code Optional.ofNullable(get(type, null))}.
     * <p>
     * Returns the internal value wrapped as Optional.
     *
     * @param type the desired type of the data
     * @param <T>  the expected type of the contents of this value
     * @return the internal value (cast to the given type) wrapped as Optional or an empty Optional if the value
     * was empty or the cast failed.
     */
    @Nonnull
    public <T> Optional<T> asOptional(@Nonnull Class<T> type) {
        return Optional.ofNullable(get(type, null));
    }

    /**
     * Returns the internal value wrapped as Optional while expecting it to be an integer number.
     * <p>
     * If the value is empty or not an integer, an empty Optional will be returned.
     *
     * @return the internal value wrapped as Optional or an empty Optional if the value is not filled or non-integer
     */
    @Nonnull
    public Optional<Integer> asOptionalInt() {
        return Optional.ofNullable(getInteger());
    }

    /**
     * Returns the internal value wrapped as Optional.
     * <p>
     * If the value is empty, an empty Optional will be returned.
     *
     * @return the internal value wrapped as Optional or an empty Optional if the value is not filled
     */
    @Nonnull
    public Optional<String> asOptionalString() {
        return isFilled() ? Optional.of(asString()) : Optional.empty();
    }

    /**
     * Returns a value which wraps {@code this + separator + value}
     * <p>
     * If the current value is empty, the given value is returned (without the separator). If the given
     * value is an empty string, the current value is returned (without the separator).
     *
     * @param separator the separator to be put in between the two. If the given value is <tt>null</tt>, "" is assumed
     * @param value     the value to be appended to the current value.
     * @return a <tt>Value</tt> representing the current value appended with the given value and separated
     * with the given separator
     */
    @Nonnull
    public Value append(@Nullable String separator, @Nullable Object value) {
        if (Strings.isEmpty(value)) {
            return this;
        }
        if (isEmptyString()) {
            return Value.of(value);
        }
        if (separator == null) {
            separator = "";
        }
        return Value.of(this + separator + value);
    }

    /**
     * Returns a value which wraps {@code this + separator + suffix}
     * <p>
     * If this value is empty an empty value will be returned without appending the given suffix. If the given
     * suffix is empty, this value is returned.
     *
     * @param separator the separator to be put in between the two. If the given value is <tt>null</tt>, "" is assumed
     * @param suffix    the value to be appended to the current value.
     * @return a <tt>Value</tt> representing the current value appended with the given value and separated
     * with the given separator
     */
    @Nonnull
    public Value tryAppend(@Nullable String separator, @Nullable Object suffix) {
        if (Strings.isEmpty(suffix)) {
            return this;
        }
        if (isEmptyString()) {
            return Value.EMPTY;
        }
        if (separator == null) {
            separator = "";
        }
        return Value.of(this + separator + suffix);
    }

    /**
     * Returns a value which wraps {@code value + separator + this}
     * <p>
     * If the current value is empty, the given value is returned (without the separator). If the given
     * value is an empty string, the current value is returned (without the separator).
     *
     * @param separator the separator to be put in between the two. If the given value is <tt>null</tt>, "" is assumed
     * @param value     the value to be appended to the current value.
     * @return a <tt>Value</tt> representing the given value appended with the current value and separated
     * with the given separator
     */
    @Nonnull
    public Value prepend(@Nullable String separator, @Nullable Object value) {
        if (Strings.isEmpty(value)) {
            return this;
        }
        if (isEmptyString()) {
            return Value.of(value);
        }
        if (separator == null) {
            separator = "";
        }
        return Value.of(value + separator + this);
    }

    /**
     * Cuts and returns the first n given characters of the string representation of this value.
     * <p>
     * <b>Note:</b> This modifies the internal state of this value, since the number of characters is cut from
     * the string representation of the current object and the remaining string is stored as new internal value.
     * <p>
     * If the wrapped value is empty, "" is returned. If the string representation of the wrapped object
     * is shorter than maxNumberOfCharacters, the remaining string is returned and the internal value is set to
     * <tt>null</tt>.
     * <p>
     * This can be used to cut a string into sub strings of a given length:
     * <pre>
     * {@code
     *             Value v = Value.of("This is a long string...");
     *             while(v.isFilled()) {
     *                 System.out.println("Up to 5 chars of v: "+v.eat(5));
     *             }
     * }
     * </pre>
     *
     * @param maxNumberOfCharacters the max length of the string to cut from the wrapped value
     * @return the first maxNumberOfCharacters of the wrapped values string representation, or less if it is shorter.
     * Returns "" if the wrapped value is empty.
     */
    @Nonnull
    public String eat(int maxNumberOfCharacters) {
        if (isEmptyString()) {
            return "";
        }
        String value = asString();
        if (value.length() < maxNumberOfCharacters) {
            data = null;
            return value;
        }
        data = value.substring(maxNumberOfCharacters);
        return value.substring(0, maxNumberOfCharacters);
    }

    /**
     * Checks if the current value is numeric (integer or double).
     *
     * @return <tt>true</tt> if the wrapped value is either a {@link Number} or an {@link Amount} or
     * if it is a string which can be converted to a long or double
     */
    public boolean isNumeric() {
        return data != null && (data instanceof Number || data instanceof Amount || NUMBER.matcher(asString(""))
                                                                                          .matches());
    }

    /**
     * Returns the wrapped object
     *
     * @return the wrapped object of this <tt>Value</tt>
     */
    @Nullable
    public Object get() {
        return data;
    }

    /**
     * Returns the internal data or the given <tt>defaultValue</tt>
     *
     * @param defaultValue the value to use if the inner value is <tt>null</tt>
     * @return the wrapped value or the given defaultValue if the wrapped value is <tt>null</tt>
     */
    public Object get(Object defaultValue) {
        return data == null ? defaultValue : data;
    }

    /**
     * If the underlying data is a {@link Collection} this will return the first element wrapped as value. Otherwise
     * <tt>this</tt> is returned.
     *
     * @return the first element of the underlying collection or the element itself wrapped as value
     */
    @Nonnull
    public Value first() {
        if (data instanceof Collection) {
            Iterator<?> iter = ((Collection<?>) data).iterator();
            if (iter.hasNext()) {
                return Value.of(iter.next());
            }
            return EMPTY;
        }

        return this;
    }

    /**
     * Converts or casts the wrapped object to the given <tt>targetClazz</tt>
     *
     * @param targetClazz  the desired class to which the wrapped value should be converted or cast.
     * @param defaultValue the default value if the wrapped object is empty or cannot be cast to the given target.
     * @param <T>          the type to coerce to
     * @return a converted instance of type targetClass or the defaultValue if no conversion was possible
     * @throws IllegalArgumentException if the given <tt>targetClazz</tt> is unknown
     */
    @SuppressWarnings("unchecked")
    public <T> T coerce(Class<T> targetClazz, T defaultValue) {
        if (Boolean.class.equals(targetClazz) || boolean.class.equals(targetClazz)) {
            if (isEmptyString()) {
                return (T) Boolean.FALSE;
            }
            if (data instanceof Boolean) {
                return (T) data;
            }
            return (T) NLS.parseMachineString(Boolean.class, String.valueOf(data));
        }
        if (data == null) {
            return defaultValue;
        }
        if (targetClazz.isAssignableFrom(data.getClass())) {
            return (T) data;
        }
        return continueCoerceWithBasicTypes(targetClazz, defaultValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T continueCoerceWithBasicTypes(Class<T> targetClazz, T defaultValue) {
        if (Integer.class.equals(targetClazz) || int.class.equals(targetClazz)) {
            if (data instanceof Double) {
                return (T) (Integer) ((Long) Math.round((Double) data)).intValue();
            }
            return (T) getInteger();
        }
        if (Long.class.equals(targetClazz) || long.class.equals(targetClazz)) {
            if (data instanceof Double) {
                return (T) (Long) Math.round((Double) data);
            }
            return (T) getLong();
        }
        if (String.class.equals(targetClazz)) {
            return (T) NLS.toMachineString(data);
        }
        if (BigDecimal.class.equals(targetClazz)) {
            return (T) getBigDecimal();
        }
        if (Amount.class.equals(targetClazz)) {
            return (T) getAmount();
        }
        return continueCoerceWithDateTypes(targetClazz, defaultValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T continueCoerceWithDateTypes(Class<T> targetClazz, T defaultValue) {
        if (LocalDate.class.equals(targetClazz) && is(TemporalAccessor.class,
                                                      Calendar.class,
                                                      Date.class,
                                                      java.sql.Date.class,
                                                      Timestamp.class)) {
            return (T) asLocalDate((LocalDate) defaultValue);
        }
        if (LocalDateTime.class.equals(targetClazz) && is(TemporalAccessor.class,
                                                          Calendar.class,
                                                          Date.class,
                                                          java.sql.Date.class,
                                                          Timestamp.class)) {
            return (T) asLocalDateTime((LocalDateTime) defaultValue);
        }
        if (ZonedDateTime.class.equals(targetClazz) && is(TemporalAccessor.class,
                                                          Calendar.class,
                                                          Date.class,
                                                          java.sql.Date.class,
                                                          Timestamp.class)) {
            return (T) asZonedDateTime((ZonedDateTime) defaultValue);
        }

        if (LocalTime.class.equals(targetClazz) && is(TemporalAccessor.class,
                                                      Calendar.class,
                                                      Date.class,
                                                      java.sql.Date.class,
                                                      Timestamp.class,
                                                      Time.class)) {
            return (T) asLocalTime((LocalTime) defaultValue);
        }

        return continueCoerceWithEnumTypes(targetClazz, defaultValue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T continueCoerceWithEnumTypes(Class<T> targetClazz, T defaultValue) {
        if (targetClazz.isEnum()) {
            try {
                if (Strings.isEmpty(asString(""))) {
                    return defaultValue;
                }
                return (T) Enum.valueOf((Class<Enum>) targetClazz, asString(""));
            } catch (Exception e) {
                Exceptions.ignore(e);
                return (T) Enum.valueOf((Class<Enum>) targetClazz, asString("").toUpperCase());
            }
        }
        return continueCoerceWithConversion(targetClazz, defaultValue);
    }

    private <T> T continueCoerceWithConversion(Class<T> targetClazz, T defaultValue) {
        if (data instanceof String) {
            try {
                return NLS.parseMachineString(targetClazz, data.toString().trim());
            } catch (Exception e) {
                Exceptions.ignore(e);
                return defaultValue;
            }
        }

        throw new IllegalArgumentException(Strings.apply("Cannot convert '%s' to target class: %s ",
                                                         data,
                                                         targetClazz));
    }

    /**
     * Returns the wrapped value if it is an instance of the given clazz or the <tt>defaultValue</tt> otherwise.
     *
     * @param clazz        the desired class of the return type
     * @param defaultValue the value which is returned if the wrapped value is not assignable to the given class.
     * @param <V>          the expected type of the wrapped value
     * @return the wrapped value if the given <tt>clazz</tt> is assignable from wrapped values class
     * or the <tt>defaultValue</tt> otherwise
     */
    @SuppressWarnings("unchecked")
    public <V> V get(Class<V> clazz, V defaultValue) {
        Object result = get();
        if (result == null || !clazz.isAssignableFrom(result.getClass())) {
            return defaultValue;
        }
        return (V) result;
    }

    /**
     * Returns the data converted to a string, or <tt>null</tt> if the wrapped value is null
     * <p>
     * The conversion method used is {@link NLS#toMachineString(Object)}. <b>Note</b> that this method will
     * perform an automatic {@link String#trim()} if the wrapped value is a String. Use {@link #getRawString()}
     * to suppress this behaviour.
     * <p>
     * Also note that there is a null-safe method {@link #asString()} or {@link #asString(String)} which replaces
     * <tt>null</tt> with an empty string.
     *
     * @return a string representation of the wrapped object or <tt>null</tt> if the wrapped value is <tt>null</tt>
     * @see #getRawString()
     */
    @Nullable
    public String getString() {
        return isNull() ? null : NLS.toMachineString(data);
    }

    /**
     * Returns the wrapped data converted to a string or <tt>defaultValue</tt> if the wrapped value is <tt>null</tt>
     * <p>
     * The conversion method used is {@link NLS#toMachineString(Object)}. <b>Note</b> that this method will
     * perform an automatic {@link String#trim()} if the wrapped value is a String. Use {@link #getRawString()}
     * to suppress this behaviour. However, be aware that this method will not replace <tt>null</tt> with an empty
     * string.
     *
     * @param defaultValue the value to use if the wrapped object was <tt>null</tt>
     * @return a string representation of the wrapped object
     * or <tt>defaultValue</tt> if the wrapped value is <tt>null</tt>
     * @see #getString()
     * @see #getRawString()
     */
    @Nonnull
    public String asString(@Nonnull String defaultValue) {
        return isNull() ? defaultValue : NLS.toMachineString(data);
    }

    /**
     * Returns the wrapped data converted to a string or <tt>""</tt> if the wrapped value is <tt>null</tt>
     * <p>
     * This is a convenience method for {@code asString("")}. <b>Note</b> that this method will
     * * perform an automatic {@link String#trim()} if the wrapped value is a String.
     *
     * @return a string representation of the wrapped object or <tt>""</tt> if the wrapped value is <tt>null</tt>
     * @see #getString()
     * @see #getRawString()
     */
    @Nonnull
    public String asString() {
        return NLS.toMachineString(data);
    }

    /**
     * Returns a string representation of the wrapped value by using {@link #asString()}.
     *
     * @return a string representation of the wrapped value
     */
    @Override
    public String toString() {
        return asString();
    }

    /**
     * Returns the unprocessed string representation of the wrapped value.
     * <p>
     * In contrast to {@link #getString()} this method <b>will not</b> trim the wrapped string value.
     *
     * @return either the raw and untrimmed string value if one is wrapped, otherwise the same as {@link #getString()}
     */
    @Nullable
    public String getRawString() {
        if (data instanceof String string) {
            return string;
        }

        return getString();
    }

    /**
     * Replaces "" with <tt>null</tt> and returns the newly wrapped value.
     * <p>
     * Note that this will only check for an empty string. If whitespaces like "   " should also be
     * replaced with <tt>null</tt> {@code this.trimmed().replaceEmptyWithNull()} has to be called.
     *
     * @return {@link Value#EMPTY} if an empty string is wrapped otherwise <tt>this</tt> will be returned
     */
    @Nonnull
    public Value replaceEmptyWithNull() {
        if (isEmptyString()) {
            return Value.EMPTY;
        } else {
            return this;
        }
    }

    /**
     * Boilerplate method which will replace empty strings and "only whitespace" strings by <tt>null</tt>.
     * <p>
     * Note that if a string is present, it will remain unprocessed (untrimmed) within the value.
     *
     * @return {@link Value#EMPTY} if an empty string or one that only consists of whitespace is wrapped,
     * otherwise <tt>this</tt> will be returned
     */
    @Nonnull
    public Value replaceWhitespaceWithNull() {
        if (trimmed().isEmptyString()) {
            return Value.EMPTY;
        } else {
            return this;
        }
    }

    /**
     * Returns the wrapped data converted to a string like {@link #asString()}
     * while "smart rounding" ({@link NLS#smartRound(double)} <tt>Double</tt> and <tt>BigDecimal</tt> values.
     * <p>
     * This method behaves just like <tt>asString</tt>, except for <tt>Double</tt> and <tt>BigDecimal</tt> values
     * where the output is "smart rounded". Therefore, 12.34 will be formatted as {@code 12.34} but 1.000 will
     * be formatted as {@code 1}
     *
     * @return a string representation of the wrapped object as generated by <tt>asString</tt>
     * except for <tt>Double</tt> or <tt>BigDecimal</tt> values, which are "smart rounded".
     * @see NLS#smartRound(double)
     */
    @Nonnull
    public String asSmartRoundedString() {
        if (data == null) {
            return "";
        }
        if (data instanceof Double doubleValue) {
            return NLS.smartRound(doubleValue);
        }
        if (data instanceof BigDecimal bigDecimal) {
            return NLS.smartRound(bigDecimal.doubleValue());
        }
        return asString();
    }

    /**
     * Converts the wrapped value to a <tt>boolean</tt> or returns the given <tt>defaultValue</tt>
     * if no conversion is possible.
     * <p>
     * To convert a value, {@link Boolean#parseBoolean(String)} is used, where {@code toString} is called on all
     * non-string objects.
     *
     * @param defaultValue the value to be used if the wrapped value cannot be converted to a boolean.
     * @return <tt>true</tt> if the wrapped value is <tt>true</tt>
     * or if the string representation of it is {@code "true"}. Returns <tt>false</tt> otherwise,
     * especially if the wrapped value is <tt>null</tt>
     */
    public boolean asBoolean(boolean defaultValue) {
        if (isNull() || Strings.isEmpty(data)) {
            return defaultValue;
        }
        if (data instanceof Boolean booleanValue) {
            return booleanValue;
        }

        // fast-track for common cases without the need to involve NLS framework
        if ("true".equalsIgnoreCase(String.valueOf(data))) {
            return true;
        }
        if ("false".equalsIgnoreCase(String.valueOf(data))) {
            return false;
        }

        return NLS.parseUserString(Boolean.class, String.valueOf(data).trim());
    }

    /**
     * Boilerplate method for {@code asBoolean(false)}
     *
     * @return <tt>true</tt> if the wrapped value is <tt>true</tt>
     * or if the string representation of it is {@code "true"}. Returns <tt>false</tt> otherwise,
     * especially if the wrapped value is <tt>null</tt>
     */
    public boolean asBoolean() {
        return asBoolean(false);
    }

    /**
     * Returns the int value for the wrapped value or <tt>defaultValue</tt> if the wrapped value isn't an integer and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is an <tt>Integer</tt> or <tt>BigDecimal</tt>, it is either directly returned or converted
     * by calling {@link java.math.BigDecimal#longValue()}.
     * <p>
     * Otherwise {@link Integer#parseInt(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     *
     * @param defaultValue the value to be used, if no conversion to <tt>int</tt> is possible.
     * @return the wrapped value cast or converted to <tt>int</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public int asInt(int defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof Integer integer) {
                return integer;
            }
            if (data instanceof BigDecimal bigDecimal) {
                return (int) bigDecimal.longValue();
            }

            return Integer.parseInt(String.valueOf(data).trim());
        } catch (NumberFormatException e) {
            Exceptions.ignore(e);
            return defaultValue;
        }
    }

    /**
     * Returns the int value for the wrapped value or <tt>null</tt> if the wrapped value isn't an integer and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is an <tt>Integer</tt> or <tt>BigDecimal</tt>, it is either directly returned or converted
     * by calling {@link java.math.BigDecimal#longValue()}.
     * <p>
     * Otherwise {@link Integer#parseInt(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, <tt>null</tt> will be returned.
     *
     * @return the wrapped value cast or converted to <tt>Integer</tt> or <tt>null</tt>
     * if no conversion is possible.
     */
    @Nullable
    public Integer getInteger() {
        try {
            if (isNull()) {
                return null;
            }
            if (data instanceof Integer integer) {
                return integer;
            }
            if (data instanceof BigDecimal) {
                return (int) ((BigDecimal) data).longValue();
            }
            return Integer.parseInt(String.valueOf(data).trim());
        } catch (NumberFormatException e) {
            Exceptions.ignore(e);
            return null;
        }
    }

    /**
     * Returns the long value for the wrapped value or <tt>defaultValue</tt> if the wrapped value isn't a long and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>Long</tt>, <tt>Integer</tt> or <tt>BigDecimal</tt>,
     * it is either directly returned or converted by calling {@link java.math.BigDecimal#longValue()}.
     * <p>
     * Otherwise {@link Long#parseLong(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     *
     * @param defaultValue the value to be used, if no conversion to <tt>long</tt> is possible.
     * @return the wrapped value cast or converted to <tt>long</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public long asLong(long defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof Long longValue) {
                return longValue;
            }
            if (data instanceof Integer integer) {
                return integer;
            }
            if (data instanceof BigDecimal bigDecimal) {
                return bigDecimal.longValue();
            }
            return Long.parseLong(String.valueOf(data).trim());
        } catch (NumberFormatException e) {
            Exceptions.ignore(e);
            return defaultValue;
        }
    }

    /**
     * Returns the long value for the wrapped value or <tt>null</tt> if the wrapped value isn't a long and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>Long</tt>, <tt>Integer</tt> or <tt>BigDecimal</tt>, it is either directly
     * returned or by calling {@link java.math.BigDecimal#longValue()}.
     * <p>
     * Otherwise {@link Long#parseLong(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, <tt>null</tt> will be returned.
     *
     * @return the wrapped value cast or converted to <tt>Long</tt> or <tt>null</tt>
     * if no conversion is possible.
     */
    @Nullable
    public Long getLong() {
        try {
            if (isNull()) {
                return null;
            }
            if (data instanceof Long longValue) {
                return longValue;
            }
            return Long.parseLong(String.valueOf(data).trim());
        } catch (NumberFormatException e) {
            Exceptions.ignore(e);
            return null;
        }
    }

    /**
     * Returns the double value for the wrapped value or <tt>defaultValue</tt> if the wrapped value isn't a double and
     * cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>Double</tt>, <tt>Long</tt>, <tt>Integer</tt> or <tt>BigDecimal</tt>,
     * it is either directly returned or converted by calling {@link java.math.BigDecimal#doubleValue()}.
     * <p>
     * Otherwise {@link Double#parseDouble(String)} is called on the string representation of the wrapped value. If
     * parsing fails, or if the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     *
     * @param defaultValue the value to be used, if no conversion to <tt>double</tt> is possible.
     * @return the wrapped value cast or converted to <tt>double</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public double asDouble(double defaultValue) {
        try {
            if (isNull()) {
                return defaultValue;
            }
            if (data instanceof Double doubleValue) {
                return doubleValue;
            }
            if (data instanceof Long longValue) {
                return longValue;
            }
            if (data instanceof Integer integer) {
                return integer;
            }
            if (data instanceof BigDecimal bigDecimal) {
                return bigDecimal.doubleValue();
            }
            return Double.parseDouble(String.valueOf(data).trim());
        } catch (NumberFormatException e) {
            Exceptions.ignore(e);
            return defaultValue;
        }
    }

    /**
     * Returns the wrapped value as {@link java.time.LocalDate} or <tt>defaultValue</tt> if the wrapped value
     * cannot be converted.
     * <p>
     * If the wrapped value is an <tt>Instant</tt>, <tt>LocalDateTime</tt>, <tt>ZonedDateTime</tt>,
     * <tt>Date</tt>, <tt>Calendar</tt>, <tt>java.sql.Date</tt>, <tt>Timestamp</tt> or <tt>long</tt>,
     * it is converted to a {@link java.time.LocalDate}.
     *
     * @param defaultValue the value to be used, if no conversion is possible
     * @return the wrapped value cast or converted to <tt>LocalDate</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public LocalDate asLocalDate(LocalDate defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        if (is(Instant.class)) {
            return LocalDate.from((Instant) data);
        }
        if (is(LocalDate.class)) {
            return (LocalDate) data;
        }
        if (is(LocalDateTime.class)) {
            return ((LocalDateTime) data).toLocalDate();
        }
        if (is(ZonedDateTime.class)) {
            return ((ZonedDateTime) data).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        }
        if (is(Date.class)) {
            return Instant.ofEpochMilli(((Date) data).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (is(Calendar.class)) {
            return Instant.ofEpochMilli(((Calendar) data).getTimeInMillis())
                          .atZone(ZoneId.systemDefault())
                          .toLocalDate();
        }
        if (is(java.sql.Date.class)) {
            return Instant.ofEpochMilli(((java.sql.Date) data).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (is(Timestamp.class)) {
            return Instant.ofEpochMilli(((java.sql.Timestamp) data).getTime())
                          .atZone(ZoneId.systemDefault())
                          .toLocalDate();
        }
        if (is(long.class) || is(Long.class)) {
            return Instant.ofEpochMilli((long) data).atZone(ZoneId.systemDefault()).toLocalDate();
        }

        return defaultValue;
    }

    /**
     * Returns the wrapped value as {@link java.time.LocalDateTime} or <tt>defaultValue</tt> if the wrapped value
     * cannot be converted.
     * <p>
     * If the wrapped value is an <tt>Instant</tt>, <tt>LocalDateTime</tt>, <tt>ZonedDateTime</tt>,
     * <tt>Date</tt>, <tt>Calendar</tt>, <tt>java.sql.Date</tt>, <tt>Timestamp</tt> or <tt>long</tt>,
     * it is converted to a {@link java.time.LocalDateTime}.
     *
     * @param defaultValue the value to be used, if no conversion is possible
     * @return the wrapped value cast or converted to <tt>LocalDateTime</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public LocalDateTime asLocalDateTime(LocalDateTime defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        if (is(Instant.class)) {
            return LocalDateTime.from((Instant) data);
        }
        if (is(LocalDateTime.class)) {
            return (LocalDateTime) data;
        }
        if (is(LocalTime.class)) {
            return ((LocalTime) data).atDate(LocalDate.now());
        }
        if (is(ZonedDateTime.class)) {
            return ((ZonedDateTime) data).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (is(Date.class)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Date) data).getTime()), ZoneId.systemDefault());
        }
        if (is(Calendar.class)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Calendar) data).getTimeInMillis()),
                                           ZoneId.systemDefault());
        }
        if (is(java.sql.Date.class)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((java.sql.Date) data).getTime()),
                                           ZoneId.systemDefault());
        }
        if (is(Timestamp.class)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((java.sql.Timestamp) data).getTime()),
                                           ZoneId.systemDefault());
        }
        if (is(long.class) || is(Long.class)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli((long) data), ZoneId.systemDefault());
        }

        return defaultValue;
    }

    /**
     * Returns the wrapped value as {@link java.time.LocalTime} or <tt>defaultValue</tt> if the wrapped value
     * cannot be converted.
     * <p>
     * If the wrapped value is an <tt>Instant</tt>, <tt>LocalDateTime</tt>, <tt>ZonedDateTime</tt>,
     * <tt>Date</tt>, <tt>Calendar</tt>, <tt>java.sql.Date</tt>, <tt>Timestamp</tt> or <tt>long</tt>,
     * it is converted to a {@link java.time.LocalTime}.
     *
     * @param defaultValue the value to be used, if no conversion is possible
     * @return the wrapped value cast or converted to <tt>LocalTime</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public LocalTime asLocalTime(LocalTime defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        if (is(Instant.class)) {
            return LocalTime.from((Instant) data);
        }
        if (is(LocalDateTime.class)) {
            return ((LocalDateTime) data).toLocalTime();
        }
        if (is(LocalTime.class)) {
            return (LocalTime) data;
        }
        if (is(ZonedDateTime.class)) {
            return ((ZonedDateTime) data).withZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
        }
        if (is(Date.class)) {
            return LocalDateTime.from(Instant.ofEpochMilli(((Date) data).getTime())).toLocalTime();
        }
        if (is(Calendar.class)) {
            return LocalDateTime.from(Instant.ofEpochMilli(((Calendar) data).getTimeInMillis())).toLocalTime();
        }
        if (is(java.sql.Date.class)) {
            return LocalDateTime.from(Instant.ofEpochMilli(((java.sql.Date) data).getTime())).toLocalTime();
        }
        if (is(Timestamp.class)) {
            return LocalDateTime.from(Instant.ofEpochMilli(((java.sql.Timestamp) data).getTime())).toLocalTime();
        }
        if (is(long.class) || is(Long.class)) {
            return LocalDateTime.from(Instant.ofEpochMilli((long) data)).toLocalTime();
        }

        return defaultValue;
    }

    /**
     * Returns the wrapped value as {@link java.time.ZonedDateTime} or <tt>defaultValue</tt> if the wrapped value
     * cannot be converted.
     * <p>
     * If the wrapped value is an <tt>Instant</tt>, <tt>LocalDateTime</tt>, <tt>ZonedDateTime</tt>,
     * <tt>Date</tt>, <tt>Calendar</tt>, <tt>java.sql.Date</tt>, <tt>Timestamp</tt> or <tt>long</tt>,
     * it is converted to a {@link java.time.ZonedDateTime}.
     *
     * @param defaultValue the value to be used, if no conversion is possible
     * @return the wrapped value cast or converted to <tt>ZonedDateTime</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public ZonedDateTime asZonedDateTime(ZonedDateTime defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        if (is(Instant.class)) {
            return ZonedDateTime.from((Instant) data);
        }
        if (is(LocalDate.class)) {
            return ((LocalDate) data).atStartOfDay(ZoneId.systemDefault());
        }
        if (is(LocalDateTime.class)) {
            return ((LocalDateTime) data).atZone(ZoneId.systemDefault());
        }
        if (is(LocalTime.class)) {
            return ((LocalTime) data).atDate(LocalDate.now()).atZone(ZoneId.systemDefault());
        }
        if (is(ZonedDateTime.class)) {
            return (ZonedDateTime) data;
        }
        if (is(Date.class)) {
            return ZonedDateTime.from(Instant.ofEpochMilli(((Date) data).getTime()));
        }
        if (is(Calendar.class)) {
            return ZonedDateTime.from(Instant.ofEpochMilli(((Calendar) data).getTimeInMillis()));
        }
        if (is(java.sql.Date.class)) {
            return ZonedDateTime.from(Instant.ofEpochMilli(((java.sql.Date) data).getTime()));
        }
        if (is(Timestamp.class)) {
            return ZonedDateTime.from(Instant.ofEpochMilli(((java.sql.Timestamp) data).getTime()));
        }
        return defaultValue;
    }

    /**
     * Returns the wrapped value as {@link java.time.Instant} or <tt>defaultValue</tt> if the wrapped value
     * cannot be converted.
     * <p>
     * If the wrapped value is an <tt>Instant</tt>, <tt>LocalDateTime</tt>, <tt>ZonedDateTime</tt>,
     * <tt>Date</tt>, <tt>Calendar</tt>, <tt>java.sql.Date</tt>, <tt>Timestamp</tt> or <tt>long</tt>,
     * it is converted to an {@link java.time.Instant}.
     *
     * @param defaultValue the value to be used, if no conversion is possible
     * @return the wrapped value cast or converted to <tt>Instant</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public Instant asInstant(Instant defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        if (is(Instant.class)) {
            return (Instant) data;
        }
        if (is(LocalDate.class)) {
            return ((LocalDate) data).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (is(LocalDateTime.class)) {
            return ((LocalDateTime) data).atZone(ZoneId.systemDefault()).toInstant();
        }
        if (is(LocalTime.class)) {
            return ((LocalTime) data).atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant();
        }
        if (is(ZonedDateTime.class)) {
            return ((ZonedDateTime) data).toInstant();
        }
        if (is(Date.class)) {
            return Instant.ofEpochMilli(((Date) data).getTime());
        }
        if (is(Calendar.class)) {
            return Instant.ofEpochMilli(((Calendar) data).getTimeInMillis());
        }
        if (is(java.sql.Date.class)) {
            return Instant.ofEpochMilli(((java.sql.Date) data).getTime());
        }
        if (is(Timestamp.class)) {
            return Instant.ofEpochMilli(((java.sql.Timestamp) data).getTime());
        }
        return defaultValue;
    }

    /**
     * Converts the wrapped number into an {@link java.time.Instant} by assuming the number represents a
     * "unix timestamp" in seconds.
     *
     * @param defaultValue used if an invalid or non-numeric value was found
     * @return the <tt>Instant</tt> as determined by the wrapped timestamp value or <tt>defaultValue</tt> if no
     * conversion was possible
     */
    public Instant asInstantOfEpochSeconds(Instant defaultValue) {
        long epochSeconds = asLong(-1);
        if (epochSeconds < 0) {
            return defaultValue;
        }
        return Instant.ofEpochSecond(epochSeconds);
    }

    /**
     * Converts the wrapped number into an {@link java.time.Instant} by assuming the number represents a
     * "unix timestamp" in milliseconds.
     *
     * @param defaultValue used if an invalid or non-numeric value was found
     * @return the <tt>Instant</tt> as determined by the wrapped timestamp value or <tt>defaultValue</tt> if no
     * conversion was possible
     */
    public Instant asInstantOfEpochMillis(Instant defaultValue) {
        long epochSeconds = asLong(-1);
        if (epochSeconds < 0) {
            return defaultValue;
        }
        return Instant.ofEpochMilli(epochSeconds);
    }

    /**
     * Converts the wrapped number into  {@link java.time.LocalDateTime} by assuming the number represents a
     * "unix timestamp" in milliseconds.
     *
     * @param defaultValue used if an invalid or non-numeric value was found
     * @return the <tt>LocalDateTime</tt> as determined by the wrapped timestamp value or <tt>defaultValue</tt> if no
     * conversion was possible
     */
    public LocalDateTime asLocalDateTimeOfEpochMillis(LocalDateTime defaultValue) {
        Instant temporal = asInstantOfEpochMillis(null);
        if (temporal == null) {
            return defaultValue;
        }
        return LocalDateTime.ofInstant(temporal, ZoneId.systemDefault());
    }

    /**
     * Converts the wrapped number into  {@link java.time.LocalDateTime} by assuming the number represents a
     * "unix timestamp" in seconds.
     *
     * @param defaultValue used if an invalid or non-numeric value was found
     * @return the <tt>LocalDateTime</tt> as determined by the wrapped timestamp value or <tt>defaultValue</tt> if no
     * conversion was possible
     */
    public LocalDateTime asLocalDateTimeOfEpochSeconds(LocalDateTime defaultValue) {
        Instant temporal = asInstantOfEpochSeconds(null);
        if (temporal == null) {
            return defaultValue;
        }
        return LocalDateTime.ofInstant(temporal, ZoneId.systemDefault());
    }

    /**
     * Parses the value as {@link LocalDate} using the given {@link DateTimeFormatter formatter}.
     *
     * @param formatter the formatter for the expected date format
     * @return the parsed LocalDate wrapped as a Value on success. Returns the Value itself, if it or the given
     * formatter were empty, or if a parsing error occurred.
     */
    public Value tryParseLocalDate(DateTimeFormatter formatter) {
        if (isNull() || formatter == null) {
            return this;
        }
        LocalDate localDate = asLocalDate(null);
        if (localDate != null) {
            return Value.of(localDate);
        }
        try {
            return Value.of(LocalDate.parse(getString(), formatter));
        } catch (DateTimeParseException exception) {
            Exceptions.ignore(exception);
            return this;
        }
    }

    /**
     * Parses the value as {@link LocalDate} using the date format for the given language.
     *
     * @param languageCode the language to format the date for
     * @return the parsed LocalDate wrapped as a Value on success. Returns the Value itself, if it or the given
     * language were empty, or if a parsing error occurred.
     */
    public Value tryParseLocalDate(String languageCode) {
        if (Strings.isEmpty(languageCode)) {
            return this;
        }
        return tryParseLocalDate(NLS.getDateFormat(languageCode));
    }

    /**
     * Returns the <tt>BigDecimal</tt> value for the wrapped value or <tt>defaultValue</tt> if the wrapped value
     * isn't a BigDecimal and cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>BigDecimal</tt>, <tt>Double</tt>, <tt>Long</tt> or <tt>Integer</tt>,
     * it is either directly returned or converted by calling <tt>java.math.BigDecimal#valueOf</tt>.
     * <p>
     * Otherwise {@link BigDecimal#BigDecimal(String, java.math.MathContext)} is called on the string representation
     * of the wrapped value (with "," replaced to ".") and <tt>MathContext.UNLIMITED</tt>. If parsing fails, or if
     * the wrapped value was <tt>null</tt>, the <tt>defaultValue</tt> will be returned.
     *
     * @param defaultValue the value to be used, if no conversion to <tt>BigDecimal</tt> is possible.
     * @return the wrapped value cast or converted to <tt>BigDecimal</tt> or <tt>defaultValue</tt>
     * if no conversion is possible.
     */
    public BigDecimal getBigDecimal(BigDecimal defaultValue) {
        BigDecimal result = getBigDecimal();
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    /**
     * Returns the <tt>BigDecimal</tt> value for the wrapped value or <tt>null</tt> if the wrapped value
     * isn't a BigDecimal and cannot be converted to one.
     * <p>
     * If the wrapped value is a <tt>BigDecimal</tt>, <tt>Double</tt>, <tt>Long</tt> or <tt>Integer</tt>,
     * it is either directly returned or converted by calling <tt>java.math.BigDecimal#valueOf</tt>.
     * <p>
     * Otherwise {@link BigDecimal#BigDecimal(String, java.math.MathContext)} is called on the string representation
     * of the wrapped value (with "," replaced to ".") and <tt>MathContext.UNLIMITED</tt>. If parsing fails, or if
     * the wrapped value was <tt>null</tt>, the <tt>null</tt> will be returned.
     *
     * @return the wrapped value cast or converted to <tt>BigDecimal</tt> or <tt>null</tt>
     * if no conversion is possible.
     */
    @Nullable
    public BigDecimal getBigDecimal() {
        try {
            if (isNull()) {
                return null;
            }
            if (data instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }
            if (data instanceof Amount amount) {
                return amount.getAmount();
            }
            if (data instanceof Double doubleValue) {
                return BigDecimal.valueOf(doubleValue);
            }
            if (data instanceof Long longValue) {
                return BigDecimal.valueOf(longValue);
            }
            if (data instanceof Integer integer) {
                return BigDecimal.valueOf(integer);
            }
            return new BigDecimal(asString().replace(',', '.').trim(), MathContext.UNLIMITED);
        } catch (NumberFormatException e) {
            Exceptions.ignore(e);
            return null;
        }
    }

    /**
     * Returns the <tt>Amount</tt> for the wrapped value.
     * <p>
     * Note that this will enforce a scale of {@link Amount#SCALE} (5) for the given value to ensure a consistent
     * behaviour. If the given value already has the desired scale set, use {@link #getRoundedAmount()}.
     * <p>
     * If the wrapped value can be converted to a BigDecimal ({@link #getBigDecimal(java.math.BigDecimal)},
     * an <tt>Amount</tt> for the result is returned. Otherwise, an empty <tt>Amount</tt> is returned.
     *
     * @return the wrapped value converted to <tt>Amount</tt>. The result might be an empty amount, if the wrapped
     * value is <tt>null</tt> or if no conversion was possible.
     * @see #getBigDecimal(java.math.BigDecimal)
     */
    public Amount getAmount() {
        if (data instanceof Amount amount) {
            return amount;
        }
        return Amount.of(getBigDecimal());
    }

    /**
     * Returns the <tt>Amount</tt> for the wrapped value.
     * <p>
     * Note that this keeps the scale of the given value.
     * <p>
     * If the wrapped value can be converted to a BigDecimal ({@link #getBigDecimal(java.math.BigDecimal)},
     * an <tt>Amount</tt> for the result is returned. Otherwise, an empty <tt>Amount</tt> is returned.
     *
     * @return the wrapped value converted to <tt>Amount</tt>. The result might be an empty amount, if the wrapped
     * value is <tt>null</tt> or if no conversion was possible.
     * @see #getBigDecimal(java.math.BigDecimal)
     */
    public Amount getRoundedAmount() {
        if (data instanceof Amount amount) {
            return amount;
        }
        return Amount.ofRounded(getBigDecimal());
    }

    /**
     * Converts the wrapped value to an enum constant of the given <tt>clazz</tt>.
     *
     * @param clazz the type of the enum to use
     * @param <E>   the type of the enum
     * @return an enum constant of the given <tt>clazz</tt> with the same name as the wrapped value
     * or <tt>null</tt> if no matching constant was found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <E extends Enum<E>> E asEnum(Class<E> clazz) {
        if (data == null) {
            return null;
        }
        if (clazz.isAssignableFrom(data.getClass())) {
            return (E) data;
        }
        try {
            return Enum.valueOf(clazz, String.valueOf(data).trim());
        } catch (Exception e) {
            Exceptions.ignore(e);
            return null;
        }
    }

    /**
     * Converts the wrapped value to an enum constant of the given <tt>clazz</tt>.
     *
     * @param clazz the enum to convert to
     * @param <E>   to generic type of the enum
     * @return the enum constant wrapped as optional or an empty optional if no conversion was possible.
     */
    @Nonnull
    public <E extends Enum<E>> Optional<E> getEnum(Class<E> clazz) {
        return Optional.ofNullable(asEnum(clazz));
    }

    /**
     * Checks if the string representation of the wrapped value starts with the given string.
     *
     * @param value the substring with which the string representation must start
     * @return <tt>true</tt> if the string representation starts with <tt>value</tt>, <tt>false</tt> otherwise.
     * If the current value is empty, it is treated as ""
     */
    public boolean startsWith(@Nonnull String value) {
        return asString().startsWith(value);
    }

    /**
     * Checks if the string representation of the wrapped value ends with the given string.
     *
     * @param value the substring with which the string representation must end
     * @return <tt>true</tt> if the string representation ends with <tt>value</tt>, <tt>false</tt> otherwise.
     * If the current value is empty, it is treated as ""
     */
    public boolean endsWith(@Nonnull String value) {
        return asString().endsWith(value);
    }

    /**
     * Returns a trimmed version of the string representation of the wrapped value.
     * <p>
     * The conversion method used is {@link #asString()}, therefore an empty value will yield {@code ""}.
     * <p>
     * Note that calling this method is almost always pointless as {@link #asString()} does an automatic trim.
     * Therefore, this will only provide an effect if a non-string value is wrapped which yields a string representation
     * with leading or trailing whitespaces.
     *
     * @return a string representing the wrapped value without leading or trailing spaces.
     * @see String#trim()
     * @see #trimmed()
     */
    @Nonnull
    public String trim() {
        return asString().trim();
    }

    /**
     * Trims the internal contents and returns the resulting value.
     *
     * @return a value which wraps the trimmed contents of this value.
     * @see String#trim()
     */
    public Value trimmed() {
        if (isNull()) {
            return this;
        }

        return Value.of(asString().trim());
    }

    /**
     * Returns the first N (<tt>length</tt>) characters of the string representation of the wrapped value.
     * <p>
     * If the wrapped value is <tt>null</tt>, {@code ""} will be returned. If the string representation is
     * shorter than <tt>length</tt>, the whole string is returned.
     * <p>
     * If <tt>length</tt> is negative, the string representation <b>without</b> the first N (<tt>length</tt>)
     * characters is returned. If the string representation is too short, {@code ""} is returned.
     *
     * @param length the number of characters to return or to omit (if <tt>length</tt> is negative)
     * @return the first N characters (or less if the string representation of the wrapped value is shorter)
     * or the string representation without the first N characters (or "" if the representation is too short)
     * if <tt>length is negative</tt>. Returns {@code ""} if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String left(int length) {
        if (isNull()) {
            return "";
        }
        String value = asString();
        if (length < 0) {
            length = length * -1;
            if (value.length() < length) {
                return "";
            }
            return value.substring(length);
        } else {
            if (value.length() < length) {
                return value;
            }
            return value.substring(0, length);
        }
    }

    /**
     * Returns the last N (<tt>length</tt>) characters of the string representation of the wrapped value.
     * <p>
     * If the wrapped value is <tt>null</tt>, {@code ""} will be returned. If the string representation is
     * shorter than <tt>length</tt>, the whole string is returned.
     * <p>
     * If <tt>length</tt> is negative, the string representation <b>without</b> the last N (<tt>length</tt>)
     * characters is returned. If the string representation is too short, {@code ""} is returned.
     *
     * @param length the number of characters to return or to omit (if <tt>length</tt> is negative)
     * @return the last N characters (or less if the string representation of the wrapped value is shorter)
     * or the string representation without the last N characters (or "" if the representation is too short)
     * if <tt>length is negative</tt>. Returns {@code ""} if the wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String right(int length) {
        if (isNull()) {
            return "";
        }
        String value = asString();
        if (length < 0) {
            length = length * -1;
            if (value.length() < length) {
                return value;
            }
            return value.substring(0, value.length() - length);
        } else {
            if (value.length() < length) {
                return value;
            }
            return value.substring(value.length() - length);
        }
    }

    /**
     * Returns the substring of the internal value starting right after the last occurrence of the given separator.
     * <p>
     * If the separator is not found in the string, or if the internal value is empty, "" is returned.
     * <p>
     * An example would be:
     * <pre>
     *         {@code Value.of("test.tmp.pdf").afterLast("."); // returns "pdf"}
     * </pre>
     *
     * @param separator the separator string to search for
     * @return the substring right after the last occurrence of the given separator. This will not include the
     * separator itself.
     */
    @Nonnull
    public String afterLast(@Nonnull String separator) {
        if (!isEmptyString()) {
            int idx = asString().lastIndexOf(separator);
            if (idx > -1) {
                return left(idx * -1 - 1);
            }
        }
        return "";
    }

    /**
     * Returns the substring of the internal value containing everything up to the last occurrence of the given
     * separator.
     * <p>
     * If the separator is not found in the string, or if the internal value is empty, "" is returned.
     * <p>
     * An example would be:
     * <pre>
     *         {@code Value.of("test.tmp.pdf").beforeLast("."); // returns "test.tmp"}
     * </pre>
     *
     * @param separator the separator string to search for
     * @return the substring up to the last occurrence of the given separator. This will not include the separator
     * itself.
     */
    @Nonnull
    public String beforeLast(@Nonnull String separator) {
        if (!isEmptyString()) {
            int idx = asString().lastIndexOf(separator);
            if (idx > -1) {
                return left(idx);
            }
        }
        return "";
    }

    /**
     * Returns the substring of the internal value starting right after the first occurrence of the given separator.
     * <p>
     * If the separator is not found in the string, or if the internal value is empty, "" is returned.
     * <p>
     * An example would be:
     * <pre>
     *         {@code Value.of("test.tmp.pdf").afterFirst("."); // returns "tmp.pdf"}
     * </pre>
     *
     * @param separator the separator string to search for
     * @return the substring right after the first occurrence of the given separator. This will not include the
     * separator itself.
     */
    @Nonnull
    public String afterFirst(@Nonnull String separator) {
        if (!isEmptyString()) {
            int idx = asString().indexOf(separator);
            if (idx > -1) {
                return left(idx * -1 - 1);
            }
        }
        return "";
    }

    /**
     * Returns the substring of the internal value containing everything up to the first occurrence of the given
     * separator.
     * <p>
     * If the separator is not found in the string, or if the internal value is empty, "" is returned.
     * <p>
     * An example would be:
     * <pre>
     *         {@code Value.of("test.tmp.pdf").beforeFirst("."); // returns "test"}
     * </pre>
     *
     * @param separator the separator string to search for
     * @return the substring up to the first occurrence of the given separator. This will not include the separator
     * itself.
     */
    @Nonnull
    public String beforeFirst(@Nonnull String separator) {
        if (!isEmptyString()) {
            int idx = asString().indexOf(separator);
            if (idx > -1) {
                return left(idx);
            }
        }
        return "";
    }

    /**
     * Returns a substring of the string representation of the wrapped value.
     * <p>
     * Returns the substring starting at <tt>startIndex</tt> and ending at <tt>endIndex</tt>. If the given
     * end index is greater than the string length, the complete substring from <tt>startIndex</tt> to the end of
     * the string is returned. If the <tt>startIndex</tt> is greater than the string length, {@code ""} is
     * returned.
     *
     * @param startIndex the index of the first character to be included in the sub string
     * @param endIndex   the index of the last character to be included in the sub string
     * @return a substring like {@link String#substring(int, int)} or {@code ""} if the wrapped value
     */
    @Nonnull
    public String substring(int startIndex, int endIndex) {
        if (isNull()) {
            return "";
        }
        String value = asString();
        if (startIndex > value.length()) {
            return "";
        }
        return value.substring(startIndex, Math.min(value.length(), endIndex));
    }

    /**
     * Returns the length of the string representation of the wrapped value.
     *
     * @return the length of the string representation of the wrapped value or 0, if the wrapped value is <tt>null</tt>
     */
    public int length() {
        if (isNull()) {
            return 0;
        }
        return asString().length();
    }

    /**
     * Returns an uppercase version of the string representation of the wrapped value.
     *
     * @return an uppercase version of the string representation of the wrapped value or {@code ""} if the
     * wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String toUpperCase() {
        if (isNull()) {
            return "";
        }
        return asString().toUpperCase();
    }

    /**
     * Returns a wrapped string which represents the {@link #toUpperCase() upper-case} representation of this value.
     *
     * @return a value which wraps the upper-cased version of this value
     */
    public Value upperCase() {
        return isNull() ? this : Value.of(asString().toUpperCase());
    }

    /**
     * Returns a lowercase version of the string representation of the wrapped value.
     *
     * @return a lowercase version of the string representation of the wrapped value or {@code ""} if the
     * wrapped value is <tt>null</tt>
     */
    @Nonnull
    public String toLowerCase() {
        if (isNull()) {
            return "";
        }
        return asString().toLowerCase();
    }

    /**
     * Returns a wrapped string which represents the {@link #toLowerCase() lower-case} representation of this value.
     *
     * @return a value which wraps the lower-cased version of this value
     */
    public Value lowerCase() {
        return isNull() ? this : Value.of(asString().toLowerCase());
    }

    /**
     * Checks if the value implements one of the given classes.
     *
     * @param classes the classes to check against
     * @return <tt>true</tt> if the wrapped value is assignable to one of the given <tt>classes</tt>
     */
    public boolean is(Class<?>... classes) {
        if (data == null) {
            return false;
        }
        for (Class<?> clazz : classes) {
            if (clazz.isAssignableFrom(data.getClass())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Replaces the given <tt>pattern</tt> with the given replacement in the string representation
     * of the wrapped object
     *
     * @param pattern     the pattern to replace
     * @param replacement the replacement to be used for <tt>pattern</tt>
     * @return a <tt>Value</tt> where all occurrences of pattern in the string <tt>representation</tt> of the
     * wrapped value are replaced by <tt>replacement</tt>. If the wrapped value is null, <tt>this</tt>
     * is returned.
     */
    @Nonnull
    public Value replace(String pattern, String replacement) {
        if (data != null) {
            return Value.of(data.toString().replace(pattern, replacement));
        }
        return this;
    }

    /**
     * Replaces the given regular expression <tt>pattern</tt> with the given replacement in the string representation
     * of the wrapped object
     *
     * @param pattern     the regular expression to replace
     * @param replacement the replacement to be used for <tt>pattern</tt>
     * @return a <tt>Value</tt> where all occurrences of pattern in the string <tt>representation</tt> of the
     * wrapped value are replaced by <tt>replacement</tt>. If the wrapped value is null, <tt>this</tt>
     * is returned.
     */
    @Nonnull
    public Value regExReplace(String pattern, String replacement) {
        if (data != null) {
            data = data.toString().replaceAll(pattern, replacement);
        }
        return this;
    }

    @Override
    public boolean equals(Object other) {
        // Compare for identity
        if (this == other) {
            return true;
        }
        // Unwrap values
        if (other instanceof Value value) {
            other = value.data;
        }
        // Compare for object identity
        if (data == other) {
            return true;
        }
        // Compare with null
        if (data == null) {
            return other == null;
        }
        // Call equals against wrapped data
        return data.equals(other);
    }

    /**
     * Determines if the wrapped value is equal to one of the given objects.
     * <p>
     * Instead of using {@code if (!value.in(...)) {}} consider {@link #notIn(Object...)}.
     *
     * @param objects the set of objects to check against
     * @return <tt>true</tt> if the wrapped data is contained in the given objects array, <tt>false</tt> otherwise
     */
    public boolean in(Object... objects) {
        for (Object obj : objects) {
            if (obj == null) {
                if (data == null) {
                    return true;
                }
            } else if (obj == data || obj.equals(data)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if the wrapped value isn't equal to any of the given objects.
     * <p>
     * This is the inverse of {@link #in(Object...)}
     *
     * @param objects the set of objects to check against
     * @return <tt>true</tt> if the wrapped data isn't contained in the given objects array, <tt>false</tt> otherwise
     */
    public boolean notIn(Object... objects) {
        return !in(objects);
    }

    /**
     * Determines if the string representation of the wrapped value is equal to the string representation of the given
     * object.
     * <p>In this case equality does not take differences of upper and lower case characters into account. Therefore,
     * this is boilerplate for {@code asString().equalsIgnoreCase(otherString.toString())}
     * (With proper <tt>null</tt> checks.)
     *
     * @param otherString the input to compare against
     * @return <tt>true</tt> if the string representation of the wrapped object is equal to the string representation
     * of the given parameter, where differences of lower- and uppercase are not taken into account.
     * @see String#equalsIgnoreCase(String)
     */
    public boolean equalsIgnoreCase(Object otherString) {
        if (Strings.isEmpty(otherString)) {
            return isEmptyString();
        }
        return asString().equalsIgnoreCase(otherString.toString());
    }

    @Override
    public int hashCode() {
        return data == null ? 0 : data.hashCode();
    }

    /**
     * Returns a <tt>Value</tt> containing a translated value using the string representation
     * of the wrapped value as key.
     *
     * @return a <tt>Value</tt> containing a translated value by calling {@link NLS#get(String)}
     * if the string representation of the wrapped value starts with {@code $}.
     * The dollar sign is skipped when passing the key to <tt>NLS</tt>. Otherwise <tt>this</tt> is returned.
     * @see NLS#get(String)
     * @deprecated User {@link sirius.kernel.settings.Settings#getTranslatedString(String)} if this value is
     * loaded from a Config/Settings. Otherwise use {@link NLS#smartGet(String)}
     */
    @Nonnull
    @CheckReturnValue
    @Deprecated
    public Value translate() {
        return translate(null);
    }

    /**
     * Returns a <tt>Value</tt> containing a translated value using the string representation
     * of the wrapped value as key.
     *
     * @param lang a two-letter language code for which the translation is requested
     * @return a <tt>Value</tt> containing a translated value by calling {@link NLS#get(String, String)}
     * if the string representation of the wrapped value starts with {@code $}.
     * The dollar sign is skipped when passing the key to <tt>NLS</tt>. Otherwise <tt>this</tt> is returned.
     * @see NLS#get(String, String)
     * @deprecated Use {@link sirius.kernel.settings.Settings#getTranslatedString(String, String)} if this value is
     * loaded from a Config/Settings. Otherwise use {@link NLS#smartGet(String, String)}
     */
    @Nonnull
    @CheckReturnValue
    @Deprecated
    public Value translate(String lang) {
        if (isFilled() && is(String.class)) {
            return Value.of(NLS.smartGet(asString(), lang));
        } else {
            return this;
        }
    }
}
