
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
     * Return value for Void callbacks
     */
    public static final Value<Void> VOID = null;

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
