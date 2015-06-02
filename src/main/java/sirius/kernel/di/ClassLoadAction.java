/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

/**
 * Implementations of this class will be automatically detected and applied to
 * all matching classes on startup.
 */
public interface ClassLoadAction {
    /**
     * Returns the trigger-annotation which is used to identify classes of
     * interest.
     *
     * @return a class of an annotation which identifies classes processed by this implementation.
     * Returns <tt>null</tt> to process each class.
     */
    @Nullable
    Class<? extends Annotation> getTrigger();

    /**
     * Invoked for each class which contains the trigger-annotation.
     *
     * @param ctx   the context in which the part should be inserted
     * @param clazz the class to process
     * @throws Exception if the given class cannot be instantiated or registered as desired
     */
    void handle(@Nonnull MutableGlobalContext ctx, @Nonnull Class<?> clazz) throws Exception;
}
