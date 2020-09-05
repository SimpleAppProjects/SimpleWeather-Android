package com.thewizrd.shared_resources.lifecycle;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public abstract class LifecycleAwareFragment extends Fragment {
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * Check if the current fragment's lifecycle is alive
     *
     * @return Returns true if fragment's lifecycle is at least initialized
     */
    public final boolean isAlive() {
        return getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED);
    }

    /**
     * Check if the current fragment's view lifecycle is alive
     *
     * @return Returns true if fragment's lifecycle is at least created and the view has been created but not yet destroyed
     */
    public final boolean isViewAlive() {
        boolean isFragmentCreated = getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED);
        if (isFragmentCreated) {
            try {
                return getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED);
            } catch (IllegalStateException ignored) {
                // Can't access the Fragment View's LifecycleOwner when the fragment's getView() is null i.e., before onCreateView() or after onDestroyView()
            }
        }
        return false;
    }

    /**
     * Runs the action on the main UI thread
     *
     * @param action The action to be run
     */
    protected final void runOnUiThread(@NonNull final Runnable action) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            mMainHandler.post(action);
        } else {
            action.run();
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed
     */
    protected final void run(@NonNull final LifecycleRunnable action) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            runOnUiThread(new CheckAliveRunnable(getLifecycle(), action));
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed
     */
    protected final void run(@NonNull final Runnable action) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            runOnUiThread(new CheckAliveRunnable(getLifecycle(), action));
        }
    }

    /**
     * Launches and runs the given runnable when the fragment is in the started
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the fragment's lifecycle
     */
    protected final void runWhenStarted(@NonNull final LifecycleRunnable action) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            runOnUiThread(new CheckAliveRunnable(getLifecycle(), action));
        } else {
            getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                void start() {
                    getLifecycle().removeObserver(this);
                    runOnUiThread(new CheckAliveRunnable(getLifecycle(), action));
                }
            });
        }
    }

    /**
     * Launches and runs the given runnable when the fragment is in the started
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the fragment's lifecycle
     */
    protected final void runWhenStarted(@NonNull final Runnable action) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            runOnUiThread(new CheckAliveRunnable(getLifecycle(), action));
        } else {
            getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                void start() {
                    getLifecycle().removeObserver(this);
                    runOnUiThread(new CheckAliveRunnable(getLifecycle(), action));
                }
            });
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the viewLifeCycleOwner's lifecycle
     */
    protected final void runWithView(@NonNull final LifecycleRunnable action) {
        if (isViewAlive()) {
            runOnUiThread(new CheckAliveRunnable(getViewLifecycleOwner().getLifecycle(), action));
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the viewLifeCycleOwner's lifecycle
     */
    protected final void runWithView(@NonNull final Runnable action) {
        if (isViewAlive()) {
            runOnUiThread(new CheckAliveRunnable(getViewLifecycleOwner().getLifecycle(), action));
        }
    }

    /**
     * Launches and runs the given runnable when the fragment is in the started
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the viewLifeCyclerOwner's lifecycle
     */
    protected final void runWhenViewStarted(@NonNull final LifecycleRunnable action) {
        if (isViewAlive()) {
            runOnUiThread(new CheckAliveRunnable(getViewLifecycleOwner().getLifecycle(), action));
        } else {
            getViewLifecycleOwner().getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                void start() {
                    getViewLifecycleOwner().getLifecycle().removeObserver(this);
                    runOnUiThread(new CheckAliveRunnable(getViewLifecycleOwner().getLifecycle(), action));
                }
            });
        }
    }

    /**
     * Launches and runs the given runnable when the fragment is in the started
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the viewLifeCyclerOwner's lifecycle
     */
    protected final void runWhenViewStarted(@NonNull final Runnable action) {
        if (isViewAlive()) {
            runOnUiThread(new CheckAliveRunnable(getViewLifecycleOwner().getLifecycle(), action));
        } else {
            getViewLifecycleOwner().getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                void start() {
                    getViewLifecycleOwner().getLifecycle().removeObserver(this);
                    runOnUiThread(new CheckAliveRunnable(getViewLifecycleOwner().getLifecycle(), action));
                }
            });
        }
    }
}
