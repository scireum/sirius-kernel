/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.SpecInfo;

/**
 * Processes the {@link Scope} on a per method / feature basis for Spock based {@link BaseSpecification specs}.
 */
public class ScopeExtension extends AbstractAnnotationDrivenExtension<Scope> {

    @Override
    public void visitSpecAnnotation(Scope annotation, SpecInfo spec) {
        // Nothing to do, this is handled by the ScenarioRunner so that it also works with JUnit tests...
    }

    @Override
    public void visitFeatureAnnotation(Scope annotation, FeatureInfo feature) {
        feature.setSkipped(!ScenarioSuite.isScopeEnabled(annotation.value()));
    }
}
