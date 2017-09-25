package se.code77.jq;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import se.code77.jq.util.AsyncTests;
import se.code77.jq.util.BlockingDataHolder;

import static se.code77.jq.util.Assert.assertData;
import static se.code77.jq.util.Assert.assertEquals;
import static se.code77.jq.util.Assert.assertFalse;
import static se.code77.jq.util.Assert.assertNotNull;
import static se.code77.jq.util.Assert.assertRejected;
import static se.code77.jq.util.Assert.assertSame;
import static se.code77.jq.util.Assert.assertTrue;

public class ValueTests extends AsyncTests {

    @Test
    public void value_get() {
        Value<String> value = Value.wrap(TEST_VALUE1);

        assertEquals(TEST_VALUE1, value.get());
    }

    @Test
    public void value_getWithTimeout() {
        Value<String> value = Value.wrap(TEST_VALUE1);

        assertEquals(TEST_VALUE1, value.get(1, TimeUnit.NANOSECONDS));
    }

    @Test
    public void value_cancel() {
        Value<String> value = Value.wrap(TEST_VALUE1);

        assertEquals(false, value.cancel(true));
        assertEquals(false, value.cancel(false));
    }

    @Test
    public void value_isCancelled() {
        Value<String> value = Value.wrap(TEST_VALUE1);

        assertEquals(false, value.isCancelled());
        assertEquals(false, value.cancel(true));
        assertEquals(false, value.isCancelled());
    }

    @Test
    public void value_isDone() {
        Value<String> value = Value.wrap(TEST_VALUE1);

        assertEquals(true, value.isDone());
        assertEquals(false, value.cancel(true));
        assertEquals(true, value.isDone());
    }

    private <T> void setFlag(BlockingDataHolder<T> holder, T data) {
        if (holder != null) {
            holder.set(data);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Test
    public void value_wrapVoid() {
        try {
            final BlockingDataHolder<String> holder = new BlockingDataHolder<>();

            Value<Void> v = Value.wrap(new Value.VoidCallable() {
                @Override
                public void call() throws Exception {
                    setFlag(holder, TEST_VALUE1);
                }
            });

            assertEquals(Value.VOID, v);
            assertData(holder, 100, TEST_VALUE1);
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Test
    public void value_wrapVoidThrows() {
        final BlockingDataHolder<Exception> holder = new BlockingDataHolder<>();

        try {
            Value<Void> v = Value.wrap(new Value.VoidCallable() {
                @Override
                public void call() throws Exception {
                    setFlag(null, null);
                }
            });

            assertTrue(false);
        } catch (Exception e) {
            holder.set(e);
        }

        assertData(holder, 100);
    }
}
