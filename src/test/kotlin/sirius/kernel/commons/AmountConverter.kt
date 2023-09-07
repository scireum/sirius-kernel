/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.params.converter.ArgumentConverter

class AmountConverter : ArgumentConverter {
    override fun convert(source: Any?, context: ParameterContext?): Any? {
        if (source !is String) {
            return Amount.NOTHING
        }
        return Amount.ofMachineString(source)
    }
}