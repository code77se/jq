package se.code77.jq.util;

import se.code77.jq.Promise;
import se.code77.jq.config.Config;
import se.code77.jq.config.Config.LogLevel;
import se.code77.jq.config.simple.EventThread;
import se.code77.jq.config.simple.SimpleConfig;

public class TestEnv {
    public static class TestThread extends EventThread {
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

    private static TestThread sTestThread;

    public static TestThread getTestThread() {
        return sTestThread;
    }

    public static synchronized void start() {
        if (sTestThread == null) {
            sTestThread = new TestThread("Async event thread");
            sTestThread.start();

            Config.setConfig(new SimpleConfig(false, LogLevel.VERBOSE, sTestThread));
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
}
