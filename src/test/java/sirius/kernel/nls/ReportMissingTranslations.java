/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import sirius.kernel.TestLifecycleParticipant;
import sirius.kernel.di.std.Register;

/**
 * Reports missing translations by failing the testsuite.
 * <p>
 * Invokes {@link Babelfish#reportMissingTranslations()} to (intentionally) crash the test suite in case of missing
 * translations.
 */
@Register
public class ReportMissingTranslations implements TestLifecycleParticipant {

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public void beforeTests() {
        // No setup required
    }

    @Override
    public void afterTests() {
        NLS.getTranslationEngine().reportMissingTranslations();
    }
}
