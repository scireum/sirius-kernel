/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps a given <tt>OutputStream</tt> and blocks every call to {@link #close()}.
 * <p>
 * Note that instead of {@link #close()}, we call {@link #flush()} on the underlying stream.
 */
public class UncloseableOutputStream extends OutputStream {

    private final OutputStream delegate;

    /**
     * Creates a new instance which wraps the given delegate.
     *
     * @param delegate the stream to delegate all method calls to, except for <tt>close</tt>
     */
    public UncloseableOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.flush();
    }
}
