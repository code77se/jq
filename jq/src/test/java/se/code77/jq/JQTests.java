package se.code77.jq;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static se.code77.jq.util.Assert.*;

import se.code77.jq.Promise.State;
import se.code77.jq.Promise.StateSnapshot;
import se.code77.jq.util.AsyncTests;
import se.code77.jq.util.SlowTask;
import se.code77.jq.util.TestEnv;

public class JQTests extends AsyncTests {
    @Test
    public void resolve_isResolved() {
        Promise<String> p = JQ.resolve(TEST_VALUE1);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void resolveVoid_isResolved() {
        Promise<Void> p = JQ.resolve();
        assertResolved(p, null);
    }

    @Test
    public void reject_isRejected() throws InterruptedException {
        Promise<Void> p = JQ.reject(newReason(TEST_REASON1));
        TestEnv.waitForIdle();
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void all_isResolvedList() throws InterruptedException {
        Promise<List<String>> p = JQ.all(
                Arrays.asList(JQ.resolve(TEST_VALUE1), JQ.work(new SlowTask<>(TEST_VALUE2, 1000))));

        Thread.sleep(500);
        assertPending(p);
        Thread.sleep(1000);
        assertResolved(p, Arrays.asList(TEST_VALUE1, TEST_VALUE2));
    }

    @Test
    public void all_isResolvedList_mixedPromisesValues() throws InterruptedException {
        Promise<List<String>> p = JQ.all(
                Arrays.asList(Value.wrap(TEST_VALUE1), JQ.work(new SlowTask<>(TEST_VALUE2, 1000))));

        Thread.sleep(500);
        assertPending(p);
        Thread.sleep(1000);
        assertResolved(p, Arrays.asList(TEST_VALUE1, TEST_VALUE2));
    }

    @Test
    public void all_isResolvedList_onlyValues() throws InterruptedException {
        Promise<List<String>> p = JQ.all(
                Arrays.asList(Value.wrap(TEST_VALUE1), Value.wrap(TEST_VALUE2)));

        Thread.sleep(500);
        assertResolved(p, Arrays.asList(TEST_VALUE1, TEST_VALUE2));
    }

    @Test
    public void all_isResolvedVarArg() throws InterruptedException {
        Promise<List<String>> p = JQ.all(
                JQ.resolve(TEST_VALUE1), JQ.resolve(TEST_VALUE2));

        TestEnv.waitForIdle();
        Thread.sleep(500);
        assertResolved(p, Arrays.asList(TEST_VALUE1, TEST_VALUE2));
    }

    @Test
    public void all_isRejected() throws InterruptedException {
        Promise<List<String>> p = JQ.all(
                JQ.resolve(TEST_VALUE1), JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 1000)));

        Thread.sleep(500);
        assertPending(p);
        Thread.sleep(1500);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void all_isPending() throws InterruptedException {
        // at least one promise is not done -> resulting is pending forever
        Promise<List<String>> p = JQ.all(
                JQ.work(new SlowTask<>(TEST_VALUE1, 1000)), JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 2000)));

