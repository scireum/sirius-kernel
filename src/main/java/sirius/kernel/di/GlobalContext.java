/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.Named;
import sirius.kernel.di.std.Priorized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Used to access parts managed by the {@link Injector}.
 * <p>
 * This is the central repository containing all parts managed by the injector. Parts stored inhere can be either
 * accessed via the <tt>getPart</tt> or <tt>findPart</tt> methods. Also all annotations processed by an appropriate
 * {@link FieldAnnotationProcessor} (like {@link sirius.kernel.di.std.Part}) will use this context to find the
 * requested part.
 */
public interface GlobalContext {

    /**
     * Finds the previously registered part for the given lookup class.
     * <p>
     * If several parts where registered for this class, the first one is chosen. If no part was registered,
     * <tt>null</tt> is returned.
     *
     * @param clazz the class used to lookup the requested part
     * @param <P>   the type of the requested part
     * @return the first part registered for the given class or <tt>null</tt> if no part was registered yet.
     */
    @Nullable
    <P> P getPart(@Nonnull Class<P> clazz);

    /**
     * Retrieves a part of the requested type with the given unique name.
     * <p>
     * If no matching part is found, <tt>null</tt> is returned.
     *
     * @param uniqueName the name for which the part was registered
     * @param clazz      one of the lookup classes for which the part was registered
     * @param <P>        the type of the requested part
     * @return the part which the given unique name, registered for the given class, or <tt>null</tt> if no matching
     * part was found.
     */
    @Nullable
    <P> P getPart(@Nonnull String uniqueName, @Nonnull Class<P> clazz);

    /**
     * Returns the instance of <tt>implementationClass</tt> which has been registered for <tt>lookupClass</tt>.
     * <p>
     * This is more of an edge case, where we know that an instance of a given class has been registered, but not for
     * its own class, but (at least) for the given <tt>lookupClass</tt>.
     *
     * @param lookupClass         the class for which the requested part has been registered
     * @param implementationClass the actual class of the part to fetch
     * @param <P>                 the generic type of the implementation class
     * @return the registered instance or <tt>null</tt> if none was present
     */
    @Nullable
    <P> P getPartByType(@Nonnull Class<? extends Named> lookupClass, @Nonnull Class<P> implementationClass);

    /**
     * Like {@link #getPart(String, Class)} this method tried to find the part with the given name, registered for the
     * given lookup class. Rather than returning <tt>null</tt> when no part is found, this throws a
     * {@link sirius.kernel.health.HandledException} with an appropriate message.
     *
     * @param uniqueName the name for which the part was registered
     * @param clazz      one of the lookup classes for which the part was registered
     * @param <P>        the type of the requested part
     * @return the part which the given unique name, registered for the given class.
     * @throws sirius.kernel.health.HandledException if no matching part was found
     */
    @Nonnull
    <P> P findPart(@Nonnull String uniqueName, @Nonnull Class<P> clazz);

    /**
     * Returns all parts which are currently registered for the given lookup class.
     *
     * @param lookupClass one of the lookup classes for which the parts of interest were registered
     * @param partType    the expected type of the parts returned
     * @param <L>         the type of the lookup class
     * @param <P>         the type of the requested parts
     * @return a collection of all parts registered for the given class. If no parts were found,
     * an empty collection is returned
     */
    @Nonnull
    <L, P> Collection<P> getParts(@Nonnull Class<L> lookupClass, @Nonnull Class<? extends P> partType);

    /**
     * Returns all parts which are currently registered for the given lookup class.
     *
     * @param partInterface one of the lookup classes for which the parts of interest were registered
     * @param <P>           the type of the requested parts
     * @return a collection of all parts registered for the given class. If no parts were found,
     * an empty collection is returned
     */
    @Nonnull
    <P> Collection<P> getParts(@Nonnull Class<? extends P> partInterface);

    /**
     * Returns all parts which are currently registered for the given lookup class, sorted by their priority
     *
     * @param partInterface one of the lookup classes for which the parts of interest were registered
     * @param <P>           the type of the requested parts
     * @return a list of all parts registered for the given class. If no parts were found,
     * an empty list is returned
     */
    @Nonnull
    <P extends Priorized> List<P> getPriorizedParts(@Nonnull Class<? extends P> partInterface);

    /**
     * Returns all parts which are currently registered for the given lookup class and have a name attached.
     *
     * @param partInterface one of the lookup classes for which the parts of interest were registered
     * @param <P>           the type of the requested parts
     * @return a collection of all parts registered for the given class with a name. If no parts were found,
     * an empty collection is returned.
     */
    @Nonnull
    <P> Collection<Tuple<String, P>> getNamedParts(@Nonnull Class<P> partInterface);

    /**
     * Returns a {@link PartCollection} which contains all parts registered for the given lookup class.
     *
     * @param partInterface one of the lookup classes for which the parts of interest were registered
     * @param <P>           the type of the requested parts
     * @return a <tt>PartCollection</tt> containing all parts registered for the given class. If no parts were found,
     * an empty collection is returned
     */
    @Nonnull
    <P> PartCollection<P> getPartCollection(@Nonnull Class<P> partInterface);

    /**
     * Processes all annotations of the given objects class (or super classes).
     *
     * @param object the object which annotations should be processed to fill the respective fields
     * @param <T>    the type of the object to permit a qualified return type
     * @return the "wired" object, which has no filled fields. This is just returned for convenience and will not
     * another instance or clone of the given object.
     */
    @Nonnull
    <T> T wire(@Nonnull T object);

    /**
     * Registers a new part for the given name and lookup classes.
     * <p>
     * In contrast to {@link sirius.kernel.di.MutableGlobalContext#registerPart(String, Object, Class[])} this
     * method may replace already existing implementations.
     *
     * @param uniqueName    the unique name of the part
     * @param part          the part itself
     * @param partInterface the lookup class for which the part is registered
     */
    void registerDynamicPart(@Nonnull String uniqueName, @Nonnull Object part, @Nonnull Class<?> partInterface);
}
