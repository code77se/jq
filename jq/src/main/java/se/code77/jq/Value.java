
package se.code77.jq;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A simple wrapped value implementing the {@link Future}
 * interface. The value is immediately available through the
 * {@link Future#get()} method, and the Future is always considered to be
 * "done". Since the value is avaialable already when creating the object,
 * canceling is not applicable.
 *
 * @param <V> Type of the value being wrapped
 */
public final class Value<V> implements Future<V> {
    /**
     * A wrapper for calling void methods and returning Value.VOID.
     */
    public interface VoidCallable {
        /**
         * This callback should call the void method.
         * @throws Exception Any exception thrown by the callback.
         */
        void call() throws Exception;
    }

    /**
     * Return value for Void callbacks
     */
    public static final Value<Void> VOID = null;

    /**
     * Return value for Boolean callbacks
     */
    public static final Value<Boolean> TRUE = Value.wrap(true);

    /**
     * Return value for Boolean callbacks
     */
    public static final Value<Boolean> FALSE = Value.wrap(false);

    private final V mValue;

    private Value(V value) {
        mValue = value;
    }

    /**
     * Wrap the given value
     *
     * @param <V> Type of the value to be wrapped
     * @param value The value
     * @return The wrapped value
     */
    public static <V> Value<V> wrap(V value) {
        return new Value<V>(value);
    }

    /**
     * Convenience for calling a void method and returning a Future&lt;Void&gt;.
     * Examples:
     *
     * <pre>
     *     return Value.wrap(this::doSomething);

     *     return Value.wrap(() -&gt; doSomethingWithArgs(42));
     * </pre>
     *
     * @param callable The callback making the call to a void method
     * @return Value.VOID
     * @throws Exception if the invoked void method threw an exception
     */
    public static Value<Void> wrap(VoidCallable callable) throws Exception {
        callable.call();

        return Value.VOID;
    }

    /**
     * Not applicable, since the value is already available upon construction
     *
     * @return Always false
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Not applicable, since the value is already available upon construction
     *
     * @return Always false
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Not applicable, since the value is already available upon construction
     *
     * @return Always true
     */
    @Override
    public boolean isDone() {
        return true;
    }

    /**
     * Returns the wrapped value. Will never block since the value is already
     * available upon construction
     *
     * @return The wrapped value
     */
    @Override
    public final V get() {
        return mValue;
    }

    /**
     * Returns the wrapped value. Will never block nor cause any timeout, since
     * the value is already available upon construction
     *
     * @return The wrapped value
     */
    @Override
    public V get(long timeout, TimeUnit unit) {
        return mValue;
    }
}
