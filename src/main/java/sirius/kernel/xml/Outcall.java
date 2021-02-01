/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.commons.Context;
import sirius.kernel.commons.Streams;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Microtiming;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to call an URL and send or receive data.
 * <p>
 * This is basically a thin wrapper over <tt>HttpURLConnection</tt> which adds some boilder plate code and a bit
 * of logging / monitoring.
 */
public class Outcall {

    private static final int DEFAULT_CONNECT_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
    private static final int DEFAULT_READ_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private static final String REQUEST_METHOD_POST = "POST";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=utf-8";
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");
    private static final X509TrustManager TRUST_SELF_SIGNED_CERTS = new TrustingSelfSignedTrustManager();

    /**
     * Keeps track of hosts for which we ran into a connect timeout.
     * <p>
     * These hosts are blacklisted for a short amout of time ({@link #connectTimeoutBlacklistDuration}) to prevent
     * cascading failures.
     */
    private static final Map<String, Long> timeoutBlacklist = new ConcurrentHashMap<>();

    /**
     * If the {@link #timeoutBlacklist} contains more than the given number of entries, we remove all expired ones
     * manually. These might be hosts which are only connected sporadiccaly and had a hickup. Everything else will be
     * kept clean in {@link #checkTimeoutBlacklist(URL)}.
     */
    private static final int TIMEOUT_BLACKLIST_HIGHT_WATERMARK = 100;

    @ConfigValue("http.outcall.connectTimeoutBlacklistDuration")
    private static Duration connectTimeoutBlacklistDuration;

    private final HttpURLConnection connection;
    private Charset charset = StandardCharsets.UTF_8;

    private static final Average timeToFirstByte = new Average();

    /**
     * Creates a new <tt>Outcall</tt> to the given URL.
     *
     * @param url the url to call
     * @throws IOException in case of any IO error
     */
    public Outcall(URL url) throws IOException {
        checkTimeoutBlacklist(url);

        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
    }

    private void checkTimeoutBlacklist(URL url) throws IOException {
        if (connectTimeoutBlacklistDuration.isZero()) {
            return;
        }

        Long timeout = timeoutBlacklist.get(url.getHost());
        if (timeout != null) {
            if (timeout > System.currentTimeMillis()) {
                throw new IOException(Strings.apply(
                        "Connecting to host %s is currently rejected due to connectivity issues.",
                        url.getHost()));
            } else {
                timeoutBlacklist.remove(url.getHost());
            }
        }
    }

    /**
     * Returns the average time to first byte across all outcalls.
     * @return the average TTFB across all outcalls
     */
    public static Average getTimeToFirstByte() {
        return timeToFirstByte;
    }

    /**
     * Sents the given context as POST to the designated server.
     *
     * @param params  the data to POST
     * @param charset the charset to use when encoding the post data
     * @return the outcall itself for fluent method calls
     * @throws IOException in case of any IO error
     */
    public Outcall postData(Context params, Charset charset) throws IOException {
        markAsPostRequest();
        connection.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED);
        this.charset = charset;

