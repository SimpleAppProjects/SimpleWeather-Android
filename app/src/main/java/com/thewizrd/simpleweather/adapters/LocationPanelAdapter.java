package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.LocationPanel;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapterInterface;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class LocationPanelAdapter extends RecyclerView.Adapter<LocationPanelAdapter.ViewHolder> implements ItemTouchHelperAdapterInterface {
    private ObservableArrayList<LocationPanelViewModel> mDataset;
    private RequestManager mGlide;
    private Handler mMainHandler;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;
    private RecyclerOnClickListenerInterface onLongClickListener;
    private OnListChangedListener<LocationPanelViewModel> onListChangedCallback;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(RecyclerOnClickListenerInterface onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void setOnListChangedCallback(OnListChangedListener<LocationPanelViewModel> onListChangedCallback) {
        this.onListChangedCallback = onListChangedCallback;

        if (onListChangedCallback != null)
            mDataset.addOnListChangedCallback(onListChangedCallback);
    }

    public List<LocationPanelViewModel> getDataset() {
        // Create copy of list
        return ArrayUtils.toArrayList(mDataset.toArray(new LocationPanelViewModel[0]));
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mBgImageView;
        LocationPanel mLocView;
        ProgressBar mProgressBar;

        ViewHolder(LocationPanel v) {
            super(v);
            mLocView = v;
            mBgImageView = v.findViewById(R.id.image_view);
            mProgressBar = v.findViewById(R.id.progressBar);
            mProgressBar.setVisibility(View.VISIBLE);
            mLocView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, getAdapterPosition());
                }
            });
            mLocView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onLongClickListener != null)
                        onLongClickListener.onClick(v, getAdapterPosition());
                    return true;
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationPanelAdapter(RequestManager glide, List<LocationPanelViewModel> myDataset) {
        this.mGlide = glide;
        mMainHandler = new Handler(Looper.getMainLooper());

        mDataset = new ObservableArrayList<>();
        if (myDataset != null) {
            mDataset.addAll(myDataset);
        }
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public LocationPanelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        LocationPanel v = new LocationPanel(parent.getContext());
        // set the view's size, margins, paddings and layout parameters
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, parent.getContext().getResources().getDisplayMetrics());
        layoutParams.setMargins(0, margin, 0, margin); // l, t, r, b
        v.setLayoutParams(layoutParams);
        return new LocationPanelAdapter.ViewHolder(v);
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull final LocationPanelAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final LocationPanelViewModel panelView = mDataset.get(position);

        // Background
        if (panelView != null && !StringUtils.isNullOrWhitespace(panelView.getBackground())) {
            mGlide.asBitmap()
                    .load(panelView.getBackground())
                    .apply(new RequestOptions()
                            .centerCrop()
                            .error(holder.mLocView.getColorDrawable())
                            .placeholder(holder.mLocView.getColorDrawable()))
                    .into(new BitmapImageViewTarget(holder.mBgImageView));
        } else {
            mGlide.clear(holder.mBgImageView);
            holder.mBgImageView.setImageDrawable(holder.mLocView.getColorDrawable());
        }

        holder.mLocView.setWeather(panelView);
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }

    public void add(LocationPanelViewModel item) {
        mDataset.add(item);
        notifyItemInserted(mDataset.indexOf(item));
    }

    public void remove(int position) {
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    public void removeAll() {
        int size = getItemCount();
        mDataset.clear();
        notifyItemRangeRemoved(0, size);
    }

    public LocationPanelViewModel getPanelViewModel(int position) {
        return mDataset.get(position);
    }

    private void removeLocation(final int position) {
        // Remove location from list
        Settings.deleteLocation(
                mDataset.get(position).getLocationData().getQuery());

        // Remove panel
        if (Thread.currentThread() != mMainHandler.getLooper().getThread()) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    remove(position);
                }
            });
        }
    }

    public void onItemMove(int fromPosition, int toPosition) {
        mDataset.move(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void onItemDismiss(final int position) {
        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                removeLocation(position);
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        ShortcutCreator.updateShortcuts();
                    }
                });
            }
        });
    }
}