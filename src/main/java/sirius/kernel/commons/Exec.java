package sirius.kernel.commons;

import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A robust wrapper around calls to external programs.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Exec {

    /**
     * Can be used to log errors and infos when executing external programs.
     */
    public static Log LOG = Log.get("exec");

    /*
     * Reads the given stream in a separate thread.
     */
    private static class StreamEater implements Runnable {

        private final InputStream stream;
        private final StringBuffer logger;
        private final ValueHolder<IOException> exHolder = new ValueHolder<IOException>(null);

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
        private String log;

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
     * Executes the given command and returns a transcript of stderr and stdout.
     *
     * @param command the command to execute
     * @return the transcript of stderr and stdout produced by the executed command
     * @throws ExecException in case the external program fails
     */
    public static String exec(String command) throws ExecException {
        StringBuffer logger = new StringBuffer();
        try {
            Process p = Runtime.getRuntime().exec(command);
            StreamEater errEater = StreamEater.eat(p.getErrorStream(), logger);
            StreamEater outEater = StreamEater.eat(p.getInputStream(), logger);
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                throw new ExecException(e, logger.toString());
            }
            if (errEater.exHolder.get() != null) {
                throw new ExecException(errEater.exHolder.get(), logger.toString());
            }
            if (outEater.exHolder.get() != null) {
                throw new ExecException(outEater.exHolder.get(), logger.toString());
            }
            return logger.toString();
        } catch (Throwable e) {
            throw new ExecException(e, logger.toString());
        }
    }
}
