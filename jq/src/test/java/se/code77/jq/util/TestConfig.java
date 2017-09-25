package se.code77.jq.util;

import java.util.PriorityQueue;

import se.code77.jq.Promise.UnhandledRejectionException;
import se.code77.jq.config.Config;
import se.code77.jq.config.Config.LogLevel;

public class TestConfig {
    private static TestThread sTestThread;

    public static TestThread getTestThread() {
        return sTestThread;
    }

    public static synchronized void start() {
        if (sTestThread == null) {
            sTestThread = new TestThread("Async event thread");
            sTestThread.start();

            Config config = new Config(false, LogLevel.VERBOSE) {
                @Override
                protected Dispatcher getDefaultDispatcher() {
                    return getDispatcher(sTestThread);
                }

                @Override
                public Logger getLogger() {
                    return new TestLogger(LogLevel.DEBUG);
                }
            }.registerDispatcher(sTestThread, new TestDispatcher(sTestThread));

            Config.setConfig(config);
        }
    }

    public static synchronized void stop() {
        if (sTestThread != null) {
            try {
                sTestThread.waitForIdle();
                sTestThread.exit();
                sTestThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sTestThread = null;
        }
    }

    public static synchronized void waitForIdle() throws InterruptedException {
        if (sTestThread != null) {
            sTestThread.waitForIdle();
        }
    }

    private static class TestDispatcher implements Config.Dispatcher {

        private TestThread mThread;

        public TestDispatcher(TestThread t) {
            mThread = t;
        }

        public TestDispatcher() {
            mThread = (TestThread) Thread.currentThread();
        }

        @Override
        public void dispatch(Runnable r) {
            dispatch(r, 0);
        }

        @Override
        public void dispatch(Runnable r, long ms) {
            mThread.addEvent(r, ms);
        }

    }

    public static class TestThread extends Thread {
        public TestThread(String name) {
            super(name);
        }

        private static class Event implements Comparable<Event> {
            public final Runnable r;
            public final long due;

            public Event(Runnable r, long due) {
                this.r = r;
                this.due = due;
            }

            @Override
            public int compareTo(Event o) {
                return (int) (due - o.due);
            }
        }

        private final PriorityQueue<Event> mEvents = new PriorityQueue<Event>();
        private boolean mStopped;
        private boolean mIdle = true;
        private final Object mIdleLock = new Object();
        private UnhandledRejectionException mUnhandledException;

        private synchronized Event getEvent() throws InterruptedException {
            while (!Thread.interrupted()) {
                Event e = mEvents.peek();

                setIdle(e == null);

                if (e == null) {
                    wait();
                } else {
                    long timeLeft = e.due - System.currentTimeMillis();

                    if (timeLeft > 0) {
                        wait(timeLeft);
                    } else {
                        return mEvents.poll();
                    }
                }
            }

            throw new InterruptedException();
        }

        @Override
        public void run() {
            System.out.println("Starting event thread");
            while (!mStopped) {
                try {
                    Event event = getEvent();

                    try {
                        event.r.run();
                    } catch (UnhandledRejectionException e) {
                        System.out.println("Exception in dispatched event " + e);
                        mStopped = true;
                        mUnhandledException = e;
                    }
                } catch (InterruptedException ex) {
                }
            }
            System.out.println("Exiting event thread");
        }

        public void exit() {
            System.out.println("exit");
            mStopped = true;
            interrupt();
        }

        public synchronized void addEvent(Runnable r, long ms) {
            mEvents.add(new Event(r, System.currentTimeMillis() + ms));
            notifyAll();
        }

        private void setIdle(boolean idle) {
            synchronized (mIdleLock) {
                mIdle = idle;
                mIdleLock.notifyAll();
            }
        }

        public void waitForIdle() throws InterruptedException {
            waitForIdle(100);
        }

        public void waitForIdle(long margin) throws InterruptedException {
            if (mStopped) {
                return;
            }
            synchronized (mIdleLock) {
                do {
                    while (!mIdle) {
                        mIdleLock.wait();
                    }
                    Thread.sleep(margin);
                } while (!mIdle);
            }
        }

        public UnhandledRejectionException getUnhandledException() {
            return mUnhandledException;
        }
    }

    private static class TestLogger implements Config.Logger {
        private final LogLevel mLogLevel;

        public TestLogger(LogLevel logLevel) {
            mLogLevel = logLevel;
        }

        private boolean hasLevel(LogLevel level) {
            return mLogLevel.ordinal() <= level.ordinal();
        }

        private void log(LogLevel level, String s) {
            if (hasLevel(level)) {
                System.out.println(System.currentTimeMillis() + " [PROMISE." + level + "] "
                        + s);
            }
        }

        @Override
        public void verbose(String s) {
            log(LogLevel.VERBOSE, s);
        }

        @Override
        public void debug(String s) {
            log(LogLevel.DEBUG, s);
        }

        @Override
        public void info(String s) {
            log(LogLevel.INFO, s);
        }

        @Override
        public void warn(String s) {
            log(LogLevel.WARN, s);
        }

        @Override
        public void error(String s) {
            log(LogLevel.ERROR, s);
        }

    }
}
