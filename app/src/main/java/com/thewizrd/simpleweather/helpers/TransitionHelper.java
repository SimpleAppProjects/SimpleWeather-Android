package com.thewizrd.simpleweather.helpers;

import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.transition.Scale;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class TransitionHelper {
    private static final float SCALE_OPEN_ENTER = 0.85f;
    private static final float SCALE_OPEN_EXIT = 1.15f;
    private static final float SCALE_CLOSE_ENTER = 1.1f;
    private static final float SCALE_CLOSE_EXIT = 0.9f;

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void onCreate(@NonNull Fragment f) {
        TransitionSet enterTransition = new TransitionSet()
                .addTransition(new Fade(Fade.IN).setStartDelay(35).setDuration(50).setInterpolator(new LinearInterpolator()));
        Scale scaleIn = (Scale) new Scale().setDuration(300).setInterpolator(new FastOutSlowInInterpolator());
        scaleIn.setEntering(true);
        enterTransition.addTransition(scaleIn);

        TransitionSet returnTransition = new TransitionSet()
                .addTransition(new Fade(Fade.IN).setStartDelay(66).setDuration(50).setInterpolator(new LinearInterpolator()));
        Scale scaleOut = (Scale) new Scale().setDuration(300).setInterpolator(new FastOutSlowInInterpolator());
        scaleOut.setEntering(false);
        returnTransition.addTransition(scaleOut);

        f.setEnterTransition(enterTransition);
        f.setExitTransition(null);
        f.setReenterTransition(returnTransition);
        f.setReturnTransition(null);

        f.postponeEnterTransition();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

    /**
     * Manual shared element transition
     * Based on implementation:
     * https://medium.com/@aitorvs/android-shared-element-transitions-for-all-b90e9361507d
     **/
    public static final String ARGS_TRANSITION = "extra_ani";

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void performElementTransition(@NonNull Fragment f, @NonNull View sharedView) {
        if (f.requireArguments().containsKey(ARGS_TRANSITION)) {
            Bundle mStartValues = f.requireArguments().getBundle(ARGS_TRANSITION);
            prepareScene(mStartValues, sharedView);
            runEnterAnimation(sharedView);
            f.requireArguments().remove(ARGS_TRANSITION);
        }
    }

    /**
     * Helper method to capture the view values to animate
     *
     * @param view target view
     * @return Bundle with the captured values
     */
    public static Bundle captureElementValues(@NonNull View view) {
        Bundle b = new Bundle();

        captureScaleValues(b, view);
        captureScreenLocationValues(b, view);
        return b;
    }

    private static void captureScaleValues(@NonNull Bundle b, @NonNull View view) {
        b.putInt("width", view.getWidth());
        b.putInt("height", view.getHeight());
    }

    private static void captureScreenLocationValues(@NonNull Bundle b, @NonNull View view) {
        int[] screenLocation = new int[2];
        view.getLocationOnScreen(screenLocation);
        b.putInt("left", screenLocation[0]);
        b.putInt("top", screenLocation[1]);
    }

    /**
     * This method preps the scene. Captures the end values, calculates deltas with start values and
     * reposition the view in the target layout
     */
    private static void prepareScene(Bundle mStartValues, @NonNull View view) {
        Bundle mEndValues = new Bundle();
        // do the first capture to scale the image
        captureScaleValues(mEndValues, view);

        // calculate the scale factors
        float scaleX = scaleDelta(mStartValues, mEndValues, "width");
        float scaleY = scaleDelta(mStartValues, mEndValues, "height");

        // scale the image
        view.setScaleX(scaleX);
        view.setScaleY(scaleY);
        view.setAlpha(0f);

        // as scaling the image will change the top and left coordinates, we need to re-capture
        // the values to proper figure out the translation deltas w.r.t. to start view
        captureScreenLocationValues(mEndValues, view);

        int deltaX = translationDelta(mStartValues, mEndValues, "left");
        int deltaY = translationDelta(mStartValues, mEndValues, "top");
        // finally, translate the end view to where the start view was
        view.setTranslationX(deltaX);
        view.setTranslationY(deltaY);
    }

    /**
     * This method will run the entry animation
     */
    private static void runEnterAnimation(@NonNull View view) {
        // We can now make it visible
        view.setVisibility(View.VISIBLE);

        // finally, run the animation
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.animate()
                .setDuration(375)
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0)
                .translationY(0)
                .start();
        view.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    /**
     * Helper method to calculate the scale delta given start and end values
     *
     * @param startValues  start values {@link Bundle}
     * @param endValues    end values {@link Bundle}
     * @param propertyName property name
     * @return scale delta value
     */
    private static float scaleDelta(
            @NonNull Bundle startValues,
            @NonNull Bundle endValues,
            @NonNull String propertyName) {

        int startValue = startValues.getInt(propertyName);
        int endValue = endValues.getInt(propertyName);
        float delta = (float) startValue / endValue;

        return delta;
    }

    /**
     * Helper method to calculate the translation deltas given start and end values
     *
     * @param startValues  start values {@link Bundle}
     * @param endValues    end values {@link Bundle}
     * @param propertyName property name
     * @return translation delta between start and end values
     */
    private static int translationDelta(
            @NonNull Bundle startValues,
            @NonNull Bundle endValues,
            @NonNull String propertyName) {

        int startValue = startValues.getInt(propertyName);
        int endValue = endValues.getInt(propertyName);
        int delta = startValue - endValue;

        return delta;
    }
}
