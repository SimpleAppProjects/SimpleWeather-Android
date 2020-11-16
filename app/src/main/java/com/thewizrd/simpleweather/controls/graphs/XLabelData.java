package com.thewizrd.simpleweather.controls.graphs;

public class XLabelData {
    private CharSequence xLabel;
    private CharSequence xIcon;
    private int xIconRotation;

    public XLabelData(CharSequence label) {
        this.xLabel = label;
    }

    public XLabelData(CharSequence label, CharSequence icon) {
        this(label);
        this.xIcon = icon;
    }

    public XLabelData(CharSequence label, CharSequence icon, int iconRotation) {
        this(label, icon);
        xIconRotation = iconRotation;
    }

    public CharSequence getLabel() {
        return xLabel;
    }

    public void setLabel(CharSequence label) {
        this.xLabel = label;
    }

    public CharSequence getIcon() {
        return xIcon;
    }

    public void setIcon(CharSequence icon) {
        this.xIcon = icon;
    }

    public int getIconRotation() {
        return xIconRotation;
    }

    public void setIconRotation(int rotation) {
        this.xIconRotation = rotation;
    }
}
