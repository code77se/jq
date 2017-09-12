package se.code77.jq;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import se.code77.jq.JQ.Deferred;
import se.code77.jq.Promise.OnFulfilledCallback;
import se.code77.jq.Promise.UnhandledRejectionException;
import se.code77.jq.util.AsyncTests;
import se.code77.jq.util.BlockingDataHolder;
import se.code77.jq.util.DataFulfilledCallback;
import se.code77.jq.util.DataProgressedCallback;
import se.code77.jq.util.DataRejectedCallback;
import se.code77.jq.util.FinallyCalledCallback;
import se.code77.jq.util.SlowTask;
import se.code77.jq.util.TestConfig;

import static se.code77.jq.util.Assert.*;

public class PromiseTests extends AsyncTests {

    @Test
    public void pending_isPending() throws Exception {
        Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;

        assertPending(p);

        BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        p.then(new DataFulfilledCallback<String, Void>(then1));

        assertNoData(then1, 2000);
        deferred.resolve(TEST_VALUE1);
    }

    @Test
    public void resolved_isResolved() throws Exception {
        Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;

        // Test one-arg then
        BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        p.then(new DataFulfilledCallback<String, Void>(then1));

        // Test two-arg then
        BlockingDataHolder<String> then2 = new BlockingDataHolder<>();
        p.then(new DataFulfilledCallback<String, Void>(then2), null);

        deferred.resolve(TEST_VALUE1);

        assertData(then1, 2000, TEST_VALUE1);
        assertData(then2, 2000, TEST_VALUE1);
        assertResolved(p, TEST_VALUE1);
    }


