/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Used to call an URL and send or receive data.
 * <p>
 * This is basically a thin wrapper over <tt>HttpURLConnection</tt> which adds some boilder plate code and a bit
 * of logging / monitoring.
 */
public class Outcall {

    private HttpURLConnection connection;
    private Charset charset = Charsets.UTF_8;
    private final URL url;

    /**
     * Creates a new <tt>Outcall</tt> to the given URL.
     *
     * @param url the url to call
     * @throws IOException in case of any IO error
     */
    public Outcall(URL url) throws IOException {
        this.url = url;
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
    }

    /**
     * Creates a new <tt>Outcall</tt> to the given URL, sending the given parameters as POST.
     *
     * @param url    the url to call
     * @param params the parameters to POST.
     * @throws IOException in case of any IO error
     * @deprecated use {@code new Outcall(url).postData(params, Charsets.UTF_8)} instead
     */
    @Deprecated
    public Outcall(URL url, Context params) throws IOException {
        this(url, params, Charsets.UTF_8);
    }

    /**
     * Creates a new <tt>Outcall</tt> to the given URL, sending the given parameters as POST.
     *
     * @param url     the url to call
     * @param params  the parameters to POST.
     * @param charset determines the charset to use when encoding the uploaded data
     * @throws IOException in case of any IO error
     * @deprecated use {@code new Outcall(url).postData(params, charset)} instead
     */
    @Deprecated
    public Outcall(URL url, Context params, Charset charset) throws IOException {
        this.url = url;
        this.charset = charset;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + charset.name());
        postData(params, charset);
    }

    /**
     * Sents the given context as POST to the designated server.
     *
     * @param params the data to POST
     * @param charset the charset to use when encoding the post data
     * @return the outcall itself for fluent method calls
     * @throws IOException in case of any IO error
     */
    public Outcall postData(Context params, Charset charset) throws IOException {
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
     * Provides access to the result of the call.
     * <p>
     * Once this method is called, the call will be started and data will be read.
     *
     * @return the stream returned by the call
     * @throws IOException in case of any IO error
     */
    public InputStream getInput() throws IOException {
        return connection.getInputStream();
    }

    /**
     * Provides access to the input of the call.
     *
     * @return the stream of data sent to the call / url
     * @throws IOException in case of any IO error
     */
    public OutputStream getOutput() throws IOException {
        return connection.getOutputStream();
    }

    /**
     * Sets the header of the HTTP call.
     *
     * @param name  name of the header to set
     * @param value value of the header to set
     */
    public void setRequestProperty(String name, String value) {
        connection.setRequestProperty(name, value);
    }

    /**
     * Sets the HTTP Authorization header.
     *
     * @param user     the username to use
     * @param password the password to use
     * @throws IOException in case of any IO error
     */
    public void setAuthParams(String user, String password) throws IOException {
        if (Strings.isEmpty(user)) {
            return;
        }
        try {
            String userAndPassword = user + ":" + password;
            String encodedAuthorization = BaseEncoding.base64().encode(userAndPassword.getBytes(charset.name()));
            setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
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
        StringWriter writer = new StringWriter();
        InputStreamReader reader = new InputStreamReader(getInput(), getContentEncoding());
        CharStreams.copy(reader, writer);
        reader.close();

        return writer.toString();
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
        if (connection.getContentEncoding() == null) {
            return Charsets.UTF_8;
        }
        try {
            return Charset.forName(connection.getContentEncoding());
        } catch (Exception e) {
            Exceptions.ignore(e);
            return Charsets.UTF_8;
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
