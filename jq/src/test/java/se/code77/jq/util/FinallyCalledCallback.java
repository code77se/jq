package se.code77.jq.util;

import java.util.concurrent.Future;

import se.code77.jq.Promise;

public class FinallyCalledCallback extends DataCallback<Boolean, Void> implements Promise.OnFinallyFutureCallback {
    public FinallyCalledCallback(BlockingDataHolder<Boolean> holder) {
        super(holder);
    }

    public FinallyCalledCallback(BlockingDataHolder<Boolean> holder, Future<Void> nextValue) {
        super(holder, nextValue);
    }

    @Override
    public Future<Void> onFinally() throws Exception {
        mHolder.set(true);

        return mNextValue;
    }
}
