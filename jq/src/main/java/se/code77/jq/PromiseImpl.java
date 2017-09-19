
package se.code77.jq;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import se.code77.jq.JQ.Deferred;
import se.code77.jq.config.Config;
import se.code77.jq.config.Config.Dispatcher;
import se.code77.jq.config.Config.Logger;

class PromiseImpl<V> implements Promise<V> {
    static class RejectionInfo {
        public enum Source {
            DEFERRED,
            PARENT,
            HANDLER
        }

        private final Source source;
        private final Promise<?> parentPromise;
        private final Link<?, ?> link;

        public RejectionInfo(Source source, Promise<?> parentPromise, Link<?, ?> link) {
            this.source = source;
            this.parentPromise = parentPromise;
            this.link = link;
        }

        public RejectionInfo(Source source) {
            this(source, null, null);
        }
    }
    private static final class Link<V, NV> {
        private final OnFulfilledCallback<V, NV> onFulfilledCallback;
        private final OnRejectedCallback<NV> onRejectedCallback;
        private final OnProgressedCallback onProgressedCallback;
        private final Dispatcher dispatcher;
        @SuppressWarnings("rawtypes")
        private final PromiseImpl nextPromise; // Raw type necessary

        private Link(
                OnFulfilledCallback<V, NV> onFulfilledCallback,
                OnRejectedCallback<NV> onRejectedCallback,
                OnProgressedCallback onProgressedCallback, Dispatcher dispatcher,
                PromiseImpl<NV> nextPromise) {
            this.onFulfilledCallback = onFulfilledCallback;
            this.onRejectedCallback = onRejectedCallback;
            this.onProgressedCallback = onProgressedCallback;
            this.dispatcher = dispatcher;
            this.nextPromise = nextPromise;
        }

        @Override
        public String toString() {
            return "Link@" + Integer.toHexString(hashCode()) + ": " + onFulfilledCallback + ", " + onRejectedCallback + ", " + nextPromise;
        }
    }

    private final List<Link<V, ?>> mLinks = new ArrayList<>();
    private final boolean mTerminate;
    private StateSnapshot<V> mState;
    private Float mProgress;
    private final String mConstructionStack;

    private PromiseImpl(boolean terminate) {
        mState = new StateSnapshot<V>(State.PENDING, null, null);
        mTerminate = terminate;
        mConstructionStack = isDebug() ? getStackTrace(new Exception("Dummy exception"), true) : null;
    }

    PromiseImpl() {
        this(false);
    }

    @Override
    public StateSnapshot<V> inspect() {
        return mState;
    }

    @Override
    public boolean isFulfilled() {
        return mState.state == State.FULFILLED;
    }

    @Override
    public boolean isRejected() {
        return mState.state == State.REJECTED;
    }

    @Override
    public boolean isPending() {
        return mState.state == State.PENDING;
    }

    @Override
    public final <NV> Promise<NV> then(
            OnFulfilledCallback<V, NV> onFulfilled, OnRejectedCallback<NV> onRejected, OnProgressedCallback onProgressed) {
        if (onFulfilled == null) {
            throw new NullPointerException("Fulfilled handler must not be null");
        }

        return addLink(onFulfilled, onRejected, onProgressed, false);
    }
    @Override
    public final <NV> Promise<NV> then(
            OnFulfilledCallback<V, NV> onFulfilled, OnRejectedCallback<NV> onRejected) {
        return then(onFulfilled, onRejected, null);
    }

    @Override
    public <NV> Promise<NV> then(OnFulfilledCallback<V, NV> onFulfilled) {
        return then(onFulfilled, null);
    }

    @Override
    public <NV> Promise<NV> thenResolve(final NV nextValue) {
        return then(new OnFulfilledCallback<V, NV>() {
            @Override
            public Future<NV> onFulfilled(V value) throws Exception {
                return Value.wrap(nextValue);
            }
        });
    }

    @Override
    public <NV> Promise<NV> thenReject(final Exception reason, Class<NV> nextValueClass) {
        return then(new OnFulfilledCallback<V, NV>() {
            @Override
            public Future<NV> onFulfilled(V value) throws Exception {
                throw reason;
            }
        });
    }

    @Override
    public Promise<V> tap(final OnTapCallback<V> onTap) {
        return then(new OnFulfilledCallback<V, V>() {
            @Override
            public Future<V> onFulfilled(V value) throws Exception {
                try {
                    onTap.onTap(value);
                } catch (RuntimeException e) {
                    // Swallow
                }
                return Value.wrap(value);
            }
        });
    }

