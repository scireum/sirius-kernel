/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.morphium;

import com.google.common.collect.Maps;
import sirius.kernel.di.Injector;

import java.util.Map;

/**
 * Helper class to utilize available {@link sirius.kernel.di.morphium.AdapterFactory} instances to perform
 * conversions (to adapt objects).
 * <p>
 * This class is automatically utilized by {@link sirius.kernel.di.morphium.Adaptable} but kept public in case
 * custom implementation need to access this functionality.
 */
public class Adapters {

    private static Map<Class<?>, AdapterFactory<?>> factories;

    private Adapters() {
    }

    @SuppressWarnings("unchecked")
    private static <A> AdapterFactory<A> getFactory(Class<A> targetType) {
        if (factories == null) {
            Map<Class<?>, AdapterFactory<?>> result = Maps.newHashMap();
            for (AdapterFactory<?> factory : Injector.context().getParts(AdapterFactory.class)) {
                result.put(factory.getAdapterClass(), factory);
            }
            factories = result;
        }

        return (AdapterFactory<A>) factories.get(targetType);
    }

    /**
     * Determines if the given object can be adapted into the given target type.
     *
     * @param adaptable the object to adapt
     * @param type      the target type
     * @return <tt>true</tt> if a conversion (adaption) is possible, <tt>false</tt> otherwise
     */
    public static boolean canMake(Adaptable adaptable, Class<?> type) {
        AdapterFactory<?> factory = getFactory(type);
        if (factory == null) {
            return false;
        }

        Object result = factory.make(adaptable);
        return result != null;
    }

    /**
     * Tries to adapt the given object to match the given target type.
     *
     * @param adaptable   the object to adapt
     * @param adapterType the target type
     * @param <A>         the generic type of adapterType
     * @return an adapted object matching the given type or <tt>null</tt> to indicate that no conversion (adaption)
     * was possible
     */
    public static <A> A make(Adaptable adaptable, Class<A> adapterType) {
        AdapterFactory<A> factory = getFactory(adapterType);
        if (factory == null) {
            return null;
        }

        return factory.make(adaptable);
    }
}
