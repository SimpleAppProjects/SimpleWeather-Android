package com.thewizrd.shared_resources.utils;

public interface ILoadingCollection {
    boolean hasMoreItems();

    boolean isLoading();

    long loadMoreItems(long count);
}
