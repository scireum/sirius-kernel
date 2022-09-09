/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.async.Operation;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * A robust wrapper around calls to external programs.
 */
public class Exec {

    /**
     * Can be used to log errors and infos when executing external programs.
     */
    public static final Log LOG = Log.get("exec");
    private static final String[] EMPTY_ARRAY = new String[0];

    private Exec() {
    }

    /*
     * Reads the given stream in a separate thread.
     */
    private static class StreamEater implements Runnable {

        private final InputStream stream;
        private final StringBuilder logger;
        private final ValueHolder<IOException> exHolder = new ValueHolder<>(null);
        private final Semaphore completionSynchronizer;

        StreamEater(InputStream stream, StringBuilder log, Semaphore completionSynchronizer)
                throws InterruptedException {
            this.stream = stream;
            this.logger = log;
            this.completionSynchronizer = completionSynchronizer;
            this.completionSynchronizer.acquire();
        }

        @Override
        public void run() {
            try (InputStreamReader isr = new InputStreamReader(stream); BufferedReader br = new BufferedReader(isr)) {
                Thread.currentThread()
                      .setName(StreamEater.class.getSimpleName() + "-" + Thread.currentThread().getId());
                String line = br.readLine();
                while (line != null) {
                    logger.append(line);
                    logger.append("\n");
                    line = br.readLine();
                }
            } catch (IOException e) {
                logger.append(NLS.toUserString(e));
                exHolder.set(e);
            } finally {
                this.completionSynchronizer.release();
            }
        }

        /**
         * Creates a new stream eater logging to the given buffer for the given stream.
         *
         * @param stream                 the stream to read
         * @param logger                 the target for all characters read
         * @param completionSynchronizer a semaphore where a permit is acquired and released once all output hase been processed
         * @return a new stream eater which is already running in a separate thread
         */
        static StreamEater eat(InputStream stream, StringBuilder logger, Semaphore completionSynchronizer)
                throws InterruptedException {
            StreamEater eater = new StreamEater(stream, logger, completionSynchronizer);
            new Thread(eater).start();
            return eater;
        }
    }

    /**
     * Thrown if a call to an external program fails
     */
    public static class ExecException extends Exception {

        @Serial
        private static final long serialVersionUID = -4736872491172480346L;
        private final String log;

        ExecException(Throwable root, String log) {
            super(root);
            this.log = log;
        }

        /**
         * Provides access to the contents of the programs stdout and stderr.
         *
         * @return a transcript of the called programs stderr and stdout
         */
        public String getLog() {
            return log;
        }
    }

    /**
     * Executes the given command and returns a transcript of stderr and stdout
     *
     * @param command the command to execute
     * @return the transcript of stderr and stdout produced by the executed command
     * @throws ExecException in case the external program fails or returns an exit code other than 0.
     */
    public static String exec(String command) throws ExecException {
        return exec(command, false);
    }

    /**
     * Executes the given command with and returns a transcript of stderr and stdout.
     * <p>
     * This is using a default operation timeout of five minutes - after which it is logged as hanging.
     *
     * @param command         the command to execute
     * @param ignoreExitCodes if an exit code other than 0 should result in an exception being thrown
     * @return the transcript of stderr and stdout produced by the executed command
     * @throws ExecException in case the external program fails
     */
    public static String exec(String command, boolean ignoreExitCodes) throws ExecException {
        return exec(command, ignoreExitCodes, Duration.ofMinutes(5));
    }

    /**
     * Executes the given command and returns a transcript of stderr and stdout.
     *
     * @param command         the command to execute
     * @param ignoreExitCodes if an exit code other than 0 should result in an exception being thrown
     * @param opTimeout       the duration after which the execution should be logged as hanging
     * @return the transcript of stderr and stdout produced by the executed command
     * @throws ExecException in case the external program fails
     */
    public static String exec(String command, boolean ignoreExitCodes, Duration opTimeout) throws ExecException {
        return exec(command, ignoreExitCodes, opTimeout, null);
    }

    /**
     * Executes the given command and returns a transcript of stderr and stdout.
     *
     * @param command         the command to execute
     * @param ignoreExitCodes if an exit code other than 0 should result in an exception being thrown
     * @param opTimeout       the duration after which the execution should be logged as hanging
     * @param directory       the working directory of the subprocess,
     *                        or <tt>null</tt> if the subprocess should inherit the working directory of the current process.
     * @return the transcript of stderr and stdout produced by the executed command
     * @throws ExecException in case the external program fails
     */
    public static String exec(String command, boolean ignoreExitCodes, Duration opTimeout, @Nullable File directory)
            throws ExecException {
        StringBuilder logger = new StringBuilder();
        try (Operation op = new Operation(() -> command, opTimeout)) {
            Process p = new ProcessBuilder().command(parseCommandToArray(command))
                                            .directory(directory)
                                            .redirectErrorStream(true)
                                            .start();
            Semaphore completionSynchronizer = new Semaphore(1);
            StreamEater outEater = StreamEater.eat(p.getInputStream(), logger, completionSynchronizer);
            doExec(ignoreExitCodes, logger, p);

            // Wait for the stream eaters to complete...
            completionSynchronizer.acquire(1);

            if (outEater.exHolder.get() != null) {
                throw new ExecException(outEater.exHolder.get(), logger.toString());
            }
            return logger.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecException(e, logger.toString());
        } catch (Exception e) {
            throw new ExecException(e, logger.toString());
        }
    }

    private static String[] parseCommandToArray(String command) {
        List<String> commandList = new ArrayList<>();
        CommandParser commandParser = new CommandParser(command);
        commandList.add(commandParser.parseCommand());
        commandList.addAll(commandParser.getArgs());
        return commandList.toArray(EMPTY_ARRAY);
    }

    private static void doExec(boolean ignoreExitCodes, StringBuilder logger, Process p) throws ExecException {
        try {
            int code = p.waitFor();
            if (code != 0 && !ignoreExitCodes) {
                Exception root = new Exception("Command returned with exit code " + code);
                throw new ExecException(root, logger.toString());
            }
        } catch (InterruptedException e) {
            Exceptions.ignore(e);
            Thread.currentThread().interrupt();
        }
    }
}
