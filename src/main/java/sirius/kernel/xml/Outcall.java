/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.Sirius;
import sirius.kernel.async.Operation;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Monoflop;
import sirius.kernel.commons.Streams;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Microtiming;
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
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
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
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String REQUEST_METHOD_HEAD = "HEAD";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ACCEPT_DEFAULT_VALUE = "*/*";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=utf-8";
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");
    private static final X509TrustManager TRUST_SELF_SIGNED_CERTS = new TrustingSelfSignedTrustManager();
    private static final int MAX_REDIRECTS = 5;

    /**
     * Keeps track of hosts for which we ran into a connect timeout.
     * <p>
     * These hosts are blacklisted for a short amount of time ({@link #connectTimeoutBlacklistDuration}) to prevent
     * cascading failures.
     */
    private static final Map<String, Long> timeoutBlacklist = new ConcurrentHashMap<>();

    /**
     * If the {@link #timeoutBlacklist} contains more than the given number of entries, we remove all expired ones
     * manually. These might be hosts which are only connected sporadically and had a hiccup. Everything else will be
     * kept clean in {@link #checkTimeoutBlacklist(URI)}.
     */
    private static final int TIMEOUT_BLACKLIST_HIGH_WATERMARK = 100;

    @ConfigValue("http.outcall.timeouts.default.connectTimeout")
    private static Duration defaultConnectTimeout;

    @ConfigValue("http.outcall.timeouts.default.readTimeout")
    private static Duration defaultReadTimeout;

    @ConfigValue("http.outcall.connectTimeoutBlacklistDuration")
    private static Duration connectTimeoutBlacklistDuration;

    private HttpClient client;
    private HttpRequest request;
    private final HttpClient.Builder clientBuilder;
    private final HttpRequest.Builder requestBuilder;
    private HttpResponse<InputStream> response;
    private HttpClient.Redirect redirectPolicy = HttpClient.Redirect.NORMAL;
    private Charset charset = StandardCharsets.UTF_8;

    // Provide an output stream for old apis
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private boolean postFromOutput = false;

    private static String defaultUserAgent;
    private static final Average timeToFirstByte = new Average();

    private static String buildDefaultUserAgent() {
        if (defaultUserAgent == null) {
            // default format is 'product.name/product.version (+product.baseUrl)', but version or baseUrl could be empty
            StringBuilder userAgentString = new StringBuilder(Sirius.getSettings().getString("product.name"));
            String version = Sirius.getSettings().getString("product.version");
            if (Strings.isFilled(version)) {
                userAgentString.append("/");
                userAgentString.append(version);
            }
            String baseUrl = Sirius.getSettings().getString("product.baseUrl");
            if (Strings.isFilled(baseUrl)) {
                userAgentString.append(" (+");
                userAgentString.append(baseUrl);
                userAgentString.append(")");
            }
            defaultUserAgent = userAgentString.toString();
        }
        return defaultUserAgent;
    }

    /**
     * Creates a new <tt>Outcall</tt> to the given URL.
     *
     * @param uri the url to call
     * @throws IOException if the host is blacklisted
     */
    public Outcall(URI uri) throws IOException {
        checkTimeoutBlacklist(uri);

        clientBuilder = HttpClient.newBuilder().connectTimeout(defaultConnectTimeout);
        requestBuilder = HttpRequest.newBuilder(uri)
                                    .header(HEADER_USER_AGENT, buildDefaultUserAgent())
                                    .header(HEADER_ACCEPT, HEADER_ACCEPT_DEFAULT_VALUE)
                                    .timeout(defaultReadTimeout);
    }

    /**
     * Allows to modify the client before the request is sent by returning the builder that is used to create it.
     *
     * @return the underlying {@link HttpClient.Builder}
     */
    public HttpClient.Builder modifyClient() {
        if (client != null) {
            throw new IllegalStateException("Can no longer modify client, request has already been sent!");
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
        setRequestProperty(HEADER_IF_MODIFIED_SINCE,
                           ifModifiedSince.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        return this;
    }

    /**
     * Sets the HTTP Authorization header.
     *
     * @param user     the username to use
     * @param password the password to use
     * @return the outcall itself for fluent method calls
     * @throws IOException in case of any IO error
     */
    public Outcall setAuthParams(String user, String password) throws IOException {
        if (Strings.isEmpty(user)) {
            return this;
        }

        try {
            String userAndPassword = user + ":" + password;
            String encodedAuthorization = Base64.getEncoder().encodeToString(userAndPassword.getBytes(charset.name()));
            setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
        return this;
    }

    /**
     * Makes the underlying connection trust self-signed certs.
     * <p>
     * This will make the connection trust <strong>only</strong> self-signed certificates!
     *
     * @return the outcall itself for fluent method calls
     */
    public Outcall trustSelfSignedCertificates() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUST_SELF_SIGNED_CERTS}, new SecureRandom());
            modifyClient().sslContext(sslContext);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw Exceptions.handle(e);
        }

        return this;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used
     * when opening a communications link to the resource referenced
     * by this outcall. If the timeout expires before the
     * connection can be established, a
     * {@link java.net.http.HttpConnectTimeoutException} is raised. A timeout of zero is
     * interpreted as an infinite timeout.
     *
     * @param timeoutMillis specifies the connect timeout value in milliseconds
     */
    public void setConnectTimeout(int timeoutMillis) {
        modifyClient().connectTimeout(Duration.ofMillis(timeoutMillis));
    }

    /**
     * Sets the read timeout to a specified timeout, in
     * milliseconds. A non-zero value specifies the timeout when
     * reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available
     * for read, a {@link java.net.http.HttpTimeoutException} is raised. A
     * timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeoutMillis specifies the timeout value to be used in milliseconds
     */
    public void setReadTimeout(int timeoutMillis) {
        modifyRequest().timeout(Duration.ofMillis(timeoutMillis));
    }

    /**
     * Sets the connect and read timeout to the values specified in the config block http.outcall.timeouts.*
     * where * equals the configKey parameter.
     * <p>
     * See the http.outcall.timeouts.soap block in component-kernel.conf for reference.
     *
     * @param configKey the config key of the timeout configuration block
     * @return this for fluent method calls
     */
    public Outcall withConfiguredTimeout(@Nonnull String configKey) {
        Extension extension = Sirius.getSettings().getExtension("http.outcall.timeouts", configKey);

        setConnectTimeout((int) extension.getConfig().getDuration("connectTimeout").toMillis());
        setReadTimeout((int) extension.getConfig().getDuration("readTimeout").toMillis());

        return this;
    }

    /**
     * Sends the given context as POST to the designated server.
     *
     * @param params  the data to POST
     * @param charset the charset to use when encoding the post data
     * @return the outcall itself for fluent method calls
     * @throws IOException in case of any IO error
     */
    public Outcall postData(Context params, Charset charset) throws IOException {
        this.charset = charset;

        StringBuilder parameterString = new StringBuilder();
        Monoflop monoflop = Monoflop.create();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (monoflop.successiveCall()) {
                parameterString.append("&");
            }
            parameterString.append(URLEncoder.encode(entry.getKey(), charset.name()));
            parameterString.append("=");
            parameterString.append(URLEncoder.encode(NLS.toMachineString(entry.getValue()), charset.name()));
        }
        modifyRequest().setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED)
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
     * @throws IOException if the method cannot be reset or if the requested method isn't valid for HTTP.
     */
    public Outcall markAsHeadRequest() throws IOException {
        modifyRequest().method(REQUEST_METHOD_HEAD, HttpRequest.BodyPublishers.noBody());
        return this;
    }

    /**
     * Sets a HTTP cookie
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
     * Provides access to a output stream that writes into this call.
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
     * @throws IOException in case of any IO error
     */
    public HttpResponse<InputStream> getResponse() throws IOException {
        connect();
        return response;
    }

    private void connect() throws IOException {
        if (response != null) {
            return;
        }

        if (client == null) {
            client = clientBuilder.build();
        }
        if (request == null) {
            if (postFromOutput) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
            }
            request = requestBuilder.build();
        }

        int connections = 0;
        do {
            makeRequest();
            connections++;
        } while (connections < MAX_REDIRECTS && checkAndBuildRedirectRequest(response));
    }

    private void makeRequest() throws IOException {
        Watch watch = Watch.start();
        try (Operation op = new Operation(() -> "Outcall to " + request.uri().getHost() + request.uri().getPath(),
                                          client.connectTimeout().orElse(defaultConnectTimeout).plusSeconds(1))) {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread was interrupted!");
        } catch (HttpTimeoutException | ConnectException | SocketTimeoutException e) {
            addToTimeoutBlacklist();
            throw e;
        } finally {
            timeToFirstByte.addValue(watch.elapsedMillis());
            if (Microtiming.isEnabled()) {
                watch.submitMicroTiming("OUTCALL", request.uri().getHost() + request.uri().getPath());
            }
        }
    }

    private void checkTimeoutBlacklist(URI uri) throws IOException {
        if (connectTimeoutBlacklistDuration.isZero()) {
            return;
        }

        Long timeout = timeoutBlacklist.get(uri.getHost());
        if (timeout != null) {
            if (timeout > System.currentTimeMillis()) {
                throw new IOException(Strings.apply(
                        "Connecting to host %s is currently rejected due to connectivity issues.",
                        uri.getHost()));
            } else {
                timeoutBlacklist.remove(uri.getHost());
            }
        }
    }

    private void addToTimeoutBlacklist() {
        if (connectTimeoutBlacklistDuration.isZero()) {
            return;
        }

        long now = System.currentTimeMillis();
        timeoutBlacklist.put(request.uri().getHost(), now + connectTimeoutBlacklistDuration.toMillis());
        if (timeoutBlacklist.size() > TIMEOUT_BLACKLIST_HIGH_WATERMARK) {
            // We collected a bunch of hosts - try to some cleanup (remove all hosts for which the timeout expired)...
            timeoutBlacklist.forEach((host, timeout) -> {
                if (timeout < now) {
                    timeoutBlacklist.remove(host);
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
        } catch (IOException e) {
            // This is consistent with the internal behaviour of HttpUrlConnection :-/ ...
            Exceptions.ignore(e);
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
     * @return the date of the given header wrapped in an Optional or empty if the field does not exists or can not be parsed as date
     */
    public Optional<LocalDateTime> getHeaderFieldDate(String name) {
        return Optional.ofNullable(getHeaderField(name)).flatMap(value -> {
            try {
                return Optional.of(LocalDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDateTime());
            } catch (Exception e) {
                Exceptions.ignore(e);
                return Optional.empty();
            }
        });
    }

    /**
     * Tries to parse a file name from the content disposition header.
     * <p>
     * The format of the header is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be downloaded to the file system.
     *
     * @return an Optional containing the file name given by the header, or Optional.empty if no file name is given
     */
    public Optional<String> parseFileNameFromContentDisposition() {
        return ContentDispositionParser.parseFileName(getHeaderField(HEADER_CONTENT_DISPOSITION));
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
            Matcher m = CHARSET_PATTERN.matcher(contentType);
            if (m.find()) {
                return Charset.forName(m.group(1).trim().toUpperCase());
            } else {
                return StandardCharsets.UTF_8;
            }
        } catch (Exception e) {
            Exceptions.ignore(e);
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
     * Checks to see if a new request is needed and builds it into the active {@link #request}.
     */
    private boolean checkAndBuildRedirectRequest(HttpResponse<InputStream> response) throws IOException {
        if (redirectPolicy == HttpClient.Redirect.NEVER) {
            // redirect disabled, no further checks needed
            return false;
        }
        int responseCode = response.statusCode();
        if (isRedirecting(responseCode)) {
            URI redirectedURI = makeRedirectedURI(response.headers());
            if (canRedirect(redirectedURI)) {
                // copy and modify the base request builder
                HttpRequest.Builder redirectBuilder = requestBuilder.copy();
                redirectBuilder.uri(redirectedURI);
                if (shouldSwitchToGet(responseCode, request.method())) {
                    redirectBuilder.GET();
                }
                request = redirectBuilder.build();
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isRedirecting(int statusCode) {
        if (statusCode < 300) {
            // < 300: not a redirect code
            return false;
        }
        if (statusCode > 308) {
            // 309-399 Unassigned => don't follow
            // > 399: not a redirect code
            return false;
        }

        return switch (statusCode) {
            // 300: MultipleChoice => don't follow
            // 304: Not Modified => don't follow
            // 305: Proxy Redirect => don't follow.
            // 306: Unused => don't follow
            case 300, 304, 305, 306 -> false;
            // 301: Moved permanently => follow
            // 302: Found => follow
            // 303: See other => follow
            // 307: Temp Redirect => follow
            // 308: Permanent Redirect => follow
            default -> true;
        };
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Explain("Duplicated case is there to visualize expected values.")
    private boolean shouldSwitchToGet(int statusCode, String originalMethod) {
        return switch (statusCode) {
            case 301, 302 -> "POST".equals(originalMethod);
            case 303 -> true;
            case 307, 308 -> false;

            default -> false; // unexpected
        };
    }

    private URI makeRedirectedURI(HttpHeaders headers) throws IOException {
        Optional<String> locationHeader = headers.firstValue("Location");
        if (locationHeader.isEmpty()) {
            throw new ConnectException("Invalid redirection");
        }
        URI redirectedURI = makeURIFromLocation(locationHeader.get());

        // redirect could be relative to original URL
        redirectedURI = request.uri().resolve(redirectedURI);
        return redirectedURI;
    }

    private URI makeURIFromLocation(String location) throws ConnectException {
        try {
            return new URI(location);
        } catch (URISyntaxException exception) {
            Exceptions.ignore(exception);
            try {
                // There are illegal chars in the redirect header url, try again with encoding the header
                URL urlObject = new URL(location);
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
}
