/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Terminates the framework once all tests of a launcher session have been executed.
 * <p>
 * This is the counterpart of {@link SiriusExtension} which boots the framework for the first test requiring it.
 * Performing the teardown here (instead of within a JVM shutdown hook) is essential: an exception thrown by a
 * {@link TestLifecycleParticipant} escapes into the build tool and therefore actually fails the test run. Within a
 * shutdown hook the very same exception would be swallowed, as the build result has already been determined by then.
 * <p>
 * This listener is registered via the {@link java.util.ServiceLoader} (see
 * {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}), so it is also picked up by
 * downstream projects which use this test-jar.
 */
public class SiriusTestSessionListener implements LauncherSessionListener {

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        // Note that build tools may open additional sessions (e.g. to scan the classpath) which never start the
        // framework at all. TestHelper.performTearDown() turns into a no-op for those.
        TestHelper.performTearDown();
    }
}
