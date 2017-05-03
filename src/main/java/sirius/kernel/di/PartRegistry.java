/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import sirius.kernel.Sirius;
import sirius.kernel.async.ExecutionPoint;
import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

/**
 * An instance of PartRegistry is kept by {@link sirius.kernel.di.Injector} to track all registered
 * parts.
 */
class PartRegistry implements MutableGlobalContext {

    /*
     * Contains all registered parts
     */
    private final MultiMap<Class<?>, Object> parts = MultiMap.createSynchronized();

    /*
     * Contains classes which are replaced by customer customizations (custom classes). This map is used
     * to speed up checking while initializing the system.
     *
     * Content: ClassToReplace -> Replacement
     */
    private final Map<Class<?>, Object> shadowMap = Maps.newHashMap();

    /*
     * Contains all registered parts with a unique name. These parts will also
     * be contained in parts. This is just a lookup map if searched by unique
     * name.
     */
    private final Map<Class<?>, Map<String, Object>> namedParts = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked")
    @Override
    public <P> P getPart(Class<P> clazz) {
        Collection<?> items = parts.get(clazz);
        if (items.isEmpty()) {
            return null;
        }
        if (items.size() > 1) {
            Injector.LOG.WARN(
                    "Retrieving a Part for %s from multiple implementations (%s) - picking a random one! Use @Replace to clarify! Context: %s",
                    clazz.getName(),
                    items,
                    ExecutionPoint.snapshot().toString());
        }
        return (P) items.iterator().next();
    }

