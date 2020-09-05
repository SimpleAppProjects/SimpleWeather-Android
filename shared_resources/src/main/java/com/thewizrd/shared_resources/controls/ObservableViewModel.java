package com.thewizrd.shared_resources.controls;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.ViewModel;

public abstract class ObservableViewModel extends ViewModel implements Observable {
    private boolean isCleared = false;
    private PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    public void notifyChange() {
        if (!isCleared) {
            callbacks.notifyCallbacks(this, 0, null);
        }
    }

    public void notifyPropertyChanged(int fieldID) {
        if (!isCleared) {
            callbacks.notifyCallbacks(this, fieldID, null);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        isCleared = true;
    }
}
