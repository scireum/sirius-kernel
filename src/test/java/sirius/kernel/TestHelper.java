/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.nls.NLS;

/**
 * Initializes and stops Sirius as part of the tests.
 */
public class TestHelper {

    private TestHelper() {
    }

    public static void setUp() {
        Sirius.start(new Setup(Setup.Mode.TEST, Sirius.class.getClassLoader()));
        NLS.setDefaultLanguage("de");
    }

    public static void tearDown() {
        Sirius.stop();
        NLS.getTranslationEngine().reportMissingTranslations();
    }
}
