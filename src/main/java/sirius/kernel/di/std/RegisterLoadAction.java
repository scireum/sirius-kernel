/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import com.google.common.collect.Sets;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.ClassLoadAction;
import sirius.kernel.di.Injector;
import sirius.kernel.di.MutableGlobalContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the {@link Register} annotation.
 *
 * @see ClassLoadAction
 * @see Register
 */
public class RegisterLoadAction implements ClassLoadAction {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    @Override
    public Class<? extends Annotation> getTrigger() {
        return Register.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Class<?> clazz) throws Exception {
        Register registerAnnotation = clazz.getAnnotation(Register.class);
        if (!Sirius.isFrameworkEnabled(registerAnnotation.framework())) {
            return;
        }

        // Warn (but still handle) if an additional @Framework annotation is present...
        if (clazz.isAnnotationPresent(Framework.class)) {
            Injector.LOG.WARN(
                    "%s uses @Register and @Framework. Delete the @Framework annotation and use the 'framework'"
                    + " parameter of the @Register annotation.",
                    clazz.getName());
            if (!Sirius.isFrameworkEnabled(clazz.getAnnotation(Framework.class).value())) {
                return;
            }
        }

        Set<Class<?>> registeredClasses = computeEffectiveClasses(clazz, registerAnnotation);
        if (registeredClasses == null) {
            return;
        }

        Object part = clazz.getDeclaredConstructor().newInstance();

        String name = computeEffectiveName(clazz, registerAnnotation, part);
        if (Strings.isFilled(name)) {
            ctx.registerPart(name, part, registeredClasses.toArray(EMPTY_CLASS_ARRAY));
        } else {
            ctx.registerPart(part, registeredClasses.toArray(EMPTY_CLASS_ARRAY));
        }
    }

    private Set<Class<?>> computeEffectiveClasses(Class<?> clazz, Register registerAnnotation) {
        Set<Class<?>> registeredClasses = Sets.newHashSet(registerAnnotation.classes());
        Set<Class<?>> detectedClasses = findAutoRegisterClasses(clazz);

        // Provide support for the legacy mechanism, which was "all available interfaces"...
        if (detectedClasses.isEmpty()) {
            detectedClasses = new HashSet<>(Arrays.asList(clazz.getInterfaces()));
            if (!detectedClasses.isEmpty()) {
                Injector.LOG.FINE("Using the fallback method to determine which %s will be registered for: %s",
                                  clazz.getName());
            }
        }

        // Warn if the classes list can (and should) be omitted...
        if (registeredClasses.equals(detectedClasses)) {
            Injector.LOG.WARN(
                    "%s wears a @Register with a list of classes which is also auto-redected. Consider removing the classes list...",
                    clazz.getName());
        } else {
            String nonAutoClasses = registeredClasses.stream()
                                                     .filter(iface -> !isAutoRegistered(iface))
                                                     .map(Class::getName)
                                                     .collect(Collectors.joining(", "));
            if (Strings.isFilled(nonAutoClasses)) {
                Injector.LOG.FINE("%s registers for non AutoRegister classes: %s", clazz.getName(), nonAutoClasses);
            }
        }

        // Uses the detected classes instead of the registered one, if none were given (this is the common case)...
        if (registeredClasses.isEmpty()) {
            registeredClasses = detectedClasses;
        }

        // If none of the methods above yield any class or interface to register for, emit a warning...
        if (registeredClasses.isEmpty()) {
            Injector.LOG.WARN(
                    "%s wears a @Register annotation but neither implements an interface nor lists which classes to "
                    + "register for...",
                    clazz.getName());
        }

        return registeredClasses;
    }

    private Set<Class<?>> findAutoRegisterClasses(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        collectAutoRegisteredClasses(clazz, result);

        return result;
    }

    private void collectAutoRegisteredClasses(Class<?> clazz, Set<Class<?>> result) {
        if (isAutoRegistered(clazz)) {
            result.add(clazz);
        }

        Arrays.stream(clazz.getInterfaces()).filter(this::isAutoRegistered).forEach(result::add);
        if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass())) {
            collectAutoRegisteredClasses(clazz.getSuperclass(), result);
        }
    }

    private boolean isAutoRegistered(Class<?> clazz) {
        return clazz.isAnnotationPresent(AutoRegister.class);
    }

    private String computeEffectiveName(Class<?> clazz,
                                        Register registerAnnotation,
                                        Object part) {
        String name = registerAnnotation.name();
        if (part instanceof Named) {
            if (Strings.isFilled(name)) {
                Injector.LOG.WARN(
                        "%s implements Named and still provides a name in the @Register annotation. Using value "
                        + "provided by Named.getName()...",
                        clazz.getName());
            }
            name = ((Named) part).getName();
            if (Strings.isEmpty(name)) {
                Injector.LOG.WARN("%s implements Named but Named.getName() returned an empty string...",
                                  clazz.getName());
            }
        }

        return name;
    }

}
