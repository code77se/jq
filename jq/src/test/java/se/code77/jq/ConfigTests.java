package se.code77.jq;

import org.junit.Test;

import se.code77.jq.config.Config;
import se.code77.jq.util.AsyncTests;
import se.code77.jq.util.BlockingDataHolder;
import se.code77.jq.util.TestEnv;

import static se.code77.jq.util.Assert.*;

public class ConfigTests extends AsyncTests {
    @Test
    public void testDispatcherRegister() {
        final Config config = Config.getConfig();
        final BlockingDataHolder<Config.Dispatcher> dd = new BlockingDataHolder<>();
        final Thread thread = new Thread() {
            @Override
            public void run() {
                dd.set(config.createDispatcher());
            }
        };
        final Config.Dispatcher dispatcher = new Config.Dispatcher() {
            @Override
            public void dispatch(Runnable r) {
            }

            @Override
            public void dispatch(Runnable r, long ms) {
            }
        };

        assertFalse(config.isDispatcherThread(Thread.currentThread()));
        assertTrue(config.isDispatcherThread(TestEnv.getTestThread()));

        config.registerDispatcher(thread, dispatcher);
        assertTrue(config.isDispatcherThread(thread));

        Config.Dispatcher d = config.createDispatcher();
        assertFalse("Should not get dispatcher " + d + " for thread " + Thread.currentThread(),
                d == dispatcher);

        thread.start();
        assertData(dd, 500, dispatcher);

        config.unregisterDispatcher(thread);
        assertFalse(config.isDispatcherThread(thread));
    }

    @Test
    public void testBlockingCallWarning() {
        final BlockingDataHolder<String> warning = new BlockingDataHolder<>();
        final Config config = new Config(false) {
            @Override
            public Logger getLogger() {
                return new Logger() {
                    @Override
                    public void verbose(String s) {
                    }

                    @Override
                    public void debug(String s) {
                    }

                    @Override
                    public void info(String s) {
                    }

                    @Override
                    public void warn(String s) {
                        warning.set(s);
                    }

                    @Override
                    public void error(String s) {
                    }
                };
            }
        };
        final Promise p = JQ.defer().promise;
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    p.get();
                } catch (Exception e) {
                }
            }
        };
        final Config.Dispatcher dispatcher = new Config.Dispatcher() {
            @Override
            public void dispatch(Runnable r) {
            }

            @Override
            public void dispatch(Runnable r, long ms) {
            }
        };

        config.registerDispatcher(thread, dispatcher);
        Config.setConfig(config);
        assertNoData(warning, 100);
        thread.start();
        assertData(warning, 1000);

        thread.interrupt();
        config.unregisterDispatcher(thread);
    }
}
