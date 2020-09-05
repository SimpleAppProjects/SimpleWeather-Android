package com.thewizrd.shared_resources;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.utils.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncTask<T> {

    protected static ExecutorService sThreadPool = Executors.newCachedThreadPool();

    public T await(@NonNull final Callable<T> callable) {
        try {
            return sThreadPool.submit(callable).get();
        } catch (InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
            return null;
        } catch (ExecutionException ex) {
            throw this.<RuntimeException>maskException(ex.getCause());
        }
    }

    public static void run(@NonNull final Runnable runnable) {
        sThreadPool.submit(runnable);
    }

    public static void run(@NonNull final Runnable runnable, final long millisDelay) {
        run(runnable, millisDelay, null);
    }

    public static void run(@NonNull final Runnable runnable, final long millisDelay, @Nullable final CancellationToken token) {
        sThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(millisDelay);
                } catch (InterruptedException e) {
                    Logger.writeLine(Log.ERROR, e);
                }

                if (token != null && token.isCancellationRequested())
                    return;

                runnable.run();
            }
        });
    }

    public static <T> Task<T> create(@NonNull final Callable<T> callable) {
        final TaskCompletionSource<T> tcs = new TaskCompletionSource<>();

        sThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    T result = callable.call();
                    tcs.setResult(result);
                } catch (Exception e) {
                    tcs.setException(e);
                }
            }
        });

        return tcs.getTask();
    }

    public static <T> T await(@NonNull Task<T> task) throws ExecutionException, InterruptedException {
        return Tasks.await(task);
    }

    public static <T> T await(@NonNull Task<T> task, long timeout, @NonNull TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, timeout, unit);
    }

    protected <T extends Throwable> T maskException(Throwable t) throws T {
        throw (T) t;
    }
}

