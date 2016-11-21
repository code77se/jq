
package se.code77.jq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import se.code77.jq.Promise.OnFulfilledCallback;
import se.code77.jq.Promise.OnRejectedCallback;
import se.code77.jq.Promise.State;
import se.code77.jq.Promise.StateSnapshot;

/**
 * JQ is a promise implementation in Java, aiming to conform to the Promises/A+
 * specification. See the overview for more info on promises and code examples.
 * <br>
 * The JQ class itself contains static helpers and convenience methods related
 * to promises. For example {@link #defer()}, {@link #defer(DeferredHandler)}
 * and {@link #defer(Callable)} used to create new promises that will be
 * connected to a task and returned to clients, as well as various helper
 * methods related to synchronization of multiple promises etc. It also contains
 * static counterparts to most instance methods on {@link Promise}.
 */
public final class JQ {
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    /**
     * A deferred object is used by methods implementing asynchronous operations
     * exposed by promises. Whereas the Promise object itself only offers means
     * of observing the Promise's state, deferred is the object on which
     * implementation actually resolves or rejects the promise. A deferred
     * object can be achieved in one of two ways:
     * <br>
     * <ol>
     * <li>Calling {@link #defer()} returns a Deferred object. The method
     * will return {@link Deferred#promise} and start the task producing the
     * result to be resolved. Once the result is completed or failed,
     * {@link Deferred#resolve(Object)} or {@link Deferred#reject(Exception)}
     * will be called, respectively.
     * <li>Calling {@link #defer(DeferredHandler)} works similar to 1)
     * but offers a quicker way of creating a Deferred object and returning the promise
     * for it in one operation, while defining the task to be run in a supplied
     * {@link DeferredHandler}.
     * </ol>
     * 
     * @param <V> Type of the value carried by the promise
     */
    public static final class Deferred<V> {
        private Deferred() {
        }

        /**
         * The promise exposed by this deferred.
         */
        public final Promise<V> promise = new PromiseImpl<V>();

        /**
         * Resolve the promise
         * 
         * @param value The value to resolve
         */
        public void resolve(V value) {
            ((PromiseImpl<V>) promise)._resolve(value);
        }

        /**
         * Resolve (or reject) the promise with the outcome of a future (which may be another
         * promise).
         * Effectively, the given future will be promisified and this deferred's promise will
         * inherit the state of that promise.
         *
         * @param future The future to resolve with
         */
        public void resolve(Future<V> future) {
            Promise<V> p = JQ.wrap(future);

            p.then(new OnFulfilledCallback<V, Void>() {
                @Override
                public Future<Void> onFulfilled(V value) throws Exception {
                    ((PromiseImpl<V>) promise)._resolve(value);
                    return null;
                }
            }, new OnRejectedCallback<Void>() {
                @Override
                public Future<Void> onRejected(Exception reason) throws Exception {
                    ((PromiseImpl<V>) promise)._reject(reason);
                    return null;
                }
            }).done();
        }

        /**
         * Reject the promise
         * 
         * @param reason Exception to reject the promise with
         */
        public void reject(Exception reason) {
            ((PromiseImpl<V>) promise)._reject(reason);
        }

        /**
         * Notify progress of the promise
         * @param progress Progress
         */
        public void notify(float progress) {
            ((PromiseImpl<V>) promise)._notify(progress);
        }
    }

    /**
     * Callback interface used by deferrer to get access to the Deferred object
     * 
     * @param <V> Type of the value carried by the promise to be observed by
     *            clients
     */
    public interface DeferredHandler<V> {
        /**
         * Callback implemented by the deferrer to get access to the Deferred
         * object. Caller should carry out the deferred task and upon completion
         * call {@link Deferred#resolve(Object)} or
         * {@link Deferred#reject(Exception)}, respectively.
         * 
         * @param deferred Deferred object carrying the promise to be observed
         *            by clients
         */
        public void handle(Deferred<V> deferred);
    }

