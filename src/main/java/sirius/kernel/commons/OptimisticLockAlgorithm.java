/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.Optional;

/**
 * Provides a pattern or skeleton implementation of an <b>optimistic locking algorithm</b>.
 * <p>
 * Optimistic locking is a bit of a misleading term as it doesn't actually do any locking at all. Rather it
 * has the optimistic approach (hence the name) that no locking is necessarry and performs a critical action
 * (e.g. creating an entity with a unique name). If the verifies that the action was valid (e.g. no other
 * thread created the same entity) and - if almost most cases returns happily.
 * <p>
 * If the test fails however, a cleanup action is executed which undoes the performed actions. Then we
 * for a random period and try again. If we run out of retries, we abort.
 * <p>
 * In order to use an optimistic locking approach, two preconditions must be fulfilled:
 * <ol>
 *     <li>
 *         The "optimistic" assumption must be correct - this means that the case where two threads or even nodes
 *         doing the same task "collide" must be reasonably rare, otherwise the penalty of waiting before the retry
 *         is worse than any pessimistic locking overhead.
 *     </li>
 *     <li>
 *         A cleanup must be possible. If the actions change the global state of the system and cannot easily be undone,
 *         optimistic locking isn't a valid approach. A good counter example would be sending an email - which is pretty
 *         hard to take back.
 *     </li>
 * </ol>
 * <p>
 * If these preconditions are fullfilled, optimistic locking can provide an extremely efficient way
 * of handling critical sections (tasks which might run in parallel) as there is almost no overhead.
 *
 * @param <R> the type of results being produced by the algorithm
 */
public class OptimisticLockAlgorithm<R> {

    private int minWaitMillis = 10;
    private int maxWaitMillis = 250;
    private int numberOfRetries = 5;

    /**
     * Specifies the maximal number of retries bevore giving up.
     *
     * @param numberOfRetries the maximal number of attempts to perform and validate the action
     * @return the helper itself for fluent method calls
     */
    public OptimisticLockAlgorithm<R> withRetries(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
        return this;
    }

    /**
     * Specifies the minimal wait time before retrying.
     * <p>
     * If the algorithm detects a collision, it performs a cleanup and then waits a random
     * period of time, so that two threads or nodes don't constantly collide. This specifies the
     * lower bound of this period.
     *
     * @param minWaitMillis the minimal wait time in milliseconds
     * @return the helper itself for fluent method calls
     */
    public OptimisticLockAlgorithm<R> withMinWaitMillis(int minWaitMillis) {
        this.minWaitMillis = minWaitMillis;
        return this;
    }

    /**
     * Specifies the maximal wait time before retrying.
     * <p>
     * If the algorithm detects a collision, it performs a cleanup and then waits a random
     * period of time, so that two threads or nodes don't constantly collide. This specifies the
     * upper bound of this period.
     *
     * @param maxWaitMillis the maximal wait time in milliseconds
     * @return the helper itself for fluent method calls
     */
    public OptimisticLockAlgorithm<R> withMaxWaitMillis(int maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
        return this;
    }

    /**
     * Performs the locking algorithm using the provided callbacks.
     *
     * @param factory         the producer which performs the action (creates an object etc.)
     * @param correctnessTest the test which ensures that the action was successful without any collision
     * @param cleanup         the callback which undoes the changes of <tt>factory</tt> if a collision was detected by
     *                        <tt>correctnessTest</tt>
     * @return the created object or an empty optional if the algorithm ran out of retries
     * @throws Exception in case any of the provided callbacks threw an exception
     */
    public Optional<R> tryToCreate(Producer<R> factory, Processor<R, Boolean> correctnessTest, Callback<R> cleanup)
            throws Exception {

        int retries = numberOfRetries;
        while (retries-- > 0) {
            R result = factory.create();
            if (correctnessTest.apply(result)) {
                return Optional.of(result);
            } else {
                cleanup.invoke(result);
                Wait.randomMillis(minWaitMillis, maxWaitMillis);
            }
        }

        return Optional.empty();
    }
}
