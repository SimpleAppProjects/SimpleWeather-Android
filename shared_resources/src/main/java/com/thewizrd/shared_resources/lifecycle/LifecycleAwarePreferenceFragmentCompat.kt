package com.thewizrd.shared_resources.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class LifecycleAwarePreferenceFragmentCompat : PreferenceFragmentCompat() {
    /**
     * Check if the current fragment's lifecycle is alive
     *
     * @return Returns true if fragment's lifecycle is at least initialized
     */
    val isAlive: Boolean
        get() = lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)

    /**
     * Check if the current fragment's view lifecycle is alive
     *
     * @return Returns true if fragment's lifecycle is at least created and the view has been created but not yet destroyed
     */
    val isViewAlive: Boolean
        get() {
            val isFragmentCreated = lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
            if (isFragmentCreated) {
                try {
                    return viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)
                } catch (ignored: IllegalStateException) {
                    // Can't access the Fragment View's LifecycleOwner when the fragment's getView() is null i.e., before onCreateView() or after onDestroyView()
                }
            }
            return false
        }

    /**
     * Runs the action on the main UI thread
     *
     * @param action The action to be run
     */
    protected fun runOnUiThread(action: Runnable) {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            action.run()
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed
     */
    protected fun run(action: Runnable) {
        lifecycleScope.launch {
            action.run()
        }
    }

    /**
     * Launches and runs the given runnable when the fragment is in the started
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the fragment's lifecycle
     */
    protected fun runWhenStarted(action: Runnable) {
        lifecycleScope.launchWhenStarted {
            action.run()
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the viewLifeCycleOwner's lifecycle
     */
    protected fun runWithView(action: Runnable) {
        runCatching {
            viewLifecycleOwner.lifecycleScope.launch {
                action.run()
            }
        }
    }

    /**
     * Launches and runs the given runnable when the fragment is in the started
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param action The runnable to be executed; Should be initialized with the viewLifeCyclerOwner's lifecycle
     */
    protected fun runWhenViewStarted(action: Runnable) {
        runCatching {
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                action.run()
            }
        }
    }
}