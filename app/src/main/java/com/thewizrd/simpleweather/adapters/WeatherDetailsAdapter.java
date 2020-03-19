package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.utils.ILoadingCollection;
import com.thewizrd.shared_resources.utils.ObservableLoadingArrayList;
import com.thewizrd.shared_resources.utils.RecyclerViewLoadingScrollListener;
import com.thewizrd.shared_resources.utils.RecyclerViewUtils;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.controls.WeatherDetailItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class WeatherDetailsAdapter<T extends BaseForecastItemViewModel>
        extends RecyclerView.Adapter<WeatherDetailsAdapter.ViewHolder> {
    private List<T> mDataset;
    private AsyncListDiffer<T> mDiffer;

    private RecyclerView recyclerView;
    private RecyclerView.OnScrollListener onScrollListener;
    private final int DEFAULT_FETCH_SIZE = (int) (5 * App.getInstance().getAppContext().getResources().getDisplayMetrics().scaledDensity);

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        private WeatherDetailItem mDetailPanel;

        public ViewHolder(WeatherDetailItem v) {
            super(v);
            mDetailPanel = v;
        }

        public void bind(BaseForecastItemViewModel model) {
            mDetailPanel.bind(model);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WeatherDetailsAdapter(List<T> myDataset) {
        mDataset = myDataset;
        mDiffer = new AsyncListDiffer<T>(this, diffCallback);
        mDiffer.submitList(new ArrayList<T>(mDataset));
        onScrollListener = new RecyclerViewLoadingScrollListener((ILoadingCollection) mDataset);
    }

    private DiffUtil.ItemCallback<T> diffCallback = new DiffUtil.ItemCallback<T>() {
        @Override
        public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
            return ObjectsCompat.equals(oldItem.getDate(), newItem.getDate());
        }

        @Override
        public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
            return ObjectsCompat.equals(oldItem, newItem);
        }
    };

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(new WeatherDetailItem(parent.getContext()));
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.bind(mDiffer.getCurrentList().get(position));
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (recyclerView != null && mDataset instanceof ILoadingCollection) {
            final ILoadingCollection _collection = (ILoadingCollection) mDataset;
            int totalItemCount = RecyclerViewUtils.getItemCount(recyclerView);

            if (totalItemCount < DEFAULT_FETCH_SIZE) {
                // load more
                if (_collection.hasMoreItems() && !_collection.isLoading()) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            _collection.loadMoreItems(DEFAULT_FETCH_SIZE);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        if (mDataset instanceof ObservableLoadingArrayList) {
            recyclerView.addOnScrollListener(onScrollListener);

            final ObservableLoadingArrayList<T> collection = ((ObservableLoadingArrayList<T>) mDataset);
            collection.addOnListChangedCallback(onListChangedListener);

            if (collection.isEmpty() && collection.hasMoreItems()) {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        collection.loadMoreItems(1);
                    }
                });
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (mDataset instanceof ObservableLoadingArrayList) {
            recyclerView.removeOnScrollListener(onScrollListener);
            ((ObservableLoadingArrayList<T>) mDataset).removeOnListChangedCallback(onListChangedListener);
        }
        this.recyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private OnListChangedListener<T> onListChangedListener = new OnListChangedListener<T>() {
        @Override
        public void onChanged(final ArrayList<T> sender, final ListChangedArgs<T> args) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    mDiffer.submitList(new ArrayList<>(sender));
                    return null;
                }
            });
        }
    };
}
