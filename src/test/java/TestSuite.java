/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

import com.googlecode.junittoolbox.SuiteClasses;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.runner.RunWith;
import sirius.kernel.ScenarioSuite;
import sirius.kernel.TestHelper;

// JUnit 4 annotations below
@RunWith(ScenarioSuite.class)
@SuiteClasses({"**/*Test.class", "**/*Spec.class"})
// JUnit 5 annotations below
@Suite
@IncludeClassNamePatterns({"^.*Test$", ".^.*Spec$"})
@SelectPackages("sirius")
public class TestSuite {

    @BeforeClass
    @BeforeAll
    public static void setUp() {
        TestHelper.setUp(TestSuite.class);
    }

    @AfterClass
    @AfterAll
    public static void tearDown() {
        TestHelper.tearDown(TestSuite.class);
    }
}
