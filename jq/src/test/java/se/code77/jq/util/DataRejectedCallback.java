package se.code77.jq.util;

import java.util.concurrent.Future;

import se.code77.jq.Promise.OnRejectedCallback;

public class DataRejectedCallback<NV> extends DataCallback<Exception, NV> implements OnRejectedCallback {
    public DataRejectedCallback(BlockingDataHolder<Exception> holder) {
        super(holder);
    }

    public DataRejectedCallback(BlockingDataHolder<Exception> holder, Future<NV> nextValue) {
        super(holder, nextValue);
    }

    @Override
    public Future<NV> onRejected(Exception reason) throws Exception {
        mHolder.set(reason);
        return mNextValue;
    }
}