    /**
     * Convenience for the case where you want to transform a promised value to void, i.e.,
     * ((Void)null), for example if an operation you need to wait upon returns a promise for a
     * String but you simply want to return an empty promise used for observing when the complete
     * operation is complete.
     *
     * @param <V> Type of the promise you want to convert to an empty (value) value.
     */
    public static final class OnFulfilledVoidCallback<V> implements OnFulfilledCallback<V, Void> {
        @Override
        public Future<Void> onFulfilled(V value) {
            return null;
        }
    }

    private JQ() {
    }

    /**
     * Creates a new promise and an associated Deferred object used to modify
     * the state of the promise. Clients should only gain access to the Promise
     * object and may only observe the state. The deferrer should carry out the
     * task at hand and upon completion resolve or reject the promise by calling
     * {@link Deferred#resolve(Object)} or {@link Deferred#reject(Exception)},
     * respectively.
     * 
     * <pre>
     * final Deferred&lt;String&gt; deferred = JQ.defer();
     * 
     * new Thread() {
     *     public void run() {
     *         // do some lengthy task
     *         deferred.resolve(&quot;Hello world&quot;);
     *     }
     * }.start();
     *
     * return deferred.promise;
     * </pre>
     *
     * @param <V> Type of value to be carried in the promise
     * @return Deferred object
     * @see #defer(DeferredHandler)
     */
    public static <V> Deferred<V> defer() {
        return new Deferred<V>();
    }

    /**
     * Alternative way of creating a deferred object, offering a compact way of
     * inlining the code that handles the task and resolves/rejects the promise,
     * while returning the promise in one single method call. Note that unlike
     * {@link #defer(Callable, Executor)}, the
     * {@link DeferredHandler} will be invoked on the
     * calling thread and will have to manually spawn any background threads etc
     * as needed.
     * 
     * <pre>
     * Promise&lt;String&gt; p = JQ.defer(new DeferredHandler&lt;String&gt;() {
     *     public void handle(final Deferred&lt;String&gt; deferred) {
     *         new Thread() {
     *             public void run() {
     *                 // do some lengthy task
     *                 deferred.resolve(&quot;Hello world&quot;);
     *             }
     *         }.start();
     *     }
     * });
     * </pre>
     * @param <V> Type of the value to be carried by the promise
     * @param dh DeferredHandler callback interface using the Deferred object to
     *            resolve or reject the promise upon task completion.
     * @return A new promise for the task
     * @see #defer() 
     */
    public static <V> Promise<V> defer(DeferredHandler<V> dh) {
        Deferred<V> deferred = defer();
        dh.handle(deferred);
        return deferred.promise;
    }

    /**
     * Alternative way of creating a deferred object by running the given task
     * using a default cached thread pool executor, resolving/rejecting the
     * associated promise with the result of the task execution, and returning
     * the promise.
     *
     * <pre>
     * Promise&lt;String&gt; p = JQ.defer(new Callable&lt;String&gt;() {
     *     public String call() {
     *         return &quot;Hello world&quot;;
     *     }
     * });
     * </pre>
     * @param <V> Type of the value to be carried by the promise
     * @param task The task to run. The result returned from the task will be
     *            used to resolve the promise, if any exception is thrown it
     *            will be used to reject the promise.
     * @return A new promise for the task
     * @see #defer(Callable, Executor) 
     */
    public static <V> Promise<V> defer(final Callable<V> task) {
        return defer(task, DEFAULT_EXECUTOR);
    }

