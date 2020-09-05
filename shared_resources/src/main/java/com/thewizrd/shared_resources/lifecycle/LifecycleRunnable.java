package com.thewizrd.shared_resources.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.CancellationException;

public abstract class LifecycleRunnable implements Runnable, LifecycleEventObserver {
    private Lifecycle lifecycle;
    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    private void cancel() {
        active = false;
    }

    public void ensureActive() throws CancellationException {
        if (!isActive()) {
            throw new CancellationException();
        }
    }

    public LifecycleRunnable(@NonNull Lifecycle lifecycle) {
        this.lifecycle = lifecycle;

        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            cancel();
        }
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (lifecycle.getCurrentState().compareTo(Lifecycle.State.DESTROYED) <= 0) {
            lifecycle.removeObserver(this);
            cancel();
        }
    }
}