    @Override
    public <P> PartCollection<P> getPartCollection(final Class<P> partInterface) {
        return new PartCollection<P>() {

            @Override
            public Class<P> getInterface() {
                return partInterface;
            }

            @Override
            public Collection<P> getParts() {
                return PartRegistry.this.getParts(partInterface);
            }

            @Override
            public Iterator<P> iterator() {
                return getParts().iterator();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> Collection<P> getParts(Class<? extends P> partInterface) {
        return (Collection<P>) parts.get(partInterface);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <L, P> Collection<P> getParts(@Nonnull Class<L> lookupClass, @Nonnull Class<? extends P> partType) {
        return (Collection<P>) parts.get(lookupClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> Collection<Tuple<String, P>> getNamedParts(@Nonnull Class<P> partInterface) {
        return (Collection<Tuple<String, P>>) (Object) Tuple.fromMap(namedParts.get(partInterface));
    }

    @Override
    public <T> T wire(T object) {
        // Wire....
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            wireClass(clazz, object);
            clazz = clazz.getSuperclass();
        }

        return object;
    }

    /*
     * Called to initialize all static fields with annotations
     */
    void wireClass(Class<?> clazz) {
        while (clazz != null) {
            wireClass(clazz, null);
            clazz = clazz.getSuperclass();
        }
    }

    /*
     * Called to initialize all field of the given class in the given object
     */
    private void wireClass(Class<?> clazz, Object object) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isFinal(field.getModifiers()) && (object != null
                                                            || Modifier.isStatic(field.getModifiers()))) {
                field.setAccessible(true);
                getParts(FieldAnnotationProcessor.class).stream()
                                                        .filter(p -> field.isAnnotationPresent(p.getTrigger()))
                                                        .forEach(p -> {
                                                            try {
                                                                p.handle(this, object, field);
                                                            } catch (Exception e) {
                                                                Injector.LOG.WARN(
                                                                        "Cannot process annotation %s on %s.%s: %s "
                                                                        + "(%s)",
                                                                        p.getTrigger().getName(),
                                                                        field.getDeclaringClass().getName(),
                                                                        field.getName(),
                                                                        e.getMessage(),
                                                                        e.getClass().getName());
                                                            }
                                                        });
            }
        }
    }

    @Override
    public void registerPart(Object part, Class<?>... implementedInterfaces) {
        String customizationName = Sirius.getCustomizationName(part.getClass().getName());
        if (!Sirius.isActiveCustomization(customizationName)) {
            return;
        }
        Class<?> predecessor = part.getClass().isAnnotationPresent(Replace.class) ?
                               part.getClass().getAnnotation(Replace.class).value() :
                               null;
        if (predecessor != null && customizationName == null) {
            if (Sirius.isStartedAsTest() && part.getClass().getSimpleName().endsWith("Mock")) {
                Injector.LOG.WARN("%s is mocked by %s", predecessor, part.getClass());
            } else {
                Injector.LOG.WARN(
                        "@Replace should be only used within a customization. %s (%s) seems to be a base class!",
                        part,
                        part.getClass());
            }
        }
        Object successor = shadowMap.get(part.getClass());
        for (Class<?> iFace : implementedInterfaces) {
            if (successor == null) {
                parts.put(iFace, part);
                if (predecessor != null) {
                    synchronized (parts) {
                        parts.getUnderlyingMap().get(iFace).removeIf(p -> p.getClass().equals(predecessor));
                    }
                }
            } else {
                synchronized (parts) {
                    Collection<Object> partList = parts.getUnderlyingMap().get(iFace);
                    if (partList == null || !partList.contains(successor)) {
                        parts.put(iFace, part);
                    }
                }
            }
        }
        if (predecessor != null) {
            shadowMap.put(predecessor, part);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> P getPart(String uniqueName, Class<P> clazz) {
        Map<String, Object> partsOfClass = namedParts.get(clazz);
        if (partsOfClass == null) {
            return null;
        }
        if (uniqueName == null) {
            return null;
        }
        return (P) partsOfClass.get(uniqueName);
    }

    @Override
    public synchronized void registerDynamicPart(String uniqueName, Object part, Class<?> lookupClass) {
        Map<String, Object> partsOfClass = namedParts.get(lookupClass);
        if (partsOfClass != null) {
            Object originalPart = partsOfClass.get(uniqueName);
            if (originalPart != null) {
                partsOfClass.remove(uniqueName);
                Collection<Object> specificParts = parts.getUnderlyingMap().get(lookupClass);
                if (specificParts != null) {
                    specificParts.remove(originalPart);
                }
            }
        }
        registerPart(uniqueName, part, lookupClass);
    }

    @Override
    public synchronized void registerPart(String uniqueName, Object part, Class<?>... implementedInterfaces) {
        String customizationName = Sirius.getCustomizationName(part.getClass().getName());
        if (!Sirius.isActiveCustomization(customizationName)) {
            return;
        }
        for (Class<?> clazz : implementedInterfaces) {
            Map<String, Object> partsOfClass = namedParts.get(clazz);
            if (partsOfClass != null) {
                Object currentPart = partsOfClass.get(uniqueName);
                if (currentPart != null) {
                    String currentCustomization = Sirius.getCustomizationName(currentPart.getClass().getName());
                    int comp = Sirius.compareCustomizations(currentCustomization, customizationName);
                    if (comp < 0) {
                        // Don't override a customized part with a system part
                        return;
                    } else if (comp == 0) {
                        throw new IllegalArgumentException(Strings.apply(
                                "The part '%s' cannot be registered as '%s' for class '%s'. The id is already taken "
                                + "by: %s (%s)",
                                part,
                                clazz.getName(),
                                uniqueName,
                                partsOfClass.get(uniqueName),
                                partsOfClass.get(uniqueName).getClass().getName()));
                    }
                }
            } else {
                partsOfClass = Collections.synchronizedMap(new TreeMap<>());
                namedParts.put(clazz, partsOfClass);
            }
            partsOfClass.put(uniqueName, part);
        }
        registerPart(part, implementedInterfaces);
    }

    @Override
    public <P> P findPart(String uniqueName, Class<P> clazz) {
        P part = getPart(uniqueName, clazz);
        if (part == null) {
            throw new NoSuchElementException(Strings.apply("Cannot find %s of type %s", uniqueName, clazz.getName()));
        }
        return part;
    }

    /*
     * Processes all annotations of all known parts.
     */
    void processAnnotations() {
        Set<Object> initializedObjects = Sets.newHashSet();
        parts.getUnderlyingMap().values().stream().flatMap(e -> e.stream()).forEach(part -> {
            wire(part);
            if (part instanceof Initializable) {
                initialize(part, initializedObjects);
            }
        });
    }

    private void initialize(Object part, Set<Object> initializedObjects) {
        if (initializedObjects.contains(part)) {
            return;
        }
        try {
            initializedObjects.add(part);
            ((Initializable) part).initialize();
        } catch (Exception e) {
            Injector.LOG.WARN("Error initializing %s (%s)", part, part.getClass().getName());
            Injector.LOG.WARN(e);
        }
    }
}
