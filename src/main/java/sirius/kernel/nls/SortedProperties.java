/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Provides a {@link Properties} class with keys sorted lexicographically
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
class SortedProperties extends Properties {

    @Override
    @SuppressWarnings({"raw","unchecked"})
    public synchronized Enumeration<Object> keys() {
        Enumeration<Object> keysEnum = super.keys();
        Vector keyList = new Vector();
        while (keysEnum.hasMoreElements()) {
            keyList.add(keysEnum.nextElement());
        }
        Collections.sort(keyList);
        return keyList.elements();
    }
}
