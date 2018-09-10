/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

@Register(classes = BackgroundLoop.class)
public class FastTestLoop extends BackgroundLoop {

    public static AtomicInteger counter = new AtomicInteger();

    @Override
    protected double maxCallFrequency() {
        return 1;
    }

    @Nonnull
    @Override
    public String getName() {
        return "TestLoop";
    }

    @Override
    protected String doWork() throws Exception {
        counter.incrementAndGet();
        return null;
    }
}
