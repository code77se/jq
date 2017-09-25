package se.code77.jq.util;

import se.code77.jq.Promise;
import se.code77.jq.config.Config;
import se.code77.jq.config.Config.LogLevel;
import se.code77.jq.config.simple.EventDispatcher;
import se.code77.jq.config.simple.EventThread;

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
            }.registerDispatcher(sTestThread, new EventDispatcher(sTestThread));

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

    private static class TestThread extends EventThread {
        private Promise.UnhandledRejectionException mUnhandledException;

        public TestThread(String name) {
            super(name);
        }

        @Override
        protected void handleEvent(Event event) {
            try {
                super.handleEvent(event);
            } catch (Promise.UnhandledRejectionException e) {
                log("Exception in dispatched event " + e);
                mUnhandledException = e;
                exit();
            }
        }

        public Promise.UnhandledRejectionException getUnhandledException() {
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
