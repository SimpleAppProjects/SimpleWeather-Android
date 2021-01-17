package com.thewizrd.simpleweather.controls.graphs;

import androidx.annotation.DrawableRes;

public class XLabelData {
    private CharSequence xLabel;
    private @DrawableRes
    int xIcon;
    private int xIconRotation;

    public XLabelData(CharSequence label) {
        this.xLabel = label;
    }

    public XLabelData(CharSequence label, @DrawableRes int icon) {
        this(label);
        this.xIcon = icon;
    }

    public XLabelData(CharSequence label, @DrawableRes int icon, int iconRotation) {
        this(label, icon);
        xIconRotation = iconRotation;
    }

    public CharSequence getLabel() {
        return xLabel;
    }

    public void setLabel(CharSequence label) {
        this.xLabel = label;
    }

    public @DrawableRes
    int getIcon() {
        return xIcon;
    }

    public void setIcon(@DrawableRes int icon) {
        this.xIcon = icon;
    }

    public int getIconRotation() {
        return xIconRotation;
    }

    public void setIconRotation(int rotation) {
        this.xIconRotation = rotation;
    }
}
