package com.thewizrd.shared_resources;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.utils.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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

    public static Task<Void> create(Callable<Void> callable) {
        return Tasks.call(Executors.newSingleThreadExecutor(), callable);
    }

    protected <T extends Throwable> T maskException(Throwable t) throws T {
        throw (T) t;
    }
}