        Thread.sleep(500);
        assertPending(p);
        Thread.sleep(1000);
        assertPending(p);
    }

    @Test
    public void any_isResolvedByFirst() throws InterruptedException {
        Promise<String> p = JQ.any(
                Arrays.asList(JQ.work(new SlowTask<>(TEST_VALUE1, 100)), JQ.work(new SlowTask<>(TEST_VALUE2, 1000))));

        Thread.sleep(500);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void any_isResolvedByFirstAfterOtherRejected() throws InterruptedException {
        Promise<String> p = JQ.any(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 200)),
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 100))));

        Thread.sleep(500);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void any_isResolvedByLast() throws InterruptedException {
        Promise<String> p = JQ.any(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 1000)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 100))));

        Thread.sleep(500);
        assertResolved(p, TEST_VALUE2);
    }

    @Test
    public void any_isResolvedByLastAfterOtherRejected() throws InterruptedException {
        Promise<String> p = JQ.any(
                Arrays.asList(
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 100)),
                        JQ.work(new SlowTask<>(TEST_VALUE1, 200))));

        Thread.sleep(500);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void any_isRejected() throws InterruptedException {
        Promise<String> p = JQ.any(
                Arrays.asList(
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 100)),
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON2), 200))));

        Thread.sleep(500);
        assertRejected(p);
    }

    @Test
    public void any_isPending() throws InterruptedException {
        Promise<String> p = JQ.any(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 1000)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 2000))));

        Thread.sleep(500);
        assertPending(p);
    }

    @Test
    public void race_isResolvedByFirst() throws InterruptedException {
        Promise<String> p = JQ.race(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 100)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 1000))));

        Thread.sleep(500);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void race_isResolvedByLast() throws InterruptedException {
        Promise<String> p = JQ.race(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 1000)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 100))));

        Thread.sleep(500);
        assertResolved(p, TEST_VALUE2);
    }

    @Test
    public void race_isRejectedByFirst() throws InterruptedException {
        Promise<String> p = JQ.race(
                Arrays.asList(
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 100)),
                        JQ.work(new SlowTask<>(TEST_VALUE1, 1000))));

        Thread.sleep(500);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void race_isRejectedByLast() throws InterruptedException {
        Promise<String> p = JQ.race(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 1000)),
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 100))));

        Thread.sleep(500);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void race_isPending() throws InterruptedException {
        Promise<String> p = JQ.race(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 1000)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 2000))));

        Thread.sleep(500);
        assertPending(p);
    }

    @Test
    public void allSettled_isResolvedAllResolved() throws InterruptedException {
        Promise<List<StateSnapshot<String>>> p = JQ.allSettled(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 100)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 1000))));

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertResolved(p, Arrays.asList(
                new StateSnapshot<>(State.FULFILLED, TEST_VALUE1, null),
                new StateSnapshot<>(State.FULFILLED, TEST_VALUE2, null)));
    }

    @Test
    public void allSettled_isResolvedAllResolvedOrRejected() throws InterruptedException {
        Promise<List<StateSnapshot<String>>> p = JQ.allSettled(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 100)),
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 200)),
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON2), 1000))));

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertResolved(p, Arrays.asList(
                new StateSnapshot<>(State.FULFILLED, TEST_VALUE1, null),
                new StateSnapshot<String>(State.REJECTED, null, newReason(TEST_REASON1)),
                new StateSnapshot<String>(State.REJECTED, null, newReason(TEST_REASON2))));
    }

    @Test
    public void allSettled_isResolvedAllRejected() throws InterruptedException {
        Promise<List<StateSnapshot<String>>> p = JQ.allSettled(
                Arrays.asList(
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON1), 100)),
                        JQ.work(new SlowTask<String>(newReason(TEST_REASON2), 1000))));

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertResolved(p, Arrays.asList(
                new StateSnapshot<String>(State.REJECTED, null, newReason(TEST_REASON1)),
                new StateSnapshot<String>(State.REJECTED, null, newReason(TEST_REASON2))));
    }

    @Test
    public void allSettled_isPending() throws InterruptedException {
        Promise<List<StateSnapshot<String>>> p = JQ.allSettled(
                Arrays.asList(
                        JQ.work(new SlowTask<>(TEST_VALUE1, 100)),
                        JQ.work(new SlowTask<>(TEST_VALUE2, 1000))));

        Thread.sleep(500);
        assertPending(p);
    }

    @Test
    public void wrap_isReturnedForPromise() {
        Future<String> future = JQ.resolve(TEST_VALUE1);

        assertEquals(future, JQ.wrap(future));
    }

    @Test
    public void wrap_isResolvedForValue() throws InterruptedException {
        Future<String> future = Value.wrap(TEST_VALUE1);

        Promise<String> p = JQ.wrap(future);

        TestEnv.waitForIdle();
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void wrap_isResolvedForNull() throws InterruptedException {
        Promise<String> p = JQ.wrap(null);

        TestEnv.waitForIdle();
        assertResolved(p, null);
    }

    @Test
    public void wrap_isResolvedForFutureTaskCompleted() throws InterruptedException {
        FutureTask<String> future = new FutureTask<>(new SlowTask<>(TEST_VALUE1, 1000));
        Executors.newSingleThreadExecutor().execute(future);

        Promise<String> p = JQ.wrap(future);

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertResolved(p, TEST_VALUE1);
    }

    @Test
    public void wrap_isRejectedForFutureTaskFailedWithException() throws InterruptedException {
        FutureTask<String> future = new FutureTask<>(new SlowTask<String>(newReason(TEST_REASON1), 1000));
        Executors.newSingleThreadExecutor().execute(future);

        Promise<String> p = JQ.wrap(future);

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertRejected(p, TEST_REASON1);
    }

    @Test
    public void wrap_isRejectedForFutureTaskFailedWithError() throws InterruptedException {
        FutureTask<String> future = new FutureTask<>(new SlowTask<String>(new Error("Foobar!"), 1000));
        Executors.newSingleThreadExecutor().execute(future);

        Promise<String> p = JQ.wrap(future);

        Thread.sleep(500);
        assertPending(p);

        Thread.sleep(1000);
        assertRejected(p, ExecutionException.class);
    }

    @Test
    public void wrap_isRejectedForFutureTaskInterrupted() throws InterruptedException {
        FutureTask<String> future = new FutureTask<>(new SlowTask<>(TEST_VALUE1, 1000));
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(future);

        Promise<String> p = JQ.wrap(future);

        Thread.sleep(500);
        assertPending(p);

        es.shutdownNow();

        Thread.sleep(500);
        assertRejected(p, InterruptedException.class);
    }

    @Test
    public void wrap_isRejectedForFutureTaskCancelled() throws InterruptedException {
        FutureTask<String> future = new FutureTask<>(new SlowTask<>(TEST_VALUE1, 1000));
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(future);

        Promise<String> p = JQ.wrap(future);

        Thread.sleep(500);
        assertPending(p);

        future.cancel(true);

        Thread.sleep(500);
        assertRejected(p, CancellationException.class);
    }

    // when, fail, done, timeout, delay are just convenience wrappers. That's not evident from a black box pov, but are they worth writing tests for?
}
