package com.thewizrd.simpleweather;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.helpers.WindowColorsInterface;

public abstract class CustomPreferenceFragmentCompat extends PreferenceFragmentCompat
        implements OnBackPressedFragmentListener {

    protected Toolbar mToolbar;
    protected AppCompatActivity mActivity;
    protected WindowColorsInterface mWindowColorsIface;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
        mWindowColorsIface = (WindowColorsInterface) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
        mWindowColorsIface = null;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    protected abstract @StringRes
    int getTitle();

    @Override
    public void onResume() {
        super.onResume();

        if (mWindowColorsIface != null)
            mWindowColorsIface.setWindowBarColors(Colors.SIMPLEBLUE);

        // Title
        mToolbar.setTitle(getTitle());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_settings, container, false);

        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        mToolbar = root.findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onBackPressed();
            }
        });

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        inflatedView.setLayoutParams(lp);

        root.addView(inflatedView);

        return root;
    }
}
