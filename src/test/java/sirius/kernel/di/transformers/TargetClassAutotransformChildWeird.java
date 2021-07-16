/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

@AutoTransform(source = ParentClass.class,
        targets = TargetClassAutotransform.class,
        target = TargetClassAutotransformChildWeird.class)
public class TargetClassAutotransformChildWeird extends TargetClassAutotransform {

    public TargetClassAutotransformChildWeird(ParentClass parent) {
        parent.attach(TargetClassAutotransform.class, this);
        parent.attach(TargetClassAutotransformChildWeird.class, this);
    }
}
