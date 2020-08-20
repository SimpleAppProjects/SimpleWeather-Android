package androidx.core.app;

import android.app.job.JobParameters;
import android.app.job.JobServiceEngine;
import android.app.job.JobWorkItem;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.thewizrd.shared_resources.utils.Logger;

/**
 * Implementation of a JobServiceEngine for interaction with SafeJobIntentService.
 */
@RequiresApi(26)
public final class SafeJobServiceEngineImpl extends JobServiceEngine
        implements JobIntentService.CompatJobEngine {
    static final String TAG = "JobServiceEngineImpl";

    static final boolean DEBUG = false;

    final JobIntentService mService;
    final Object mLock = new Object();
    JobParameters mParams;

    final class WrapperWorkItem implements JobIntentService.GenericWorkItem {
        final JobWorkItem mJobWork;

        WrapperWorkItem(JobWorkItem jobWork) {
            mJobWork = jobWork;
        }

        @Override
        public Intent getIntent() {
            return mJobWork.getIntent();
        }

        @Override
        public void complete() {
            synchronized (mLock) {
                if (mParams != null) {
                    try {
                        mParams.completeWork(mJobWork);
                    } catch (SecurityException s) {
                        Logger.writeLine(Log.ERROR, s);
                    }
                }
            }
        }
    }

    SafeJobServiceEngineImpl(JobIntentService service) {
        super(service);
        mService = service;
    }

    @Override
    public IBinder compatGetBinder() {
        return getBinder();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (DEBUG) Log.d(TAG, "onStartJob: " + params);
        mParams = params;
        // We can now start dequeuing work!
        mService.ensureProcessorRunningLocked(false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (DEBUG) Log.d(TAG, "onStartJob: " + params);
        boolean result = mService.doStopCurrentWork();
        synchronized (mLock) {
            // Once we return, the job is stopped, so its JobParameters are no
            // longer valid and we should not be doing anything with them.
            mParams = null;
        }
        return result;
    }

    /**
     * Dequeue some work.
     */
    @Override
    public JobIntentService.GenericWorkItem dequeueWork() {
        JobWorkItem work = null;
        synchronized (mLock) {
            if (mParams == null) {
                return null;
            }
            try {
                work = mParams.dequeueWork();
            } catch (SecurityException s) {
                Logger.writeLine(Log.ERROR, s);
            }
        }
        if (work != null) {
            work.getIntent().setExtrasClassLoader(mService.getClassLoader());
            return new WrapperWorkItem(work);
        } else {
            return null;
        }
    }
}
