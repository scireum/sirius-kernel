/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.Strings

class ConfigBuilderSpec extends BaseSpecification {

    def "config builder works"() {
        given:
        def configBuilder = new ConfigBuilder()
        when:
        configBuilder.addVariable("scope.foo", "true");
        then:
        Strings.areEqual(configBuilder.toString(), "scope.foo = true")
    }

    def "scope unfolding works"() {
        given:
        def configBuilder = new ConfigBuilder()
        when:
        configBuilder.addVariable("scope.foo", "true");
        configBuilder.addVariable("scope.bar", "false");
        then:
        Strings.areEqual(configBuilder.toString(), "scope {\n" +
                "    foo = true\n" +
                "    bar = false\n" +
                "}")
    }

    def "complex example works"() {
        given:
        def configBuilder = new ConfigBuilder()
        when:
        configBuilder.addVariable("scopeA.foo", "true");
        configBuilder.addVariable("scopeA.bar", "true");
        configBuilder.addVariable("scopeB.foo", "\"enabled\"");
        configBuilder.addVariable("scopeB.scopeC.foo", "\"disabled\"");
        configBuilder.addVariable("scopeB.scopeC.bar", "\"enabled\"");
        configBuilder.addVariable("foo", "\"disabled\"");
        configBuilder.addVariable("foo.bar.test", "42");
        then:
        Strings.areEqual(configBuilder.toString(), "foo = \"disabled\"\n" +
                "foo.bar.test = 42\n" +
                "\n" +
                "scopeA {\n" +
                "    foo = true\n" +
                "    bar = true\n" +
                "}\n" +
                "\n" +
                "scopeB {\n" +
                "    foo = \"enabled\"\n" +
                "\n" +
                "    scopeC {\n" +
                "        foo = \"disabled\"\n" +
                "        bar = \"enabled\"\n" +
                "    }\n" +
                "}")
    }
}
