package com.thewizrd.shared_resources.controls;

public class ComboBoxItem {
    private String display;
    private String value;

    public ComboBoxItem(String display, String value) {
        this.display = display;
        this.value = value;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return display;
    }
}
