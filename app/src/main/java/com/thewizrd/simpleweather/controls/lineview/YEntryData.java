package com.thewizrd.simpleweather.controls.lineview;

public class YEntryData implements Comparable<YEntryData> {
    private float y;
    private CharSequence yLabel;

    public YEntryData(float yValue, CharSequence label) {
        this.y = yValue;
        this.yLabel = label;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public CharSequence getLabel() {
        return yLabel;
    }

    public void setLabel(CharSequence label) {
        this.yLabel = label;
    }

    @Override
    public int compareTo(YEntryData o) {
        if (o == null) {
            return 1;
        } else {
            return Float.compare(y, o.y);
        }
    }
}
