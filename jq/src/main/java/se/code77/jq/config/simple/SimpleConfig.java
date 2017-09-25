package se.code77.jq.config.simple;

import se.code77.jq.config.Config;

public class SimpleConfig extends Config {
    private final EventThread mMainThread;

    public SimpleConfig(boolean monitorUnterminated, LogLevel logLevel, EventThread mainThread) {
        super(monitorUnterminated, logLevel);

        mMainThread = mainThread;

        registerDispatcher(mainThread, new EventDispatcher(mainThread));
    }

    public SimpleConfig(boolean monitorUnterminated, EventThread mainThread) {
        this(monitorUnterminated, LogLevel.WARN, mainThread);
    }

    @Override
    protected Dispatcher getDefaultDispatcher() {
        return getDispatcher(mMainThread);
    }

    @Override
    public Logger getLogger() {
        return new AbstractLogger(logLevel) {
            @Override
            protected void log(LogLevel level, String msg) {
                System.out.println(System.currentTimeMillis() + " [PROMISE." + level + "] "
                        + msg);
            }
        };
    }
}
