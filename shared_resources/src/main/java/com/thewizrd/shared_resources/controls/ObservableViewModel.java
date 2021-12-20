package com.thewizrd.shared_resources.controls;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.ViewModel;

public abstract class ObservableViewModel extends ViewModel implements Observable {
    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    public void notifyChange() {
        callbacks.notifyChange(this, 0);
    }

    public void notifyPropertyChanged(int fieldID) {
        callbacks.notifyChange(this, fieldID);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        callbacks.clear();
    }
}
