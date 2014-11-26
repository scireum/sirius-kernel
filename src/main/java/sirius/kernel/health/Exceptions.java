/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import com.google.common.collect.Maps;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.nls.NLS;

import java.util.Map;

/**
 * Central point for handling all system errors and exceptions.
 * <p>
 * Provides various methods to handle errors and exceptions. Each method returns a {@link HandledException} which
 * signals the developer that no further action is required (error is logged and reacted upon). Also, those
 * exceptions always contain a translated error message which can be directly shown to the user
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Exceptions {

    /**
     * Used as a fallback logger, if no logger was provided. Logs everything as "errors".
     */
    protected static final Log LOG = Log.get("errors");

    /**
     * Used to log exceptions which are normally just discarded. This are either exception handled with
     * {@link #ignore(Throwable)} or exceptions created by {@link #createHandled()}
     */
    protected static final Log IGNORED_EXCEPTIONS_LOG = Log.get("ignored");

    /*
     * Filled by the Injector - contains all handles which participate in the exception handling process
     */
    @Parts(ExceptionHandler.class)
    private static PartCollection<ExceptionHandler> handlers;

    /*
     * Used to cut endless loops while handling errors
     */
    private static ThreadLocal<Boolean> frozen = new ThreadLocal<Boolean>();

    /*
     * Adds the exception message and the exception class to the given params array. Handles null values for
     * e gracefully
     */
    private static Object[] extendParams(Throwable e, Object[] params) {
        Object[] newParams = null;
        if (params == null) {
            newParams = new Object[2];
        } else {
            newParams = new Object[params.length + 2];
            System.arraycopy(params, 0, newParams, 0, params.length);
        }
        if (e != null) {
            newParams[newParams.length - 2] = e.getMessage();
            newParams[newParams.length - 1] = e.getClass().getName();
        } else {
            newParams[newParams.length - 2] = NLS.get("HandledException.unknownError");
            newParams[newParams.length - 1] = "UnknownError";
        }
        return newParams;
    }

    /**
     * Fluent API to create a <tt>HandledException</tt> based on given parameters
     * <p>
     * The intention is to use a call like:
     * <pre>
     * <code>
     *    Exceptions.handler()
     *      .error(anException)     // Sets the exception to handle
     *      .to(aLogger)            // Sets the logger to use for logging
     *      .withNLSKey("nls.key")  // Sets the i18n key to create the error message
     *      .set("param",value)     // Sets a named parameter which occurs in the message
     *      .handle();              // logs an creates the HandledException
     * </code>
     * </pre>
     * <p>
     * Since none of the methods must be called (except <tt>handle()</tt> of course), this provides a lot of
     * flexibility and permits to handle several different error situations without having methods with long
     * parameter lists and lots of null values.
     * <p>
     * The {@link #set(String, Object)} method can be called several times to set different parameters. The reason
     * why named parameters are used is because the resulting messages in the .properties files are easier to
     * translate and also the order of the parameters can be different in different languages.
     *
     * @author Andreas Haufler (aha@scireum.de)
     * @since 2013/08
     */
    public static class ErrorHandler {
        private Log log = LOG;
        private Throwable ex;
        private String systemErrorMessage;
        private Object[] systemErrorMessageParams;
        private boolean processError = true;
        private String key = "HandledException.exception";
        private Map<String, Object> params = Maps.newTreeMap();

        /**
         * Use {@link Exceptions#handle()} to create an <tt>ErrorHandler</tt>
         *
         * @param processError determines if the error should be processed
         *                     (logged and sent to all {@link ExceptionHandler}) or if just a
         *                     {@link sirius.kernel.health.HandledException} is to be created
         */
        protected ErrorHandler(boolean processError) {
            super();
            this.processError = processError;
        }

        /**
         * Specifies which exception leaded to the error being handled.
         *
         * @param e the exception which needs to be attached to this error handler
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler error(Throwable e) {
            this.ex = e;
            return this;
        }

        /**
         * Specifies the logger which is used to log the generated exception.
         *
         * @param log the logger used to log the generated <tt>HandledException</tt>
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler to(Log log) {
            this.log = log;
            return this;
        }

        /**
         * Specifies the i18n key which is passed to {@link NLS#fmtr(String)} to create the internal formatter
         * used to generate the translated error message.
         * <p>
         * This message may contain two parameters which don't need to be passed in: <tt>errorMessage</tt>
         * and <tt>errorClass</tt> which contain the message of the exception being handled
         * as well as the type name of it.
         *
         * @param key the translation key used to fetch the translated error message
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler withNLSKey(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets an untranslated error message, used by rare system errors.
         * <p>
         * Still a translated message will be created, which notifies the user about the system error and provides
         * the untranslated error message, generated by this method. These messages should be in english.
         *
         * @param englishMessagePattern contains a pattern used to generate the error message. May contain
         *                              placeholders as understood by {@link Strings#apply(String, Object...)}.
         * @param params                parameters used to format the resulting error message based on the given pattern
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler withSystemErrorMessage(String englishMessagePattern, Object... params) {
            this.systemErrorMessage = englishMessagePattern;
            this.systemErrorMessageParams = params;
            return this;
        }

        /**
         * Specifies a parameter which is replaced in the generated error message.
         *
         * @param parameter the name of the parameter which should be replaced. This must occur as
         *                  <code>${parameter}</code> in the translated message to be replaced
         * @param value     the value to be used as replacement for the parameter. The given value will be converted
         *                  to a string using {@link NLS#toUserString(Object)}
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler set(String parameter, Object value) {
            this.params.put(parameter, value);
            return this;
        }

        /**
         * Generates and logs the resulting <tt>HandledException</tt>.
         * <p>
         * The generated exception can be either thrown (it subclasses RuntimeException and therefore
         * needs no throws clause). Alternatively it may be passed along or even be just discarded.
         *
         * @return a <tt>HandledException</tt> which notifies surrounding calls that an error occurred, which has
         * already been taken care of.
         */
        public HandledException handle() {
            if (ex != null && ex instanceof HandledException) {
                return (HandledException) ex;
            }
            try {
                String message = null;
                if (Strings.isEmpty(systemErrorMessage)) {
                    // Add exception infos
                    set("errorMessage", ex == null ? NLS.get("HandledException.unknownError") : ex.getMessage());
                    set("errorClass", ex == null ? "UnknownError" : ex.getClass().getName());
                    // Format resulting error message
                    message = NLS.fmtr(key).set(params).format();
                } else {
                    // Generate system error message and prefix with translated info about the system error
                    message = NLS.apply("HandledException.systemError",
                            Strings.apply(systemErrorMessage, extendParams(ex, systemErrorMessageParams)));
                }
                HandledException result = new HandledException(message, ex);
                if (processError) {
                    log.SEVERE(result);

                    // Injector might not have run yet
                    if (handlers != null && !Boolean.TRUE.equals(frozen.get())) {
                        try {
                            frozen.set(Boolean.TRUE);
                            String location = null;
                            if (ex != null && ex.getStackTrace().length > 0) {
                                location = formatStackTraceElement(ex.getStackTrace()[0]);
                            } else if (result.getStackTrace().length > 0) {
                                StackTraceElement[] trace = Thread.currentThread().getStackTrace();
                                int index = 1;
                                while ((location == null || location.startsWith("sirius.kernel.health.Exceptions")) && index < trace.length) {
                                    location = formatStackTraceElement(trace[index]);
                                    index++;
                                }
                            }
                            for (ExceptionHandler handler : handlers) {
                                try {
                                    handler.handle(new Incident(log.getName(),
                                            location,
                                            CallContext.getCurrent().getMDC(),
                                            result));
                                } catch (Throwable e) {
                                    // Just log the exception - anything else might call a rather long infinite loop
                                    LOG.SEVERE(new Exception(Strings.apply(
                                            "An error occurred while calling the ExceptionHandler: %s - %s (%s)",
                                            handler,
                                            e.getMessage(),
                                            e.getClass().getName()), e));
                                }
                            }
                        } finally {
                            frozen.set(Boolean.FALSE);
                        }
                    }
                } else {
                    IGNORED_EXCEPTIONS_LOG.INFO(result);
                }
                return result;
            } catch (Throwable t) {
                // We call as few external methods a possible here, since things are really messed up right now
                t.printStackTrace();
                return new HandledException("Kernel Panic: Exception-Handling threw another exception: " + t.getMessage() + " (" + t
                        .getClass()
                        .getName() + ")", t);
            }
        }

        /*
         * Formats a given StackTraceElement as [class].[method] ([file]:[line])
         */
        private static String formatStackTraceElement(StackTraceElement element) {
            if (element == null) {
                return null;
            }
            return element.getClassName() + "." + element.getMethodName() + " (" + element.getFileName() + ":" + element
                    .getLineNumber() + ")";
        }

        @Override
        public String toString() {
            return "ErrorHandler{" +
                    "params=" + params +
                    ", key='" + key + '\'' +
                    ", systemErrorMessage='" + systemErrorMessage + '\'' +
                    ", ex=" + ex +
                    '}';
        }
    }

    /**
     * Generates a new {@link ErrorHandler} which gracefully handles all kinds of errors
     *
     * @return a new <tt>ErrorHandler</tt> to handle an error or exception
     */
    public static ErrorHandler handle() {
        return new ErrorHandler(true);
    }

    /**
     * Generates a new {@link ErrorHandler} which creates a <tt>HandledException</tt> without actually logging or
     * processing it.
     * <p>
     * This can be used to generate a <tt>HandledException</tt> based on a user error (invalid input)
     * which doesn't need to be logged.
     *
     * @return a new <tt>ErrorHandler</tt> to handle an error or exception
     */
    public static ErrorHandler createHandled() {
        return new ErrorHandler(false);
    }

    /**
     * Boilerplate method the directly handle the given exception without a special message or logger
     *
     * @param e the exception to handle
     * @return a <tt>HandledException</tt> which notifies surrounding calls that an error occurred, which has
     * already been taken care of.
     */
    public static HandledException handle(Throwable e) {
        return handle().error(e).handle();
    }

    /**
     * Boilerplate method the directly handle the given exception without a special message
     *
     * @param log the logger used to log the exception
     * @param e   the exception to handle
     * @return a <tt>HandledException</tt> which notifies surrounding calls that an error occurred, which has
     * already been taken care of.
     */
    public static HandledException handle(Log log, Throwable e) {
        return handle().error(e).to(log).handle();
    }

    /**
     * Can be used to mark an exception as ignored.
     * <p>
     * Instead of leading a try / catch block empty, the method can be invoked. Therefore it is known, that the
     * exception is wanted to be ignored. Additionally, the <tt>ignoredExceptions</tt> logger can be turned on,
     * to still see those exceptions.
     *
     * @param t the exception to be ignored. This exception will be discarded unless the <tt>ignoredExceptions</tt>
     *          logger is set to INFO.
     */
    public static void ignore(Throwable t) {
        IGNORED_EXCEPTIONS_LOG.INFO(t);
    }

}
