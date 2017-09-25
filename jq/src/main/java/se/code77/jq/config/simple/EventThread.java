package se.code77.jq.config.simple;

import java.util.PriorityQueue;

public class EventThread extends Thread {
    public EventThread(String name) {
        super(name);
    }

    protected static final class Event implements Comparable<Event> {
        public final Runnable r;
        public final long due;

        public Event(Runnable r, long due) {
            this.r = r;
            this.due = due;
        }

        @Override
        public int compareTo(Event o) {
            return (int) (due - o.due);
        }
    }

    private final PriorityQueue<Event> mEvents = new PriorityQueue<Event>();
    private boolean mStopped;
    private boolean mIdle = true;
    private final Object mIdleLock = new Object();

    protected void log(String msg) {
        System.out.println(msg);
    }

    private final synchronized Event getEvent() throws InterruptedException {
        while (!Thread.interrupted()) {
            Event e = mEvents.peek();

            setIdle(e == null);

            if (e == null) {
                wait();
            } else {
                long timeLeft = e.due - System.currentTimeMillis();

                if (timeLeft > 0) {
                    wait(timeLeft);
                } else {
                    return mEvents.poll();
                }
            }
        }

        throw new InterruptedException();
    }

    @Override
    public final void run() {
        log("Starting event thread");
        while (!mStopped) {
            try {
                Event event = getEvent();

                handleEvent(event);
            } catch (InterruptedException ex) {
            }
        }
        log("Exiting event thread");
    }

    protected void handleEvent(Event event) {
        event.r.run();
    }

    public final void exit() {
        log("exit");
        mStopped = true;
        interrupt();
    }

    public final synchronized void post(Runnable r, long ms) {
        mEvents.add(new Event(r, System.currentTimeMillis() + ms));
        notifyAll();
    }

    private final void setIdle(boolean idle) {
        synchronized (mIdleLock) {
            mIdle = idle;
            mIdleLock.notifyAll();
        }
    }

    public final void waitForIdle() throws InterruptedException {
        waitForIdle(100);
    }

    public final void waitForIdle(long margin) throws InterruptedException {
        if (mStopped) {
            return;
        }
        synchronized (mIdleLock) {
            do {
                while (!mIdle) {
                    mIdleLock.wait();
                }
                Thread.sleep(margin);
            } while (!mIdle);
        }
    }
}
