/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import javax.annotation.Nonnull;

/**
 * Visible for instances of {@link sirius.kernel.di.ClassLoadAction} and {@link FieldAnnotationProcessor} during the system
 * initialization to make parts visible in the <tt>GlobalContext</tt>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface MutableGlobalContext extends GlobalContext {
    /**
     * Registers the given part for the given lookup classes.
     * <p>
     * Note that the given part does not need to implement the given interfaces or classes.
     *
     * @param part          the object to be stored in the global context.
     * @param lookupClasses the list of classes (don't need to be interfaces) by which this part can be
     *                      fetched from the global context. At least one class should be given, otherwise the
     *                      part will be discarded.
     */
    void registerPart(@Nonnull Object part, @Nonnull Class<?>... lookupClasses);

    /**
     * Registers the given part for the given name and lookup classes.
     *
     * @param uniqueName    the name of this part, which can be used to retrieve this part from the global
     *                      context. This name doesn't have to be globally unique, but within <b>each</b> of
     *                      the given lookup classes.
     * @param part          the object to be stored in the global context.
     * @param lookupClasses the list of classes (don't need to be interfaces) by which this part can be
     *                      fetched from the global context. At least one class should be given, otherwise the
     *                      part will be discarded.
     */
    void registerPart(@Nonnull String uniqueName, @Nonnull Object part, @Nonnull Class<?>... lookupClasses);

}
