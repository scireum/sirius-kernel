package sirius.kernel.commons

import sirius.kernel.BaseSpecification

class URLBuilderSpec extends BaseSpecification {

    def "baseURL is handled correctly"() {
        when:
        def urlBuilder = new URLBuilder(baseUrl)
        then:
        urlBuilder.toString() == expectedUrl

        where:
        baseUrl                       | expectedUrl
        "http://sirius-lib.net"       | "http://sirius-lib.net"
        "http://sirius-lib.net/"      | "http://sirius-lib.net"
        "sirius-lib.net"              | "sirius-lib.net"
        "sirius-lib.net/"             | "sirius-lib.net"
        "www.sirius-lib.net"          | "www.sirius-lib.net"
        "www.sirius-lib.net/"         | "www.sirius-lib.net"
        "www.sirius-lib.net/example"  | "www.sirius-lib.net/example"
        "www.sirius-lib.net/example/" | "www.sirius-lib.net/example"
        "http://www.sirius-lib.net"   | "http://www.sirius-lib.net"
        "http://www.sirius-lib.net/"  | "http://www.sirius-lib.net"
        "localhost:80"                | "localhost:80"
        "localhost:80/"               | "localhost:80"
        "127.0.0.1:80"                | "127.0.0.1:80"
        "127.0.0.1:80/"               | "127.0.0.1:80"
    }

    def "baseURL creation with protocol / host constructor is working"() {
        when:
        def urlBuilder = new URLBuilder(protocol, host)
        then:
        urlBuilder.toString() == expectedUrl

        where:
        protocol                  | host                         | expectedUrl
        URLBuilder.PROTOCOL_HTTP  | "sirius-lib.net"             | "http://sirius-lib.net"
        URLBuilder.PROTOCOL_HTTPS | "sirius-lib.net"             | "https://sirius-lib.net"
        URLBuilder.PROTOCOL_HTTP  | "www.sirius-lib.net"         | "http://www.sirius-lib.net"
        URLBuilder.PROTOCOL_HTTPS | "www.sirius-lib.net"         | "https://www.sirius-lib.net"
        URLBuilder.PROTOCOL_HTTP  | "www.sirius-lib.net/example" | "http://www.sirius-lib.net/example"
        URLBuilder.PROTOCOL_HTTPS | "www.sirius-lib.net/example" | "https://www.sirius-lib.net/example"
        URLBuilder.PROTOCOL_HTTP  | "localhost:80"               | "http://localhost:80"
        URLBuilder.PROTOCOL_HTTPS | "localhost:80"               | "https://localhost:80"
        URLBuilder.PROTOCOL_HTTP  | "127.0.0.1:80"               | "http://127.0.0.1:80"
        URLBuilder.PROTOCOL_HTTPS | "127.0.0.1:80"               | "https://127.0.0.1:80"
    }

    def "adding a single part is handled correctly"() {
        when:
        def urlBuilder = new URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addPart(part)
        then:
        urlBuilder.toString() == expectedUrl

        where:
        part                      | expectedUrl
        ""                        | "http://sirius-lib.net/"
        "example"                 | "http://sirius-lib.net/example"
        "file.jpg"                | "http://sirius-lib.net/file.jpg"
        "example/second/file.jpg" | "http://sirius-lib.net/example/second/file.jpg"
    }

    def "adding multiple parts through varargs is handled correctly"() {
        when:
        def urlBuilder = new URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addPart(*part)
        then:
        urlBuilder.toString() == expectedUrl

        where:
        part                                                 | expectedUrl
        ["", "", "", ""]                                     | "http://sirius-lib.net/"
        ["example", "/", "file", ".", "jpg"]                 | "http://sirius-lib.net/example/file.jpg"
        ["very", "long", "example"]                          | "http://sirius-lib.net/verylongexample"
        ["example", "", "/", "", "file", "", ".", "", "jpg"] | "http://sirius-lib.net/example/file.jpg"
    }

    def "the standard method for adding parameters encodes the value"() {
        when:
        def urlBuilder = new URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "test value")
        then:
        urlBuilder.toString() == "http://sirius-lib.net?test=test+value"

    }

    def "a single parameter is added and url encoded correctly"() {
        when:
        def urlBuilder = new URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter(key, value, urlencode)
        then:
        urlBuilder.toString() == expectedUrl

        where:
        key    | value        | urlencode | expectedUrl
        "test" | ""           | true      | "http://sirius-lib.net?test="
        "test" | ""           | false     | "http://sirius-lib.net?test="
        "test" | "value"      | true      | "http://sirius-lib.net?test=value"
        "test" | "value"      | false     | "http://sirius-lib.net?test=value"
        "test" | "test value" | true      | "http://sirius-lib.net?test=test+value"
        "test" | "test+value" | false     | "http://sirius-lib.net?test=test+value"
        "test" | "#value"     | true      | "http://sirius-lib.net?test=%23value"
        "test" | "%23value"   | false     | "http://sirius-lib.net?test=%23value"
    }

    def "multiple parameters are added correctly"() {
        when:
        def urlBuilder = new URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test1", "value1")
        urlBuilder.addParameter("test2", "value2")
        urlBuilder.addParameter("test3", "value3")
        then:
        urlBuilder.toString() == "http://sirius-lib.net?test1=value1&test2=value2&test3=value3"
    }

    def "can't add parts after a parameter has been added"() {
        when:
        def urlBuilder = new URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "value")
        urlBuilder.addPart("late")
        then:
        thrown IllegalStateException
    }

}