package se.code77.jq.util;

import org.junit.After;
import org.junit.Before;

public class AsyncTests {
    protected static final String TEST_VALUE1 = "Hello";
    protected static final String TEST_VALUE2 = "World";
    protected static final String TEST_VALUE3 = "JQ";
    protected static final Integer TEST_INTEGER1 = 42;
    protected static final Double TEST_DOUBLE1 = 1.0;
    protected static final String TEST_REASON1 = "Reason 1";
    protected static final String TEST_REASON2 = "Reason 2";

    public static class TestException extends Exception {
        public TestException(String message) {
            super(message);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TestException &&
                    ((TestException)o).getMessage().equals(getMessage());
        }
    }

    public static TestException newReason(String reason) {
        return new TestException(reason);
    }

    @Before
    public void setup() {
        TestConfig.start();
    }

    @After
    public void tearDown() {
        TestConfig.stop();
    }
}