    @Override
    public <NV> Promise<NV> spread(final OnFulfilledSpreadCallback<V, NV> onFulfilled, OnRejectedCallback<NV> onRejected) {
        if (onFulfilled == null) {
            throw new NullPointerException("Fulfilled handler must not be null");
        }

        return then(new OnFulfilledCallback<V, NV>() {
            @Override
            public Future<NV> onFulfilled(V value) throws Exception {
                final Method m = getOnFulfilledMethod(onFulfilled);
                final Object[] args = new Object[m.getParameterTypes().length];

                if (value instanceof List) {
                    List<?> values = (List<?>) value;

                    for (int i = 0; i < args.length && i < values.size(); i++) {
                        args[i] = values.get(i);
                    }
                } else if (value != null) {
                    throw new IllegalSpreadCallbackException("Resolved value for spread callback is not a List");
                }

                try {
                    return (Future<NV>) m.invoke(onFulfilled, args);
                } catch (InvocationTargetException ite) {
                    Throwable e = ite.getTargetException();

                    if (e instanceof Exception) {
                        throw (Exception) e;
                    } else {
                        throw (Error) e;
                    }
                } catch (Exception e) {
                    throw new IllegalSpreadCallbackException("Could not invoke spread callback", e);
                }
            }
        }, onRejected);
    }

    @Override
    public <NV> Promise<NV> spread(final OnFulfilledSpreadCallback<V, NV> onFulfilled) {
        return spread(onFulfilled, null);
    }

    @Override
    public final Promise<V> fail(OnRejectedCallback<V> onRejected) {
        return addLink(null, onRejected, null, false);
    }

    @Override
    public final Promise<V> progress(OnProgressedCallback onProgressed) {
        return addLink(null, null, onProgressed, false);
    }

    @Override
    public Promise<V> fin(final OnFinallyCallback onFinally) {
        return then(new OnFulfilledCallback<V, V>() {
            @Override
            public Future<V> onFulfilled(V value) throws Exception {
                onFinally.onFinally();

                return Value.wrap(value);
            }
        }, new OnRejectedCallback<V>() {
            @Override
            public Future<V> onRejected(Exception reason) throws Exception {
                onFinally.onFinally();

                throw reason;
            }
        });
    }

    @Override
    public Promise<V> fin(final OnFinallyFutureCallback onFinally) {
        return then(new OnFulfilledCallback<V, V>() {
            @Override
            public Future<V> onFulfilled(final V value) throws Exception {
                return JQ.wrap(onFinally.onFinally()).then(new OnFulfilledCallback<Void, V>() {
                    @Override
                    public Future<? extends V> onFulfilled(Void v) throws Exception {
                        return Value.wrap(value);
                    }
                });
            }
        }, new OnRejectedCallback<V>() {
            @Override
            public Future<V> onRejected(final Exception reason) throws Exception {
                return JQ.wrap(onFinally.onFinally()).then(new OnFulfilledCallback<Void, V>() {
                    @Override
                    public Future<? extends V> onFulfilled(Void v) throws Exception {
                        throw reason;
                    }
                });
            }
        });
    }

    @Override
    public final synchronized void done(
            OnFulfilledCallback<V, Void> onFulfilled, OnRejectedCallback<Void> onRejected) {
        // This terminates the chain by making the next promise terminating,
        // meaning it will throw unhandled exceptions instead of pass them
        // on.
        addLink(onFulfilled, onRejected, null, true);
    }

    @Override
    public final void done(OnFulfilledCallback<V, Void> onFulfilled) {
        done(onFulfilled, (OnRejectedCallback<Void>) null);
    }

    @Override
    public final void done() {
        done((OnFulfilledCallback<V, Void>) null);
    }

    @Override
    public Promise<V> timeout(final long ms) {
        final Deferred<V> deferred = JQ.defer();

        getDispatcher().dispatch(new Runnable() {
            @Override
            public void run() {
                if (deferred.promise.isPending()) {
                    deferred.reject(new TimeoutException("Promise timed out"));
                }
            }
        }, ms);

        then(new OnFulfilledCallback<V, Void>() {
            @Override
            public Future<Void> onFulfilled(V value) throws Exception {
                if (deferred.promise.isPending()) {
                    deferred.resolve(value);
                }
                return null;
            }
        }, new OnRejectedCallback<Void>() {
            @Override
            public Future<Void> onRejected(Exception reason) throws Exception {
                if (deferred.promise.isPending()) {
                    deferred.reject(reason);
                }
                return null;
            }
        }).done();

        return deferred.promise;
    }

    @Override
    public Promise<V> delay(final long ms) {
        return then(new OnFulfilledCallback<V, V>() {
            @Override
            public Future<V> onFulfilled(final V value) throws Exception {
                final Deferred<V> deferred = JQ.defer();

                getDispatcher().dispatch(new Runnable() {
                    @Override
                    public void run() {
                        deferred.resolve(value);
                    }
                }, ms);

                return deferred.promise;
            }
        });
    }

