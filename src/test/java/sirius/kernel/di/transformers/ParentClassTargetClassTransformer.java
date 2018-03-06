/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

@Register
public class ParentClassTargetClassTransformer implements Transformer<ParentClass, TargetClass> {

    @Override
    public Class<ParentClass> getSourceClass() {
        return ParentClass.class;
    }

    @Override
    public Class<TargetClass> getTargetClass() {
        return TargetClass.class;
    }

    @Nullable
    @Override
    public TargetClass make(@Nonnull ParentClass source) {
        return new TargetClass();
    }
}
