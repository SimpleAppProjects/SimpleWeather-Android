package com.thewizrd.shared_resources;

import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;

import java.util.concurrent.ExecutionException;

public class AsyncTaskEx<T, X extends Exception> extends AsyncTask<T> {

    public T await(final CallableEx<T, X> callable) throws X {
        try {
            return sThreadPool.submit(callable).get();
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
