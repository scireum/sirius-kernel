/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import com.google.common.collect.Maps;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Part;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

/**
 * Provides a basic implementation fo {@link Transformable} which supports a composition pattern.
 * <p>
 * Next to the <tt>Adapter Pattern</tt> supported via {@link Transformers}, this implementation permits to
 * add components via the <tt>attach</tt> methods which can later be queried via <tt>tryAs, as</tt> and <tt>is</tt>.
 * <p>
 * This class can be used as base class or embedded into another class (using {@link #Composable(Object)}) to
 * make it <tt>Transformable</tt>.
 * </p>
 */
public class Composable implements Transformable {

    private Object source;
    protected Map<Class<?>, Object> components;

    private static final Object NULL = new Object();

    @Part
    private static Transformers adapters;

    /**
     * Default constructor used, when <tt>Composable</tt> is used as parent class.
     */
    public Composable() {
        this.source = this;
    }

    /**
     * Provides a constructor which can be used to support the composition pattern.
     *
     * @param source the class or object which is made {@link Transformable}.
     */
    public Composable(Object source) {
        this.source = source;
    }

    @Override
    public boolean is(@Nonnull Class<?> type) {
        if (source.getClass().isAssignableFrom(type)) {
            return true;
        }

        if (components != null) {
            Object result = components.get(type);
            if (result != null) {
                return result != NULL;
            }
        }

        return tryAs(type).isPresent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A as(@Nonnull Class<A> adapterType) {
        if (source.getClass().isAssignableFrom(adapterType)) {
            return (A) this;
        }

        return tryAs(adapterType).orElseThrow(() -> {
            return new IllegalArgumentException(Strings.apply("Cannot transform %s into %s",
                                                              getClass().getName(),
                                                              adapterType.getName()));
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> Optional<A> tryAs(@Nonnull Class<A> adapterType) {
        if (source.getClass().isAssignableFrom(adapterType)) {
            return Optional.of((A) this);
        }

        if (components != null) {
            Object result = components.get(adapterType);
            if (result != null) {
                return unwrapComponent(result);
            }
        }

        Object result = adapters.make(source, adapterType);
        if (result == null) {
            result = NULL;
        }

        attach(adapterType, result);
        return unwrapComponent(result);
    }

    @SuppressWarnings("unchecked")
    private <A> Optional<A> unwrapComponent(Object result) {
        if (result == NULL) {
            return Optional.empty();
        } else {
            return Optional.of((A) result);
        }
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
     * Adds the given component as instance for its class.
     * <p>
     * This is a boilerplate method for {@code attach(component.getClass(), component)}.
     *
     * @param component the component to attach for its own class
     */
    public void attach(Object component) {
        attach(component.getClass(), component);
    }
}