    @Test
    public void rejected_isRejected() throws Exception {
        Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;

        // Test one-arg fail
        BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();
        p.fail(new DataRejectedCallback<String>(fail1));

        // Test two-arg then
        BlockingDataHolder<Exception> fail2 = new BlockingDataHolder<>();
        p.then(new OnFulfilledCallback<String, Void>() {
            @Override
            public Future<Void> onFulfilled(String value) throws Exception {
                return null;
            }
        }, new DataRejectedCallback<Void>(fail2));

        deferred.reject(newReason(TEST_REASON1));

        assertData(fail1, 2000, newReason(TEST_REASON1));
        assertData(fail2, 2000, newReason(TEST_REASON1));
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void preResolved_isResolved() throws Exception {
        Promise<String> p = JQ.resolve(TEST_VALUE1);

        BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        p.then(new DataFulfilledCallback<String, Void>(then1));

        assertData(then1, 2000, TEST_VALUE1);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void preRejected_isRejected() throws Exception {
        Promise<Void> p = JQ.reject(newReason(TEST_REASON1));

        BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();
        p.fail(new DataRejectedCallback<>(fail1));

        assertData(fail1, 2000, newReason(TEST_REASON1));
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void resolved_isNotRejected() {
        Promise<String> p = JQ.resolve(TEST_VALUE1);

        BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();
        p.fail(new DataRejectedCallback<>(fail1));

        assertNoData(fail1, 2000);
    }

    @Test
    public void rejected_isNotResolved() {
        Promise<Void> p = JQ.reject(newReason(TEST_REASON1));

        BlockingDataHolder<Void> then1 = new BlockingDataHolder<>();
        p.then(new DataFulfilledCallback<Void, Void>(then1));

        assertNoData(then1, 2000);
    }

    @Test
    public void resolved_isImmutable() {
        final Deferred<String> deferred = JQ.defer();

        Promise<String> p = deferred.promise;
        deferred.resolve(TEST_VALUE1);

        deferred.resolve(TEST_VALUE2);
        assertResolved(p, TEST_VALUE1);

        deferred.reject(newReason(TEST_REASON1));
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void rejected_isImmutable() {
        final Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;

        deferred.reject(newReason(TEST_REASON1));

        deferred.reject(newReason(TEST_REASON2));
        assertRejected(p, TEST_REASON1);

        deferred.resolve(TEST_VALUE1);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void deferred_isResolvedWithResolvedPromise() throws Exception {
        final Deferred<Integer> deferred = JQ.defer();
        Promise<Integer> p = deferred.promise;
        Future<Integer> f = JQ.work(new SlowTask<>(42, 500));

        assertPending(p);
        deferred.resolve(f);

        Thread.sleep(1000);
        assertResolved(p, 42);
    }

    @Test
    public void deferred_isResolvedWithRejectedPromise() throws Exception {
        final Deferred<Integer> deferred = JQ.defer();
        Promise<Integer> p = deferred.promise;
        Future<Integer> f = JQ.work(new SlowTask<Integer>(newReason(TEST_REASON1), 500));

        assertPending(p);
        deferred.resolve(f);

        Thread.sleep(1000);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void deferred_isResolvedWithValue() throws Exception {
        final Deferred<Integer> deferred = JQ.defer();
        Promise<Integer> p = deferred.promise;
        Future<Integer> f = Value.wrap(42);

        assertPending(p);
        deferred.resolve(f);

        Thread.sleep(1000);
        assertResolved(p, 42);
    }

    @Test
    public void deferred_isResolvedWithFutureTask() throws Exception {
        final Deferred<Integer> deferred = JQ.defer();
        Promise<Integer> p = deferred.promise;
        FutureTask<Integer> f = new FutureTask<>(new SlowTask<>(42, 500));
        Executors.newSingleThreadExecutor().execute(f);

        assertPending(p);
        deferred.resolve(f);

        Thread.sleep(1000);
        assertResolved(p, 42);
    }

    @Test
    public void deferred_isResolvedWithNull() throws Exception {
        final Deferred<Integer> deferred = JQ.defer();
        Promise<Integer> p = deferred.promise;
        Future<Integer> f = null;

        deferred.resolve(f);

        Thread.sleep(1000);
        assertResolved(p, null);
    }

    @Test
    public void cancel_isNotSupported() {
        final Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;

        assertFalse(p.cancel(true));
        assertFalse(p.isCancelled());
    }

    @Test
    public void resolved_isResolvedSync() throws Exception {
        Promise<String> p = JQ.resolve(TEST_VALUE1);

        assertEquals(TEST_VALUE1, p.get());
    }

    @Test
    public void rejected_isRejectedSync() throws Exception {
        final Promise<Void> p = JQ.reject(newReason(TEST_REASON1));

        Exception e = assertThrows(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                return p.get();
            }
        }, ExecutionException.class);

        assertEquals(newReason(TEST_REASON1), e.getCause());
    }

    @Test
    public void pending_hasTimeoutSync() {
        Deferred<String> deferred = JQ.defer();
        final Promise<String> p = deferred.promise;

        assertThrows(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return p.get(100, TimeUnit.MILLISECONDS);
            }
        }, TimeoutException.class);
    }

    private <T> void resolveChain(final Future<T> future, final T expected) throws InterruptedException {
        final BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        final BlockingDataHolder<T> then2 = new BlockingDataHolder<>();
        final BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).then(
                new DataFulfilledCallback<>(then1, future)).then(
                new DataFulfilledCallback<>(then2)).fail(
                new DataRejectedCallback<>(fail1));

        assertData(then1, 2000, TEST_VALUE1);
        assertData(then2, 2000, expected);
        assertNoData(fail1, 2000);
    }

    @Test
    public void chained_isResolvedWithValue() throws InterruptedException {
        // OnFulfilled returns Value, chained fulfillment handlers are invoked
        resolveChain(Value.wrap(42), 42);
    }

    @Test
    public void chained_isResolvedWithPromise() throws InterruptedException {
        // OnFulfilled returns new Promise, chained fulfillment handlers are invoked

        resolveChain(JQ.work(new SlowTask<>(42)), 42);
    }

    @Test
    public void chained_isResolvedWithFutureTask() throws InterruptedException {
        // OnFulfilled returns java.util.concurrent.Future, chained fulfillment handlers are invoked
        FutureTask<Integer> future = new FutureTask<>(new SlowTask<>(42));
        Executors.newSingleThreadExecutor().execute(future);

        resolveChain(future, 42);
    }

    @Test
    public void chained_isRejectedWithException() {
        // OnFulfilled throws exception, chained fulfillment handlers are not invoked, trailing rejection handler is

        final BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        final BlockingDataHolder<Integer> then2 = new BlockingDataHolder<>();
        final BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).then(
                new DataFulfilledCallback<String, Integer>(then1) {
                    @Override
                    public Future<Integer> onFulfilled(String value) throws Exception {
                        super.onFulfilled(value);
                        throw newReason(TEST_REASON1);
                    }
                }).then(
                new DataFulfilledCallback<>(then2), new DataRejectedCallback<>(fail1));

        assertData(then1, 2000, TEST_VALUE1);
        assertNoData(then2, 2000);
        assertData(fail1, 2000, newReason(TEST_REASON1));
    }

