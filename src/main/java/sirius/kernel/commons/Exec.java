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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;

/**
 * A robust wrapper around calls to external programs.
 */
public class Exec {

    /**
     * Can be used to log errors and infos when executing external programs.
     */
    public static final Log LOG = Log.get("exec");

    private Exec() {
    }

    /*
     * Reads the given stream in a separate thread.
     */
    private static class StreamEater implements Runnable {

        private final InputStream stream;
        private final StringBuffer logger;
        private final ValueHolder<IOException> exHolder = new ValueHolder<>(null);

        StreamEater(InputStream stream, StringBuffer log) {
            this.stream = stream;
            logger = log;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread()
                      .setName(StreamEater.class.getSimpleName() + "-" + Thread.currentThread().getId());
                InputStreamReader isr = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();
                while (line != null) {
                    logger.append(line);
                    logger.append("\n");
                    line = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                logger.append(NLS.toUserString(e));
                exHolder.set(e);
            }
        }

        /**
         * Creates a new stream eater logging to the given buffer for the given stream.
         *
         * @param stream the stream to read
         * @param logger the target for all characters read
         * @return a new stream eater which is already running in a separate thread
         */
        static StreamEater eat(InputStream stream, StringBuffer logger) {
            StreamEater eater = new StreamEater(stream, logger);
            new Thread(eater).start();
            return eater;
        }
    }

    /**
     * Thrown if a call to an external program fails
     */
    public static class ExecException extends Exception {

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
     * Executes the given command and returns a transcript of stderr and stdout.
     *
     * @param command         the command to execute
     * @param ignoreExitCodes if an exit code other than 0 should result in an exception being thrown
     * @return the transcript of stderr and stdout produced by the executed command
     * @throws ExecException in case the external program fails
     */
    public static String exec(String command, boolean ignoreExitCodes) throws ExecException {
        StringBuffer logger = new StringBuffer();
        try(Operation op = new Operation(() -> command, Duration.ofMinutes(5))) {
            Process p = Runtime.getRuntime().exec(command);
            StreamEater errEater = StreamEater.eat(p.getErrorStream(), logger);
            StreamEater outEater = StreamEater.eat(p.getInputStream(), logger);
            doExec(ignoreExitCodes, logger, p);
            if (errEater.exHolder.get() != null) {
                throw new ExecException(errEater.exHolder.get(), logger.toString());
            }
            if (outEater.exHolder.get() != null) {
                throw new ExecException(outEater.exHolder.get(), logger.toString());
            }
            return logger.toString();
        } catch (Exception e) {
            throw new ExecException(e, logger.toString());
        }
    }

    public static void doExec(boolean ignoreExitCodes, StringBuffer logger, Process p) throws ExecException {
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
