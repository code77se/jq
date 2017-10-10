package se.code77.jq.util;

import java.util.concurrent.Future;

import se.code77.jq.Promise;

public class DataRejectedBaseCallback<E extends Exception, NV> extends DataCallback<E, NV> implements Promise.OnRejectedBaseCallback<E, NV> {
    public DataRejectedBaseCallback(BlockingDataHolder<E> holder) {
        super(holder);
    }

    public DataRejectedBaseCallback(BlockingDataHolder<E> holder, Future<NV> nextValue) {
        super(holder, nextValue);
    }

    @Override
    public Future<NV> onRejected(E reason) throws Exception {
        mHolder.set(reason);
        return mNextValue;
    }
}
