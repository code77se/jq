
package se.code77.jq;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Promises/A+ style promise offering simple, chainable asynchronous invocations.
 * 
 * @param <V> Type of the value carried by the promise
 */
public interface Promise<V> extends Future<V> {
    /**
     * Callback interface for resolved/fulfilled promises.
     * 
     * @param <V> Type of the value resolved by the promise on which this
     *            callback is registered by calling
     *            {@link Promise#then(OnFulfilledCallback)} or any of its
     *            overloaded variants.
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnFulfilledCallback<V, NV> {
        /**
         * The promise on which this callback was registered has been
         * resolved/fulfilled.
         * 
         * @param value The resolved value of the promise.
         * @return A direct value (wrapped in a {@link Value#wrap(Object)}, a
         *         new Promise or any other kind of Future.
         * @throws Exception Any exception thrown by this method will lead to
         *             the rejection of the next promise in the chain.
         */
        Future<? extends NV> onFulfilled(V value) throws Exception;
    }

    /**
     * Base interface for {@link Promise#spread(OnFulfilledSpreadCallback)}, normally not used
     * directly.
     * Sub-interfaces are defined for 1-5 arguments; if more are needed, implement this interface
     * with a Future&lt;NV&gt; onFulfilled method with the desired number of arguments
     * matching the promised list.
     *
     * @param <L> Type of the value resolved by the promise on which this
     *            callback is registered by calling
     *            {@link Promise#spread(OnFulfilledSpreadCallback)} or any of its
     *            overloaded variants. Must be a List.
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     *
     */
    public interface OnFulfilledSpreadCallback<L, NV> {
    }

    /**
     * Spread callback with 1 argument. Used for spreading the first element of a promised list
     * to an individual argument.
     * If the list does not hold sufficient elements the remaining arguments will be null.
     * @param <E> Type of the elements in the promised list, i.e., if the promise is resolved to a
     *           List&lt;String&gt; then E is String
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnFulfilledSpreadCallback1<E, NV> extends OnFulfilledSpreadCallback<List<E>, NV> {
        Future<? extends NV> onFulfilled(E e1) throws Exception;
    }

    /**
     * Spread callback with 2 arguments. Used for spreading the 2 first elements of a promised list
     * to individual arguments.
     * If the list does not hold sufficient elements the remaining arguments will be null.
     * @param <E> Common super-type of the elements in the promised list, i.e., if the promise is resolved to a
     *           List&lt;CharSequence&gt; then E is CharSequence
     * @param <E1> Type of the first element, sub-type of E
     * @param <E2> Type of the second element, sub-type of E
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnFulfilledSpreadCallback2<E, E1 extends E, E2 extends E, NV> extends OnFulfilledSpreadCallback<List<E>, NV> {
        Future<? extends NV> onFulfilled(E1 e1, E2 e2) throws Exception;
    }

    /**
     * Spread callback with 3 arguments. Used for spreading the 3 first elements of a promised list
     * to individual arguments.
     * If the list does not hold sufficient elements the remaining arguments will be null.
     * @param <E> Common super-type of the elements in the promised list, i.e., if the promise is resolved to a
     *           List&lt;CharSequence&gt; then E is CharSequence
     * @param <E1> Type of the first element, sub-type of E
     * @param <E2> Type of the second element, sub-type of E
     * @param <E3> Type of the third element, sub-type of E
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnFulfilledSpreadCallback3<E, E1 extends E, E2 extends E, E3 extends E, NV> extends OnFulfilledSpreadCallback<List<E>, NV> {
        Future<? extends NV> onFulfilled(E1 e1, E2 e2, E3 e3) throws Exception;
    }

    /**
     * Spread callback with 4 arguments. Used for spreading the 4 first elements of a promised list
     * to individual arguments.
     * If the list does not hold sufficient elements the remaining arguments will be null.
     * @param <E> Common super-type of the elements in the promised list, i.e., if the promise is resolved to a
     *           List&lt;CharSequence&gt; then E is CharSequence
     * @param <E1> Type of the first element, sub-type of E
     * @param <E2> Type of the second element, sub-type of E
     * @param <E3> Type of the third element, sub-type of E
     * @param <E4> Type of the fourth element, sub-type of E
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnFulfilledSpreadCallback4<E, E1 extends E, E2 extends E, E3 extends E, E4 extends E, NV> extends OnFulfilledSpreadCallback<List<E>, NV> {
        Future<? extends NV> onFulfilled(E1 e1, E2 e2, E3 e3, E4 e4) throws Exception;
    }

    /**
     * Spread callback with 5 arguments. Used for spreading the 5 first elements of a promised list
     * to individual arguments.
     * If the list does not hold sufficient elements the remaining arguments will be null.
     * @param <E> Common super-type of the elements in the promised list, i.e., if the promise is resolved to a
     *           List&lt;CharSequence&gt; then E is CharSequence
     * @param <E1> Type of the first element, sub-type of E
     * @param <E2> Type of the second element, sub-type of E
     * @param <E3> Type of the third element, sub-type of E
     * @param <E4> Type of the fourth element, sub-type of E
     * @param <E5> Type of the fifth element, sub-type of E
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnFulfilledSpreadCallback5<E, E1 extends E, E2 extends E, E3 extends E, E4 extends E, E5 extends E, NV> extends OnFulfilledSpreadCallback<List<E>, NV> {
        Future<? extends NV> onFulfilled(E1 e1, E2 e2, E3 e3, E4 e4, E5 e5) throws Exception;
    }

    /**
     * Callback interface for rejected promises, handling only a sub-type of Exception.
     *
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     * @param <E> Type of exception to be handled by this rejection handler
     *
     */
    public interface OnRejectedBaseCallback<E extends Exception, NV> {
        /**
         * The promise on which this callback was registered has been rejected.
         *
         * @param reason The exception that caused the promise to be rejected.
         * @return A direct value (wrapped in a {@link Value#wrap(Object)}, a
         *         new Promise or any other kind of Future.
         * @throws Exception Any exception thrown by this method will lead to
         *             the rejection of the next promise in the chain.
         */
        Future<? extends NV> onRejected(E reason) throws Exception;
    }

    /**
     * Callback interface for rejected promises, catching any Exception.
     *
     * @param <NV> Type of the value returned by the callback, which will be
     *            used to resolve the next promise in the chain.
     */
    public interface OnRejectedCallback<NV> extends OnRejectedBaseCallback<Exception, NV> {
    }

    /**
     * Callback interface for promise progress
     */
    public interface OnProgressedCallback {
        /**
         * The promise on which this callback was registered has progressed
         *
         * @param progress Progress
         */
        void onProgressed(float progress);
    }

    /**
     * Callback interface used for {@link #tap(OnTapCallback)} used to observe fulfilled promises
     * without modifying the next promise.
     * @param <V> Type of value carried by the promise
     */
    public interface OnTapCallback<V> {
        /**
         * The promise has been fulfilled with the given value
         * @param value Value
         */
        void onTap(V value);
    }

    /**
     * An exception thrown by a promise which is terminated without any
     * rejection callback. A promise is terminated by calling
     * {@link Promise#done()} or any of its overloaded variants. Note that if a
     * promise is NOT terminated, unhandled errors will pass unnoticed.
     */
    @SuppressWarnings("serial")
    public static final class UnhandledRejectionException extends RuntimeException {
        private final Exception mReason;

        UnhandledRejectionException(Exception reason) {
            mReason = reason;
        }

        public Exception getRejection() {
            return mReason;
        }

        @Override
        public String toString() {
            return getClass().getName() + ": " + mReason.toString();
        }
    }

    /**
     * Thrown for mismatching callbacks used with {@link Promise#spread(OnFulfilledSpreadCallback)}
     */
    public static final class IllegalSpreadCallbackException extends RuntimeException {
        public IllegalSpreadCallbackException(String message) {
            super(message);
        }

        public IllegalSpreadCallbackException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * State of a promise. All promises will initially be {@link State#PENDING}
     * and may change into {@link State#FULFILLED} or {@link State#REJECTED}
     * only once.
     */
    public static enum State {
        /**
         * The promise is not completed yet, typically because the task promising the value is still ongoing.
         */
        PENDING,
        /**
         * The promise is fulfilled, typically because the task promising the value succeeded.
         */
        FULFILLED,
        /**
         * The promise is rejected, typically because the task promising the value failed. 
         */
        REJECTED
    }

    /**
     * The state snapshot of a promise as returned by {@link Promise#inspect()}.
     * 
     * @param <V> Type of the value carried by the promise.
     */
    public static final class StateSnapshot<V> {
        /**
         * The current state of the promise
         */
        public final State state;
        /**
         * The value (only applicable if state is {@link State#FULFILLED})
         */
        public final V value;
        /**
         * The rejection reason (only applicable if state is {@link State#REJECTED})
         */
        public final Exception reason;

        StateSnapshot(State state, V value, Exception reason) {
            this.state = state;
            this.value = value;
            this.reason = reason;
        }

        @Override
        public int hashCode() {
            return state.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof StateSnapshot) {
                StateSnapshot that = (StateSnapshot)o;

                return state.equals(that.state) &&
                        (value == null ? that.value == null : value.equals(that.value)) &&
                        (reason == null ? that.reason == null : reason.equals(that.reason));
            }

            return false;
        }

        @Override
        public String toString() {
            switch (state) {
                case FULFILLED:
                    return state.toString() + ": " + value;
                case REJECTED:
                    return state.toString() + ": " + reason.toString();
                default:
                    return state.toString();
            }
        }
    }

    /**
     * Get the state snapshot of this promise
     * 
     * @return State snapshot
     */
    public StateSnapshot<V> inspect();

    /**
     * Returns true if this promise is fulfilled (resolved)
     * 
     * @return true if fulfilled, false otherwise
     */
    public boolean isFulfilled();

    /**
     * Returns true if this promise is rejected
     * 
     * @return true if rejected, false otherwise
     */
    public boolean isRejected();

    /**
     * Returns true if this promise is pending
     * 
     * @return true if pending, false otherwise
     */
    public boolean isPending();

    /**
     * Observe the state of this promise by adding callbacks which will be
     * called when the promise is resolved, rejected or progresses. This method will return
     * a new promise which will be chained to this promise, so that any value
     * returned from or exception thrown by any of the invoked callbacks will be
     * carried over to the new promise. This means that callbacks may be chained
     * such as p.then(...).then(...).then(...).fail(...) etc. The supplied
     * callbacks will be invoked on the thread from which this method is called.
     * If the promise is already completed when this method is called, the
     * relevant callback (onFulfilled or onRejected) will be dispatched for
     * immediate execution in a future event loop turn, but will never be
     * inlined. Similarly, if progress is previously notified when this method is
     * called, the onProgressed callback will be dispatched for immediate execution.
     * Note that this method MUST be run on a thread implementing an event loop.
     *
     * @param <NV> Type of the value returned by the callback handlers
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     * @param onProgressed Progress handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     * @throws NullPointerException if onFulfilled is null
     */
    public <NV> Promise<NV> then(
            OnFulfilledCallback<V, NV> onFulfilled, OnRejectedCallback<NV> onRejected, OnProgressedCallback onProgressed);

    /**
     * Observe the state of this promise by adding callbacks which will be
     * called when the promise is resolved or rejected. This method will return
     * a new promise which will be chained to this promise, so that any value
     * returned from or exception thrown by any of the invoked callbacks will be
     * carried over to the new promise. This means that callbacks may be chained
     * such as p.then(...).then(...).then(...).fail(...) etc. The supplied
     * callbacks will be invoked on the thread from which this method is called.
     * If the promise is already completed when this method is called, the
     * relevant callback (onFulfilled or onRejected) will be dispatched for
     * immediate execution in a future event loop turn, but will never be
     * inlined.
     * Note that this method MUST be run on a thread implementing an event loop.
     *
     * @param <NV> Type of the value returned by the callback handlers
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     * @throws NullPointerException if onFulfilled is null
     */
    public <NV> Promise<NV> then(
            OnFulfilledCallback<V, NV> onFulfilled, OnRejectedCallback<NV> onRejected);

    /**
     * Observe the state of this promise by adding a fulfillment callback which
     * will be called when the promise is resolved. This is the same as calling
     * {@link #then(OnFulfilledCallback, OnRejectedCallback)} with null as the
     * onRejected argument.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @see #then(OnFulfilledCallback, OnRejectedCallback)
     * @param <NV> Type of the value returned by the callback handlers
     * @param onFulfilled Fulfillment handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from the supplied
     *         callback handler.
     * @throws NullPointerException if onFulfilled is null
     */
    public <NV> Promise<NV> then(OnFulfilledCallback<V, NV> onFulfilled);

    /**
     * Convenience method, equivalent to adding a fulfillment callback that merely returns the given value.
     *
     * @param nextValue Value to resolve the next promise with
     * @param <NV> Type of the value carried by the next promise
     * @return A new promise that, provided the current promise is resolved, will be resolved with the given value.
     */
    public <NV> Promise<NV> thenResolve(NV nextValue);

    /**
     * Convenience method, equivalent to adding a fulfillment callback that merely throws the given exception
     * @param reason Exception to reject the next promise with
     * @param <NV> Type of the value carried by the next promise
     * @return A new promise that, provided the current promise is resolved, will be rejected with the given reason.
     */
    public <NV> Promise<NV> thenReject(Exception reason, Class<NV> nextValueClass);

    /**
     * Observe the state of this promise by adding a handler that will be invoked when the promise
     * is fulfilled, but without modifying the next promise.
     *
     * @param onTap Simple, observe-only fulfillment handler
     * @return A new promise which will inherit the state of the current promise
     */
    public Promise<V> tap(OnTapCallback<V> onTap);

    /**
     * Like {@link Promise#then(OnFulfilledCallback, OnRejectedCallback)} but only for promised List values. The
     * elements in the list will be spread as individual arguments on the supplied callback, which
     * should implement a suitable sub-interface of OnFulfilledSpreadCallback
     * (e.g. OnFulfilledSpreadCallback3) for the appropriate number of arguments. This is useful
     * when an operation with a fixed amount of distinct values are called and resolved as a List,
     * e.g. using {@link JQ#all(List)}.
     *
     * @see #then(OnFulfilledCallback, OnRejectedCallback)
     * @param <NV> Type of the value returned by the callback handlers
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from the supplied
     *         callback handler.
     * @throws NullPointerException if onFulfilled is null
     */
    public <NV> Promise<NV> spread(
            OnFulfilledSpreadCallback<V, NV> onFulfilled, OnRejectedCallback<NV> onRejected);

    /**
     * Like {@link Promise#then(OnFulfilledCallback)} but only for promised List values. The
     * elements in the list will be spread as individual arguments on the supplied callback, which
     * should implement a suitable sub-interface of OnFulfilledSpreadCallback
     * (e.g. OnFulfilledSpreadCallback3) for the appropriate number of arguments. This is useful
     * when an operation with a fixed amount of distinct values are called and resolved as a List,
     * e.g. using {@link JQ#all(List)}
     *
     * @see #then(OnFulfilledCallback, OnRejectedCallback)
     * @param <NV> Type of the value returned by the callback handlers
     * @param onFulfilled Fulfillment handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from the supplied
     *         callback handler.
     * @throws NullPointerException if onFulfilled is null
     */
    public <NV> Promise<NV> spread(OnFulfilledSpreadCallback<V, NV> onFulfilled);

    /**
     * Observe the state of this promise by adding a rejection callback which
     * will be called when the promise is rejected. This is the same as calling
     * {@link #then(OnFulfilledCallback, OnRejectedCallback)} with null as the
     * onFulfilled argument.
     * Analogous to a catch clause in synchronous code.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @see #then(OnFulfilledCallback, OnRejectedCallback)
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from the supplied
     *         callback handler.
     */
    public Promise<V> fail(OnRejectedCallback<V> onRejected);

    /**
     * Convenience method for observing the state of this promise by adding a rejection callback which
     * will be called when the promise is rejected only with a given subclass of Exception.
     * Analogous to a catch clause in synchronous code.
     * Note that this method MUST be run on a thread implementing an event loop.
     *
     * @see #fail(OnRejectedCallback)
     * @param <E> sub-type of Exception
     * @param reasonClass Class of Exception to be caught by the given handler
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from the supplied
     *         callback handler.
     */
    public <E extends Exception> Promise<V> fail(Class<E> reasonClass, OnRejectedBaseCallback<E, V> onRejected);

    /**
     * Observe the progress of this promise by adding a progress callback which
     * will be called whenever progress of the promise is notified. This is the same
     * as calling {@link #then(OnFulfilledCallback, OnRejectedCallback, OnProgressedCallback)}
     * with null as the onFulfilled and onRejected arguments.
     * Note that this method MUST be run on a thread implementing an event loop.
     *
     * @see #then(OnFulfilledCallback, OnRejectedCallback, OnProgressedCallback)
     *
     * @param onProgressed Progress handler
     * @return A new promise that will inherit the state of this promise.
     */
    public Promise<V> progress(OnProgressedCallback onProgressed);

    /**
     * Callback interface invoked for promises that are either fulfilled or rejected.
     *
     */
    public interface OnFinallyCallback {
        /**
         * Called when a promise is either fulfilled or rejected.
         * If this method throws an exception the next promise will be rejected with it, otherwise
         * the next promise will not be effected by the execution of this method.
         *
         * @throws Exception An exception
         */
        void onFinally() throws Exception;
    }

    /**
     * Callback interface invoked for promises that are either fulfilled or rejected.
     *
     */
    public interface OnFinallyFutureCallback {
        /**
         * Called when a promise is either fulfilled or rejected.
         * This method may return a value or a promise.
         * * If it returns a promise, the next promise will wait for the returned promise to be resolved.
         * * If the returned promise is resolved, the next promise will not be affected but simply resolved with its original value.
         * * If the returned promise is rejected OR if this method throws an exception the next promise will be rejected with the reason produced by this method.
         * * If it returns a value, the next promise will be resolved with its original value and not affected by the execution of this method.
         *
         * @throws Exception An exception
         */
        Future<Void> onFinally() throws Exception;
    }

    /**
     * Observe the state of this promise by adding a finally callback which
     * will be called when the promise is either fulfilled or rejected.
     * Note that this method MUST be run on a thread implementing an event loop.
     * Analogous to a finally clause in synchronous code.
     *
     * This is a convenience method for a finally callback that does not return anything.
     *
     * @see #fin(OnFinallyFutureCallback)
     * @see #then(OnFulfilledCallback, OnRejectedCallback)
     * @param onFinally finally callback
     * @return A new promise that inherits the state of the previous promise, unless the finally
     * callback throws an exception, in which case the new promise will be rejected with that
     * exception.
     */
    public Promise<V> fin(OnFinallyCallback onFinally);

    /**
     * Observe the state of this promise by adding a finally callback which
     * will be called when the promise is either fulfilled or rejected.
     * Note that this method MUST be run on a thread implementing an event loop.
     * Analogous to a finally clause in synchronous code.
     *
     * If the finally callback returns a promise, the next promise will wait for that promise to be resolved before continuing.
     *
     * @see #fin(OnFinallyCallback)
     * @see #then(OnFulfilledCallback, OnRejectedCallback)
     * @param onFinally finally callback
     * @return A new promise that inherits the state of the previous promise, unless the finally
     * callback throws an exception or returns a rejected promise, in which case the new promise will be rejected with that
     * exception.
     */
    public Promise<V> fin(OnFinallyFutureCallback onFinally);

    /**
     * Adds the supplied callback handlers, just like calling
     * {@link #then(OnFulfilledCallback, OnRejectedCallback)}, but also adds a
     * default rejection handler and then terminates the promise chain. It's
     * good practice to terminate a promise chain to avoid unhandled rejections
     * to silently pass unnoticed. If an unterminated chain of promises without
     * a rejection handler is rejected, nothing will happen. However, a
     * terminated chain will result in a UnhandledRejectionException carrying
     * the rejection reason to be thrown on the dispatcher thread. Since this
     * exception will typically not be possible to catch, this means the
     * application is likely to crash. Clients should take caution to always
     * include a rejection handler, and should consider termination of promise
     * chains as an aid during development.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     */
    public void done(
            OnFulfilledCallback<V, Void> onFulfilled, OnRejectedCallback<Void> onRejected);

    /**
     * Adds the supplied fulfillment handler, just like calling
     * {@link #then(OnFulfilledCallback)}, but also adds a default rejection
     * handler and then terminates the promise chain. It's good practice to
     * terminate a promise chain to avoid unhandled rejections to silently pass
     * unnoticed. If an unterminated chain of promises without a rejection
     * handler is rejected, nothing will happen. However, a terminated chain
     * will result in a UnhandledRejectionException carrying the rejection
     * reason to be thrown on the dispatcher thread. Since this exception will
     * typically not be possible to catch, this means the application is likely
     * to crash. Clients should take caution to always include a rejection
     * handler, and should consider termination of promise chains as an aid
     * during development.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @param onFulfilled Fulfillment handler
     */
    public void done(OnFulfilledCallback<V, Void> onFulfilled);

    /**
     * Adds a default rejection handler and then terminates the promise chain.
     * It's good practice to terminate a promise chain to avoid unhandled
     * rejections to silently pass unnoticed. If an unterminated chain of
     * promises without a rejection handler is rejected, nothing will happen.
     * However, a terminated chain will result in a UnhandledRejectionException
     * carrying the rejection reason to be thrown on the dispatcher thread.
     * Since this exception will typically not be possible to catch, this means
     * the application is likely to crash. Clients should take caution to always
     * include a rejection handler, and should consider termination of promise
     * chains as an aid during development.
     * Note that this method MUST be run on a thread implementing an event loop.
     */
    public void done();

    /**
     * Add a timeout to a promise by returning a new promise that will be
     * rejected with a TimeoutException when the specified time has passed if
     * this promise isn't completed by then.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @param ms Timeout, in milliseconds
     * @return New promise
     */
    public Promise<V> timeout(final long ms);

    /**
     * Delays a resolved promise by the specified time, by creating a new
     * promise that will inherit the state of this promise; if this promise is
     * rejected, the new promise will also be immediately rejected but if this
     * promise is resolved, the new promise will be resolved when the specified
     * time has passed.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @param ms Time to delay resolved promise, in milliseconds
     * @return New promise
     */
    public Promise<V> delay(final long ms);

    /**
     * Creates a new promise that will wait for the two promises to complete,
     * and which will be resolved if and only if the two promises are resolved
     * with the exact same value. If any of the promises are rejected, the new
     * promise will be immediately rejected as well. If both promises are
     * resolved but with different values, the new promise will be rejected.
     * Note that this method MUST be run on a thread implementing an event loop.
     * 
     * @param that A promise
     * @return A promise that will be resolved only if both promises are
     *         resolved with the same value; rejected otherwise
     */
    public Promise<V> join(Promise<V> that);

    /**
     * Returns the resolved value carried by this promise. If the value is not
     * readily available, this method will block while waiting for the promise
     * to be completed or an interrupt to occur. Calling this method on a
     * dispatcher thread might cause a deadlock. If the promise is rejected, an
     * ExecutionException will be thrown, wrapping the rejection reason.
     * 
     * @return The value carried by this promise
     * @throws InterruptedException If the blocked thread was interrupted
     * @throws ExecutionException If the promise was rejected, the reason will
     *             be wrapped in an ExecutionException.
     */
    @Override
    public V get() throws InterruptedException, ExecutionException;

    /**
     * Returns the resolved value carried by this promise. If the value is not
     * readily available, this method will block while waiting for the promise
     * to be completed or an interrupt to occur, however for no longer than the
     * supplied duration. Calling this method on a dispatcher thread might cause
     * a deadlock. If the promise is rejected, an ExecutionException will be
     * thrown, wrapping the rejection reason.
     * 
     * @return The value carried by this promise
     * @throws InterruptedException If the blocked thread was interrupted
     * @throws ExecutionException If the promise was rejected, the reason will
     *             be wrapped in an ExecutionException.
     * @throws TimeoutException If the promise was not completed before the
     *             specified duration passed.
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException;

    /**
     * Canceling promises is not supported since a promise is completely
     * decoupled from the task and only observable (its state may be changed
     * only through the Deferred object). This method will always return false.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Since cancellation is not supported this method will always return false.
     */
    @Override
    public boolean isCancelled();

    /**
     * Returns true if this promise is completed, i.e., either resolved or
     * rejected.
     */
    @Override
    public boolean isDone();
}
