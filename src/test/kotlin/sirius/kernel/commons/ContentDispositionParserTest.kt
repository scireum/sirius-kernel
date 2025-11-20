/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

/**
 * Tests the [ContentDispositionParser] class.
 */
class ContentDispositionParserTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|', textBlock = """
        attachment; filename="test.pdf"                           | test.pdf
        inline; filename=test.pdf                                 | test.pdf
        attachment; filename=test.pdf ; size="2000"               | test.pdf
        attachment; size="2000" ; filename=test.pdf               | test.pdf
        attachment; filename="test pdf doc.pdf"                   | test pdf doc.pdf
        inline; filename*=UTF-8''test.pdf                         | test.pdf
        inline; filename*="UTF-8''test.pdf"                       | test.pdf
        inline; filename*="UTF-8''test%20pdf%20doc.pdf"           | test pdf doc.pdf
        inline; filename*=UTF-8''test%20pdf%20doc.pdf             | test pdf doc.pdf
        attachment; filename*=iso-8859-1'en'file%27%20%27name.jpg | file' 'name.jpg"""
    )
    fun `parseContentDisposition regex works for various scenarios`(input: String, output: String) {
        assertEquals(output, ContentDispositionParser.parseFileName(input).get())
    }
}
