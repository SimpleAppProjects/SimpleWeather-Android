package com.thewizrd.shared_resources;

public interface CallableEx<T, X extends Exception> extends java.util.concurrent.Callable<T> {
    @Override
    T call() throws X;
}
