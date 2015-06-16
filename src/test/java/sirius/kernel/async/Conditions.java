/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.health.Exceptions;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helps to test multi-threaded code.
 * <p>
 * Threads can wait for a condition to become <tt>true</tt> by calling {@link #await(Class, String, Duration)}.
 * Other threads can trigger the condition by calling {@link #trigger(Class, String)}. Once a condition was
 * triggered it remains so - therefore calling {@code await} again will immediatelly return.
 */
public class Conditions {

    private static class Mark {
        boolean reached;
        Condition c = lock.newCondition();
    }

    private static Lock lock = new ReentrantLock();
    private static Map<String, Mark> conditions = new ConcurrentHashMap<>();

    /**
     * Waits for the given condition to become triggered.
     * <p>
     * As this is intended to be used in test classes, the {@code namespace} parameter can be used to guarantee
     * that contidion names are unique accross tests.
     * <p>
     * The method will block at most for 10 seconds. If the condition is not triggered in this period, an exception
     * will
     * be thrown.
     *
     * @param namespace the name of the (test) class specifying the condition. Used to guarantee that condition names
     *                  are unique.
     * @param condition the name of the condition to wait for.
     */
    public static void await(Class<?> namespace, String condition) {
        await(namespace, condition, Duration.ofSeconds(10));
    }

    /**
     * Waits for the given condition to become triggered.
     * <p>
     * As this is intended to be used in test classes, the {@code namespace} parameter can be used to guarantee
     * that contidion names are unique accross tests.
     *
     * @param namespace the name of the (test) class specifying the condition. Used to guarantee that condition names
     *                  are unique.
     * @param condition the name of the condition to wait for.
     * @param timeout   the timeout limiting how long the methods blocks.
     */
    public static void await(Class<?> namespace, String condition, Duration timeout) {
        Mark m = conditions.computeIfAbsent(namespace.getName() + "-" + condition, k -> new Mark());
        if (m.reached) {
            return;
        }
        lock.lock();
        try {
            m.c.await(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Exceptions.ignore(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Triggers the given condition.
     * <p>
     * Note that a condition once triggered, remains so. Therefore all further calls to {@code await} for this
     * condition will return immediatelly.
     *
     * @param namespace the name of the (test) class specifying the condition. Used to guarantee that condition names
     *                  are unique.
     * @param condition the name of the condition to trigger.
     */
    public static void trigger(Class<?> namespace, String condition) {
        Mark m = conditions.computeIfAbsent(namespace.getName() + "-" + condition, k -> new Mark());
        m.reached = true;
        lock.lock();
        try {
            m.c.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
