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
 * Created by aha on 24.11.14.
 */
public class Adapters {

    private static Map<Class<?>, AdapterFactory<?>> factories;

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

    public static boolean canMake(Adaptable adaptable, Class<?> type) {
        AdapterFactory<?> factory = getFactory(type);
        if (factory == null) {
            return false;
        }

        Object result = factory.make(adaptable);
        return result != null;
    }

    public static <A> A make(Adaptable adaptable, Class<A> adapterType) {
        AdapterFactory<A> factory = getFactory(adapterType);
        if (factory == null) {
            return null;
        }

        return factory.make(adaptable);
    }
}
