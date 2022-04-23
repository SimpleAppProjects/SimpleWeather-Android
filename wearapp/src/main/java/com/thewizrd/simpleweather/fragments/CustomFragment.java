package com.thewizrd.simpleweather.fragments;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import com.thewizrd.shared_resources.ApplicationLibKt;
import com.thewizrd.shared_resources.lifecycle.LifecycleAwareFragment;
import com.thewizrd.shared_resources.utils.SettingsManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class CustomFragment extends LifecycleAwareFragment {

    @IntDef({
            Toast.LENGTH_SHORT,
            Toast.LENGTH_LONG
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ToastDuration {
    }

    private FragmentActivity mActivity;
    private final SettingsManager settingsMgr = ApplicationLibKt.getAppLib().getSettingsManager();

    public final FragmentActivity getFragmentActivity() {
        return mActivity;
    }

    protected final SettingsManager getSettingsManager() {
        return settingsMgr;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        super.onDestroy();
    }

    public void showToast(@StringRes int resId, @ToastDuration int duration) {
        runWithView(() -> {
            if (mActivity != null) {
                Toast.makeText(mActivity, resId, duration).show();
            }
        });
    }

    public void showToast(CharSequence message, @ToastDuration int duration) {
        runWithView(() -> {
            if (mActivity != null && isVisible()) {
                Toast.makeText(mActivity, message, duration).show();
            }
        });
    }
}
