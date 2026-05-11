/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import sirius.kernel.async.CallContext;

/**
 * JUnit 5 extension to support the Sirius framework lifecycle. Annotated test classes will be provisioned with
 * a running framework and a cleared {@link CallContext} before each test.
 * <p>
 * Note: This currently does not support {@link sirius.kernel.Scenario scenarios}.
 */
public class SiriusExtension implements BeforeAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        // Allow setting restricted HTTP headers, like `Origin:` … Only very few tests actually need this, but it is
        // required to set the property before any HttpURLConnection is created, so we do it here globally
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        TestHelper.setUp(SiriusExtension.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        CallContext.initialize();
    }
}
