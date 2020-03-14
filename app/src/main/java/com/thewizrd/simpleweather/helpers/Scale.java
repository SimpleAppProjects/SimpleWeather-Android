package com.thewizrd.simpleweather.helpers;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionValues;
import androidx.transition.Visibility;

public class Scale extends Visibility {
    private final static String PROPNAME_SCALE_X = "PROPNAME_SCALE_X";
    private final static String PROPNAME_SCALE_Y = "PROPNAME_SCALE_Y";

    private final float mScaleX;
    private final float mScaleY;

    public Scale(float scaleX, float scaleY) {
        this.mScaleX = scaleX;
        this.mScaleY = scaleY;
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues values) {
        values.values.put(PROPNAME_SCALE_X, values.view.getScaleX());
        values.values.put(PROPNAME_SCALE_Y, values.view.getScaleY());
    }

    /**
     * Utility method to handle creating and running the Animator.
     */
    private Animator createAnimation(final View view, boolean appearing, TransitionValues values) {
        final float initialStartX = view.getScaleX();
        final float initialStartY = view.getScaleY();

        float startX = appearing ? mScaleX : 1f;
        float startY = appearing ? mScaleY : 1f;
        float endX = appearing ? 1f : mScaleX;
        float endY = appearing ? 1f : mScaleY;

        if (values != null) {
            Float savedScaleX = (Float) values.values.get(PROPNAME_SCALE_X);
            Float savedScaleY = (Float) values.values.get(PROPNAME_SCALE_Y);

            if (savedScaleX != null && savedScaleX != initialStartX) {
                startX = savedScaleX;
            }
            if (savedScaleY != null && savedScaleY != initialStartY) {
                startY = savedScaleY;
            }
        }

        view.setScaleX(startX);
        view.setScaleY(startY);

        AnimatorSet animator = new AnimatorSet();
        animator.play(ObjectAnimator.ofFloat(view, View.SCALE_X, startX, endX))
                .with(ObjectAnimator.ofFloat(view, View.SCALE_Y, startY, endY));
        addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                view.setScaleX(initialStartX);
                view.setScaleY(initialStartY);
                transition.removeListener(this);
            }
        });
        return animator;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return createAnimation(view, true, startValues);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return createAnimation(view, false, startValues);
    }
}
