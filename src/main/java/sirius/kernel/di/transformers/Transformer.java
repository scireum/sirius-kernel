/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import sirius.kernel.di.std.Priorized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Transforms a <tt>Transformable</tt> into a given target type.
 * <p>
 * Used by {@link Transformers} to transform a certain type into another one. This
 * permits to transform existing classes into interfaces or other classes without modifying them.
 *
 * @param <S> the source type which is supported as input of the transformation
 * @param <T> the target type which is supported as output of the transformation
 */
public interface Transformer<S, T> extends Priorized {

    /**
     * As multiple adapters for the same pair of types might be present, the priority is used to specify precedence.
     *
     * @return the priority (lower is better) of this transformer
     */
    @Override
    default int getPriority() {
        return Priorized.DEFAULT_PRIORITY;
    }

    /**
     * Determines if child classes of the provided source class are also supported by this transformer.
     * <p>
     * In order to avoid unwanted transformations, this can be set to false. In this case, only the matching
     * class to the provided source class is transformed.
     *
     * @return <tt>true</tt> if child classes should also be transformed via this transformer, <tt>false</tt> otherwise
     */
    default boolean supportChildClasses() {
        return true;
    }

    /**
     * Returns the source type for which this factory can perform transformations.
     *
     * @return the source type for which transformations are supported.
     */
    Class<S> getSourceClass();

    /**
     * Returns the target type for which this factory can perform transformations.
     *
     * @return the target type for which transformations are supported.
     */
    Class<T> getTargetClass();

    /**
     * Generates a new object of the desired target type for the given object to transform.
     *
     * @param source the object to transform
     * @return the transformed instance (matching the target class) or <tt>null</tt> to indicate that no conversion
     * was possible.
     */
    @Nullable
    T make(@Nonnull S source);
}
