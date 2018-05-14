/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.googlecode.junittoolbox.WildcardPatternSuite;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import sirius.kernel.commons.Lambdas;
import sirius.kernel.commons.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides a special test suite which executes all tests in all {@link Scenario scenarios}.
 * <p>
 * This approach can be used to execute the same tests in multiple environments (e.g. by
 * provisioning different containers via {@link DockerHelper}).
 * <p>
 * As this is a subclass of {@link WildcardPatternSuite} an appropriate
 * {@code @SuiteClasses} annotation must be present.
 */
public class ScenarioSuite extends WildcardPatternSuite {

    private List<Scenario> scenarios;

    private static class ScenarioRunner extends Runner {

        public static final Description FRAMEWORK_SETUP =
                Description.createTestDescription(Sirius.class, "Framework setup");
        public static final Description FRAMEWORK_TEARDOWN =
                Description.createTestDescription(Sirius.class, "Framework teardown");
        private String scenarioFile;
        private String filter;
        private List<Runner> allTests;

        protected ScenarioRunner(String scenarioFile, String filter, List<Runner> allTests) {
            this.scenarioFile = scenarioFile;
            this.filter = filter;
            this.allTests = allTests;
        }

        protected List<Runner> getEffectiveTests() {
            if (Strings.isEmpty(filter)) {
                return allTests;
            } else {
                Pattern filterPattern = Pattern.compile(filter);
                return allTests.stream()
                               .filter(r -> filterPattern.matcher(r.getDescription().getClassName()).matches())
                               .collect(Collectors.toList());
            }
        }

        @Override
        public Description getDescription() {
            Description description = Description.createSuiteDescription(determineScenarioName());
            description.addChild(FRAMEWORK_SETUP);
            getEffectiveTests().forEach(runner -> description.addChild(runner.getDescription()));
            description.addChild(FRAMEWORK_TEARDOWN);
            return description;
        }

        @NotNull
        public String determineScenarioName() {
            if (Strings.isEmpty(scenarioFile)) {
                return "Main Scenario";
            }

            return "Scenario: " + scenarioFile.replace(".conf", "");
        }

        @Override
        public void run(RunNotifier runNotifier) {
            System.setProperty(Sirius.SIRIUS_TEST_SCENARIO_PROPERTY, scenarioFile == null ? "" : scenarioFile);
            runNotifier.fireTestStarted(FRAMEWORK_SETUP);
            try {
                TestHelper.setUp(ScenarioSuite.class);
            } finally {
                runNotifier.fireTestFinished(FRAMEWORK_SETUP);
            }

            try {
                getEffectiveTests().forEach(runner -> runner.run(runNotifier));
            } finally {
                runNotifier.fireTestStarted(FRAMEWORK_TEARDOWN);
                try {
                    TestHelper.tearDown(ScenarioSuite.class);
                } finally {
                    runNotifier.fireTestFinished(FRAMEWORK_TEARDOWN);
                }
            }
        }
    }

    /**
     * Creates a new suite.
     * <p>
     * Use {@literal @RunWith(ScenarioSuite)} for a test suite.
     *
     * @param klass   ignored
     * @param builder the builder used to builde the suite
     * @throws InitializationError in case of an error during initialization
     */
    public ScenarioSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(ScenarioSuite.class, builder);
        this.scenarios = Arrays.asList(klass.getAnnotationsByType(Scenario.class));
    }

    @Override
    protected List<Runner> getChildren() {
        List<Runner> tests = super.getChildren();
        List<Runner> result = new ArrayList<>();
        result.add(new ScenarioRunner(null, null, tests));
        if (scenarios != null) {
            scenarios.stream()
                     .map(scenario -> new ScenarioRunner(scenario.file(), scenario.filter(), tests))
                     .collect(Lambdas.into(result));
        }

        return result;
    }
}
