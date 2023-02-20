/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Central point for handling all system errors and exceptions.
 * <p>
 * Provides various methods to handle errors and exceptions. Each method returns a {@link HandledException} which
 * signals the developer that no further action is required (error is logged and reacted upon). Also, those
 * exceptions always contain a translated error message which can be directly shown to the user
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

    /**
     * Used to log warnings if deprecated APIs are called.
     *
     * @see #logDeprecatedMethodUse()
     */
    protected static final Log DEPRECATION_LOG = Log.get("deprecated");

    private static final String PARAM_RAW_MESSAGE = "message";
    private static final String MESSAGE_MODE_RAW = "_raw";

    /*
     * Filled by the Injector - contains all handles which participate in the exception handling process
     */
    @Parts(ExceptionHandler.class)
    private static PartCollection<ExceptionHandler> handlers;

    /*
     * Used to cut endless loops while handling errors
     */
    private static final ThreadLocal<Boolean> frozen = new ThreadLocal<>();

    private Exceptions() {
    }

    /**
     * Fluent API to create a <tt>HandledException</tt> based on given parameters
     * <p>
     * The intention is to use a call like:
     * <pre>
     * {@code
     *    Exceptions.handler()
     *      .error(anException)     // Sets the exception to handle
     *      .to(aLogger)            // Sets the logger to use for logging
     *      .withNLSKey("nls.key")  // Sets the i18n key to create the error message
     *      .set("param",value)     // Sets a named parameter which occurs in the message
     *      .handle();              // logs an creates the HandledException
     * }
     * </pre>
     * <p>
     * Since none of the methods must be called (except <tt>handle()</tt> of course), this provides a lot of
     * flexibility and permits to handle several different error situations without having methods with long
     * parameter lists and lots of null values.
     * <p>
     * The {@link #set(String, Object)} method can be called several times to set different parameters. The reason
     * why named parameters are used is because the resulting messages in the .properties files are easier to
     * translate and also the order of the parameters can be different in different languages.
     */
    public static class ErrorHandler {
        private Log log = LOG;
        private Throwable ex;
        private String systemErrorMessage;
        private Object[] systemErrorMessageParams;
        private boolean processError = true;
        private String key = "HandledException.exception";
        private final Map<String, Object> params = new TreeMap<>();
        private final Map<ExceptionHint, Object> hints = new HashMap<>();

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
         * <p>
         * Note: When {@link ErrorHandler#withSystemErrorMessage(String, Object...)} is used, two extra
         * parameters are available for the string template. The first is the exception message and the second
         * is the exception class. These will be appended after all the user-given parameters. Therefore,
         * one can add <tt>%s (%s)</tt> to the message provided here to output the actual error and
         * exception type if needed.
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
         * Directly specifies the message for the exception.
         * <p>
         * This will neither perform any NLS lookups nor append or prepend any text to the given message.
         *
         * @param message the message to use for the generated exception
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler withDirectMessage(String message) {
            return withNLSKey(MESSAGE_MODE_RAW).set(PARAM_RAW_MESSAGE, message);
        }

        /**
         * Sets an untranslated error message, used by rare system errors.
         * <p>
         * Still a translated message will be created, which notifies the user about the system error and provides
         * the untranslated error message, generated by this method. These messages should be in english.
         * <p>
         * Note: When combining with {@link ErrorHandler#error(Throwable)}, two extra
         * parameters are available for the string template. The first is the exception message and the second
         * is the exception class. These will be appended after all the user-given parameters. Therefore,
         * one can add <tt>%s (%s)</tt> to the message provided here to output the actual error and
         * exception type if needed.
         *
         * @param englishMessagePattern contains a pattern used to generate the error message. May contain
         *                              placeholders as understood by {@link Strings#apply(String, Object...)}.
         * @param params                parameters used to format the resulting error message based on the given
         *                              pattern
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler withSystemErrorMessage(String englishMessagePattern, Object... params) {
            this.systemErrorMessage = englishMessagePattern;
            this.systemErrorMessageParams = params == null ? null : params.clone();
            return this;
        }

        /**
         * Specifies a parameter which is replaced in the generated error message.
         *
         * @param parameter the name of the parameter which should be replaced. This must occur as
         *                  {@code ${parameter}} in the translated message to be replaced
         * @param value     the value to be used as replacement for the parameter. The given value will be converted
         *                  to a string using {@link NLS#toUserString(Object)}
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler set(String parameter, Object value) {
            this.params.put(parameter, value);
            return this;
        }

        /**
         * Adds a hint which can later be retrieved from the generated {@link HandledException}.
         *
         * @param hint  the name of the hint
         * @param value the value to store
         * @return <tt>this</tt> in order to fluently call more methods on this handler
         */
        public ErrorHandler hint(ExceptionHint hint, Object value) {
            this.hints.put(hint, value);
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
        @SuppressWarnings({"squid:S1148", "CallToPrintStackTrace"})
        @Explain("This log statement is our last resort when we're in deep trouble.")
        public HandledException handle() {
            if (ex instanceof HandledException handledException) {
                return handledException;
            }

            if (Exceptions.getRootCause(ex) instanceof HandledException) {
                processError = false;
                LOG.FINE("Did not process the exception %s because its root (%s) was already handled",
                         ex.getMessage(),
                         Exceptions.getRootCause(ex).getMessage());
            }

            try {
                String message = computeMessage();
                HandledException result = new HandledException(message, hints, ex);
                if (processError) {
                    log.SEVERE(result);
                    notifyHandlers(result);
                } else {
                    IGNORED_EXCEPTIONS_LOG.INFO(result);
                }
                return result;
            } catch (Exception t) {
                // We call as few external methods a possible here, since things are really messed up right now
                t.printStackTrace();
                return new HandledException("Kernel Panic: Exception-Handling threw another exception: "
                                            + t.getMessage()
                                            + " ("
                                            + t.getClass().getName()
                                            + ")", Collections.emptyMap(), t);
            }
        }

        private String computeMessage() {
            if (Strings.isFilled(systemErrorMessage)) {
                // Generate system error message and prefix with translated info about the system error
                return NLS.fmtr("HandledException.systemError")
                          .set("error", Strings.apply(systemErrorMessage, extendParams(ex, systemErrorMessageParams)))
                          .format();
            } else if (MESSAGE_MODE_RAW.equals(key) && params.containsKey(PARAM_RAW_MESSAGE)) {
                return String.valueOf(params.get(PARAM_RAW_MESSAGE));
            } else {
                // Add exception infos
                set("errorMessage", ex == null ? NLS.get("HandledException.unknownError") : ex.getMessage());
                set("errorClass", ex == null ? "UnknownError" : ex.getClass().getName());
                // Format resulting error message
                return NLS.fmtr(key).set(params).format();
            }
        }

        private void notifyHandlers(HandledException result) {
            // Injector might not have run yet
            if (handlers == null || Boolean.TRUE.equals(frozen.get())) {
                return;
            }
            try {
                frozen.set(Boolean.TRUE);
                String location = computeLocation(result);
                for (ExceptionHandler handler : handlers) {
                    try {
                        handler.handle(new Incident(log.getName(),
                                                    location,
                                                    CallContext.getCurrent().getMDC(),
                                                    result));
                    } catch (Exception e) {
                        // Just log the exception - anything else might call a rather long infinite loop
                        LOG.SEVERE(new Exception(Strings.apply(
                                "An error occurred while calling the ExceptionHandler: %s - %s (%s)",
                                handler,
                                e.getMessage(),
                                e.getClass().getName()), e));
                    }
                }
            } finally {
                frozen.remove();
            }
        }

        private String computeLocation(HandledException result) {
            String location = null;
            if (ex != null && ex.getStackTrace().length > 0) {
                location = formatStackTraceElement(ex.getStackTrace()[0]);
            } else if (result.getStackTrace().length > 0) {
                StackTraceElement[] trace = Thread.currentThread().getStackTrace();
                int index = 1;
                while ((location == null || location.startsWith("sirius.kernel.health.Exceptions"))
                       && index < trace.length) {
                    location = formatStackTraceElement(trace[index]);
                    index++;
                }
            }
            return location;
        }

        /*
         * Adds the exception message and the exception class to the given params array. Handles null values for
         * e gracefully
         */
        private Object[] extendParams(Throwable e, Object[] params) {
            Object[] newParams;
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

        /*
         * Formats a given StackTraceElement as [class].[method] ([file]:[line])
         */
        private static String formatStackTraceElement(StackTraceElement element) {
            if (element == null) {
                return null;
            }
            return element.getClassName()
                   + "."
                   + element.getMethodName()
                   + " ("
                   + element.getFileName()
                   + ":"
                   + element.getLineNumber()
                   + ")";
        }

        @Override
        public String toString() {
            return "ErrorHandler{"
                   + "params="
                   + params
                   + ", key='"
                   + key
                   + '\''
                   + ", systemErrorMessage='"
                   + systemErrorMessage
                   + '\''
                   + ", ex="
                   + ex
                   + '}';
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

    /**
     * Can be used to log if a deprecated method has been called.
     * <p>
     * This method must be called from the deprecated one and will report the name of the deprecated method
     * and its caller.
     */
    public static void logDeprecatedMethodUse() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack.length < 4) {
            Exceptions.handle().withSystemErrorMessage("Cannot log deprecated API call for short stacktrace!").handle();
        }

        StringBuilder msg = new StringBuilder();
        StackTraceElement deprecatedMethod = stack[2];
        StackTraceElement caller = stack[3];
        msg.append(Strings.apply("The deprecated method '%s.%s' was called by '%s.%s'",
                                 deprecatedMethod.getClassName(),
                                 deprecatedMethod.getMethodName(),
                                 caller.getClassName(),
                                 caller.getMethodName()));
        List<Tuple<String, String>> mdc = CallContext.getCurrent().getMDC();
        if (mdc != null) {
            msg.append("\n---------------------------------------------------\n");
            for (Tuple<String, String> t : mdc) {
                msg.append(t.getFirst()).append(": ").append(t.getSecond()).append("\n");
            }
        }

        DEPRECATION_LOG.WARN(msg);
    }

    /**
     * Retrieves the actual root {@link Throwable} which ended in the given exception.
     *
     * @param e the throwable to begin with
     * @return the root {@link Throwable} of the given one
     */
    public static Throwable getRootCause(@Nullable Throwable e) {
        if (e == null) {
            return null;
        }

        Throwable cause = e;

        int circuitBreaker = 11;
        while (circuitBreaker > 0 && cause.getCause() != null && !cause.equals(cause.getCause())) {
            cause = cause.getCause();
            circuitBreaker--;
        }

        return cause;
    }
}