    /**
     * Alternative way of creating a deferred object by running the given task
     * using the given executor, resolving/rejecting the associated promise with
     * the result of the task execution, and returning the promise.
     *
     * @see #defer() <pre>
     * Promise&lt;String&gt; p = JQ.defer(myExec, new Callable&lt;String&gt;() {
     *     public String call() {
     *         return &quot;Hello world&quot;;
     *     }
     * });
     * </pre>
     * @param <V> Type of the value to be carried by the promise
     * @param task The task to run. The result returned from the task will be
     *            used to resolve the promise, if any exception is thrown it
     *            will be used to reject the promise.
     * @param executor Executor to run the task
     * @return A new promise for the task
     */
    public static <V> Promise<V> defer(final Callable<V> task, Executor executor) {
        final Deferred<V> deferred = defer();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    deferred.resolve(task.call());
                } catch (Exception e) {
                    deferred.reject(e);
                }
            }
        });

        return deferred.promise;
    }

    /**
     * Create a pre-resolved promise for a simple, immediately available value.
     *
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     * @return New promise
     */
    public static <V> Promise<V> resolve(final V value) {
        return defer(new DeferredHandler<V>() {
            @Override
            public void handle(Deferred<V> deferred) {
                deferred.resolve(value);
            }
        });
    }

    /**
     * Create a promise for a void (non-)value. Effectively this is just a way
     * of starting a promise chain. It can be useful in situations where
     * branches in the code causes a series of asynchronous operations to be
     * called sometimes, and sometimes not. By starting the chain of promise
     * calls with a {@link #resolve()}, the following callbacks may
     * return either values or new promises to further calls, they will be
     * properly resolved and propagated into a promise regardless.
     * 
     * @return A new promise
     */
    public static Promise<Void> resolve() {
        return resolve((Void) null);
    }

    /**
     * Wrap a Future in a promise. Once the future is done, the promise will be
     * resolved or rejected depending on the outcome. If future is already a
     * promise, future itself will be returned by this method.
     * 
     * @param <V> Type of the value to be carried by the promise
     * @param future Future to wrap
     * @return New promise
     */
    public static <V> Promise<V> wrap(final Future<V> future) {
        if (future == null) {
            return resolve(null);
        } else if (future instanceof Promise) {
            return (Promise<V>) future;
        } else if (future instanceof Value) {
            return resolve(((Value<V>) future).get());
        } else {
            return defer(new Callable<V>() {
                @Override
                public V call() throws Exception {
                    try {
                        return future.get();
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();

                        if (cause instanceof Exception) {
                            throw (Exception) cause;
                        } else {
                            // Promises can only rejected with
                            // Exceptions, not any Throwable.
                            // Simply throw the ExecutionException.
                            throw e;
                        }
                    }

                }
            });
        }
    }

    /**
     * Create a pre-rejected promise for the given exception
     *
     * @param <T> Type of the value to be carried by the return promise
     * @param reason Exception to reject the promise with
     * @return A new promise
     */
    public static <T> Promise<T> reject(final Exception reason) {
        return defer(new DeferredHandler<T>() {
            @Override
            public void handle(Deferred<T> deferred) {
                deferred.reject(reason);
            }
        });
    }

    /**
     * Static convenience version of then. This method is equivalent to
     * JQ.resolve(value).then(onFulfilled, onRejected).
     * 
     * @see Promise#then(OnFulfilledCallback, OnRejectedCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param <NV> Type of the value to be returned by the callback handlers
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     */
    public static <V, NV>
            Promise<NV> when(
                    final V value, OnFulfilledCallback<V, NV> onFulfilled,
                    OnRejectedCallback<NV> onRejected) {
        return resolve(value).then(onFulfilled, onRejected);
    }

    /**
     * Static convenience version of then. This method is equivalent to
     * JQ.resolve(value).then(onFulfilled).
     * 
     * @see Promise#then(OnFulfilledCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param <NV> Type of the value to be returned by the callback handlers
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     */
    public static <V, NV> Promise<NV> when(final V value,
            OnFulfilledCallback<V, NV> onFulfilled) {
        return when(value, onFulfilled, null);
    }

    /**
     * Static convenience version of then. This method is equivalent to
     * JQ.resolve(value).then(onFulfilled, onRejected).
     * 
     * @see Promise#then(OnFulfilledCallback, OnRejectedCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param <NV> Type of the value to be returned by the callback handlers
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     */
    public static <V, NV>
            Promise<NV> when(
                    final Value<V> value, OnFulfilledCallback<V, NV> onFulfilled,
                    OnRejectedCallback<NV> onRejected) {
        return when(value.get(), onFulfilled, onRejected);
    }

    /**
     * Static convenience version of then. This method is equivalent to
     * JQ.resolve(value).then(onFulfilled).
     * 
     * @see Promise#then(OnFulfilledCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param <NV> Type of the value to be returned by the callback handlers
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     */
    public static <V, NV> Promise<NV> when(final Value<V> value,
            OnFulfilledCallback<V, NV> onFulfilled) {
        return when(value, onFulfilled, null);
    }

    /**
     * Static convenience version of fail. This method is equivalent to
     * JQ.resolve(value).fail(onRejected).
     * 
     * @see Promise#fail(OnRejectedCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param <NV> Type of the value to be returned by the callback handlers
     * @param value Value to resolve the promise with
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     */
    public static <V, NV> Promise<NV> fail(final V value,
            OnRejectedCallback<NV> onRejected) {
        return when(value, null, onRejected);
    }

    /**
     * Static convenience version of fail. This method is equivalent to
     * JQ.resolve(value).fail(onRejected).
     * 
     * @see Promise#fail(OnRejectedCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param <NV> Type of the value to be returned by the callback handlers
     * @param value Value to resolve the promise with
     * @param onRejected Rejection handler
     * @return A new promise that will be resolved with the value
     *         returned/rejected with the reason thrown from any of the callback
     *         handlers.
     */
    public static <V, NV> Promise<NV> fail(final Value<V> value,
            OnRejectedCallback<NV> onRejected) {
        return fail(value.get(), onRejected);
    }

    /**
     * Static convenience version of done. This method is equivalent to
     * JQ.resolve(value).done(onFulfilled, onRejected).
     * 
     * @see Promise#done(OnFulfilledCallback, OnRejectedCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     */
    public static <V> void done(
            final V value, OnFulfilledCallback<V, Void> onFulfilled,
            OnRejectedCallback<Void> onRejected) {
        resolve(value).done(onFulfilled, onRejected);
    }

    /**
     * Static convenience version of done. This method is equivalent to
     * JQ.resolve(value).done(onFulfilled, onRejected).
     * 
     * @see Promise#done(OnFulfilledCallback, OnRejectedCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     * @param onRejected Rejection handler
     */
    public static <V> void done(
            final Value<V> value, OnFulfilledCallback<V, Void> onFulfilled,
            OnRejectedCallback<Void> onRejected) {
        done(value.get(), onFulfilled, onRejected);
    }

    /**
     * Static convenience version of done. This method is equivalent to
     * JQ.resolve(value).done(onFulfilled).
     * 
     * @see Promise#done(OnFulfilledCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     */
    public static <V> void done(final V value,
            OnFulfilledCallback<V, Void> onFulfilled) {
        done(value, onFulfilled, null);
    }

    /**
     * Static convenience version of done. This method is equivalent to
     * JQ.resolve(value).done(onFulfilled).
     * 
     * @see Promise#done(OnFulfilledCallback)
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     * @param onFulfilled Fulfillment handler
     */
    public static <V> void done(final Value<V> value,
            OnFulfilledCallback<V, Void> onFulfilled) {
        done(value.get(), onFulfilled);
    }

    /**
     * Static convenience version of done. This method is equivalent to
     * JQ.resolve(value).done().
     * 
     * @see Promise#done()
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     */
    public static <V> void done(final V value) {
        done(value, null, null);
    }

    /**
     * Static convenience version of done. This method is equivalent to
     * JQ.resolve(value).done().
     * 
     * @see Promise#done()
     * @param <V> Type of the value to be carried by the promise
     * @param value Value to resolve the promise with
     */
    public static <V> void done(final Value<V> value) {
        done(value.get());
    }

    /**
     * Static convenience version of {@link Promise#timeout(long)}. Adds a timeout to a
     * promise by returning a new promise that will be rejected with a
     * TimeoutException when the specified time has passed if this promise isn't
     * completed by then.
     * 
     * @see Promise#timeout(long)
     * @param <V> Type of the value carried by the promise
     * @param promise Promise to add a timeout to
     * @param ms Timeout, in milliseconds
     * @return New promise
     */
    public static <V> Promise<V> timeout(Promise<V> promise, long ms) {
        return promise.timeout(ms);
    }

    /**
     * Static convenience version of {@link Promise#delay(long)}. Delays a resolved
     * promise by the specified time, by creating a new promise that will
     * inherit the state of this promise; if this promise is rejected, the new
     * promise will also be immediately rejected but if this promise is
     * resolved, the new promise will be resolved when the specified time has
     * passed.
     *
     * @see Promise#delay(long)
     * @param <V> Type of the value carried by the promise
     * @param promise Promise to add a timeout to
     * @param ms Time to delay resolved promise, in milliseconds
     * @return New promise
     */
    public static <V> Promise<V> delay(Promise<V> promise, final long ms) {
        return promise.delay(ms);
    }

    /**
     * Static convenience version of {@link Promise#join(Promise)}. Creates a new
     * promise that will wait for the two promises to complete, and which will
     * be resolved if and only if the two promises are resolved with the exact
     * same value. If any of the promises are rejected, the new promise will be
     * immediately rejected as well. If both promises are resolved but with
     * different values, the new promise will be rejected.
     *
     * @see Promise#join(Promise)
     * @param <V> Type of the value carried by the promises
     * @param p1 A promise
     * @param p2 A promise
     * @return A promise that will be resolved only if both promises are
     *         resolved with the same value; rejected otherwise
     */
    public static <V> Promise<V> join(Promise<V> p1, Promise<V> p2) {
        return p1.join(p2);
    }

    private static abstract class PromiseListStateChecker<V> {
        protected List<StateSnapshot<V>> mStates;
        protected int mFulfilledCount;
        protected int mRejectedCount;

        protected abstract void onStateChanged(int position);

        public final void checkStates(final List<Promise<V>> promises) {
            mStates = new ArrayList<>(promises.size());
            mFulfilledCount = 0;
            mRejectedCount = 0;

            for (Promise<V> p : promises) {
                mStates.add(p.inspect());
            }

            for (int i = 0; i < promises.size(); i++) {
                final int pos = i;
                final Promise<V> p = promises.get(pos);

                p.then(new Promise.OnFulfilledCallback<V, Void>() {
                    @Override
                    public Future<Void> onFulfilled(V value) throws Exception {
                        mStates.set(pos, p.inspect());
                        mFulfilledCount++;

                        onStateChanged(pos);
                        return null;
                    }
                }, new Promise.OnRejectedCallback<Void>() {
                    @Override
                    public Future<Void> onRejected(Exception reason) throws Exception {
                        mStates.set(pos, p.inspect());
                        mRejectedCount++;

                        onStateChanged(pos);
                        return null;
                    }
                }).done();
            }
        }
    }

    /**
     * Creates a promise that will be resolved when all promises in a supplied
     * set are resolved. If any promise in the list is rejected, the returned
     * promise will also be immediately rejected. This can be used to
     * synchronize a set of parallel operations.
     *
     * @param <V> Type of the value carried by the promises
     * @param promises Set of promises
     * @return A new promise, awaiting the resolution of all promises in the
     *         set
     */
    public static <V> Promise<List<V>> all(final List<Promise<V>> promises) {
        return defer(new DeferredHandler<List<V>>() {
            @Override
            public void handle(final Deferred<List<V>> deferred) {
                if (promises.size() == 0) {
                    deferred.resolve(new ArrayList<V>());
                } else {
                    new PromiseListStateChecker<V>() {
                        @Override
                        public void onStateChanged(int position) {
                            if (mFulfilledCount == mStates.size()) {
                                List<V> result = new ArrayList<>();

                                for (StateSnapshot<V> state : mStates) {
                                    result.add(state.value);
                                }

                                deferred.resolve(result);
                            } else if (mStates.get(position).state == State.REJECTED
                                    && mRejectedCount == 1) {
                                deferred.reject(mStates.get(position).reason);
                            }
                        }
                    }.checkStates(promises);
                }
            }
        });
    }

    /**
     * Creates a promise that will be resolved when all promises in a supplied
     * set are resolved. If any promise in the list is rejected, the returned
     * promise will also be immediately rejected. This can be used to
     * synchronize a set of parallel operations.
     *
     * @param <V> Type of the value carried by the promises
     * @param promises Set of promises
     * @return A new promise, awaiting the resolution of all promises in the
     *         set
     */
    @SafeVarargs
    public static <V> Promise<List<V>> all(final Promise<V>... promises) {
        return all(Arrays.asList(promises));
    }

    /**
     * Creates a promise that will be resolved as soon as any promise in a
     * supplied set is resolved. If all promises in the list are rejected, the
     * returned promise will also be rejected. This can be used to find the
     * quickest way of completing a task that can be carried out in several ways
     * in parallel.
     *
     * @param <V> Type of the value carried by the promises
     * @param promises Set of promises
     * @return A new promise, awaiting the resolution of the single quickest
     *         promise to be resolved in the set
     */
    public static <V> Promise<V> any(final List<Promise<V>> promises) {
        return defer(new DeferredHandler<V>() {
            @Override
            public void handle(final Deferred<V> deferred) {
                if (promises.size() == 0) {
                    deferred.resolve((V)null);
                } else {
                    new PromiseListStateChecker<V>() {
                        @Override
                        public void onStateChanged(int position) {
                            final StateSnapshot<V> state = mStates.get(position);

                            if (state.state == State.FULFILLED && mFulfilledCount == 1) {
                                deferred.resolve(state.value);
                            } else if (mRejectedCount == mStates.size()) {
                                deferred.reject(new Exception(
                                        "Can't get fulfillment value from any promise, all "
                                                + "promises were rejected."));
                            }
                        }
                    }.checkStates(promises);
                }
            }
        });
    }

    /**
     * Creates a promise that will be resolved as soon as all promises in a
     * supplied set are completed, i.e., <i>either</i> resolved or rejected. The
     * returned promise is hence never rejected.
     *
     * @param <V> Type of the value carried by the promises
     * @param promises Set of promises
     * @return A new promise, awaiting the completion of all promises in the set
     */
    public static <V> Promise<List<StateSnapshot<V>>> allSettled(final List<Promise<V>> promises) {
        return defer(new DeferredHandler<List<StateSnapshot<V>>>() {
            @Override
            public void handle(final Deferred<List<StateSnapshot<V>>> deferred) {
                if (promises.size() == 0) {
                    deferred.resolve(new ArrayList<StateSnapshot<V>>());
                } else {
                    new PromiseListStateChecker<V>() {
                        @Override
                        public void onStateChanged(int position) {
                            if (mFulfilledCount + mRejectedCount == mStates.size()) {
                                deferred.resolve(mStates);
                            }
                        }
                    }.checkStates(promises);
                }
            }
        });
    }

    /**
     * Creates a promise that will inherit the state of the first promise in the
     * supplied set to be completed, i.e., either resolved or rejected. This can
     * be used to find the quickest way of completing a task that can be carried
     * out in several ways in parallel, disregarding whether the task completed
     * successfully or not.
     *
     * @param <V> Type of the value carried by the promises
     * @param promises Set of promises
     * @return A new promise, awaiting the completion of the single quickest
     *         promise to be completed in the set
     */
    public static <V> Promise<V> race(final List<Promise<V>> promises) {
        return defer(new DeferredHandler<V>() {
            @Override
            public void handle(final Deferred<V> deferred) {
                new PromiseListStateChecker<V>() {
                    @Override
                    public void onStateChanged(int position) {
                        final StateSnapshot<V> state = mStates.get(position);

                        if (state.state == State.FULFILLED && mFulfilledCount == 1) {
                            deferred.resolve(state.value);
                        } else if (state.state == State.REJECTED && mRejectedCount == 1) {
                            deferred.reject(state.reason);
                        }
                    }
                }.checkStates(promises);
            }
        });
    }
}
