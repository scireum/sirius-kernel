/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import sirius.kernel.di.std.AutoRegister;
import sirius.kernel.di.std.Priorized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Transforms a <tt>Transformable</tt> into a given target type.
 * <p>
 * Used by {@link Transformers} to transform a certain type into another one. This permits to transform existing
 * classes into interfaces or other classes without modifying them.
 * <p>
 * Note, if a transformer simply invokes the constructor of the target class, passing in the source instance,
 * the {@link AutoTransform} annotation can directly be placed on the target class (along with the appropriate
 * public constructor). The framework will then synthesize an appropriate transformer automatically.
 *
 * @param <S> the source type which is supported as input of the transformation
 * @param <T> the target type which is supported as output of the transformation
 */
@AutoRegister
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
