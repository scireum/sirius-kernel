/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings

import com.typesafe.config.Config
import sirius.kernel.BaseSpecification
import sirius.kernel.Sirius

import java.util.stream.Collectors

class SettingsSpec extends BaseSpecification {

    def "inner configs are delivered in their given order"() {
        when:
        def keys = Sirius.getSettings().getConfigs("test-configs").stream().map({ c -> c.getString("value") }).collect(
                Collectors.toList())
        then:
        keys == ["2", "1", "3"]
    }

    def "inner configs are delivered as sorted by their priority if given"() {
        when:
        def keys = Sirius.getSettings().getConfigs("test-configs-sorted").stream().map({ c -> c.getString("value") }).collect(
                Collectors.toList())
        then:
        keys == ["1", "2", "3"]
    }

    def "no exception is thrown for retrieving a non-existent extension, even when settings are strict"() {
        when:
        def extension = Sirius.getSettings().getExtension("non-existent", "not-specified")
        then:
        extension == null
        and:
        noExceptionThrown()
    }
}
