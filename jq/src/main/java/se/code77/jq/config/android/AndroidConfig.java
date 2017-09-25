
package se.code77.jq.config.android;

import se.code77.jq.config.Config;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class AndroidConfig extends Config {
    public AndroidConfig(boolean monitorUnterminated) {
        this(monitorUnterminated, LogLevel.WARN);
    }

    public AndroidConfig(boolean monitorUnterminated, LogLevel logLevel) {
        super(monitorUnterminated, logLevel);

        Looper mainLooper = Looper.getMainLooper();

        registerDispatcher(mainLooper.getThread(), new AndroidDispatcher(mainLooper));
    }

    public static class AndroidDispatcher implements Dispatcher {
        protected final Handler mHandler;

        public AndroidDispatcher(Looper looper) {
            mHandler = new Handler(looper);
        }

        @Override
        public void dispatch(Runnable r) {
            mHandler.post(r);
        }

        @Override
        public void dispatch(Runnable r, long ms) {
            mHandler.postDelayed(r, ms);
        }
    }

    public static class AndroidLogger extends AbstractLogger {
        private final String mTag;

        public AndroidLogger(LogLevel logLevel) {
            this(logLevel, "Promise");
        }

        public AndroidLogger(LogLevel logLevel, String tag) {
            super(logLevel);
            mTag = tag;
        }

        @Override
        protected void log(LogLevel level, String msg) {
            switch (level) {
                case VERBOSE:
                    Log.v(mTag, msg);
                    break;

                case DEBUG:
                    Log.d(mTag, msg);
                    break;

                case INFO:
                    Log.i(mTag, msg);
                    break;

                case WARN:
                    Log.w(mTag, msg);
                    break;

                case ERROR:
                    Log.e(mTag, msg);
                    break;
            }
        }
    }

    @Override
    protected Dispatcher getDefaultDispatcher() {
        return getDispatcher(Looper.getMainLooper().getThread());
    }

    @Override
    public Logger getLogger() {
        return new AndroidLogger(logLevel);
    }
}
