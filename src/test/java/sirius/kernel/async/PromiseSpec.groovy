/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.ValueHolder

import java.util.function.Function

class PromiseSpec extends BaseSpecification {

    def "Multiple Callbacks work on Promises when successfully completed"() {
        given: "a promise"
        Promise<String> test = Tasks.promise()
        and: "multiple handlers"
        ValueHolder<String> resultHolder1 = ValueHolder.of(null)
        ValueHolder<String> resultHolder2 = ValueHolder.of(null)
        when: "we attach the handlers to the promise"
        test.onSuccess(resultHolder1)
        test.onSuccess(resultHolder2)
        and: "successfully complete the promise"
        test.success("Hello")
        then: "both handlers contain the expected result"
        resultHolder1.get() == "Hello"
        resultHolder2.get() == "Hello"
        and: "also the promise is considered successfuly..."
        test.isSuccessful()
        and: "...completed"
        test.isCompleted()
    }

    def "Callbacks on an already completed promise work"() {
        given: "a promise"
        Promise<String> test = Tasks.promise()
        and: "a handler"
        ValueHolder<String> resultHolder = ValueHolder.of(null)
        when: "we successfully complete the promise"
        test.success("Hello")
        and: "and afterwards add a handler"
        test.onSuccess(resultHolder)
        then: "it still contains the expected value"
        resultHolder.get() == "Hello"
    }

    def "Multiple Callbacks work on Promises when failing"() {
        given: "a promise"
        Promise<String> test = Tasks.promise()
        and: "and a test exception"
        def exception = new Exception()
        and: "and multiple handlers"
        ValueHolder<Exception> resultHolder1 = ValueHolder.of(null)
        ValueHolder<Exception> resultHolder2 = ValueHolder.of(null)
        when: "we attache the handlers to the promise"
        test.onFailure(resultHolder1)
        test.onFailure(resultHolder2)
        and: "fail the promise with our test exception"
        test.fail(exception)
        then: "both handlers contain the expected exception"
        resultHolder1.get() == exception
        resultHolder2.get() == exception
        and: "the promise is considered 'failed'..."
        test.isFailed()
        and: "...and completed"
        test.isCompleted()
    }

    def "Callbacks on an already failed promise work"() {
        given: "a promise"
        Promise<String> test = Tasks.promise()
        and: "a test exception"
        def exception = new Exception()
        and: "mu"
        ValueHolder<Exception> resultHolder = ValueHolder.of(null)
        and: "add a dummy handler so that no output is written into the console"
        test.onFailure(ValueHolder.of(null))
        when: "the promise fails"
        test.fail(exception)
        and: "and after that we add a handler"
        test.onFailure(resultHolder)
        then: "the are still invoked"
        resultHolder.get() == exception
    }

    def "Mapping Promises work"() {
        given: "a promise accepting some input"
        Promise<String> input = Tasks.promise()
        and: "a promise which provides output based in the input promise"
        Promise<String> output = input.map({ s -> s.toUpperCase() } as Function)
        and: "a handler"
        ValueHolder<String> resultHolder = ValueHolder.of(null)
        when: "we attach the handler to the output promise"
        output.onSuccess(resultHolder)
        and: "complete the input promise with some data"
        input.success("hello")
        then: "the output promise will also be completed with the computed data"
        resultHolder.get() == "HELLO"
    }

    def "Exception handling when mapping Promises work"() {
        given: "a promise accepting some input"
        Promise<String> input = Tasks.promise()
        and: "a promise which provides output based in the input promise"
        Promise<String> output = input.map({ s -> s.toUpperCase() } as Function)
        and: "a handler"
        ValueHolder<Exception> resultHolder = ValueHolder.of(null)
        when: "we attache the handler to the output promise"
        output.onFailure(resultHolder)
        and: "complete the input with invalid data"
        input.success(null)
        then: "the handler will contain the appropriate exception"
        resultHolder.get() instanceof NullPointerException
        and: "the output promise is considered as 'failed'"
        output.isFailed()
    }

    def "A completed Promise with a value and error is marked as non successful and failed"() {
        given: "a promise"
        Promise<String> test = Tasks.promise()
        and: "and a test exception"
        def exception = new Exception()
        and: "add a dummy handler so that no output is written into the console"
        test.onFailure(ValueHolder.of(null))
        when: "mark promise as successful"
        test.success(null)
        and: "and mark it as failed as well"
        test.fail(exception)
        then: "the promise is considered as 'failed'"
        test.isFailed()
        and: "the promise is not considered as 'successful'"
        !test.isSuccessful()
    }

}
