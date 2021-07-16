/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.io.IOException;
import java.io.Reader;

/**
 * Wraps a given reader and removes a BOM (byte order mark) if present.
 * <p>
 * MS Excel places such BOM in UTF-8 encoded CSV files (which is invalid). Therefore such bytes have to be removed.
 */
public class BOMReader extends Reader {

    private boolean bomSkipped = false;
    private final Reader delegate;

    /**
     * Creates a new reader wrapping the given one.
     *
     * @param reader the reader to wrap (and filter)
     */
    public BOMReader(Reader reader) {
        this.delegate = reader;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (bomSkipped) {
            return delegate.read(cbuf, off, len);
        }
        if (len == 0) {
            return 0;
        }

        int c = delegate.read();
        if (Character.getType(c) == Character.FORMAT) {
            c = delegate.read();
        }
        bomSkipped = true;
        if (c == 0) {
            return 0;
        }
        cbuf[off++] = (char) c;
        if (len == 1) {
            return 1;
        }
        return 1 + delegate.read(cbuf, off, len - 1);
    }

    @Override
    public boolean ready() throws IOException {
        return delegate.ready();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
