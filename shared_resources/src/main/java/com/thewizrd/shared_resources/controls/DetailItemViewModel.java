package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.support.annotation.StringRes;

import com.thewizrd.shared_resources.SimpleLibrary;

public class DetailItemViewModel {
    private String label;
    private String icon;
    private String value;
    private int iconRotation;

    public DetailItemViewModel(String label, String icon, String value) {
        this.label = label;
        this.icon = icon;
        this.value = value;
    }

    public DetailItemViewModel(String label, String icon, String value, int iconRotation) {
        this.label = label;
        this.icon = icon;
        this.value = value;
        this.iconRotation = iconRotation;
    }

    public DetailItemViewModel(@StringRes int labelId, @StringRes int wiIcon, String value) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        this.label = context.getString(labelId);
        this.icon = context.getString(wiIcon);
        this.value = value;
    }

    public DetailItemViewModel(@StringRes int labelId, @StringRes int wiIcon, String value, int iconRotation) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        this.label = context.getString(labelId);
        this.icon = context.getString(wiIcon);
        this.value = value;
        this.iconRotation = iconRotation;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIconRotation() {
        return iconRotation;
    }

    public void setIconRotation(int iconRotation) {
        this.iconRotation = iconRotation;
    }
}
