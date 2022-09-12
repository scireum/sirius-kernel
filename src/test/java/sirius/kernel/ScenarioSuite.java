/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.googlecode.junittoolbox.WildcardPatternSuite;
import org.junit.jupiter.api.Tag;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.spockframework.runtime.RunContext;
import org.spockframework.runtime.SpecInfoBuilder;
import org.spockframework.runtime.Sputnik;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.SpecInfo;
import sirius.kernel.commons.Strings;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final Map<String, Boolean> scopeSettings = new HashMap<>();
    private final List<Scenario> scenarios;

    private static class ScenarioRunner extends Runner {

        public static final Description FRAMEWORK_SETUP =
                Description.createTestDescription(Sirius.class, "Framework setup");
        public static final Description FRAMEWORK_TEARDOWN =
                Description.createTestDescription(Sirius.class, "Framework teardown");
        private final String scenarioFile;
        private final String includes;
        private final String excludes;
        private final List<Runner> allTests;

        protected ScenarioRunner(String scenarioFile, String includes, String excludes, List<Runner> allTests) {
            this.scenarioFile = scenarioFile;
            this.includes = includes;
            this.excludes = excludes;
            this.allTests = allTests;
        }

        protected List<Runner> getEffectiveTests() {
            Stream<Runner> stream = allTests.stream();

            // Filter on includes / excludes defined by @Scenario
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
                // In case of a configured scenario, ensure framework shutdown
                if (scenarioFile != null) {
                    TestHelper.performTearDown();
                }
                TestHelper.setUp(ScenarioSuite.class);
            } catch (Exception e) {
                runNotifier.fireTestFailure(new Failure(FRAMEWORK_SETUP, e));
            } finally {
                runNotifier.fireTestFinished(FRAMEWORK_SETUP);
            }

            try {
                getEffectiveTests().forEach(runner -> run(runNotifier, runner));
            } finally {
                runNotifier.fireTestStarted(FRAMEWORK_TEARDOWN);
                try {
                    TestHelper.tearDown(ScenarioSuite.class);
                } catch (Exception e) {
                    runNotifier.fireTestFailure(new Failure(FRAMEWORK_TEARDOWN, e));
                } finally {
                    runNotifier.fireTestFinished(FRAMEWORK_TEARDOWN);
                }
            }
        }

        private void run(RunNotifier runNotifier, Runner runner) {
            if (ignoreUnrollFeatures(runNotifier, runner)) {
                return;
            }

            // Ignore tests with nightly tags.
            // CAVEAT: this only work when annotated on class level, like the former scope
            // This interception breaks surefires test counting
            Tag tag = runner.getDescription().getTestClass().getAnnotation(Tag.class);
            if (tag != null && !isScopeEnabled(tag.value())) {
                runNotifier.fireTestIgnored(runner.getDescription());
                return;
            }

            runner.run(runNotifier);
        }

        /**
         * Warn if {@link spock.lang.Unroll} is used as it crushes the output of JUNIT in every sane IDE.
         * <p>
         * Checks if a given suite is run via <tt>Sputnik</tt> and therefore based on Spock. If so, we check
         * if an Unroll annotation is present and abort the spec if so.
         * <p>
         * This is done as the output generated by <tt>Unroll</tt> completely screws the output as shown
         * in the JUNIT views in any sane IDE. Also, <tt>Unroll</tt> provides no real benefit except than
         * generating more output (which is pointless if it ends up anywhere in the UI).
         *
         * @param runNotifier the notifier used for reporting
         * @param runner      the runner to check
         * @return <tt>true</tt> if an invalid spec was found, <tt>false</tt> otherwise
         */
        private boolean ignoreUnrollFeatures(RunNotifier runNotifier, Runner runner) {
            if (!(runner instanceof Sputnik)) {
                return false;
            }

            SpecInfo spec = (new SpecInfoBuilder(runner.getDescription().getTestClass())).build();
            RunContext.get().createExtensionRunner(spec).run();
            if (spec.getAllFeatures().stream().anyMatch(FeatureInfo::isReportIterations)) {
                runNotifier.fireTestIgnored(runner.getDescription());
                runNotifier.fireTestFailure(new Failure(Description.TEST_MECHANISM,
                                                        new IllegalStateException(
                                                                "@Unroll not supported by ScenarioSuite: "
                                                                + runner.getDescription().getTestClass())));
                return true;
            }

            return false;
        }
    }

    /**
     * Creates a new suite.
     * <p>
     * Use {@literal @RunWith(ScenarioSuite)} for a test suite.
     *
     * @param klass   the test suite to execute
     * @param builder the builder used to build the suite
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
        result.add(new ScenarioRunner(null, null, null, tests));
        if (scenarios != null) {
            scenarios.stream()
                     .map(scenario -> new ScenarioRunner(scenario.file(),
                                                         scenario.includes(),
                                                         scenario.excludes(),
                                                         tests))
                     .forEach(result::add);
        }

        return result;
    }

    /**
     * Determines if the given test scope is enabled.
     * <p>
     * This evaluates against a JUnit groups that must be active. We test this by checking for exclusion
     * of the group.
     *
     * @param scope the scope to check
     * @return <tt>true</tt> if the scope is enabled, <tt>false</tt> otherwise
     */
    public static boolean isScopeEnabled(String scope) {
        if (Strings.isEmpty(scope)) {
            return true;
        }

        return scopeSettings.computeIfAbsent(scope, ignored -> {
            // NOTE: this is potentially dangerous, as it works on one potentiall input property and not the
            // actual system activated groups, but should work until spock will be removed
            return !System.getProperty("test.excluded.groups", "").contains(scope);
        });
    }
}
