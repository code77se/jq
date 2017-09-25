package se.code77.jq.config.simple;

import se.code77.jq.config.Config;

public class EventDispatcher implements Config.Dispatcher {
    private final EventThread mThread;

    public EventDispatcher(EventThread t) {
        mThread = t;
    }

    @Override
    public final void dispatch(Runnable r) {
        dispatch(r, 0);
    }

    @Override
    public final void dispatch(Runnable r, long ms) {
        mThread.post(r, ms);
    }

}
