package com.thewizrd.simpleweather.fragments;

import android.content.DialogInterface;

public interface WearDialogInterface {
    /**
     * The identifier for the positive button.
     */
    int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;

    /**
     * The identifier for the negative button.
     */
    int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

    void dismiss();

    void dismissAllowingStateLoss();

    interface OnClickListener {
        void onClick(WearDialogInterface dialog, int which);
    }

    interface OnMultiChoiceClickListener {
        void onClick(WearDialogInterface dialog, int which, boolean isChecked);
    }
}
