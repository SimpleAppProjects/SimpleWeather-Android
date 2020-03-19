package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.utils.ILoadingCollection;
import com.thewizrd.shared_resources.utils.ObservableLoadingArrayList;
import com.thewizrd.shared_resources.utils.RecyclerViewLoadingScrollListener;
import com.thewizrd.shared_resources.utils.RecyclerViewUtils;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.databinding.WeatherForecastPanelBinding;
import com.thewizrd.simpleweather.databinding.WeatherHrforecastPanelBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ForecastItemAdapter<T extends BaseForecastItemViewModel> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<T> mDataset;
    private AsyncListDiffer<T> mDiffer;

    private RecyclerView recyclerView;
    private RecyclerView.OnScrollListener onScrollListener;
    private final int DEFAULT_FETCH_SIZE = (int) (5 * App.getInstance().getAppContext().getResources().getDisplayMetrics().scaledDensity);

    private static class ItemType {
        static final int FORECAST = 0;
        static final int HOURLYFORECAST = 1;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        private WeatherForecastPanelBinding binding;

        public ForecastViewHolder(WeatherForecastPanelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ForecastItemViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();
        }
    }

    static class HourlyForecastViewHolder extends RecyclerView.ViewHolder {
        private WeatherHrforecastPanelBinding binding;

        public HourlyForecastViewHolder(WeatherHrforecastPanelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HourlyForecastItemViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mDiffer.getCurrentList().get(position) instanceof HourlyForecastItemViewModel) {
            return ItemType.HOURLYFORECAST;
        } else {
            return ItemType.FORECAST;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ForecastItemAdapter(List<T> myDataset) {
        mDiffer = new AsyncListDiffer<T>(this, diffCallback);
        updateItems(myDataset);
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewGroup.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int paddingHoriz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, parent.getContext().getResources().getDisplayMetrics());

        if (viewType == ItemType.HOURLYFORECAST) {
            WeatherHrforecastPanelBinding binding = WeatherHrforecastPanelBinding.inflate(inflater);
            // FOR WEAR ONLY
            // set the view's size, margins, paddings and layout parameters
            binding.getRoot().setLayoutParams(layoutParams);
            binding.getRoot().setPaddingRelative(paddingHoriz, 0, paddingHoriz, 0);
            return new HourlyForecastViewHolder(binding);
        } else {
            // create a new view
            WeatherForecastPanelBinding binding = WeatherForecastPanelBinding.inflate(inflater);
            // FOR WEAR ONLY
            // set the view's size, margins, paddings and layout parameters
            binding.getRoot().setLayoutParams(layoutParams);
            binding.getRoot().setPaddingRelative(paddingHoriz, 0, paddingHoriz, 0);
            return new ForecastViewHolder(binding);
        }
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (holder instanceof HourlyForecastViewHolder) {
            HourlyForecastViewHolder vh = (HourlyForecastViewHolder) holder;
            vh.bind((HourlyForecastItemViewModel) mDiffer.getCurrentList().get(position));
        } else if (holder instanceof ForecastViewHolder) {
            ForecastViewHolder vh = (ForecastViewHolder) holder;
            vh.bind((ForecastItemViewModel) mDiffer.getCurrentList().get(position));
        }
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void updateItems(final List<T> items) {
        if (items != mDataset) {
            if (mDataset instanceof ILoadingCollection && !(items instanceof ILoadingCollection)) {
                mDataset = new ArrayList<>(items);
                mDiffer.submitList(new ArrayList<>(items));
            } else if (items instanceof ILoadingCollection) {
                mDataset = items;
                mDiffer.submitList(new ArrayList<>(items));
                onScrollListener = new RecyclerViewLoadingScrollListener((ILoadingCollection) mDataset);
            } else {
                mDiffer.submitList(new ArrayList<>(items));
            }
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
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
