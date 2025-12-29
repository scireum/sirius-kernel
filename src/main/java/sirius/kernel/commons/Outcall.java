/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.net.HttpHeaders;
import sirius.kernel.Sirius;
import sirius.kernel.async.Operation;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Microtiming;
import sirius.kernel.io.IOExceptionSkipLog;
import sirius.kernel.nls.NLS;
import sirius.kernel.settings.Extension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to call a URL and send or receive data.
 * <p>
 * This is basically a thin wrapper over <tt>{@link HttpClient}</tt> which adds some boilerplate code and a bit
 * of logging / monitoring.
 * <p>
 * By default, we will follow redirects via {@link  java.net.http.HttpClient.Redirect#NORMAL}. However, one can use
 * {@link #noFollowRedirects()} or {@link #alwaysFollowRedirects()} to customize this behaviour.
 */
public class Outcall {

    /**
     * Date time formatter as per
     * <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-3.3.1">RFC 2616 section 3.3.1</a>
     * <p>
     * In contrast to {@link DateTimeFormatter#RFC_1123_DATE_TIME}, the day must use two digits and the date must be
     * represented in GMT (which is equal to UTC for the purpose of HTTP).
     */
    public static final DateTimeFormatter RFC2616_INSTANT =
            new DateTimeFormatterBuilder().appendPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                                          .toFormatter()
                                          .withLocale(Locale.ENGLISH)
                                          .withChronology(IsoChronology.INSTANCE)
                                          .withZone(ZoneOffset.UTC);

    /**
     * Default Accept header value allowing any media type.
     */
    public static final String HEADER_ACCEPT_DEFAULT_VALUE = "*/*";

    /**
     * HTTP HEAD request method (requests headers only, no body).
     */
    public static final String REQUEST_METHOD_HEAD = "HEAD";

    /**
     * Content type for URL-encoded form data with UTF-8 encoding.
     */
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=utf-8";

    /**
     * Prefix for Bearer token authorization scheme.
     */
    public static final String PREFIX_BEARER = "Bearer ";

    private static final String PREFIX_BASIC = "Basic ";

    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

    /**
     * Provides a constant for {@link #modifyClient(String)} to signal that the generated client must not be re-used or
     * cached.
     */
    public static final String CLIENT_SELECTOR_NO_CACHE = null;

    private static final String DEFAULT_CLIENT_SELECTOR = "_default_";

    /**
     * Keeps track of hosts for which we ran into a connect-timeout.
     * <p>
     * These hosts are blacklisted for a short amount of time ({@link #connectTimeoutBlacklistDuration}) to prevent
     * cascading failures.
     * <p>
     * We log a warning for each host which is blacklisted and is called again. To make sure this doesn't spam the logs,
     * we only log once and then set the second value of the tuple to <tt>true</tt> to prevent further log messages.
     */
    private static final Map<String, Tuple<Long, Boolean>> timeoutBlacklist = new ConcurrentHashMap<>();

    /**
     * If the {@link #timeoutBlacklist} contains more than the given number of entries, we remove all expired ones
     * manually. These might be hosts which are only connected sporadically and had a hiccup. Everything else will be
     * kept clean in {@link #checkTimeoutBlacklist()}.
     */
    private static final int TIMEOUT_BLACKLIST_HIGH_WATERMARK = 100;

    @ConfigValue("http.outcall.timeouts.default.connectTimeout")
    private static Duration defaultConnectTimeout;

    @ConfigValue("http.outcall.timeouts.default.readTimeout")
    private static Duration defaultReadTimeout;

    @ConfigValue("http.outcall.connectTimeoutBlacklistDuration")
    private static Duration connectTimeoutBlacklistDuration;

    private static String defaultUserAgent;

    private static final X509TrustManager trustManagerForSelfSignedCerts = new TrustingSelfSignedTrustManager();

    private static final Average timeToFirstByte = new Average();

    /**
     * Contains a cache of re-usable clients based on their <tt>clientSelector</tt>.
     */
    private static final Map<String, HttpClient> cachedHttpClients = new ConcurrentHashMap<>();

    private static final int MAX_REDIRECTS = 5;

    private String clientSelector = DEFAULT_CLIENT_SELECTOR;
    private HttpClient client;
    private HttpRequest request;
    private String blacklistId;
    private HttpClient.Builder clientBuilder;
    private final HttpRequest.Builder requestBuilder;
    private HttpResponse<InputStream> response;
    private HttpClient.Redirect redirectPolicy = HttpClient.Redirect.NORMAL;
    private Charset charset = StandardCharsets.UTF_8;
    private Supplier<String> oAuthAccessToken;
    private Runnable oAuthTokenRefresher;

    // Provide an output stream for old apis
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private boolean postFromOutput = false;

    /**
     * Builds the default user agent string as 'product.name/product.version (+product.baseUrl)', where version or
     * baseUrl could be empty. This is attached to every outgoing request (see {@link Outcall#Outcall(java.net.URI)}).
     *
     * @return the default user agent string
     */
    public static String buildDefaultUserAgent() {
        if (defaultUserAgent == null) {
            StringBuilder userAgentString = new StringBuilder(Sirius.getSettings().getString("product.name"));
            String version = Sirius.getSettings().getString("product.version");
            if (Strings.isFilled(version)) {
                userAgentString.append("/");
                userAgentString.append(version);
            }
            defaultUserAgent = userAgentString.toString();
        }
        return defaultUserAgent;
    }

    /**
     * Creates a new <tt>Outcall</tt> to the given URL.
     * <p>
     * Uses the uri's host as blacklist id.
     *
     * @param uri the url to call
     */
    public Outcall(URI uri) {
        this.blacklistId = uri.getHost();
        requestBuilder = HttpRequest.newBuilder(uri)
                                    .header(HttpHeaders.USER_AGENT, buildDefaultUserAgent())
                                    .header(HttpHeaders.ACCEPT, HEADER_ACCEPT_DEFAULT_VALUE)
                                    .timeout(defaultReadTimeout);
    }

    /**
     * Allows to modify the client before the request is sent by returning the builder that is used to create it.
     *
     * @return the underlying {@link HttpClient.Builder}
     * @deprecated Use {@link #modifyClient(String)}
     */
    @Deprecated
    public HttpClient.Builder modifyClient() {
        return modifyClient(null);
    }

    /**
     * Allows to modify the client before the request is sent by returning the builder that is used to create it.
     * <p>
     * Note that if a non-null <tt>clientSelector</tt> is given, the resulting {@link HttpClient} created by the
     * modified builder is cached and re-used with the benefit of having a connection pool to re-use connections
     * across multiple requests.
     *
     * @param clientSelector a unique string for all occasions where the builder is customized the same way and thus
     *                       the cached client can be re-used once it has been created.
     *                       Use {@link #CLIENT_SELECTOR_NO_CACHE} to signal that the created client should not be
     *                       re-used (e.g. if a {@link java.net.CookieManager} is used).
     * @return the underlying {@link HttpClient.Builder}
     */
    public HttpClient.Builder modifyClient(@Nullable String clientSelector) {
        if (client != null) {
            throw new IllegalStateException("Can no longer modify client, request has already been sent!");
        }

        if (!DEFAULT_CLIENT_SELECTOR.equals(this.clientSelector)
            && this.clientSelector != null
            && !this.clientSelector.equals(clientSelector)) {
            throw new IllegalStateException("Client selector cannot be changed");
        }
        this.clientSelector = clientSelector;
        if (this.clientBuilder == null) {
            this.clientBuilder = HttpClient.newBuilder().connectTimeout(defaultConnectTimeout);
        }

        return clientBuilder;
    }

    /**
     * Allows to modify the request before the request is sent by returning the builder that is used to create it.
     *
     * @return the underlying {@link HttpRequest.Builder}
     */
    public HttpRequest.Builder modifyRequest() {
        if (request != null) {
            throw new IllegalStateException("Can no longer modify request, request has already been sent!");
        }
        return requestBuilder;
    }

    /**
     * Instructs the client to not follow any redirects.
     *
     * @return the outcall itself for fluent method calls
     */
    public Outcall noFollowRedirects() {
        redirectPolicy = HttpClient.Redirect.NEVER;
        return this;
    }

    /**
     * Instructs the client to {@link java.net.http.HttpClient.Redirect#ALWAYS} follow redirects.
     *
     * @return the outcall itself for fluent method calls
     */
    public Outcall alwaysFollowRedirects() {
        redirectPolicy = HttpClient.Redirect.ALWAYS;
        return this;
    }

    /**
     * Sets the header of the HTTP call.
     *
     * @param name  name of the header to set
     * @param value value of the header to set
     * @return the outcall itself for fluent method calls
     */
    public Outcall setRequestProperty(String name, String value) {
        modifyRequest().setHeader(name, value);
        return this;
    }

    /**
     * Sets the value of the {@code if-modified-since} header of this connection.
     * <p>
     * The fetching of the object is skipped unless the object has been modified more recently than a certain time.
     * If the object will not be returned because of this, the response code will be <tt>304</tt>.
     *
     * @param ifModifiedSince a date since when the object should be modified
     * @return the outcall itself for fluent method calls
     * @throws IllegalStateException if already connected
     */
    public Outcall setIfModifiedSince(LocalDateTime ifModifiedSince) {
        setRequestProperty(HttpHeaders.IF_MODIFIED_SINCE,
                           ifModifiedSince.atZone(ZoneId.systemDefault()).format(RFC2616_INSTANT));
        return this;
    }

    /**
     * Sets the HTTP Authorization header.
     *
     * @param user     the username to use
     * @param password the password to use
     * @return the outcall itself for fluent method calls
     */
    public Outcall setAuthParams(String user, String password) {
        if (Strings.isEmpty(user)) {
            return this;
        }

        String userAndPassword = user + ":" + password;
        String encodedAuthorization = Base64.getEncoder().encodeToString(userAndPassword.getBytes(charset));
        setRequestProperty(HttpHeaders.AUTHORIZATION, PREFIX_BASIC + encodedAuthorization);
        return this;
    }

    /**
     * Makes the underlying connection trust self-signed certs.
     * <p>
     * This will make the connection trust <strong>only</strong> self-signed certificates!
     *
     * @return the outcall itself for fluent method calls
     * @deprecated Use {@link #trustSelfSignedCertificates(String)}
     */
    @Deprecated
    public Outcall trustSelfSignedCertificates() {
        return trustSelfSignedCertificates(null);
    }

    /**
     * Makes the underlying connection trust self-signed certs.
     * <p>
     * This will make the connection trust <strong>only</strong> self-signed certificates!
     *
     * @param clientSelector the selector used to cache the underlying {@link HttpClient} to facilitate connection pooling
     *                       See {@link #modifyClient(String)}.
     * @return the outcall itself for fluent method calls
     */

    public Outcall trustSelfSignedCertificates(@Nullable String clientSelector) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManagerForSelfSignedCerts}, new SecureRandom());
            modifyClient(clientSelector).sslContext(sslContext);
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            throw Exceptions.handle(exception);
        }

        return this;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used
     * when opening a communications link to the resource referenced
     * by this outcall. If the timeout expires before the connection can be established, a
     * {@link java.net.http.HttpConnectTimeoutException} is raised. A timeout of zero is
     * interpreted as an infinite timeout.
     *
     * @param timeoutMillis specifies the connect-timeout value in milliseconds
     * @deprecated Use {@code modifyClient().connectTimeout(Duration.ofMillis(timeoutMillis))} or
     * {@link #withConfiguredTimeout(String, String)}
     */
    @Deprecated
    public void setConnectTimeout(int timeoutMillis) {
        modifyClient().connectTimeout(Duration.ofMillis(timeoutMillis));
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds.
     * <p>
     * Specifies the timeout when reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available for read, a
     * {@link java.net.http.HttpTimeoutException} is raised.
     *
     * @param timeoutMillis specifies the timeout value to be used in milliseconds
     * @deprecated Use {@link #setReadTimeout(Duration)}
     */
    @Deprecated
    public void setReadTimeout(int timeoutMillis) {
        modifyRequest().timeout(Duration.ofMillis(timeoutMillis));
    }

    /**
     * Sets the read timeout to a specified timeout.
     * <p>
     * Specifies the timeout when reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available for read, a
     * {@link java.net.http.HttpTimeoutException} is raised.
     *
     * @param readTimeout specifies the timeout value to be used
     * @see #withConfiguredTimeout(String, String)
     */
    public void setReadTimeout(Duration readTimeout) {
        modifyRequest().timeout(readTimeout);
    }

    /**
     * Sets the connect-timeout and read-timeout to the values specified in the config block http.outcall.timeouts.*
     * where * equals the configKey parameter.
     * <p>
     * See the http.outcall.timeouts block in component-050-kernel.conf for reference.
     *
     * @param configKey the config key of the timeout configuration block
     * @return this for fluent method calls
     * @deprecated Use {@link #withConfiguredTimeout(String, String)}
     */
    @Deprecated
    public Outcall withConfiguredTimeout(@Nonnull String configKey) {
        return withConfiguredTimeout(null, configKey);
    }

    /**
     * Sets the connect-timeout and read-timeout to the values specified in the config block http.outcall.timeouts.*
     * where * equals the configKey parameter.
     * <p>
     * See the http.outcall.timeouts block in component-050-kernel.conf for reference.
     *
     * @param clientSelector the selector used to cache the underlying {@link HttpClient} to facilitate connection pooling
     *                       See {@link #modifyClient(String)}.
     * @param configKey      the config key of the timeout configuration block
     * @return this for fluent method calls
     */
    public Outcall withConfiguredTimeout(@Nullable String clientSelector, @Nonnull String configKey) {
        Extension extension = Sirius.getSettings().getExtension("http.outcall.timeouts", configKey);

        Duration connectTimeout = extension.getConfig().getDuration("connectTimeout");
        if (!Duration.ZERO.equals(connectTimeout)) {
            modifyClient(clientSelector).connectTimeout(connectTimeout);
        }
        Duration readTimeout = extension.getConfig().getDuration("readTimeout");
        if (!Duration.ZERO.equals(readTimeout)) {
            setReadTimeout(readTimeout);
        }

        return this;
    }

    /**
     * Sends the given context as POST to the designated server.
     *
     * @param params  the data to POST
     * @param charset the charset to use when encoding the post data
     * @return the outcall itself for fluent method calls
     */
    public Outcall postData(Context params, Charset charset) {
        this.charset = charset;

        StringBuilder parameterString = new StringBuilder();
        Monoflop monoflop = Monoflop.create();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (monoflop.successiveCall()) {
                parameterString.append("&");
            }
            parameterString.append(URLEncoder.encode(entry.getKey(), charset));
            parameterString.append("=");
            parameterString.append(URLEncoder.encode(NLS.toMachineString(entry.getValue()), charset));
        }
        modifyRequest().setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED)
                       .POST(HttpRequest.BodyPublishers.ofString(parameterString.toString(), charset));

        return this;
    }

    /**
     * Marks the request as POST request and uses the given publisher as the body to POST.
     *
     * @param bodyPublisher the body to publish
     * @return the outcall itself for fluent method calls
     */
    public Outcall markAsPostRequest(HttpRequest.BodyPublisher bodyPublisher) {
        modifyRequest().POST(bodyPublisher);
        return this;
    }

    /**
     * Marks the request as HEAD request, only requesting headers.
     * <p>
     * Note that {@link #postFromOutput()} can not be invoked on this call, as we will send no body at all.
     *
     * @return the outcall itself for fluent method calls
     */
    public Outcall markAsHeadRequest() {
        modifyRequest().method(REQUEST_METHOD_HEAD, HttpRequest.BodyPublishers.noBody());
        return this;
    }

    /**
     * Sets an HTTP cookie
     *
     * @param name  name of the cookie
     * @param value value of the cookie
     */
    public void setCookie(String name, String value) {
        if (Strings.isFilled(name) && Strings.isFilled(value)) {
            setRequestProperty("Cookie", name + "=" + value);
        }
    }

    /**
     * Provides access to an output stream that writes into this call.
     * <p>
     * This will automatically mark the underlying request as a POST request,
     * with the contents written into this stream as body.
     *
     * @return the stream of data sent to the call / url
     */
    public OutputStream postFromOutput() {
        postFromOutput = true;
        return out;
    }

    /**
     * Executes the outcall and returns the response.
     * <p>
     * Use {@link #getResponseBody()} to directly access the response. Note however, that this will throw an
     * exception if a non-OK response has been received. use this method to access the response (and its contents)
     * even though an error was received.
     *
     * @return the response, with the body as {@link InputStream}
     * @throws IOException in case of any IO error or blacklisting
     */
    public HttpResponse<InputStream> getResponse() throws IOException {
        connect();
        return response;
    }

    private void connect() throws IOException {
        if (response != null) {
            return;
        }

        checkTimeoutBlacklist();

        if (oAuthAccessToken != null) {
            setRequestProperty(HttpHeaders.AUTHORIZATION, oAuthAccessToken.get());
        }

        if (client == null) {
            this.client = setupClient();
        }
        if (request == null) {
            if (postFromOutput) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
            }
            request = requestBuilder.build();
        }

        int attempts = MAX_REDIRECTS;
        while (attempts-- > 0) {
            performRequest();
            Optional<URI> redirectedURI = checkForRedirectURI();
            if (redirectedURI.isEmpty()) {
                return;
            }
            installRedirectRequest(redirectedURI.get());
        }
    }

    private HttpClient setupClient() {
        if (clientSelector != null) {
            HttpClient cachedClient = cachedHttpClients.get(clientSelector);
            if (cachedClient != null) {
                return cachedClient;
            }
        }

        HttpClient.Builder builder =
                clientBuilder != null ? clientBuilder : HttpClient.newBuilder().connectTimeout(defaultConnectTimeout);
        HttpClient result = builder.build();
        if (clientSelector != null) {
            cachedHttpClients.put(clientSelector, result);
        }
        return result;
    }

    private void performRequest() throws IOException {
        Watch watch = Watch.start();
        try (Operation operation = new Operation(() -> "Outcall to " + request.uri().getHost() + request.uri()
                                                                                                        .getPath(),
                                                 client.connectTimeout()
                                                       .orElse(defaultConnectTimeout)
                                                       .plusSeconds(1))) {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (oAuthTokenRefresher != null && isUnauthorized(response.statusCode())) {
                oAuthTokenRefresher.run();
                oAuthTokenRefresher = null;

                requestBuilder.setHeader(HttpHeaders.AUTHORIZATION, oAuthAccessToken.get());
                request = requestBuilder.build();
                performRequest();
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread was interrupted!");
        } catch (HttpTimeoutException | ConnectException | SocketTimeoutException exception) {
            addToTimeoutBlacklist();
            throw exception;
        } finally {
            timeToFirstByte.addValue(watch.elapsedMillis());
            if (Microtiming.isEnabled()) {
                watch.submitMicroTiming("OUTCALL", request.uri().getHost() + request.uri().getPath());
            }
        }
    }

    private void checkTimeoutBlacklist() throws IOException {
        if (connectTimeoutBlacklistDuration.isZero()) {
            return;
        }

        Tuple<Long, Boolean> blacklistedHostInformation = timeoutBlacklist.get(blacklistId);
        if (blacklistedHostInformation == null) {
            return;
        }

        Long timeout = blacklistedHostInformation.getFirst();
        if (timeout == null) {
            return;
        }

        if (timeout > System.currentTimeMillis()) {
            if (Boolean.FALSE.equals(blacklistedHostInformation.getSecond())) {
                blacklistedHostInformation.setSecond(true);
                throw new IOException(Strings.apply(
                        "Connections with blacklist identifier %s get rejected until %s due to connectivity issues.",
                        blacklistId,
                        LocalDateTime.ofEpochSecond(timeout / 1000, 0, ZoneOffset.UTC)));
            }
            throw new IOExceptionSkipLog(Strings.apply(
                    "Connections with blacklist identifier %s are currently rejected due to connectivity issues.",
                    blacklistId));
        } else {
            timeoutBlacklist.remove(blacklistId);
        }
    }

    private void addToTimeoutBlacklist() {
        if (connectTimeoutBlacklistDuration.isZero()) {
            return;
        }

        long now = System.currentTimeMillis();
        timeoutBlacklist.put(blacklistId, Tuple.create(now + connectTimeoutBlacklistDuration.toMillis(), false));
        if (timeoutBlacklist.size() > TIMEOUT_BLACKLIST_HIGH_WATERMARK) {
            // We collected a bunch of hosts - try to some cleanup (remove all hosts for which the timeout expired)...
            timeoutBlacklist.forEach((id, timeout) -> {
                if (timeout.getFirst() < now) {
                    timeoutBlacklist.remove(id);
                }
            });
        }
    }

    /**
     * Provides access to the response code of the call.
     *
     * @return the response code of the call
     * @throws IOException in case of any IO error
     */
    public int getResponseCode() throws IOException {
        return getResponse().statusCode();
    }

    /**
     * Returns the response header with the given name.
     *
     * @param name the name of the header to fetch
     * @return the value of the given header in the response or <tt>null</tt> if no header with this name was submitted.
     */
    @Nullable
    public String getHeaderField(String name) {
        try {
            connect();
        } catch (IOException exception) {
            // This is consistent with the internal behaviour of HttpUrlConnection :-/ ...
            Exceptions.ignore(exception);
            return null;
        }
        if (response == null) {
            return null;
        }
        return response.headers().firstValue(name).orElse(null);
    }

    /**
     * Returns the response header with the given name as an optional {@link LocalDateTime}.
     *
     * @param name the name of the header to fetch
     * @return the date of the given header wrapped in an Optional or empty if the field does not exist or can not be parsed as date
     */
    public Optional<LocalDateTime> getHeaderFieldDate(String name) {
        return Optional.ofNullable(getHeaderField(name)).flatMap(value -> {
            try {
                return Optional.of(LocalDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDateTime());
            } catch (Exception exception) {
                Exceptions.ignore(exception);
                return Optional.empty();
            }
        });
    }

    /**
     * Tries to parse a file name from the content disposition header.
     * <p>
     * The format of the header is defined here:
     * <p>
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html">http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html</a>
     * <p>
     * This header provides a filename for content that is going to be downloaded to the file system.
     *
     * @return an Optional containing the file name given by the header, or {@link Optional#empty()} if no file name is given
     */
    public Optional<String> parseFileNameFromContentDisposition() {
        return ContentDispositionParser.parseFileName(getHeaderField(HttpHeaders.CONTENT_DISPOSITION));
    }

    /**
     * Returns the result of the call as String.
     * <p>
     * Note that just like {@link #getResponseBody()}, this will throw an <tt>IOException</tt>, if the
     * response code isn't in the <tt>200-299</tt> range.
     *
     * @return a String containing the complete result of the call
     * @throws IOException in case of any IO error
     * @see #getResponseBody()
     */
    public String getData() throws IOException {
        return Streams.readToString(new InputStreamReader(getResponseBody(), getContentEncoding()));
    }

    /**
     * Returns the response as input stream.
     * <p>
     * Note that this will throw an <tt>IOException</tt>, if the response code isn't in the <tt>200-299</tt> range.
     * Use {@link #getResponse()} and {@link HttpResponse#body()} to access the body of erroneous responses.
     *
     * @return the response of the call as input stream
     * @throws IOException in case of any IO error
     */
    public InputStream getResponseBody() throws IOException {
        if (isErroneous()) {
            throw new IOException(Strings.apply("A non-OK response (%s) was received as a result of an HTTP call",
                                                getResponse().statusCode()));
        }
        return getResponse().body();
    }

    /**
     * Determines if a non-OK (out of the <tt>200-299</tt> range) status code has been received.
     *
     * @return <tt>true</tt> if a non-OK status code has been received, <tt>false</tt> otherwise
     * @throws IOException in case of any IO error
     */
    public boolean isErroneous() throws IOException {
        return getResponse().statusCode() < 200 || getResponse().statusCode() > 299;
    }

    /**
     * Returns the charset used by the server to encode the response.
     *
     * @return the charset used by the server or <tt>UTF-8</tt> as default
     */
    public Charset getContentEncoding() {
        String contentType = getHeaderField("content-type");
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            Matcher matcher = CHARSET_PATTERN.matcher(contentType);
            if (matcher.find()) {
                return Charset.forName(matcher.group(1).trim().toUpperCase());
            } else {
                return StandardCharsets.UTF_8;
            }
        } catch (Exception exception) {
            Exceptions.ignore(exception);
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * Provides access to the underlying HTTP client.
     *
     * @return the underlying HTTP client used to perform the request
     */
    public HttpClient getClient() {
        return client;
    }

    /**
     * Provides access to the underlying request used to perform the HTTP request.
     *
     * @return the actual request sent to the server
     */
    public HttpRequest getRequest() {
        return request;
    }

    /**
     * Returns the average time to first byte across all outcalls.
     *
     * @return the average TTFB across all outcalls
     */
    public static Average getTimeToFirstByte() {
        return timeToFirstByte;
    }

    /**
     * Enables OAuth token support for this outcall.
     * <p>
     * Note that this cannot be used alongside {@link #withBearerToken(String)} as these overwrite each other.
     *
     * @param accessTokenSupplier supplies the access token to be used for OAuth. It must contain the proper
     *                            authorization type, e.g. 'Bearer <token>'
     * @param tokenRefresher      supplies the refreshed token to be used for OAuth. This means the supplier will
     *                            perform the refresh of the token using OAuth refresh token flow. The token
     *                            must contain the proper authorization type, e.g. 'Bearer &lt;token&gt;'
     * @return the current instance for fluent method calls
     * @see #PREFIX_BEARER
     */
    public Outcall withOAuth(Supplier<String> accessTokenSupplier, Runnable tokenRefresher) {
        this.oAuthAccessToken = accessTokenSupplier;
        this.oAuthTokenRefresher = tokenRefresher;
        return this;
    }

    /**
     * Adds an externally computed or supplied bearer token, like a <b>JWT</b>.
     * <p>
     * Note that this cannot be used alongside {@link #withOAuth(Supplier, Runnable)} as these overwrite each other.
     * <p>
     * Also note that the "Bearer " prefix is applied automatically.
     *
     * @param bearerToken the token to add as <tt>Authorization</tt> header.
     * @return the current instance for fluent method calls
     */
    public Outcall withBearerToken(String bearerToken) {
        this.oAuthAccessToken = () -> PREFIX_BEARER + bearerToken;
        return this;
    }

    private void installRedirectRequest(URI redirectedURI) {
        HttpRequest.Builder redirectBuilder = requestBuilder.copy();
        redirectBuilder.uri(redirectedURI);
        if (shouldSwitchToGet(response.statusCode(), request.method())) {
            redirectBuilder.GET();
        }
        request = redirectBuilder.build();
    }

    /**
     * Checks the response and {@link #redirectPolicy}, if we should redirect and generates the URI to redirect to.
     *
     * @return The URI to redirect to, wrapped in an Optional, or {@link Optional#empty()} if no redirect is needed
     * @throws IOException in case we should redirect but fail to generate a URI
     */
    private Optional<URI> checkForRedirectURI() throws IOException {
        if (redirectPolicy == HttpClient.Redirect.NEVER) {
            // redirect disabled, no further checks needed
            return Optional.empty();
        }
        if (isRedirecting(response.statusCode())) {
            URI redirectedURI = makeRedirectedURI(response.headers());
            if (canRedirect(redirectedURI)) {
                return Optional.of(redirectedURI);
            }
        }
        return Optional.empty();
    }

    private boolean isUnauthorized(int statusCode) {
        return switch (statusCode) {
            case 400, 403 -> true;
            // 400: Bad Request => authorization might be missing
            // 403: Forbidden => authentication might be valid, but authorization is missing
            default -> false;
        };
    }

    private boolean isRedirecting(int statusCode) {
        return switch (statusCode) {
            case 301, 302, 303, 307, 308 -> true;
            // 301: Moved permanently => follow
            // 302: Found => follow
            // 303: See other => follow
            // 307: Temp Redirect => follow
            // 308: Permanent Redirect => follow
            default -> false;
        };
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Explain("Duplicated case is there to visualize expected values.")
    private boolean shouldSwitchToGet(int statusCode, String originalMethod) {
        return switch (statusCode) {
            case 301, 302 -> "POST".equals(originalMethod);
            case 303 -> true;
            case 307, 308 -> false;

            default -> false;
        };
    }

    private URI makeRedirectedURI(java.net.http.HttpHeaders headers) throws IOException {
        String locationHeader =
                headers.firstValue(HttpHeaders.LOCATION).orElseThrow(() -> new ConnectException("Invalid redirection"));
        return request.uri().resolve(makeURIFromLocation(locationHeader));
    }

    private URI makeURIFromLocation(String location) throws ConnectException {
        try {
            return new URI(location);
        } catch (URISyntaxException exception) {
            Exceptions.ignore(exception);
            try {
                // There are illegal chars in the redirect header url, try again with encoding the header
                URL urlObject = URI.create(location).toURL();
                return new URI(urlObject.getProtocol(),
                               urlObject.getUserInfo(),
                               urlObject.getHost(),
                               urlObject.getPort(),
                               urlObject.getPath(),
                               urlObject.getQuery(),
                               urlObject.getRef());
            } catch (MalformedURLException | URISyntaxException secondException) {
                throw new ConnectException("Invalid redirection: " + secondException.getMessage());
            }
        }
    }

    private boolean canRedirect(URI redirectedURI) {
        String newScheme = redirectedURI.getScheme();
        String oldScheme = request.uri().getScheme();
        return switch (redirectPolicy) {
            case ALWAYS -> true;
            case NEVER -> false;
            case NORMAL -> newScheme.equalsIgnoreCase(oldScheme) || "https".equalsIgnoreCase(newScheme);
        };
    }

    private static final int DEFAULT_ATTEMPTS = 3;
    private static final int DEFAULT_RETRY_DELAY_MILLIS = 250;

    /**
     * Executes the given task and returns its result while retrying the operation in case of an {@link IOException}.
     * <p>
     * Executes the task and retries (up to two additional attempts) in case of an IO error like a connection
     * exception. Note that the task should be idempotent as it might be executed several times. If a non IO error
     * occurs of the retries run out, the appropriate exception is re-thrown
     * <p>
     * Note that this applies a simple backoff strategy to wait a show amount of time before a retry.
     *
     * @param task the task to execute
     * @param <T>  the type returned by the task
     * @return the return value generated by the task
     * @throws Exception any exception thrown by the task
     */
    public static <T> T ioRetry(Producer<T> task) throws Exception {
        int attempts = DEFAULT_ATTEMPTS;
        while (true) {
            attempts--;
            try {
                return task.create();
            } catch (Exception exception) {
                if (attempts == 0 || (!(exception instanceof IOException)
                                      && !(exception.getCause() instanceof IOException))) {
                    throw exception;
                }
                Wait.randomMillis((DEFAULT_ATTEMPTS - attempts) * DEFAULT_RETRY_DELAY_MILLIS,
                                  (1 + DEFAULT_ATTEMPTS - attempts) * DEFAULT_RETRY_DELAY_MILLIS);
            }
        }
    }

    /**
     * Executes the given unit of work and retries in case of an IO error.
     * <p>
     * For details, see {@link #ioRetry(Producer)}.
     *
     * @param unit the unit of work to execute. This should be idempotent as it might be invoked several times
     * @throws Exception any exception thrown by the unit of work
     */
    public static void ioRetry(UnitOfWork unit) throws Exception {
        ioRetry(() -> {
            unit.execute();
            return true;
        });
    }

    public static Duration getDefaultConnectTimeout() {
        return defaultConnectTimeout;
    }

    public static Duration getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    /**
     * Sets a custom blacklist id.
     * The default blacklist id is the host of the uri.
     *
     * @param blacklistId The custom blacklist id to set.
     */
    public void setBlacklistId(@Nonnull String blacklistId) {
        this.blacklistId = blacklistId;
    }
}
