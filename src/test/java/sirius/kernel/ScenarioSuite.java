/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.googlecode.junittoolbox.WildcardPatternSuite;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import sirius.kernel.commons.Lambdas;
import sirius.kernel.commons.Strings;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        private String includes;
        private String excludes;
        private List<Runner> allTests;

        protected ScenarioRunner(String scenarioFile, String includes, String excludes, List<Runner> allTests) {
            this.scenarioFile = scenarioFile;
            this.includes = includes;
            this.excludes = excludes;
            this.allTests = allTests;
        }

        protected List<Runner> getEffectiveTests() {
            Stream<Runner> stream = allTests.stream();
            if (Strings.isFilled(includes)) {
                Pattern filterPattern = Pattern.compile(includes);
                stream = stream.filter(r -> filterPattern.matcher(r.getDescription().getClassName()).matches());
            }
            if (Strings.isFilled(excludes)) {
                Pattern filterPattern = Pattern.compile(excludes);
                stream = stream.filter(r -> !filterPattern.matcher(r.getDescription().getClassName()).matches());
            }

            return stream.collect(Collectors.toList());
        }

        @Override
        public Description getDescription() {
            Description description = Description.createSuiteDescription(determineScenarioName());
            description.addChild(FRAMEWORK_SETUP);
            getEffectiveTests().forEach(runner -> description.addChild(runner.getDescription()));
            description.addChild(FRAMEWORK_TEARDOWN);
            return description;
        }

        @Nonnull
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
     * @param klass   the test suite to execute
     * @param builder the builder used to builde the suite
     * @throws InitializationError in case of an error during initialization
     */
    public ScenarioSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
        this.scenarios = Arrays.asList(klass.getAnnotationsByType(Scenario.class));
    }

    @Override
    protected List<Runner> getChildren() {
        List<Runner> tests = super.getChildren();
        List<Runner> result = new ArrayList<>();
        if (scenarios == null) {
            result.add(new ScenarioRunner(null, null, null, tests));
        } else {
            scenarios.stream()
                     .map(scenario -> new ScenarioRunner(scenario.file(),
                                                         scenario.includes(),
                                                         scenario.excludes(),
                                                         tests))
                     .collect(Lambdas.into(result));
        }

        return result;
    }
}
