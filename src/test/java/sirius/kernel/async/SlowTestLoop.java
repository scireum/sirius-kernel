/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.Wait;
import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

@Register
public class SlowTestLoop extends BackgroundLoop {

    public static AtomicInteger counter = new AtomicInteger();

    @Override
    public double maxCallFrequency() {
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
        Wait.seconds(2);
        return null;
    }
}
