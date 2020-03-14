package com.thewizrd.simpleweather.helpers;

import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionSet;

public class TransitionHelper {
    private static final float SCALE_OPEN_ENTER = 0.85f;
    private static final float SCALE_OPEN_EXIT = 1.15f;
    private static final float SCALE_CLOSE_ENTER = 1.1f;
    private static final float SCALE_CLOSE_EXIT = 0.9f;


    public static void onCreate(@NonNull Fragment f) {
        Transition enterTransition = new TransitionSet()
                .addTransition(new Fade(Fade.IN).setStartDelay(35).setDuration(50).setInterpolator(new LinearInterpolator()))
                .addTransition(new Scale(SCALE_OPEN_ENTER, SCALE_OPEN_ENTER).setDuration(300).setInterpolator(new FastOutSlowInInterpolator()));
        Transition returnTransition = new TransitionSet()
                .addTransition(new Fade(Fade.IN).setStartDelay(66).setDuration(50).setInterpolator(new LinearInterpolator()))
                .addTransition(new Scale(SCALE_CLOSE_ENTER, SCALE_CLOSE_ENTER).setDuration(300).setInterpolator(new FastOutSlowInInterpolator()));

        f.setEnterTransition(enterTransition);
        f.setExitTransition(null);
        f.setReenterTransition(returnTransition);
        f.setReturnTransition(null);

        f.postponeEnterTransition();
    }

    public static void onViewCreated(@NonNull final Fragment f, @NonNull final ViewGroup view, @Nullable final OnPrepareTransitionListener listener) {
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                if (listener != null) {
                    listener.prepareTransitions((Transition) f.getEnterTransition(), (Transition) f.getExitTransition(), (Transition) f.getReenterTransition(), (Transition) f.getReturnTransition());
                }
                f.startPostponedEnterTransition();
                return true;
            }
        });
    }

    public interface OnPrepareTransitionListener {
        void prepareTransitions(@Nullable Transition enterTransition, @Nullable Transition exitTransition, @Nullable Transition reenterTransition, @Nullable Transition returnTransition);
    }
}
