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

import java.io.*;
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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
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
     */
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
     */
    public Outcall(URL url, Context params, Charset charset) throws IOException {
        this.url = url;
        this.charset = charset;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + charset.name());
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
     * Returns the charset used by the server to encode the response.
     *
     * @return the charset used by the server or <tt>UTF-8</tt> as default
     */
    public Charset getContentEncoding() {
        try {
            return connection.getContentEncoding() == null ? Charsets.UTF_8 : Charset.forName(connection.getContentEncoding());
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
