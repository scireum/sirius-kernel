/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.ClassLoadAction;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;

/**
 * Handles the {@link Register} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see ClassLoadAction
 * @see Register
 * @since 2013/08
 */
public class AutoRegisterAction implements ClassLoadAction {

    @Override
    public Class<? extends Annotation> getTrigger() {
        return Register.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Class<?> clazz) throws Exception {
        Object part = clazz.newInstance();
        Register r = clazz.getAnnotation(Register.class);
        if (!Sirius.isFrameworkEnabled(r.framework())) {
            return;
        }
        Class<?>[] classes = r.classes();
        if (classes.length == 0) {
            classes = clazz.getInterfaces();
        }
        if (Strings.isFilled(r.name())) {
            ctx.registerPart(r.name(), part, classes);
        } else {
            ctx.registerPart(part, classes);
        }
    }

}
