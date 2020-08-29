package com.thewizrd.shared_resources.helpers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CallbackList<T> {
    private List<OnListChangedListener<T>> mCallbacks;

    public CallbackList() {
        mCallbacks = new ArrayList<>();
    }

    public void add(OnListChangedListener<T> callback) {
        mCallbacks.add(callback);
    }

    public void remove(OnListChangedListener<T> callback) {
        mCallbacks.remove(callback);
    }

    public void notifyChange(@NonNull ArrayList<T> sender, @NonNull ListChangedArgs args) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onChanged(sender, args);
        }
    }
}
