/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import sirius.kernel.nls.NLS
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the [Value] class.
 */
class ValueTest {

    private val DEFAULT_BIG_DECIMAL: BigDecimal = BigDecimal.TEN

    @Test
    fun `Test isFilled`() {
        assertEquals(true, Value.of(1).isFilled())
        assertEquals(true, Value.of(" ").isFilled())
        assertEquals(true, Value.of("Test").isFilled())
        assertEquals(false, Value.of("").isFilled())
        assertEquals(false, Value.of(null).isFilled())

    }

    @Test
    fun `Test isNumeric`() {
        assertEquals(true, Value.of(1).isNumeric())
        assertEquals(true, Value.of("1").isNumeric())
        assertEquals(true, Value.of(-1).isNumeric())
        assertEquals(true, Value.of("-1").isNumeric())
        assertEquals(true, Value.of(0).isNumeric())
        assertEquals(true, Value.of("0").isNumeric())
        assertEquals(true, Value.of(1.1).isNumeric())
        assertEquals(true, Value.of("1.1").isNumeric())
        assertEquals(false, Value.of("1.1.1").isNumeric())
        assertEquals(false, Value.of("").isNumeric())
        assertEquals(false, Value.of(null).isNumeric())
        assertEquals(false, Value.of("Test").isNumeric())
    }

    @Test
    fun `Test afterLast`() {
        assertEquals("pdf", Value.of("test.x.pdf").afterLast("."))
    }

    @Test
    fun `Test beforeLast`() {
        assertEquals("test.x", Value.of("test.x.pdf").beforeLast("."))
    }

    @Test
    fun `Test afterFirst`() {
        assertEquals("x.pdf", Value.of("test.x.pdf").afterFirst("."))
    }

    @Test
    fun `Test beforeFirst`() {
        assertEquals("test", Value.of("test.x.pdf").beforeFirst("."))
    }

    @Test
    fun `Test left`() {
        assertEquals("testA", Value.of("testA.testB").left(5))
        assertEquals(".testB", Value.of("testA.testB").left(-5))
        assertEquals("test", Value.of("test").left(5))
        assertEquals("", Value.of(null).left(5))
    }

    @Test
    fun `Test right`() {
        assertEquals("testB", Value.of("testA.testB").right(5))
        assertEquals("testA.", Value.of("testA.testB").right(-5))
        assertEquals("test", Value.of("test").right(5))
        assertEquals("", Value.of(null).right(5))

    }

    @Test
    fun `Test getBigDecimal`() {
        assertEquals(null, Value.of("").getBigDecimal())
        assertEquals(null, Value.of("Not a Number").getBigDecimal())
        assertEquals(BigDecimal.valueOf(42), Value.of("42").getBigDecimal())
        assertEquals(BigDecimal.valueOf(42.0), Value.of("42.0").getBigDecimal())
        assertEquals(BigDecimal.valueOf(42.0), Value.of("42,0").getBigDecimal())
        assertEquals(BigDecimal.valueOf(42), Value.of(42).getBigDecimal())
        assertEquals(BigDecimal.valueOf(42.0), Value.of(42.0).getBigDecimal())
        assertEquals(BigDecimal.valueOf(42), Value.of(Integer.valueOf(42)).getBigDecimal())
    }