    @Test
    public void chained_isResolvedWithoutHandler() {
        // resolved promise without fulfillment handler -> next promise is resolved with value

        final BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        final BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).fail(
                new DataRejectedCallback<>(fail1)).then(
                new DataFulfilledCallback<>(then1));

        assertData(then1, 2000, TEST_VALUE1);
        assertNoData(fail1, 2000);
    }

    @Test
    public void chained_isRejectedWithoutHandler() {
        // rejected promise without rejection handler  -> next promise is rejected with reason
        // (this is standard case and implicitly tested elsewhere, but for clarity has its own test)

        final BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        final BlockingDataHolder<Integer> then2 = new BlockingDataHolder<>();
        final BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).then(
                new DataFulfilledCallback<String, Integer>(then1) {
                    @Override
                    public Future<Integer> onFulfilled(String value) throws Exception {
                        super.onFulfilled(value);
                        throw newReason(TEST_REASON1);
                    }
                }).then(
                new DataFulfilledCallback<>(then2)).fail(
                new DataRejectedCallback<>(fail1));

        assertData(then1, 2000, TEST_VALUE1);
        assertNoData(then2, 2000);
        assertData(fail1, 2000, newReason(TEST_REASON1));
    }

    @Test
    public void terminated_isRejectedWithoutHandler() throws InterruptedException {
        // This should throw UnhandledRejectionException

        JQ.resolve(TEST_VALUE1).then(
                new OnFulfilledCallback<String, Void>() {
                    @Override
                    public Future<Void> onFulfilled(String value) throws Exception {
                        throw newReason(TEST_REASON1);
                    }
                }).done();

        Thread.sleep(1000);
        UnhandledRejectionException unhandledException = TestConfig.getTestThread().getUnhandledException();
        assertNotNull(unhandledException);
        assertTrue(UnhandledRejectionException.class.isAssignableFrom(unhandledException.getClass()));
    }

    @Test
    public void timeout_isResolved() {
        // new promise is resolved
        BlockingDataHolder<String> then1 = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).timeout(1000).then(new DataFulfilledCallback<String, Void>(then1));

        assertData(then1, 500, TEST_VALUE1);
    }

    @Test
    public void timeout_isRejected() {
        // new promise is rejected
        BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();

        JQ.reject(newReason(TEST_REASON1)).timeout(1000).fail(new DataRejectedCallback<>(fail1));

        assertData(fail1, 500, newReason(TEST_REASON1));

    }

    @Test
    public void timeout_isResolvedAfterTimeout() throws InterruptedException {
        // Promise is resolved but too late, TimeoutException is thrown
        Promise<String> p = JQ.work(new SlowTask<>(TEST_VALUE1, 2000)).timeout(1000);

        BlockingDataHolder<String> then1 = new BlockingDataHolder<>();
        p.then(new DataFulfilledCallback<>(then1));

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertRejected(p, TimeoutException.class);

        assertNoData(then1, 1000);
    }

    @Test
    public void timeout_isRejectedAfterTimeout() throws InterruptedException {
        // Promise is rejected but too late, TimeoutException is thrown
        Promise<String> p = JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 2000)).timeout(1000);

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertRejected(p, TimeoutException.class);
    }

    @Test
    public void delay_isResolved() throws InterruptedException {
        // new promise is resolved after delay
        Promise<String> p = JQ.resolve(TEST_VALUE1).delay(1000);

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void delay_isRejected() throws InterruptedException {
        // new promise is rejected immediately
        Promise<Void> p = JQ.<Void>reject(newReason(TEST_REASON1)).delay(1000);

        Thread.sleep(500);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void delay_isPending() throws InterruptedException {
        // New promise is also pending after delay has passed if original promise is not resolved yet
        Promise<String> p = JQ.work(new SlowTask<>(TEST_VALUE1, 2000)).delay(1000);

        Thread.sleep(1500);
        assertPending(p);

        Thread.sleep(1000);
        assertPending(p);

        Thread.sleep(1000);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void join_isResolvedThisThatEqual() throws InterruptedException {
        // v1 equals v2 -> new promise should be resolved with v1
        Promise<String> p1 = JQ.resolve(TEST_VALUE1);
        Promise<String> p2 = JQ.resolve(TEST_VALUE1);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertResolved(p3, TEST_VALUE1);
    }

    @Test
    public void join_isResolvedThisThatNotEqual() throws InterruptedException {
        // v1 not equals v2 -> new promise should be rejected
        Promise<String> p1 = JQ.resolve(TEST_VALUE1);
        Promise<String> p2 = JQ.resolve(TEST_VALUE2);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertRejected(p3);
    }

    @Test
    public void join_isResolvedThisNull() throws InterruptedException {
        // v1 == null, v2 != null -> new promise should be rejected
        Promise<String> p1 = JQ.resolve(null);
        Promise<String> p2 = JQ.resolve(TEST_VALUE1);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertRejected(p3);
    }

    @Test
    public void join_isResolvedThatNull() throws InterruptedException {
        // v1 != null, v2 == null -> new promise should be rejected
        Promise<String> p1 = JQ.resolve(TEST_VALUE1);
        Promise<String> p2 = JQ.resolve(null);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertRejected(p3);
    }

    @Test
    public void join_isResolvedThisThatNull() throws InterruptedException {
        // v1 == null, v2 == null -> new promise should be resolved with null
        Promise<String> p1 = JQ.resolve(null);
        Promise<String> p2 = JQ.resolve(null);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertResolved(p3, null);
    }

    @Test
    public void join_isThisRejected() throws InterruptedException {
        // this rejected, that resolved -> new promise should be rejected
        Promise<String> p1 = JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 0));
        Promise<String> p2 = JQ.resolve(TEST_VALUE1);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertRejected(p3);
    }

    @Test
    public void join_isThatRejected() throws InterruptedException {
        // this resolved, that rejected -> new promise should be rejected
        Promise<String> p1 = JQ.resolve(TEST_VALUE1);
        Promise<String> p2 = JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 0));
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertRejected(p3);
    }

    @Test
    public void join_isPending() throws InterruptedException {
        // this or that is pending -> new promise should be pending
        Promise<String> p1 = JQ.work(new SlowTask<>(TEST_VALUE1, 1000));
        Promise<String> p2 = JQ.resolve(TEST_VALUE1);
        Promise<String> p3 = p1.join(p2);

        Thread.sleep(500);
        assertPending(p3);
        Thread.sleep(1000);
        assertResolved(p3, TEST_VALUE1);

        p1 = JQ.resolve(TEST_VALUE1);
        p2 = JQ.work(new SlowTask<>(TEST_VALUE1, 1000));
        p3 = p1.join(p2);

        Thread.sleep(500);
        assertPending(p3);
        Thread.sleep(1000);
        assertResolved(p3, TEST_VALUE1);
    }

    @Test
    public void progress_isProgressedAll() {
        Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;
        BlockingDataHolder<Float> progress1 = new BlockingDataHolder<>();
        BlockingDataHolder<Float> progress2 = new BlockingDataHolder<>();

        p.progress(new DataProgressedCallback(progress1));
        p.progress(new DataProgressedCallback(progress2));

        assertNoData(progress1, 1000);
        assertNoData(progress2, 1000);

        deferred.notify(0.5f);

        assertData(progress1, 1000, 0.5f);
        assertData(progress2, 1000, 0.5f);
    }

    @Test
    public void preProgress_isProgressedOne() {
        Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;
        BlockingDataHolder<Float> progress1 = new BlockingDataHolder<>();
        BlockingDataHolder<Float> progress2 = new BlockingDataHolder<>();

        p.progress(new DataProgressedCallback(progress1));
        assertNoData(progress1, 1000);

        deferred.notify(0.5f);
        assertData(progress1, 1000, 0.5f);

        p.progress(new DataProgressedCallback(progress2));
        assertNoData(progress1, 1000);
        assertData(progress2, 1000, 0.5f);
    }

    @Test
    public void noProgress_isNotProgressed() {
        Deferred<String> deferred = JQ.defer();
        Promise<String> p = deferred.promise;
        BlockingDataHolder<Float> progress1 = new BlockingDataHolder<>();

        p.progress(new DataProgressedCallback(progress1));
        assertNoData(progress1, 1000);
    }

    @Test
    public void progress_isNotPropagated() {
        final Deferred<String> deferred1 = JQ.defer();
        final Deferred<String> deferred2 = JQ.defer();
        final Promise<String> p1 = deferred1.promise;
        final Promise<String> p2 = deferred2.promise;
        BlockingDataHolder<Float> progress1 = new BlockingDataHolder<>();

        p1.then(new OnFulfilledCallback<String, String>() {
            @Override
            public Future<String> onFulfilled(String value) throws Exception {
                return p2;
            }
        }).progress(new DataProgressedCallback(progress1));

        deferred1.notify(0.5f);
        assertNoData(progress1, 1000);

        deferred1.resolve(TEST_VALUE1);
        assertNoData(progress1, 1000);

        deferred2.notify(0.3f);
        assertData(progress1, 1000, 0.3f);
    }

    @Test
    public void finally_isCalledForResolved() {
        // Promise is resolved -> fin is called, next promise is resolved with same value
        // Equivalent to:
        // try {
        //     return "Hello";
        // } catch (Exception e) {
        //     // swallow
        // } finally {
        //     finallyCalled = true;
        // }
        // assert(finallyCalled);
        final BlockingDataHolder<Boolean> fin = new BlockingDataHolder<>();
        final BlockingDataHolder<String> then = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).fail(new Promise.OnRejectedCallback<String>() {
            @Override
            public Future<String> onRejected(Exception reason) throws Exception {
                return Value.wrap(TEST_VALUE2);
            }
        }).fin(new FinallyCalledCallback(fin)).then(new DataFulfilledCallback<String, Void>(then));

        assertData(fin, 500, true);
        assertData(then, 500, TEST_VALUE1);
    }

    @Test
    public void finally_isCalledForRejected() {
        // Promise is rejected -> fin is called, next promise is rejected with same reason
        // Equivalent to:
        // try {
        //     throw EX;
        // } catch (Exception e) {
        //     // swallow
        // } finally {
        //     finallyCalled = true;
        // }
        // assert(finallyCalled);
        final BlockingDataHolder<Boolean> fin = new BlockingDataHolder<>();
        final BlockingDataHolder<String> then = new BlockingDataHolder<>();

        JQ.<String>reject(newReason(TEST_REASON1)).fail(new Promise.OnRejectedCallback<String>() {
            @Override
            public Future<String> onRejected(Exception reason) throws Exception {
                return Value.wrap(TEST_VALUE2);
            }
        }).fin(new FinallyCalledCallback(fin)).then(new DataFulfilledCallback<String, Void>(then));

        assertData(fin, 500, true);
        assertData(then, 500, TEST_VALUE2);
    }

    @Test
    public void finally_isCalledAndThrowsForResolved() {
        // Promise is resolved -> fin is called but throws exception, next promise is rejected with that reason
        // Equivalent to:
        // try {
        //     try {
        //         return "Hello";
        //     } catch (Exception e) {
        //         // swallow
        //     } finally {
        //         throw FEX;
        //     }
        // } catch (Exception e) {
        //     assert(FEX == e)
        //     catchCalled = true;
        // }
        // assert(catchCalled);
        final BlockingDataHolder<Exception> fail = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).fail(new Promise.OnRejectedCallback<String>() {
            @Override
            public Future<String> onRejected(Exception reason) throws Exception {
                return Value.wrap(TEST_VALUE2);
            }
        }).fin(new Promise.OnFinallyCallback() {
            @Override
            public void onFinally() throws Exception {
                throw newReason(TEST_REASON2);
            }
        }).fail(new DataRejectedCallback<String>(fail));

        assertData(fail, 500, newReason(TEST_REASON2));
    }

    @Test
    public void finally_isCalledAndThrowsForRejected() {
        // Promise is rejected -> fin is called but throws exception, next promise is rejected with that reason
        // Equivalent to:
        // try {
        //     try {
        //         throw EX;
        //     } catch (Exception e) {
        //         // swallow
        //     } finally {
        //         throw FEX;
        //     }
        // } catch (Exception e) {
        //     assert(FEX == e)
        //     catchCalled = true;
        // }
        // assert(catchCalled);
        final BlockingDataHolder<Exception> fail = new BlockingDataHolder<>();

        JQ.<String>reject(newReason(TEST_REASON1)).fail(new Promise.OnRejectedCallback<String>() {
            @Override
            public Future<String> onRejected(Exception reason) throws Exception {
                return Value.wrap(TEST_VALUE2);
            }
        }).fin(new Promise.OnFinallyCallback() {
            @Override
            public void onFinally() throws Exception {
                throw newReason(TEST_REASON2);
            }
        }).fail(new DataRejectedCallback<String>(fail));

        assertData(fail, 500, newReason(TEST_REASON2));

    }

    @Test
    public void finally_isCalledForResolvedWithoutFail() {
        // Promise is resolved with no fail() -> fin is called, next promise is resolved with same value
        // Equivalent to:
        // try {
        //     return "Hello";
        // } finally {
        //     finallyCalled = true;
        // }
        // assert(finallyCalled);
        final BlockingDataHolder<Boolean> fin = new BlockingDataHolder<>();
        final BlockingDataHolder<String> then = new BlockingDataHolder<>();

        JQ.resolve(TEST_VALUE1).fin(new FinallyCalledCallback(fin)).then(new DataFulfilledCallback<>(then));

        assertData(fin, 500, true);
        assertData(then, 500, TEST_VALUE1);
    }

    @Test
    public void finally_isCalledForRejectedWithoutFail() {
        // Promise is rejected with no fail() -> fin is called, next promise is rejected with same reason
        // Equivalent to:
        // try {
        //     try {
        //         throw EX;
        //     } finally {
        //         finallyCalled = true;
        //     }
        //     assert(false);
        // } catch (Exception e) {
        //     assert(EX == e)
        //     catchCalled = true;
        // }
        // assert(finallyCalled);
        // assert(catchCalled);
        final BlockingDataHolder<Boolean> fin = new BlockingDataHolder<>();
        final BlockingDataHolder<Exception> fail = new BlockingDataHolder<>();

        JQ.<String>reject(newReason(TEST_REASON1)).fin(new FinallyCalledCallback(fin)).fail(new DataRejectedCallback<String>(fail));

        assertData(fin, 500, true);
        assertData(fail, 500, newReason(TEST_REASON1));
    }

    @Test
    public void spread_2() {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2));
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback2<String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2) throws Exception {
                assertEquals(TEST_VALUE1, e1);
                assertEquals(TEST_VALUE2, e2);
                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void spread_3() {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2), JQ.resolve(TEST_VALUE3));
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback3<String, String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2, String e3) throws Exception {
                assertEquals(TEST_VALUE1, e1);
                assertEquals(TEST_VALUE2, e2);
                assertEquals(TEST_VALUE3, e3);
                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void spread_2_to_3() {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2));
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback3<String, String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2, String e3) throws Exception {
                assertEquals(TEST_VALUE1, e1);
                assertEquals(TEST_VALUE2, e2);
                assertEquals(null, e3);
                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void spread_3_to_2() {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2), JQ.resolve(TEST_VALUE3));
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback2<String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2) throws Exception {
                assertEquals(TEST_VALUE1, e1);
                assertEquals(TEST_VALUE2, e2);
                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void spread_resolved() throws InterruptedException {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2), JQ.resolve(TEST_VALUE3));
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        final Promise<Integer> p2 = p.spread(new Promise.OnFulfilledSpreadCallback2<String, String, String, Integer>() {
            @Override
            public Future<Integer> onFulfilled(String e1, String e2) throws Exception {
                spread.set();
                return Value.wrap(42);
            }
        });

        assertData(spread, 500);
        Thread.sleep(500);
        assertResolved(p2, 42);
    }

    @Test
    public void spread_rejected() throws InterruptedException {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2));

        final Promise<Void> p2 = p.spread(new Promise.OnFulfilledSpreadCallback2<String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2) throws Exception {
                throw new IOException();
            }
        });

        Thread.sleep(500);
        assertRejected(p2, IOException.class);
    }

    @Test
    public void spread_invalidCallback() throws InterruptedException {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2));

        final Promise<Void> p2 = p.spread(new Promise.OnFulfilledSpreadCallback<List<String>, Void>() {
            public String onFulfilled(String e1) {
                return null;
            }
        });

        Thread.sleep(500);
        assertRejected(p2, Promise.IllegalSpreadCallbackException.class);
    }

    @Test
    public void spread_missingCallback() throws InterruptedException {
        final Promise<List<String>> p = JQ.all(JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2));

        final Promise<Void> p2 = p.spread(new Promise.OnFulfilledSpreadCallback<List<String>, Void>() {
        });

        Thread.sleep(500);
        assertRejected(p2, Promise.IllegalSpreadCallbackException.class);
    }

    @Test
    public void spread_nonList() throws InterruptedException {
        final Promise<String> p = JQ.resolve(TEST_VALUE1);

        final Promise<Void> p2 = p.spread(new Promise.OnFulfilledSpreadCallback<String, Void>() {
            public Future<Void> onFulfilled(Object e1) {
                return null;
            }
        });

        Thread.sleep(500);
        assertRejected(p2, Promise.IllegalSpreadCallbackException.class);
    }

    @Test
    public void spread_wrongTypes() throws InterruptedException {
        final Promise<List<Number>> p = JQ.all(JQ.<Number>resolve(TEST_DOUBLE1), JQ.<Number>resolve(TEST_INTEGER1));

        final Promise<Void> p2 = p.spread(new Promise.OnFulfilledSpreadCallback2<Number, Integer, Double, Void>() {
            public Future<Void> onFulfilled(Integer e1, Double e2) throws Exception {
                return null;
            }
        });

        Thread.sleep(500);
        assertRejected(p2, Promise.IllegalSpreadCallbackException.class);
    }

    @Test
    public void spread_emptyList() throws InterruptedException {
        final Promise<List<String>> p = JQ.all();
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback2<String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2) throws Exception {
                assertNull(e1);
                assertNull(e2);
                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void spread_nullList() throws InterruptedException {
        final Promise<List<String>> p = JQ.resolve(null);
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback2<String, String, String, Void>() {
            @Override
            public Future<Void> onFulfilled(String e1, String e2) throws Exception {
                assertNull(e1);
                assertNull(e2);
                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void spread_castArgs() {
        final Promise<List<Number>> p = JQ.all(JQ.<Number>resolve(TEST_DOUBLE1), JQ.<Number>resolve(TEST_INTEGER1));
        final BlockingDataHolder<String> spread = new BlockingDataHolder<>();

        p.spread(new Promise.OnFulfilledSpreadCallback2<Number, Double, Integer, Void>() {
            @Override
            public Future<Void> onFulfilled(Double e1, Integer e2) throws Exception {
                assertEquals(TEST_DOUBLE1, e1);
                assertEquals(TEST_INTEGER1, e2);

                spread.set();
                return null;
            }
        });

        assertData(spread, 500);
    }

    @Test
    public void thenResolve() {
        final Promise<String> p = JQ.resolve(1).thenResolve(TEST_VALUE1);
        final BlockingDataHolder<String> then1 = new BlockingDataHolder<>();

        p.then(new DataFulfilledCallback<String, Void>(then1));

        assertData(then1, 2000, TEST_VALUE1);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void thenReject() {
        final Promise<String> p = JQ.resolve(1).thenReject(newReason(TEST_REASON1), String.class);
        final BlockingDataHolder<Exception> fail1 = new BlockingDataHolder<>();

        p.fail(new DataRejectedCallback<String>(fail1));

        assertData(fail1, 2000, newReason(TEST_REASON1));
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void tap_isResolved() {
        final Promise<String> p = JQ.resolve(TEST_VALUE1);
        final BlockingDataHolder<String> tap1 = new BlockingDataHolder<>();

        p.tap(new Promise.OnTapCallback<String>() {
            @Override
            public void onTap(String value) {
                tap1.set(value);
            }
        });

        assertData(tap1, 2000, TEST_VALUE1);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void tap_isRejected() {
        final Promise<String> p = JQ.reject(newReason(TEST_REASON1));
        final BlockingDataHolder<String> tap1 = new BlockingDataHolder<>();

        p.tap(new Promise.OnTapCallback<String>() {
            @Override
            public void onTap(String value) {
                tap1.set(value);
            }
        });

        assertNoData(tap1, 2000);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void tap_isTapExceptionIgnored() {
        final Promise<String> p = JQ.resolve(TEST_VALUE1);
        final BlockingDataHolder<String> then1 = new BlockingDataHolder<>();

        p.tap(new Promise.OnTapCallback<String>() {
            @Override
            public void onTap(String value) {
                throw new RuntimeException("Should be ignored");
            }
        }).then(new DataFulfilledCallback<>(then1));

        assertData(then1, 2000, TEST_VALUE1);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void onFulfilled_returnsWildcardFuture() {
        final BlockingDataHolder<CharSequence> then1 = new BlockingDataHolder<>();
        final Promise<CharSequence> p = JQ.resolve().then(new OnFulfilledCallback<Void, CharSequence>() {
            @Override
            public Future<? extends CharSequence> onFulfilled(Void value) throws Exception {
                return Value.wrap(TEST_VALUE2);
            }
        });

        p.then(new DataFulfilledCallback<CharSequence, Void>(then1));

        assertData(then1, 2000, TEST_VALUE2);
        assertResolved(p, TEST_VALUE2);

    }
}
