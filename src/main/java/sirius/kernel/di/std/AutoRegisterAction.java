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

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Handles the {@link Register} annotation.
 *
 * @see ClassLoadAction
 * @see Register
 */
public class AutoRegisterAction implements ClassLoadAction {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    @Override
    public Class<? extends Annotation> getTrigger() {
        return Register.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Class<?> clazz) throws Exception {
        Register r = clazz.getAnnotation(Register.class);
        if (!Sirius.isFrameworkEnabled(r.framework())) {
            return;
        }

        Set<Class<?>> classes = Sets.newHashSet(r.classes());
        if (classes.isEmpty()) {
            classes = Sets.newHashSet(clazz.getInterfaces());
        }
        if (classes.isEmpty()) {
            Injector.LOG.WARN(
                    "%s wears a @Register annotation but neither implements an interface nor lists which classes to "
                    + "register for...",
                    clazz.getName());
        }

        Object part = clazz.getDeclaredConstructor().newInstance();
        String name = r.name();
        if (part instanceof Named) {
            classes.remove(Named.class);
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
        if (Strings.isFilled(name)) {
            ctx.registerPart(name, part, classes.toArray(EMPTY_CLASS_ARRAY));
        } else {
            ctx.registerPart(part, classes.toArray(EMPTY_CLASS_ARRAY));
        }
    }
}
