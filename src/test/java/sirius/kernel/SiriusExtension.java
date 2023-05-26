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
 * JUnit 5 extension to support Sirius framework lifecycle. Annotated test classes will be provisioned with
 * a running framework and a cleared {@link CallContext} before each test.
 * <p>
 * Note: This currently does not support {@link sirius.kernel.Scenario scenarios}.
 */
public class SiriusExtension implements BeforeAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TestHelper.setUp(SiriusExtension.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        CallContext.initialize();
    }
}
