/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import org.junit.jupiter.api.Tag;
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.SpecInfo;

/**
 * Processes the {@link Tag} on a per method / feature basis for Spock based {@link BaseSpecification specs}.
 */
public class JUnitGroupExtension extends AbstractAnnotationDrivenExtension<Tag> {

    @Override
    public void visitSpecAnnotation(Tag annotation, SpecInfo spec) {
        // Nothing to do, this is handled by the ScenarioRunner so that it also works with JUnit tests...
    }

    @Override
    public void visitFeatureAnnotation(Tag annotation, FeatureInfo feature) {
        feature.setSkipped(!ScenarioSuite.isScopeEnabled(annotation.value()));
    }
}
