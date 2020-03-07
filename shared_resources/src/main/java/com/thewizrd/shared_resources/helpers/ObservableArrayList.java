package com.thewizrd.shared_resources.helpers;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

public class ObservableArrayList<T> extends ArrayList<T> {
    protected transient CallbackList<T> mListeners = new CallbackList<>();

    public ObservableArrayList() {
        super();
    }

    public ObservableArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public ObservableArrayList(Collection<? extends T> c) {
        super(c);
    }

    public void addOnListChangedCallback(OnListChangedListener<T> listChangedListener) {
        if (mListeners == null)
            mListeners = new CallbackList<>();

        mListeners.add(listChangedListener);
    }

    public void removeOnListChangedCallback(OnListChangedListener<T> listChangedListener) {
        if (mListeners != null)
            mListeners.remove(listChangedListener);
    }

    public void move(int oldIndex, int newIndex) {
        Collections.swap(this, oldIndex, newIndex);

        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.MOVE, newIndex, oldIndex));
    }

    @Override
    public T set(int index, T element) {
        T val = super.set(index, element);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.REPLACE, index, index));
        return val;
    }

    @Override
    public boolean add(T t) {
        super.add(t);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.ADD, size() - 1, -1));
        return true;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.ADD, index, -1));
    }

    @Override
    public T remove(int index) {
        T val = super.remove(index);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.REMOVE, -1, index));
        return val;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index >= 0) {
            remove(index);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        int oldSize = size();
        super.clear();
        if (oldSize != 0) {
            if (mListeners != null)
                mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.RESET, -1, -1));
        }
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        int oldSize = size();
        boolean added = super.addAll(c);
        if (added) {
            if (mListeners != null)
                mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.ADD, oldSize - 1, -1));
        }
        return added;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        boolean added = super.addAll(index, c);
        if (added) {
            if (mListeners != null)
                mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.ADD, index, -1));
        }
        return added;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.REMOVE, fromIndex, -1));
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        boolean value = super.removeAll(c);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.REMOVE, -1, -1));
        return value;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean removeIf(@NonNull Predicate<? super T> filter) {
        boolean value = super.removeIf(filter);
        if (mListeners != null)
            mListeners.notifyChange(this, new ListChangedArgs(ListChangedAction.REMOVE, -1, -1));
        return value;
    }
}
