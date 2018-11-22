package com.thewizrd.shared_resources;

import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class AsyncTaskEx<T, X extends Exception> extends AsyncTask<T> {

    public T await(CallableEx<T, X> callable) throws X {
        try {
            return Executors.newSingleThreadExecutor().submit(callable).get();
        } catch (InterruptedException | NullPointerException e) {
            Logger.writeLine(Log.ERROR, e);
            return null;
        } catch (ExecutionException ex) {
            if ((X) ex.getCause() != null)
                throw this.<X>maskException(ex.getCause());
            else
                throw this.<RuntimeException>maskException(ex.getCause());
        }
    }
}
