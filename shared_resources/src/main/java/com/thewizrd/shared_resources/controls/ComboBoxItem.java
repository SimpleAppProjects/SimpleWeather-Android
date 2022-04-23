package com.thewizrd.shared_resources.controls;

import androidx.annotation.NonNull;

import java.util.Objects;

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

    @NonNull
    @Override
    public String toString() {
        return display;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComboBoxItem that = (ComboBoxItem) o;

        if (!Objects.equals(display, that.display)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = display != null ? display.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
