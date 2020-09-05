package com.thewizrd.shared_resources.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

public final class CheckAliveRunnable implements Runnable {
    private Runnable runner;
    private Lifecycle lifecycle;

    public CheckAliveRunnable(@NonNull Lifecycle lifecycle, @NonNull Runnable runnable) {
        this.lifecycle = lifecycle;
        this.runner = runnable;
    }

    @Override
    public void run() {
        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            runner.run();
        }
    }
}
