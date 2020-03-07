package com.thewizrd.shared_resources;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.utils.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncTask<T> {

    public T await(Callable<T> callable) {
        try {
            return Executors.newSingleThreadExecutor().submit(callable).get();
        } catch (InterruptedException | NullPointerException e) {
            Logger.writeLine(Log.ERROR, e);
            return null;
        } catch (ExecutionException ex) {
            throw this.<RuntimeException>maskException(ex.getCause());
        }
    }

    public static void run(Runnable runnable) {
        try {
            new Thread(runnable).start();
        } catch (NullPointerException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    public static void run(final Runnable runnable, final long millisDelay) {
        run(runnable, millisDelay, null);
    }

    public static void run(final Runnable runnable, final long millisDelay, @Nullable final CancellationToken token) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(millisDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (token != null && token.isCancellationRequested())
                        return;

                    runnable.run();
                }
            }).start();
        } catch (NullPointerException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    public static <T> Task<T> create(Callable<T> callable) {
        return Tasks.call(Executors.newSingleThreadExecutor(), callable);
    }

    public static <T> T await(Task<T> task) throws ExecutionException, InterruptedException {
        return Tasks.await(task);
    }

    public static <T> T await(@NonNull Task<T> task, long timeout, @NonNull TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, timeout, unit);
    }

    protected <T extends Throwable> T maskException(Throwable t) throws T {
        throw (T) t;
    }
}