    @Override
    public Promise<V> join(Promise<V> that) {
        return JQ.all(Arrays.asList(this, that)).then(new OnFulfilledCallback<List<V>, V>() {
            @Override
            public Future<V> onFulfilled(List<V> value) throws Exception {
                final V v1 = value.get(0);
                final V v2 = value.get(1);

                if (v1 == null && v2 == null) {
                    return null;
                } else if (v1 != null && v1.equals(v2)) {
                    return Value.wrap(v1);
                } else {
                    throw new Exception("Can't join promises for " + v1 + " and " + v2);
                }
            }
        });
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
        warnBlocking();

        while (isPending()) {
            wait();
        }

        if (isRejected()) {
            throw new ExecutionException(mState.reason);
        } else {
            return mState.value;
        }
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException,
            TimeoutException {
        warnBlocking();

        long now = System.currentTimeMillis();
        long expires = now + unit.toMillis(timeout);

        while (isPending() && now < expires) {
            wait(expires - now);
            now = System.currentTimeMillis();
        }

        if (isPending()) {
            throw new TimeoutException();
        } else if (isRejected()) {
            throw new ExecutionException(mState.reason);
        } else {
            return mState.value;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return !isPending();
    }

    @Override
    public String toString() {
        return getLogPrefix() + mState.toString() + (mTerminate ? ", TERMINATE" : "");
    }

    private void warnBlocking() {
        if (Config.getConfig().isDispatcherThread(Thread.currentThread())) {
            warn("Calling blocking method on dispatcher thread may cause deadlock.");
        }
    }

    synchronized void _resolve(V value) {
        if (!isPending()) {
            warn("Resolving non-pending promise is a no-op, ignoring");
            return;
        }

        mState = new StateSnapshot<>(State.FULFILLED, value, null);
        info("fulfilled with value '" + value + "'");
        notifyAll();
        handleCompletion();
    }

    synchronized void _reject(Exception reason, RejectionInfo info) {
        if (!isPending()) {
            warn("Rejecting non-pending promise is a no-op, ignoring");
            return;
        }

        mState = new StateSnapshot<>(State.REJECTED, null, reason);
        info("rejected with reason '" + reason + "'");

        if (isDebug()) {
            switch (info.source) {
                case DEFERRED:
                    debug("rejection source: deferred was rejected with:\n" + getStackTrace(reason));
                    break;

                case PARENT:
                    debug("rejection source: parent promise (" + info.parentPromise + ") was rejected");
                    break;

                case HANDLER:
                    debug("rejection source: exception thrown by "
                            + (info.parentPromise.isRejected() ? "rejection" : "fulfillment")
                            + " handler:\n" + getStackTrace(reason));
                    break;
            }

            if (mConstructionStack != null) {
                debug("rejected promise was created:\n" + mConstructionStack);
            }
        }

        notifyAll();
        handleCompletion();
    }

    synchronized void _notify(float progress) {
        if (!isPending()) {
            warn("Updating progress on non-pending promise is a no-op, ignoring");
            return;
        }

        mProgress = progress;
        info("notified with progress " + progress);
        handleProgress(progress);
    }

    @SuppressWarnings("unchecked")
    private synchronized final <NV> Promise<NV> addLink(OnFulfilledCallback<V, NV> onFulfilled,
                                                        OnRejectedCallback<NV> onRejected, OnProgressedCallback onProgress, boolean terminate) {
        Link<V, NV> link = new Link<>(onFulfilled, onRejected, onProgress,
                getDispatcher(),
                new PromiseImpl<NV>(terminate));

        if (isPending()) {
            mLinks.add(link);
            debug("Link added: " + link);

            if (mProgress != null) {
                handleProgress(link, mProgress);
            }
        } else {
            handleCompletion(link);
        }

        return link.nextPromise;
    }

    private void handleCompletion() {
        if (mTerminate) {
            if (isRejected()) {
                throw new UnhandledRejectionException(mState.reason);
            }
        } else {
            if (Config.getConfig().monitorUnterminated && mLinks.isEmpty()) {
                getDispatcher().dispatch(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (PromiseImpl.this) {
                            if (mLinks.isEmpty()) {
                                warn("Unterminated promise has been completed for 10 seconds without any observer");
                                warn("Promise state: " + mState.toString());
                            }
                        }
                    }
                }, 10 * 1000);
            }

            for (final Link<V, ?> link : mLinks) {
                handleCompletion(link);
            }
            mLinks.clear();
        }
    }

