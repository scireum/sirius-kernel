/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.async.Operation;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Provides a streamlined SOAP client.
 * <p>
 * Permits to talk to external systems using the SOAP protocol. Note that this client can and should be re-used.
 * Therefore once it is initialized (all <tt>withXXX</tt> methods have been called), this is also completely thread
 * safe.
 * <p>
 * Note that namespace prefixes should be declared via {@link #withNamespace(String, String)}. These are also
 * transferred to the XPATH expressions of the returned output. Therefore a constant and custom namespace
 * prefix can be used, independently of what is returned by the server (as long as the namespace URI matches).
 * Note however, that when generating the request, fully qualified tag names with matching prefixes have to
 * be provided.
 * <p>
 * By default the namespace "soapenv" ({@link #SOAP_NAMESPACE_URI} is defined and bound to {@link #SOAP_NAMESPACE_URI}.
 * This can also be changed using {@link #withNamespace(String, String)} if necessary.
 * <p>
 * Most probably a client will be setup per API or external system like:
 * <code>
 * <pre>
 *  // Setup client (once)...
 *  SOAPClient client = new SOAPClient(new URL("myhost"));
 *  client.withNamespace("foo","urn:foo");
 *
 *  // Call (in one or more threads) repeatedly...
 *  client.call("MyAction,"MyActionRequest").withParameter("foo", 1).withParameter("bar, "test).execute()
 * </pre>
 * </code>
 */
public class SOAPClient {

    private static final Attribute[] ATTRIBUTE_ARRAY = new Attribute[0];

    /**
     * Contains the prefix used for SOAP envelopes.
     */
    public static final String SOAP_NAMESPACE = "soapenv";

    /**
     * Contains the namespace URI used for SOAP envelopes.
     */
    public static final String SOAP_NAMESPACE_URI = "http://schemas.xmlsoap.org/soap/envelope/";

    public static final Log LOG = Log.get("soap");

    private static final String HEADER_SOAP_ACTION = "SOAPAction";
    private static final String TAG_SOAP_ENVELOPE = "Envelope";
    private static final String TAG_SOAP_HEADER = "Header";
    private static final String TAG_SOAP_BODY = "Body";
    private static final String PREFIX_XMLNS = "xmlns";

    /**
     * Contains the node name which contains the code of a SOAP fault.
     */
    public static final String NODE_FAULTCODE = "faultcode";

    /**
     * Contains the node name which contains the message of a SOAP fault.
     */
    public static final String NODE_FAULTSTRING = "faultstring";

    private final URL endpoint;
    private final BasicNamespaceContext namespaceContext = new BasicNamespaceContext();
    private List<Attribute> namespaceDefinitions;
    private String actionPrefix = "";
    private Consumer<XMLCall> callEnhancer;
    private final Map<String, URL> customEndpoints = new HashMap<>();
    private Consumer<BiConsumer<String, Object>> defaultParameterProvider;
    private boolean throwSOAPFaults = false;
    private Function<HandledException, HandledException> exceptionFilter = Function.identity();
    private BiFunction<StructuredNode, String, StructuredNode> resultTransformer =
            (input, ignored) -> input.queryNode(SOAP_NAMESPACE + ":" + TAG_SOAP_BODY);
    private boolean trustSelfSignedCertificates;

    private static final Average responseTime = new Average();

    /**
     * Creates a new client which talks to the given endpoint.
     * <p>
     * Note that if the actual endpoint depends on the <tt>action</tt> of the call, use
     * {@link #withCustomEndpoint(String, URL)}.
     *
     * @param endpoint the default endpoint to talk to
     */
    public SOAPClient(@Nonnull URL endpoint) {
        this.endpoint = endpoint;
        this.withNamespace(SOAP_NAMESPACE, SOAP_NAMESPACE_URI);
    }

    /**
     * Returns the average response time across all SOAP calls.
     *
     * @return the average response time across all calls
     */
    public static Average getResponseTime() {
        return responseTime;
    }

    /**
     * Defines a XML namespace prefix.
     * <p>
     * This will be defined in the request, so that tags using this prefix can be sent to the server. This will be
     * also made available to  the result of a call so that XPATH expressions can refer to this prefix (independently
     * of what prefix is actually used by the server.
     *
     * @param prefix the prefix to define
     * @param uri    the namespace URL for the given prefix
     * @return the client itself for fluent method calls
     */
    public SOAPClient withNamespace(@Nonnull String prefix, @Nonnull String uri) {
        namespaceContext.withNamespace(prefix, uri);
        namespaceDefinitions = null;
        return this;
    }

    /**
     * Defines a prefix prepended to all actions.
     * <p>
     * It is quite common for SOAP calls to use full URIs for actions. Most commonly this will be a prefix like
     * a real or a fake domain appended with the name of the actual operation. If the prefix is always the same,
     * it can be setup here and thus will simplify each <tt>call</tt>.
     *
     * @param prefix the prefix to prepend
     * @return the client itself for fluent method calls
     */
    public SOAPClient withActionPrefix(@Nonnull String prefix) {
        this.actionPrefix = prefix;
        return this;
    }

    /**
     * Permits to modify the <tt>XMLCall</tt> and its underlying <tt>Outcall</tt> right before it is sent to the server.
     *
     * @param callEnhancer an enhancer which is supplied with each call before it is executed. Note that this must be
     *                     a thread safe implementation.
     * @return the client itself for fluent method calls
     */
    public SOAPClient withCallEnhancer(@Nonnull Consumer<XMLCall> callEnhancer) {
        this.callEnhancer = callEnhancer;
        return this;
    }

    /**
     * Permits to specify a custom endpoint for a given action.
     * <p>
     * Note that the action is the one passed into the <tt>call</tt> without any <tt>actionPrefix</tt> applied.
     *
     * @param action   the action to register a custom endpoint for
     * @param endpoint the endpoint to use
     * @return the client itself for fluent method calls
     */
    public SOAPClient withCustomEndpoint(String action, URL endpoint) {
        this.customEndpoints.put(action, endpoint);

        return this;
    }

    /**
     * Permits to trust self-signed certificates.
     *
     * @return the client itself for fluent method calls
     */
    public SOAPClient withTrustSelfSignedCertificates() {
        this.trustSelfSignedCertificates = true;
        return this;
    }

    /**
     * Permits to supply a set of default parameters.
     * <p>
     * When building a simple parameter object via {@link #call(String, String)}, this can be use to supply one or
     * more default parameters for all requests.
     *
     * @param parameterProvider the consumer which can be supplied with additional parameters
     * @return the client itself for fluent method calls
     */
    public SOAPClient withDefaultParameterProvider(Consumer<BiConsumer<String, Object>> parameterProvider) {
        this.defaultParameterProvider = parameterProvider;

        return this;
    }

    /**
     * Installs a transformer which is applied to all successful results being received.
     * <p>
     * The transformer receives the response along with the action performed.
     * It can be used e.g. to install a custom error handler in case something else than SOAP faults are used.
     * <p>
     * By default, the SOAP envelope is extracted and only the body is returned.
     *
     * @param resultTransfomer the transformer which can either return a different XML response or throw an exception.
     * @return the client itself for fluent method calls
     */
    public SOAPClient withResultTransfomer(BiFunction<StructuredNode, String, StructuredNode> resultTransfomer) {
        this.resultTransformer = resultTransfomer;
        return this;
    }

    /**
     * Determines if SOAP faults should be thrown as {@link SOAPFaultException}.
     * <p>
     * Otherwise a SOAP fault will be handled as {@link HandledException}. The <tt>SOAPFaultException</tt> will
     * still be the root cause of the <tt>HandledException</tt> so that the underlying cause can still be detected.
     *
     * @return the client itself for fluent method calls
     */
    public SOAPClient throwSOAPFaults() {
        this.throwSOAPFaults = true;
        return this;
    }

    /**
     * Installs an exception filter.
     * <p>
     * This will be supplied with all exceptions thrown by the client. This can be used to hide the technical contents
     * and rather return an exception which is suited to be shown to the user.
     * <p>
     * Note that the root cause of the handled exception can be inspected. If this is a {@link SOAPFaultException}
     * then the handled exception was triggered by the server with the given SOAP fault, otherwise an IO or network
     * error was most probably the underlying cause.
     *
     * @param exceptionFilter the filter to process all client exceptions
     * @return the client itself for fluent method calls
     */
    public SOAPClient withExceptionFilter(UnaryOperator<HandledException> exceptionFilter) {
        this.exceptionFilter = exceptionFilter;
        return this;
    }

    /**
     * Invokes the given SOAP action by specifying a custom SOAP body.
     *
     * @param action      the action to invoke
     * @param bodyBuilder the builder used to setup the body of the SOAP envelope
     * @return the SOAP envelope of the response
     * @throws SOAPFaultException                    in case of a reported SOAP fault
     * @throws sirius.kernel.health.HandledException in case of any other problem (i.e. IO errors)
     */
    public StructuredNode call(@Nonnull String action, @Nonnull Consumer<XMLStructuredOutput> bodyBuilder) {
        return call(action, null, bodyBuilder);
    }

    /**
     * Invokes the given SOAP action by specifying a custom SOAP header and body.
     *
     * @param action      the action to invoke
     * @param headBuilder the builder used to setup the head of the SOAP envelope
     * @param bodyBuilder the builder used to setup the body of the SOAP envelope
     * @return the SOAP envelope of the response
     * @throws SOAPFaultException in case of a reported SOAP fault
     * @throws HandledException   in case of any other problem (i.e. IO errors)
     */
    public StructuredNode call(@Nonnull String action,
                               @Nullable Consumer<XMLStructuredOutput> headBuilder,
                               @Nonnull Consumer<XMLStructuredOutput> bodyBuilder) {
        Watch watch = Watch.start();
        URL effectiveEndpoint = customEndpoints.getOrDefault(action, endpoint);

        try (Operation op = new Operation(() -> Strings.apply("SOAP %s -> %s", action, effectiveEndpoint),
                                          Duration.ofSeconds(15))) {
            XMLCall call = XMLCall.to(effectiveEndpoint);
            call.withNamespaceContext(namespaceContext);
            call.getOutcall().markAsPostRequest();
            if (callEnhancer != null) {
                callEnhancer.accept(call);
            }

            if (trustSelfSignedCertificates) {
                call.getOutcall().trustSelfSignedCertificates();
            }

            call.addHeader(HEADER_SOAP_ACTION, actionPrefix + action);

            createEnvelope(call.getOutput(), headBuilder, bodyBuilder);

            String request = "";
            if (LOG.isFINE()) {
                request =
                        Strings.apply("Calling %s %s\n%s", effectiveEndpoint, actionPrefix + action, call.getOutput());
            }

            StructuredNode result = call.getInput().getNode(".");
            watch.submitMicroTiming("SOAP", action + " -> " + effectiveEndpoint);

            if (LOG.isFINE()) {
                LOG.FINE("---------- call ----------\n%s\n---------- response ----------\n%s---------- end ----------",
                         request,
                         result.toString());
            }

            StructuredNode fault = result.queryNode("soapenv:Body/soapenv:Fault");
            if (fault != null) {
                return handleSOAPFault(watch, action, effectiveEndpoint, fault);
            }

            return handleResult(watch, action, effectiveEndpoint, result);
        } catch (SOAPFaultException | HandledException exception) {
            throw exception;
        } catch (Exception exception) {
            return handleGeneralFault(watch, action, effectiveEndpoint, exception);
        }
    }

    protected void createEnvelope(XMLStructuredOutput output,
                                  Consumer<XMLStructuredOutput> headBuilder,
                                  Consumer<XMLStructuredOutput> bodyBuilder) {
        output.beginNamespacedOutput(SOAP_NAMESPACE,
                                     TAG_SOAP_ENVELOPE,
                                     getNamespaceDefinitions().toArray(ATTRIBUTE_ARRAY));
        {
            output.beginNamespacedObject(SOAP_NAMESPACE, TAG_SOAP_HEADER);
            {
                if (headBuilder != null) {
                    headBuilder.accept(output);
                }
            }
            output.endObject();
            output.beginNamespacedObject(SOAP_NAMESPACE, TAG_SOAP_BODY);
            {
                bodyBuilder.accept(output);
            }
            output.endObject();
        }
        output.endOutput();
    }

    private List<Attribute> getNamespaceDefinitions() {
        if (namespaceDefinitions == null) {
            namespaceDefinitions = namespaceContext.getPrefixAndUris()
                                                   .map(entry -> Attribute.set(PREFIX_XMLNS,
                                                                               entry.getKey(),
                                                                               entry.getValue()))
                                                   .collect(Collectors.toList());
        }

        return namespaceDefinitions;
    }

    /**
     * Handles the given SOAP fault.
     * <p>
     * This method can be overwritten by sublasses to provide additional logging / tracing.
     *
     * @param watch             the watch which record the total duration of the SOAP call
     * @param action            the action which was invoked
     * @param effectiveEndpoint the endpoint which has been addressed
     * @param fault             the fault that occured
     * @return an alternative response to return in case that no exception is thrown
     * @throws SOAPFaultException in case the client is configured to do so
     * @throws HandledException   as the default way of handling SOAP faults when no <tt>SOAPFaultException</tt> is
     *                            thrown
     */
    protected StructuredNode handleSOAPFault(Watch watch,
                                             @Nonnull String action,
                                             URL effectiveEndpoint,
                                             StructuredNode fault) {
        SOAPFaultException soapFaultException = new SOAPFaultException(action,
                                                                       effectiveEndpoint,
                                                                       fault.queryString(NODE_FAULTCODE),
                                                                       fault.queryString(NODE_FAULTSTRING));

        if (throwSOAPFaults) {
            throw soapFaultException;
        } else {
            throw exceptionFilter.apply(Exceptions.handle()
                                                  .to(LOG)
                                                  .error(soapFaultException)
                                                  .withSystemErrorMessage(
                                                          "A SOAP fault (%s) occured when executing '%s' against '%s': %s",
                                                          soapFaultException.getFaultCode(),
                                                          action,
                                                          effectiveEndpoint)
                                                  .handle());
        }
    }

    /**
     * Processes a successfully received SOAP result.
     * <p>
     * By default this simply invokes the <tt>resultTransformer</tt>, but it can be overwritten by subclasses for
     * additional logging / tracing. This can also modify the result being returned or throw an exception in stead.
     *
     * @param watch             the watch which record the total duration of the SOAP call
     * @param action            the action which was invoked
     * @param effectiveEndpoint the endpoint which has been addressed
     * @param result            the result SOAP envelope which was received
     * @return the SOAP envelope to process
     */
    protected StructuredNode handleResult(Watch watch, String action, URL effectiveEndpoint, StructuredNode result) {
        return resultTransformer.apply(result, action);
    }

    /**
     * Handles the given exception which occured when performing a SOAP call.
     * <p>
     * This method can be overwritten by sublasses to provide additional logging / tracing.
     *
     * @param watch             the watch which record the total duration of the SOAP call
     * @param action            the action which was invoked
     * @param effectiveEndpoint the endpoint which has been addressed
     * @param exception         the error that occured
     * @return an alternative response to return in case that no exception is thrown
     * @throws HandledException the default approach is to create an appropriate exception and pass it to the
     *                          {@link #exceptionFilter}.
     */
    protected StructuredNode handleGeneralFault(Watch watch,
                                                String action,
                                                URL effectiveEndpoint,
                                                Exception exception) {
        throw exceptionFilter.apply(Exceptions.handle()
                                              .to(LOG)
                                              .error(exception)
                                              .withSystemErrorMessage(
                                                      "An error occured when executing '%s' against '%s': %s (%s)",
                                                      action,
                                                      effectiveEndpoint)
                                              .handle());
    }

    /**
     * Invokes the given action with a plain request object.
     * <p>
     * Such an object is a simple XML node with inner nodes, one per parameter, e.g.
     * <code>
     * <pre>
     * <GetStuff>
     *     <Foo>1</Foo>
     *     <Bar>Test</Bar>
     * </GetStuff>
     * </pre>
     * </code>
     *
     * @param action            the action to invoke
     * @param parameterNodeName the tag name of the parameter node
     * @return a builder which can be used to provide a set of parameters
     */
    @CheckReturnValue
    public CallBuilder call(@Nonnull String action, @Nonnull String parameterNodeName) {
        return new CallBuilder(action, parameterNodeName);
    }

    private class CallBuilder {

        private final String action;
        private final String method;
        private final Map<String, Object> parameters = new LinkedHashMap<>();

        CallBuilder(String action, String method) {
            this.action = action;
            this.method = method;
        }

        /**
         * Specifies a parameter to add.
         *
         * @param parameter the name of the parameter tag
         * @param value     the value of the parameter tag. This will be transformed using
         *                  {@link sirius.kernel.nls.NLS#toMachineString(Object)}.
         * @return the builder itself for fluent method calls
         */
        @CheckReturnValue
        public CallBuilder withParameter(String parameter, Object value) {
            if (defaultParameterProvider != null) {
                defaultParameterProvider.accept(parameters::put);
            }

            parameters.put(parameter, value);
            return this;
        }

        /**
         * Executes  the SOAP call.
         *
         * @return the SOAP envelope of the server response
         * @throws SOAPFaultException                    in case of a reported SOAP fault
         * @throws sirius.kernel.health.HandledException in case of any other problem (i.e. IO errors)
         */
        public StructuredNode execute() {
            return call(action, body -> {
                body.beginObject(method);
                parameters.forEach(body::property);
                body.endObject();
            });
        }
    }
}
