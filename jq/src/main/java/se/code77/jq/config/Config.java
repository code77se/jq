
package se.code77.jq.config;

import java.util.HashMap;
import java.util.Map;

import se.code77.jq.Promise;
import se.code77.jq.config.android.AndroidConfig;

/**
 * Configuration for JQ. The configuration offers integration points into the
 * host environment, as well as tweaking of behaviour.
 */
public abstract class Config {
    protected final Map<Thread, Dispatcher> mDispatchers = new HashMap<>();

    /**
     * A dispatcher is capable of asynchronously dispatching events containing
     * code to be executed in a thread implementing an event loop. Each
     * dispatcher is associated with exactly one Thread, determined upon
     * construction, and all events will be dispatched to this thread.
     */
    public interface Dispatcher {
        /**
         * Dispatch the given runnable for future execution on the Thread
         * associated with this dispatcher
         *
         * @param r Code to be executed
         */
        void dispatch(Runnable r);

        /**
         * Dispatch the given runnable for future execution on the Thread
         * associated with this dispatcher, after the specified time has passed.
         *
         * @param r Code to be executed
         * @param ms Time to be passed before code is executed
         */
        void dispatch(Runnable r, long ms);
    }

    /**
     * A logger creates log prints.
     */
    public interface Logger {
        /**
         * Print a debug log
         * @param s log text
         */
        void verbose(String s);

        /**
         * Print a debug log
         * @param s log text
         */
        void debug(String s);

        /**
         * Print a info log
         * @param s log text
         */
        void info(String s);

        /**
         * Print a warning log
         * @param s log text
         */
        void warn(String s);

        /**
         * Print a error log
         * @param s log text
         */
        void error(String s);
    }

    public enum LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private static Config CONFIG;

    /**
     * Set the configuration to be used by JQ
     * @param config New configuration
     */
    public static synchronized void setConfig(Config config) {
        CONFIG = config;
    }

    /**
     * Get the configuration used by JQ. If no configuration is explicitly given
     * through {@link #setConfig(Config)}, JQ will attempt to create a default
     * configuration if the host environment can be detected. Otherwise an exception will
     * be thrown.
     *
     * @return Current configuration
     */
    public static synchronized Config getConfig() {
        if (CONFIG == null) {
            CONFIG = getDefaultConfig();

            if (CONFIG == null) {
                throw new IllegalStateException("No config supplied and no default config available");
            }
        }
        return CONFIG;
    }

    private static Config getDefaultConfig() {
        try {
            Class.forName("android.os.Handler");
            return new AndroidConfig(false);
        } catch (ClassNotFoundException e) {
        }

        return null;
    }

    protected Config(boolean monitorUnterminated) {
        this(monitorUnterminated, LogLevel.WARN);
    }

    protected Config(boolean monitorUnterminated, LogLevel logLevel) {
        this.monitorUnterminated = monitorUnterminated;
        this.logLevel = logLevel;
    }

    /**
     * Log level
     */
    public final LogLevel logLevel;

    /**
     * Set to true if unterminated promise chains should cause warnings to be
     * logged. An unterminated promise chain is a chain of promises ending with
     * a promise that has produced a result (fulfilled or rejected) without
     * anyone observing it and without explicitly terminating the chain with {@link Promise#done()}.
     */
    public final boolean monitorUnterminated;

    /**
     * Create a dispatcher for the current thread. Dispatchers are registered through
     * #registterDispatcher(Thread, Dispatcher) but the exact behavior may differ for various
     * Config implementations (e.g. there may be a default dispatcher, or the call may cause an
     * error).
     * @return Dispatcher
     */
    public Dispatcher createDispatcher() {
        Dispatcher d = getDispatcher(Thread.currentThread());

        return d != null ? d : getDefaultDispatcher();
    }

    protected Dispatcher getDispatcher(Thread thread) {
        return mDispatchers.get(thread);
    }

    protected Dispatcher getDefaultDispatcher() {
        return null;
    }

    /**
     * Get a logger
     * @return Logger
     */
    public abstract Logger getLogger();

    /**
     * Register a dispatcher for the given thread. Typically, any registered promise callbacks will
     * be invoked on the dispatcher associated with the thread registering them, but the exact
     * behavior may differ for various Config implementations.
     * @param thread Thread
     * @param dispatcher Dispatcher
     *
     * @return Config instance (for chaining)
     */
    public Config registerDispatcher(Thread thread, Dispatcher dispatcher) {
        mDispatchers.put(thread, dispatcher);

        return this;
    }

    /**
     * Unregister the (previously registered) dispatcher for the given thread
     * @param thread Thread
     *
     * @return Config instance (for chaining)
     */
    public Config unregisterDispatcher(Thread thread) {
        mDispatchers.remove(thread);

        return this;
    }

    /**
     * Check if the given thread is registered as a dispatcher thread using
     * #registerDispatcher(Thread, Dispatcher)
     * @param thread Thread
     * @return true if thread is registered as dispatcher thread
     */
    public boolean isDispatcherThread(Thread thread) {
        return mDispatchers.containsKey(thread);
    }
}
