package com.thewizrd.shared_resources.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;

public class RecyclerViewLoadingScrollListener extends RecyclerView.OnScrollListener {
    private final ILoadingCollection _collection;

    // The minimum amount of items to have below your current scroll position
    // before loading more.
    private final int DEFAULT_FETCH_SIZE = (int) (5 * SimpleLibrary.getInstance().getAppContext().getResources().getDisplayMetrics().scaledDensity);
    private int visibleThreshold = DEFAULT_FETCH_SIZE;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount = 0;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = true;

    public RecyclerViewLoadingScrollListener(ILoadingCollection _collection) {
        this._collection = _collection;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (_collection == null) return;

        int firstVisibleItemPosition = RecyclerViewUtils.findFirstVisibleItemPosition(recyclerView);
        int lastVisibleItemPosition = RecyclerViewUtils.findLastVisibleItemPosition(recyclerView);
        int totalItemCount = RecyclerViewUtils.getItemCount(recyclerView);
        visibleThreshold = Math.max(lastVisibleItemPosition - firstVisibleItemPosition, DEFAULT_FETCH_SIZE);

        // If the total item count is zero and the previous isn't, assume the
        // list is invalidated and should be reset back to initial state
        if (totalItemCount < previousTotalItemCount) {
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }

        // If it’s still loading, we check to see if the dataset count has
        // changed, if so we conclude it has finished loading and update the current page
        // number and total item count.
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }

        // If it isn’t currently loading, we check to see if we have breached
        // the visibleThreshold and need to reload more data.
        // If we do need to reload some more data, we execute onLoadMore to fetch the data.
        // threshold should reflect how many total columns there are too
        if (!loading && (lastVisibleItemPosition + visibleThreshold * 2.0f) > totalItemCount) {
            // load more
            if (_collection.hasMoreItems() && !_collection.isLoading()) {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        _collection.loadMoreItems(visibleThreshold);
                    }
                });
                loading = true;
            }
        }
    }
}
