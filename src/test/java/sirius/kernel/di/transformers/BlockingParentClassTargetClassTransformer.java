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

@Register
public class BlockingParentClassTargetClassTransformer implements Transformer<BlockingParentClass, TargetClass> {

    @Override
    public boolean supportChildClasses() {
        return false;
    }

    @Override
    public Class<BlockingParentClass> getSourceClass() {
        return BlockingParentClass.class;
    }

    @Override
    public Class<TargetClass> getTargetClass() {
        return TargetClass.class;
    }

    @Nullable
    @Override
    public TargetClass make(@Nonnull BlockingParentClass source) {
        return new TargetClass();
    }
}