        OutputStreamWriter writer = new OutputStreamWriter(getOutput(), charset.name());
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), charset.name()));
            sb.append("=");
            sb.append(URLEncoder.encode(NLS.toMachineString(entry.getValue()), charset.name()));
        }
        writer.write(sb.toString());
        writer.flush();

        return this;
    }

    /**
     * Marks the request as POST request.
     *
     * @return the outcall itself for fluent method calls
     * @throws IOException if the method cannot be reset or if the requested method isn't valid for HTTP.
     */
    public Outcall markAsPostRequest() throws IOException {
        connection.setRequestMethod(REQUEST_METHOD_POST);
        return this;
    }

    /**
     * Provides access to the result of the call.
     * <p>
     * Once this method is called, the call will be started and data will be read.
     *
     * @return the stream returned by the call
     * @throws IOException in case of any IO error
     */
    public InputStream getInput() throws IOException {
        Watch watch = Watch.start();
        try {
            return connection.getInputStream();
        } catch (SocketTimeoutException e) {
            addToTimeoutBlacklist();
            throw e;
        } catch (IOException e) {
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    return errorStream;
                }
            }

            throw e;
        } finally {
            timeToFirstByte.addValue(watch.elapsedMillis());
            if (Microtiming.isEnabled()) {
                watch.submitMicroTiming("OUTCALL", connection.getURL().getHost() + connection.getURL().getPath());
            }
        }
    }

    private void addToTimeoutBlacklist() {
        if (connectTimeoutBlacklistDuration.isZero()) {
            return;
        }

        long now = System.currentTimeMillis();
        timeoutBlacklist.put(connection.getURL().getHost(), now + connectTimeoutBlacklistDuration.toMillis());
        if (timeoutBlacklist.size() > TIMEOUT_BLACKLIST_HIGHT_WATERMARK) {
            // We collected bunch of hosts - try to some cleanup (remove all hosts for which the timeout expired)...
            timeoutBlacklist.forEach((host, timeout) -> {
                if (timeout < now) {
                    timeoutBlacklist.remove(host);
                }
            });
        }
    }

    /**
     * Provides access to the input of the call.
     *
     * @return the stream of data sent to the call / url
     * @throws IOException in case of any IO error
     */
    public OutputStream getOutput() throws IOException {
        try {
            return connection.getOutputStream();
        } catch (SocketTimeoutException e) {
            addToTimeoutBlacklist();
            throw e;
        }
    }

    /**
     * Sets the header of the HTTP call.
     *
     * @param name  name of the header to set
     * @param value value of the header to set
     * @return the outcall itself for fluent method calls
     */
    public Outcall setRequestProperty(String name, String value) {
        connection.setRequestProperty(name, value);
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
        if (connection instanceof HttpsURLConnection) {
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[]{TRUST_SELF_SIGNED_CERTS}, new SecureRandom());
                ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw Exceptions.handle(e);
            }
        }
        return this;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used
     * when opening a communications link to the resource referenced
     * by this outcall. If the timeout expires before the
     * connection can be established, a
     * java.net.SocketTimeoutException is raised. A timeout of zero is
     * interpreted as an infinite timeout.
     *
     * @param timeoutMillis specifies the connect timeout value in milliseconds
     */
    public void setConnectTimeout(int timeoutMillis) {
        connection.setConnectTimeout(timeoutMillis);
    }

    /**
     * Sets the read timeout to a specified timeout, in
     * milliseconds. A non-zero value specifies the timeout when
     * reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available
     * for read, a java.net.SocketTimeoutException is raised. A
     * timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeoutMillis specifies the timeout value to be used in milliseconds
     */
    public void setReadTimeout(int timeoutMillis) {
        connection.setReadTimeout(timeoutMillis);
    }

    /**
     * Returns the result of the call as String.
     *
     * @return a String containing the complete result of the call
     * @throws IOException in case of any IO error
     */
    public String getData() throws IOException {
        return Streams.readToString(new InputStreamReader(getInput(), getContentEncoding()));
    }

    /**
     * Returns the response header with the given name.
     *
     * @param name the name of the header to fetch
     * @return the value of the given header in the response or <tt>null</tt> if no header with this name was submitted.
     */
    @Nullable
    public String getHeaderField(String name) {
        return connection.getHeaderField(name);
    }

    /**
     * Returns the charset used by the server to encode the response.
     *
     * @return the charset used by the server or <tt>UTF-8</tt> as default
     */
    public Charset getContentEncoding() {
        String contentType = connection.getContentType();
        if (contentType == null) {
            return charset;
        }
        try {
            Matcher m = CHARSET_PATTERN.matcher(contentType);
            if (m.find()) {
                return Charset.forName(m.group(1).trim().toUpperCase());
            } else {
                return charset;
            }
        } catch (Exception e) {
            Exceptions.ignore(e);
            return StandardCharsets.UTF_8;
        }
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
}
