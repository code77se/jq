package se.code77.jq.util;

import java.util.concurrent.Future;

import se.code77.jq.Promise.OnRejectedCallback;

public class DataRejectedCallback<NV> extends DataRejectedBaseCallback<Exception, NV> implements OnRejectedCallback<NV> {
    public DataRejectedCallback(BlockingDataHolder<Exception> holder) {
        super(holder);
    }

    public DataRejectedCallback(BlockingDataHolder<Exception> holder, Future<NV> nextValue) {
        super(holder, nextValue);
    }
}