    @Test
    fun `Test getBigDecimal with default`() {
        assertEquals(DEFAULT_BIG_DECIMAL, Value.of("").getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(DEFAULT_BIG_DECIMAL, Value.of("Not a Number").getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(BigDecimal.valueOf(42), Value.of("42").getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(BigDecimal.valueOf(42.0), Value.of("42.0").getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(BigDecimal.valueOf(42.0), Value.of("42,0").getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(BigDecimal.valueOf(42), Value.of(42).getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(BigDecimal.valueOf(42.0), Value.of(42.0).getBigDecimal(DEFAULT_BIG_DECIMAL))
        assertEquals(BigDecimal.valueOf(42), Value.of(Integer.valueOf(42)).getBigDecimal(DEFAULT_BIG_DECIMAL))
    }

    @Test
    fun `Test asBoolean with default`() {
        assertEquals(false, Value.of("").asBoolean(false))
        assertEquals(false, Value.of("false").asBoolean(false))
        assertEquals(true, Value.of("true").asBoolean(false))
        assertEquals(false, Value.of(false).asBoolean(false))
        assertEquals(true, Value.of(true).asBoolean(false))
        assertEquals(true, Value.of(NLS.get("NLS.yes")).asBoolean(false))
        assertEquals(false, Value.of(NLS.get("NLS.no")).asBoolean(false))
    }

    @Test
    fun `Test asLocalDate with default`() {
        assertEquals(null, Value.of("").asLocalDate(null))
        assertEquals(LocalDate.of(1994, 5, 8), Value.of(LocalDate.of(1994, 5, 8)).asLocalDate(null))
        assertEquals(LocalDate.of(1994, 5, 8), Value.of(LocalDateTime.of(1994, 5, 8, 10, 30)).asLocalDate(null))
        assertEquals(LocalDate.of(2018, 9, 21), Value.of(1537539103268).asLocalDate(null))
    }

    @Test
    fun `Test coerce boolean and without default`() {
        assertEquals(false, Value.of("").coerce(Boolean::class.java, null))
        assertEquals(false, Value.of("false").coerce(Boolean::class.java, null))
        assertEquals(true, Value.of("true").coerce(Boolean::class.java, null))
        assertEquals(false, Value.of(false).coerce(Boolean::class.java, null))
        assertEquals(true, Value.of(true).coerce(Boolean::class.java, null))
    }

    @Test
    fun `Boxing and retrieving an amount works`() {
        assertEquals(Amount.of(0.00001), Value.of(Amount.of(0.00001)).getAmount())
    }

    @Test
    fun `Casting to integers works`() {
        assertEquals(99999, Value.of(99999).asInt(1234))
        assertEquals(99999, Value.of(99999L).asInt(1234))
        assertEquals(99999, Value.of(99999.0).asInt(1234))
        assertEquals(99999, Value.of((99999).toDouble()).asInt(1234))
        assertEquals(99999, Value.of(Amount.of((99999).toInt())).asInt(1234))
        assertEquals(1234, Value.of(Amount.NOTHING).asInt(1234))
        assertEquals(99999, Value.of("99999").asInt(1234))
        assertEquals(1234, Value.of("Keine Zahl").asInt(1234))
        assertEquals(1234, Value.of(null).asInt(1234))
    }

    @Test
    fun `Casting to integer instances works`() {
        val boxedInt: Int? = 99999
        assertEquals(boxedInt, Value.of(99999).getInteger())
        assertEquals(boxedInt, Value.of(99999L).getInteger())
        assertEquals(boxedInt, Value.of(99999.0).getInteger())
        assertEquals(boxedInt, Value.of((99999).toDouble()).getInteger())
        assertEquals(boxedInt, Value.of(Amount.of((99999).toInt())).getInteger())
        assertEquals(null, Value.of(Amount.NOTHING).getInteger())
        assertEquals(boxedInt, Value.of("99999").getInteger())
        assertEquals(null, Value.of("Keine Zahl").getInteger())
        assertEquals(null, Value.of(null).getInteger())
    }

    @Test
    fun `Casting to longs works`() {
        assertEquals(99999L, Value.of(99999).asLong(1234))
        assertEquals(99999L, Value.of(99999L).asLong(1234))
        assertEquals(99999L, Value.of(99999.0).asLong(1234))
        assertEquals(99999L, Value.of((99999).toDouble()).asLong(1234))
        assertEquals(99999L, Value.of(Amount.of((99999).toInt())).asLong(1234))
        assertEquals(1234L, Value.of(Amount.NOTHING).asLong(1234))
        assertEquals(99999L, Value.of("99999").asLong(1234))
        assertEquals(1234L, Value.of("Keine Zahl").asLong(1234))
        assertEquals(1234L, Value.of(null).asLong(1234))
    }

    @Test
    fun `Casting to long instances works`() {
        val boxedLong: Long? = 99999
        assertEquals(boxedLong, Value.of(99999).getLong())
        assertEquals(boxedLong, Value.of(99999L).getLong())
        assertEquals(boxedLong, Value.of(99999.0).getLong())
        assertEquals(boxedLong, Value.of((99999).toDouble()).getLong())
        assertEquals(boxedLong, Value.of(Amount.of((99999).toInt())).getLong())
        assertEquals(null, Value.of(Amount.NOTHING).getLong())
        assertEquals(boxedLong, Value.of("99999").getLong())
        assertEquals(null, Value.of("Keine Zahl").getLong())
        assertEquals(null, Value.of(null).getLong())
    }

    @Test
    fun `map() does not call the mapper on an empty Value`() {
        var count = 0
        val mapper: (value: Any) -> String = { value ->
            count++
            ""
        }
        Value.EMPTY.map(mapper)
        assertEquals(0, count)
    }

    @Test
    fun `flatMap() does not call the mapper on an empty Value`() {
        var count = 0
        val mapper: (value: Any) -> Optional<Value> = { value ->
            count++
            Optional.empty()
        }
        Value.EMPTY.flatMap(mapper)
        assertEquals(0, count)
    }

    @Test
    fun `asOptionalInt must not throw NPE on floats`() {
        val value = Value.of(1.1f)
        assertDoesNotThrow {
            value.asOptionalInt()
        }
    }

    @Test
    fun `append properly handles empty and null`() {
        assertEquals("x", Value.EMPTY.append(" ", "x").toString())
        assertEquals("x", Value.of("x").append(" ", null).asString())
        assertEquals("x y", Value.of("x").append(" ", "y").asString())
    }

    @Test
    fun `tryAppend only emits an output if the value is filled`() {
        assertTrue { Value.EMPTY.tryAppend(" ", "x").isEmptyString() }
        assertEquals("x", Value.of("x").tryAppend(" ", null).asString())
        assertEquals("x y", Value.of("x").tryAppend(" ", "y").asString())
    }
}

