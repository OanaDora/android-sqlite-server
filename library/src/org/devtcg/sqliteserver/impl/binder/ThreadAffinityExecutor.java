package org.devtcg.sqliteserver.impl.binder;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * This creates thread affinity with the client (all requests executing by this client on
 * the same thread will be matched onto a unique thread on the server side).  This is
 * important because {@link android.database.sqlite.SQLiteDatabase} stores state on a
 * per thread basis.
 */
public class ThreadAffinityExecutor<V> {
    private final Looper mLooper;
    private final Handler mHandler;

    public ThreadAffinityExecutor(Looper looper) {
        mLooper = looper;
        mHandler = new Handler(looper);
    }

    /**
     * Executes the Runnable on this executor's dedicated thread and wait for the result.
     *
     * @param callable Callable to run on the designated thread.
     * @return Callable result.
     * @throws ExecutionException Wrapped exception rethrown from the {@code callable}.
     */
    public V runSynchronously(Callable<V> callable) throws ExecutionException {
        if (Looper.myLooper() == mLooper) {
            return runOnCurrentThread(callable);
        } else {
            return runOnHandlerThread(callable);
        }
    }

    private V runOnCurrentThread(Callable<V> callable) throws ExecutionException {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    private V runOnHandlerThread(final Callable<V> callable) throws ExecutionException {
        // TODO: optimize this garbage (specifically: remove allocations).
        final ResultHolder<V> resultHolder = new ResultHolder<V>();
        final CountDownLatch resultLatch = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    resultHolder.result = callable.call();
                } catch (Exception e) {
                    resultHolder.exception = e;
                } finally {
                    resultLatch.countDown();
                }
            }
        });
        awaitUninterruptibly(resultLatch);
        if (resultHolder.exception != null) {
            throw new ExecutionException(resultHolder.exception);
        } else {
            return resultHolder.result;
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                // Restart loop...
            }
        }
    }

    public void shutdown() {
        // This is abrupt but in our execution model this should only be called when
        // the client either dies, or an orderly shutdown occurs.
        mLooper.quit();
    }

    private static class ResultHolder<Value> {
        public Value result;
        public Exception exception;
    }
}