    private void handleCompletion(final Link<V, ?> link) {
        debug("Handling completion for link: " + link);

        link.dispatcher.dispatch(new Runnable() {
            @SuppressWarnings({
                    "unchecked", "rawtypes"
            })
            @Override
            public void run() {
                try {
                    Future<?> nextValue;

                    debug("[" + mState + "]: Handling link " + link);

                    if (isRejected()) {
                        if (link.onRejectedCallback != null) {
                            nextValue = link.onRejectedCallback.onRejected(mState.reason);
                            verbose("Link.onRejected returned " + nextValue);
                        } else {
                            verbose("Link has no onRejected, forward to next promise");
                            rejectChild(link, mState.reason, RejectionInfo.Source.PARENT);
                            return;
                        }
                    } else {
                        if (link.onFulfilledCallback != null) {
                            nextValue = link.onFulfilledCallback.onFulfilled(mState.value);
                            verbose("Link.onFulfilled returned " + nextValue);
                        } else {
                            verbose("Link has no onFulfilled, forward to next promise");
                            resolveChild(link, mState.value);
                            return;
                        }
                    }

                    if (nextValue != null) {
                        verbose("Link returned future, next promise will inherit");
                        JQ.wrap(nextValue).then(new OnFulfilledCallback() {
                            @Override
                            public Future onFulfilled(Object value) throws Exception {
                                resolveChild(link, value);
                                return null;
                            }
                        }, new OnRejectedCallback() {
                            @Override
                            public Future onRejected(Exception reason) throws Exception {
                                rejectChild(link, reason, RejectionInfo.Source.PARENT);
                                return null;
                            }
                        }, new OnProgressedCallback() {
                            @Override
                            public void onProgressed(float progress) {
                                notifyChild(link, progress);
                            }
                        }).done();
                    } else {
                        verbose("Link returned null, next promise will resolve directly");
                        resolveChild(link, null);
                    }
                } catch (UnhandledRejectionException e) {
                    throw e;
                } catch (Exception reason) {
                    verbose("Link handler threw an exception");
                    rejectChild(link, reason, RejectionInfo.Source.HANDLER);
                }
            }
        });
    }

    private void resolveChild(Link<V, ?> link, Object value) {
        //noinspection unchecked
        link.nextPromise._resolve(value);
    }

    private void rejectChild(Link<V, ?> link, Exception reason, RejectionInfo.Source source) {
        link.nextPromise._reject(
                reason,
                new RejectionInfo(source, this, link));
    }

    private void notifyChild(Link<V, ?> link, float progress) {
        link.nextPromise._notify(progress);
    }

    private boolean isDebug() {
        switch (Config.getConfig().logLevel) {
            case DEBUG:
            case VERBOSE:
                return true;

            default:
                return false;
        }
    }

    private String getStackTrace(Exception e, boolean stripInternal) {
        String trace = getStackTrace(e);

        if (stripInternal) {
            String[] lines = trace.split("\\n");
            int firstLine = 1;

            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].contains("PromiseImpl.java") && !lines[i].contains("JQ.java")) {
                    firstLine = i;
                    break;
                }
            }

            StringBuilder sb = new StringBuilder();

            for (int i = firstLine; i < lines.length; i++) {
                sb.append(lines[i]).append("\n");
            }

            return sb.toString();
        }

        return trace;
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

    private void handleProgress(float progress) {
        for (final Link<V, ?> link : mLinks) {
            handleProgress(link, progress);
        }
    }

    private void handleProgress(final Link<V, ?> link, final float progress) {
        debug("Handling progress for link: " + link);

        link.dispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                if (link.onProgressedCallback != null) {
                    link.onProgressedCallback.onProgressed(progress);
                }
            }
        });
    }

    private <NV> Method getOnFulfilledMethod(OnFulfilledSpreadCallback<V, NV> onFulfilled) throws IllegalSpreadCallbackException {
        for (Method m : onFulfilled.getClass().getMethods()) {
            if (m.getName().equals("onFulfilled") && m.getReturnType().equals(Future.class)) {
                return m;
            }
        }

        throw new IllegalSpreadCallbackException("Spread callback has no valid onFulfilled method");
    }

    private Dispatcher getDispatcher() {
        return Config.getConfig().createDispatcher();
    }

    private Logger getLogger() {
        return Config.getConfig().getLogger();
    }

    private String getLogPrefix() {
        return "Promise@" + Integer.toHexString(hashCode()) + ": ";
    }

    private void verbose(String s) {
        getLogger().verbose(getLogPrefix() + s);
    }

    private void debug(String s) {
        getLogger().debug(getLogPrefix() + s);
    }

    private void info(String s) {
        getLogger().info(getLogPrefix() + s);
    }

    private void warn(String s) {
        getLogger().warn(getLogPrefix() + s);
    }
}