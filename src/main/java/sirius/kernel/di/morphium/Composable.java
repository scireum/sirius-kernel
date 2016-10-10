/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.morphium;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

/**
 * Provides a basic implementation fo {@link Adaptable} which supports a composition pattern.
 * <p>
 * Next to the <tt>adapter</tt> approach supported via {@link Adapters}, this implementation permits to
 * add components via the <tt>attach</tt> methods which can later be queries via <tt>tryAs, as and is</tt>.
 */
public class Composable implements Adaptable {

    private Map<Class<?>, Object> components;

    @Override
    public boolean is(@Nonnull Class<?> type) {
        if (components != null && components.containsKey(type)) {
            return true;
        }
        return Adaptable.super.is(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> Optional<A> tryAs(@Nonnull Class<A> adapterType) {
        if (components != null) {
            Object result = components.get(adapterType);
            if (result != null) {
                return Optional.of((A) result);
            }
        }

        return Adaptable.super.tryAs(adapterType);
    }

    /**
     * Adds the given component as instance for the given type.
     *
     * @param type      the type of the component which can be used to later retrieve the component
     * @param component the component itself
     * @param <T>       the generic type which ensures, that the component actually implements the given type
     */
    public <T> void attach(Class<? extends T> type, T component) {
        if (components == null) {
            components = Maps.newHashMap();
        }
        components.put(type, component);
    }

    /**
     * Adds tghe given component as instace for its class.
     * <p>
     * This is a boilerplate method for {@code attach(component.getClass(), component)}.
     *
     * @param component the component to attach for its own class
     */
    public void attach(Object component) {
        attach(component.getClass(), component);
    }
}
