package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ListChangedAction;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.ListDiffUtilCallback;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.utils.ILoadingCollection;
import com.thewizrd.shared_resources.utils.ObservableLoadingArrayList;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.databinding.WeatherForecastPanelBinding;
import com.thewizrd.simpleweather.databinding.WeatherHrforecastPanelBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForecastItemAdapter<T extends BaseForecastItemViewModel> extends RecyclerView.Adapter {
    private List<T> mDataset;
    private RecyclerView recyclerView;

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
        if (mDataset.get(position) instanceof HourlyForecastItemViewModel) {
            return ItemType.HOURLYFORECAST;
        } else {
            return ItemType.FORECAST;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ForecastItemAdapter(List<T> myDataset) {
        mDataset = myDataset;
    }

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
            vh.bind((HourlyForecastItemViewModel) mDataset.get(position));
        } else if (holder instanceof ForecastViewHolder) {
            ForecastViewHolder vh = (ForecastViewHolder) holder;
            vh.bind((ForecastItemViewModel) mDataset.get(position));
        }
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }

    public void updateItems(final List<T> items) {
        if (items != mDataset) {
            if (mDataset instanceof ILoadingCollection && !(items instanceof ILoadingCollection)) {
                mDataset = new ArrayList<>(items);
                notifyDataSetChanged();
            } else if (items instanceof ILoadingCollection) {
                mDataset = items;
                notifyDataSetChanged();
            } else {
                List<T> oldItems = new ArrayList<>(mDataset);
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ListDiffUtilCallback<T>(oldItems, items) {
                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return Objects.equals(getOldList().get(oldItemPosition).getDate(), getNewList().get(newItemPosition).getDate());
                    }
                });
                if (mDataset instanceof ILoadingCollection) {
                    mDataset = items;
                } else {
                    mDataset.clear();
                    mDataset.addAll(items);
                }
                diffResult.dispatchUpdatesTo(this);
                oldItems.clear();
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
        public void onChanged(ArrayList sender, final ListChangedArgs args) {
            if (recyclerView != null) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (args.action == ListChangedAction.ADD) {
                            notifyItemInserted(args.newStartingIndex);
                        } else if (args.action == ListChangedAction.MOVE) {
                            notifyItemMoved(args.oldStartingIndex, args.newStartingIndex);
                        } else if (args.action == ListChangedAction.REMOVE && args.oldStartingIndex >= 0) {
                            notifyItemRemoved(args.oldStartingIndex);
                        } else if (args.action == ListChangedAction.REPLACE) {
                            notifyItemChanged(args.oldStartingIndex);
                        } else {
                            notifyDataSetChanged();
                        }
                    }
                });
            }
        }
    };

    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        // The minimum amount of items to have below your current scroll position
        // before loading more.
        private final int DEFAULT_FETCH_SIZE = (int) (5 * App.getInstance().getAppContext().getResources().getDisplayMetrics().scaledDensity);
        private int visibleThreshold = DEFAULT_FETCH_SIZE;
        // The total number of items in the dataset after the last load
        private int previousTotalItemCount = 0;
        // True if we are still waiting for the last set of data to load.
        private boolean loading = true;

        private int getFirstVisibleItem(int[] firstVisibleItemPositions) {
            int minSize = 0;
            for (int i = 0; i < firstVisibleItemPositions.length; i++) {
                if (i == 0) {
                    minSize = firstVisibleItemPositions[i];
                } else if (firstVisibleItemPositions[i] < minSize) {
                    minSize = firstVisibleItemPositions[i];
                }
            }
            return minSize;
        }

        private int getLastVisibleItem(int[] lastVisibleItemPositions) {
            int maxSize = 0;
            for (int i = 0; i < lastVisibleItemPositions.length; i++) {
                if (i == 0) {
                    maxSize = lastVisibleItemPositions[i];
                } else if (lastVisibleItemPositions[i] > maxSize) {
                    maxSize = lastVisibleItemPositions[i];
                }
            }
            return maxSize;
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (!(mDataset instanceof ILoadingCollection)) return;
            final ILoadingCollection _collection = (ILoadingCollection) mDataset;

            RecyclerView.LayoutManager mLayoutMgr = recyclerView.getLayoutManager();
            int firstVisibleItemPosition = 0;
            int lastVisibleItemPosition = 0;
            int totalItemCount = mLayoutMgr != null ? mLayoutMgr.getItemCount() : 0;

            if (mLayoutMgr instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager sGLayoutMgr = ((StaggeredGridLayoutManager) mLayoutMgr);
                int[] firstVisibleItemPositions = sGLayoutMgr.findFirstVisibleItemPositions(null);
                int[] lastVisibleItemPositions = sGLayoutMgr.findLastVisibleItemPositions(null);
                // get min and maximum element within the list
                firstVisibleItemPosition = getFirstVisibleItem(firstVisibleItemPositions);
                lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
                visibleThreshold = Math.min(lastVisibleItemPosition - firstVisibleItemPosition, DEFAULT_FETCH_SIZE);
            } else if (mLayoutMgr instanceof LinearLayoutManager) {
                LinearLayoutManager linLayoutMgr = ((LinearLayoutManager) mLayoutMgr);
                // get min and maximum element within the list
                firstVisibleItemPosition = linLayoutMgr.findFirstVisibleItemPosition();
                lastVisibleItemPosition = linLayoutMgr.findLastVisibleItemPosition();
                visibleThreshold = Math.min(lastVisibleItemPosition - firstVisibleItemPosition, DEFAULT_FETCH_SIZE);
            }

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
            if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
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
    };
}
