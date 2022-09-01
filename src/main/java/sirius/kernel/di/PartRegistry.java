/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import sirius.kernel.Sirius;
import sirius.kernel.async.ExecutionPoint;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.Named;
import sirius.kernel.di.std.Priorized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final Map<Class<?>, Object> shadowMap = new HashMap<>();

    /*
     * Contains all registered parts with a unique name. These parts will also
     * be contained in parts. This is just a lookup map if searched by unique
     * name.
     */
    private final Map<Class<?>, Map<String, Object>> namedParts = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <P> P getPart(Class<P> clazz) {
        Collection<?> items = parts.get(clazz);
        if (items.isEmpty()) {
            return null;
        }
        if (items.size() > 1) {
            Injector.LOG.WARN("Retrieving a Part for %s from multiple implementations (%s) - picking a random one!"
                              + " Use @Replace to clarify! Context: %s",
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

    @Nonnull
    @Override
    public <P extends Priorized> List<P> getPriorizedParts(@Nonnull Class<? extends P> partInterface) {
        return getParts(partInterface).stream()
                                      .sorted(Comparator.comparingInt(Priorized::getPriority))
                                      .collect(Collectors.toList());
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
                getParts(FieldAnnotationProcessor.class).stream()
                                                        .filter(p -> field.isAnnotationPresent(p.getTrigger()))
                                                        .forEach(p -> {
                                                            try {
                                                                field.setAccessible(true);
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
        Object successor = shadowMap.get(part.getClass());
        Class<?> predecessor = determinePredecessor(part, customizationName);

        registerPart(part, implementedInterfaces, predecessor, successor);
    }

    private void registerPart(Object part, Class<?>[] implementedInterfaces, Class<?> predecessor, Object successor) {
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
    }

    private Class<?> determinePredecessor(Object part, String customizationName) {
        Class<?> predecessor = part.getClass().isAnnotationPresent(Replace.class) ?
                               part.getClass().getAnnotation(Replace.class).value() :
                               null;
        if (predecessor == null) {
            return null;
        }

        if (customizationName == null) {
            if (Sirius.isStartedAsTest() && part.getClass().getSimpleName().endsWith("Mock")) {
                Injector.LOG.WARN("%s is mocked by %s", predecessor, part.getClass());
            } else {
                Injector.LOG.WARN(
                        "@Replace should be only used within a customization. %s (%s) seems to be a base class!",
                        part,
                        part.getClass());
            }
        }

        shadowMap.put(predecessor, part);

        return predecessor;
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

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <P> P getPartByType(@Nonnull Class<? extends Named> lookupClass, @Nonnull Class<P> implementationClass) {
        Map<String, Object> partsOfClass = namedParts.get(lookupClass);
        if (partsOfClass == null) {
            return null;
        }

        return (P) partsOfClass.values().stream().filter(implementationClass::isInstance).findFirst().orElse(null);
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
                registerNamedPart(uniqueName, part, customizationName, clazz, partsOfClass);
            } else {
                partsOfClass = Collections.synchronizedMap(new TreeMap<>());
                namedParts.put(clazz, partsOfClass);
                partsOfClass.put(uniqueName, part);
            }
        }
        registerPart(part, implementedInterfaces);
    }

    private void registerNamedPart(String uniqueName,
                                   Object part,
                                   String customizationName,
                                   Class<?> clazz,
                                   Map<String, Object> partsOfClass) {
        if (partsOfClass.containsKey(uniqueName)) {
            checkOverwriteForNamedPart(uniqueName, part, customizationName, clazz, partsOfClass);
        } else {
            partsOfClass.put(uniqueName, part);
        }
    }

    private void checkOverwriteForNamedPart(String uniqueName,
                                            Object part,
                                            String customizationName,
                                            Class<?> clazz,
                                            Map<String, Object> partsOfClass) {
        Object currentPart = partsOfClass.get(uniqueName);
        String currentCustomization = Sirius.getCustomizationName(currentPart.getClass().getName());
        int comp = Sirius.compareCustomizations(currentCustomization, customizationName);
        if (comp > 0) {
            // Only overwrite system parts with customizations, not the other way round...
            partsOfClass.put(uniqueName, part);
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
    @SuppressWarnings({"squid:S1854", "squid:S1481"})
    @Explain("False positive - the set is used.")
    void processAnnotations() {
        Set<Object> initializedObjects = new HashSet<>();
        parts.getUnderlyingMap().values().stream().flatMap(Collection::stream).forEach(part -> {
            wire(part);
            if (part instanceof Initializable && !initializedObjects.contains(part)) {
                initializedObjects.add(part);
                initialize(part);
            }
        });
    }

    private void initialize(Object part) {
        try {
            ((Initializable) part).initialize();
        } catch (Exception e) {
            Injector.LOG.WARN("Error initializing %s (%s)", part, part.getClass().getName());
            Injector.LOG.WARN(e);
        }
    }
}
