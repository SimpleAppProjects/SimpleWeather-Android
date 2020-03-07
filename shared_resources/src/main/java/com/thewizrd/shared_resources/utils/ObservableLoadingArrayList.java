package com.thewizrd.shared_resources.utils;

import com.thewizrd.shared_resources.helpers.ObservableArrayList;

public abstract class ObservableLoadingArrayList<T> extends ObservableArrayList<T>
        implements ILoadingCollection {

    protected boolean _isLoading;
    protected boolean _hasMoreItems;
    protected boolean _refreshOnLoad;

    @Override
    public boolean isLoading() {
        return _isLoading;
    }

    @Override
    public boolean hasMoreItems() {
        return _hasMoreItems;
    }

    public void refresh() {
        if (isLoading()) {
            _refreshOnLoad = true;
        } else {
            synchronized (this) {
                clear();
            }
            _hasMoreItems = true;

            loadMoreItems(1);
        }
    }

    @Override
    public abstract long loadMoreItems(long count);
}
