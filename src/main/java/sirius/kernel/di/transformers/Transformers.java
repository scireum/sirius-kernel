/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.PriorityParts;
import sirius.kernel.di.std.Register;

import java.util.Collection;
import java.util.List;

/**
 * Helper class to utilize available {@link Transformer} instances to perform conversions.
 * <p>
 * This class is automatically utilized by {@link Composable} but kept public in case custom implementations need to
 * access this functionality.
 */
@Register(classes = Transformers.class)
public class Transformers {

    private MultiMap<Tuple<Class<?>, Class<?>>, Transformer<?, ?>> factories;

    @PriorityParts(Transformer.class)
    private List<Transformer<?, ?>> factoryList;

    private Collection<Transformer<?, ?>> getFactories(Class<?> sourceType, Class<?> targetType) {
        if (factories == null) {
            MultiMap<Tuple<Class<?>, Class<?>>, Transformer<?, ?>> result = MultiMap.createOrdered();
            for (Transformer<?, ?> factory : factoryList) {
                result.put(Tuple.create(factory.getSourceClass(), factory.getTargetClass()), factory);
            }
            factories = result;
        }

        return factories.get(Tuple.create(sourceType, targetType));
    }

    /**
     * Determines if the given object can be transformed into the given target type.
     *
     * @param source the object to transform
     * @param target the target type
     * @param <T>    the generic type of the target
     * @return <tt>true</tt> if a conversion (adaption) is possible, <tt>false</tt> otherwise
     */
    public <T> boolean canMake(Object source, Class<T> target) {
        return make(source, target) != null;
    }

    /**
     * Tries to transform the given object to match the given target type.
     * <p>
     * The class is only transformed if it implements the {@link Transformable} interface.
     * <p>
     * Transformations are done recursively until a matching transformation happens or the class tried
     * to transform does not implement the {@link Transformable} interface.
     *
     * @param source the object to transform
     * @param target the target type
     * @param <T>    the generic type of the target
     * @return a transformed object matching the given type or <tt>null</tt> to indicate that no conversion was possible
     */
    @SuppressWarnings("unchecked")
    public <T> T make(Object source, Class<T> target) {
        Class<?> classToTransform = source.getClass();

        while (Transformable.class.isAssignableFrom(classToTransform)) {
            T result = makeWithClass(source, classToTransform, target);
            if (result != null) {
                return result;
            }
            classToTransform = classToTransform.getSuperclass();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T makeWithClass(Object sourceObject, Class<?> sourceClass, Class<T> target) {
        for (Transformer<?, ?> adapterFactory : getFactories(sourceClass, target)) {
            Transformer<? super Object, T> next = (Transformer<? super Object, T>) adapterFactory;
            T result = next.make(sourceObject);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
