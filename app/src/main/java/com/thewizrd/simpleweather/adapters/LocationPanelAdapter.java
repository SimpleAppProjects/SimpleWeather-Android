package com.thewizrd.simpleweather.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.LocationPanel;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapterInterface;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class LocationPanelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ItemTouchHelperAdapterInterface, LifecycleEventObserver {

    public static class Payload {
        public static final int IMAGE_UPDATE = 0;
    }

    public static class ItemType {
        public static final int GPS_PANEL = LocationType.GPS.getValue();
        public static final int SEARCH_PANEL = LocationType.SEARCH.getValue();
        public static final int HEADER_GPS = -2;
        public static final int HEADER_FAV = -3;
        public static final int FOOTER_SPACER = -4;
    }

    public interface HeaderSetterInterface {
        void setHeader();

        void setHeaderTextColor();
    }

    private final ObservableArrayList<LocationPanelViewModel> mDataset;
    private Handler mMainHandler;

    private GPSHeaderViewHolder gpsVH;
    private FavHeaderViewHolder favVH;
    private boolean hasGPSPanel;
    private boolean hasSearchPanel;

    private RecyclerView mParentRecyclerView;
    private SnackbarManager mSnackMgr;
    private boolean isFragmentAlive;

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

    public RecyclerView.ViewHolder getGPSViewHolder() {
        return gpsVH;
    }

    public RecyclerView.ViewHolder getFavViewHolder() {
        return favVH;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class LocationPanelViewHolder extends RecyclerView.ViewHolder {
        LocationPanel mLocView;
        ProgressBar mProgressBar;

        LocationPanelViewHolder(LocationPanel v) {
            super(v);
            mLocView = v;
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
    public LocationPanelAdapter() {
        mMainHandler = new Handler(Looper.getMainLooper());
        mDataset = new ObservableArrayList<>();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mParentRecyclerView = recyclerView;
        mSnackMgr = new SnackbarManager(recyclerView);
        isFragmentAlive = true;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mSnackMgr.dismissAll();
        mSnackMgr = null;
        mParentRecyclerView = null;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event.compareTo(Lifecycle.Event.ON_PAUSE) >= 0) {
            isFragmentAlive = false;

            if (mSnackMgr != null) {
                mSnackMgr.dismissAll();
                mSnackMgr = null;
            }
        } else {
            isFragmentAlive = true;
        }
    }

    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        switch (viewType) {
            case ItemType.HEADER_GPS:
                gpsVH = new GPSHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.locations_header, parent, false));
                return gpsVH;
            case ItemType.HEADER_FAV:
                favVH = new FavHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.locations_header, parent, false));
                return favVH;
            case ItemType.FOOTER_SPACER:
                Space spacer = new Space(parent.getContext());
                int height = context.getResources().getDimensionPixelSize(R.dimen.location_panel_height);
                spacer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                return new ViewHolder(spacer);
            default:
                // create a new view
                LocationPanel v = new LocationPanel(context);
                return new LocationPanelViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        //
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        final boolean imageUpdateOnly;
        if (!payloads.isEmpty()) {
            imageUpdateOnly = payloads.get(0).equals(Payload.IMAGE_UPDATE);
        } else {
            imageUpdateOnly = false;
        }

        if (holder instanceof HeaderSetterInterface) {
            ((HeaderSetterInterface) holder).setHeader();
            ((HeaderSetterInterface) holder).setHeaderTextColor();
        } else if (holder instanceof ViewHolder) {
            // No-op
        } else {
            final LocationPanelViewHolder vHolder = (LocationPanelViewHolder) holder;
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final LocationPanelViewModel panelView = getPanelViewModel(position);

            // Background
            vHolder.mLocView.post(new Runnable() {
                @Override
                public void run() {
                    updatePanelBackground(vHolder, panelView, imageUpdateOnly);
                }
            });

            if (!imageUpdateOnly) {
                vHolder.mLocView.setWeather(panelView);
            }
        }
    }

    private void updatePanelBackground(final LocationPanelViewHolder vHolder, final LocationPanelViewModel panelView, boolean skipCache) {
        if (panelView != null && !StringUtils.isNullOrWhitespace(panelView.getBackground())) {
            vHolder.mLocView.setWeatherBackground(panelView, skipCache);
        } else {
            vHolder.mLocView.clearBackground();
        }
    }

    public boolean hasGPSHeader() {
        return hasGPSPanel;
    }

    public boolean hasSearchHeader() {
        return hasSearchPanel;
    }

    @Override
    public int getItemViewType(int position) {
        if (hasGPSPanel && hasSearchPanel && position == 0)
            return ItemType.HEADER_GPS;
        else if (hasGPSPanel && position == 0)
            return ItemType.HEADER_GPS;
        else if (hasSearchPanel && position == 0)
            return ItemType.HEADER_FAV;
        else if (hasGPSPanel && hasSearchPanel && position == 2)
            return ItemType.HEADER_FAV;
        else if (position == getItemCount() - 1)
            return ItemType.FOOTER_SPACER;

        return getPanelViewModel(position).getLocationType();
    }

    public int getViewPosition(LocationPanelViewModel item) {
        int position = mDataset.indexOf(item);

        if (position == 0)
            position++;
        else if (hasSearchPanel && hasGPSPanel && position > 0)
            position += 2;
        else if ((hasSearchPanel || hasGPSPanel) && position > 0)
            position++;

        return position;
    }

    public int getDataPosition(int position) {
        if (position == 1)
            position--;
        else if (hasSearchPanel && hasGPSPanel && position > 1)
            position -= 2;
        else if ((hasSearchPanel || hasGPSPanel) && position > 1)
            position--;

        return position;
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        int size = mDataset.size();

        if (hasGPSPanel)
            size++;
        if (hasSearchPanel)
            size++;

        size++; // For Footer

        return size;
    }

    public int getDataCount() {
        return mDataset.size();
    }

    public int getFavoritesCount() {
        int size = mDataset.size();

        if (hasGPSPanel)
            size--;

        return size;
    }

    public LocationPanelViewModel getGPSPanel() {
        if (hasGPSPanel && getPanelData(0).getLocationType() == LocationType.GPS)
            return getPanelViewModel(0);
        return null;
    }

    public LocationPanelViewModel getFirstFavPanel() {
        if (hasGPSPanel && hasSearchPanel)
            return getPanelViewModel(1);
        else if (hasSearchPanel) {
            return getPanelViewModel(0);
        }
        return null;
    }

    public void add(LocationPanelViewModel item) {
        mDataset.add(item);
        notifyItemInserted(getViewPosition(item));

        if (item.getLocationType() == LocationType.GPS.getValue())
            hasGPSPanel = true;
        else if (!hasSearchPanel)
            hasSearchPanel = true;
    }

    public void add(int index, LocationPanelViewModel item) {
        if (index > mDataset.size()) {
            if (BuildConfig.DEBUG) Log.i("LocationPanelAdapter", "Index OOB");
            mDataset.add(item);
        } else {
            mDataset.add(index, item);
        }

        notifyItemInserted(getViewPosition(item));

        if (!hasGPSPanel && item.getLocationType() == LocationType.GPS.getValue())
            hasGPSPanel = true;
        else if (!hasSearchPanel)
            hasSearchPanel = true;
    }

    public boolean remove(LocationPanelViewModel panel) {
        int viewPosition = getViewPosition(panel);

        boolean removed = mDataset.remove(panel);

        if (!removed) return false;

        if (panel.getLocationType() == LocationType.GPS.getValue())
            hasGPSPanel = false;

        notifyItemRemoved(viewPosition);
        return true;
    }

    public void removeGPSPanel() {
        LocationPanelViewModel gpsPanel = getGPSPanel();
        if (gpsPanel != null) {
            remove(gpsPanel);
        }
    }

    public void removeAll() {
        int size = getItemCount();
        mDataset.clear();
        notifyItemRangeRemoved(0, size);

        hasGPSPanel = false;
        hasSearchPanel = false;
    }

    public LocationPanelViewModel getPanelViewModel(int position) {
        if (position >= getItemCount() || mDataset.size() == 0)
            return null;

        int dataPosition = getDataPosition(position);

        if (dataPosition >= mDataset.size()) {
            return null;
        }

        return mDataset.get(dataPosition);
    }

    public LocationData getPanelData(int position) {
        if (position >= getItemCount() || mDataset.size() == 0)
            return null;

        LocationPanelViewModel panel = getPanelViewModel(position);

        if (panel == null) return null;
        return panel.getLocationData();
    }

    private void removeLocation(final LocationPanelViewModel panel) {
        // Remove location from list
        Settings.deleteLocation(
                panel.getLocationData().getQuery());

        // Remove panel
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                remove(panel);
            }
        });
    }

    public void onItemMove(int fromPosition, int toPosition) {
        mDataset.move(getDataPosition(fromPosition), getDataPosition(toPosition));
        notifyItemMoved(fromPosition, toPosition);
    }

    public void onItemDismiss(final int position) {
        final LocationPanelViewModel dismissedPanel = getPanelViewModel(position);

        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (mParentRecyclerView != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            new PanelDeleteHandler(dismissedPanel).deletePanel();
                        }
                    });
                } else {
                    removeLocation(dismissedPanel);
                }
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

    public class GPSHeaderViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        GPSHeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
        }

        @Override
        public void setHeader() {
            header.setText(R.string.label_currentlocation);
        }

        @Override
        public void setHeaderTextColor() {
            header.setTextColor(ActivityUtils.getColor(header.getContext(), android.R.attr.textColorPrimary));
        }
    }

    public class FavHeaderViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        FavHeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
        }

        @Override
        public void setHeader() {
            header.setText(R.string.label_favoritelocations);
        }

        @Override
        public void setHeaderTextColor() {
            header.setTextColor(ActivityUtils.getColor(header.getContext(), android.R.attr.textColorPrimary));
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class PanelDeleteHandler {
        // For undo
        private int mDataPosition;
        private LocationPanelViewModel mDeletedPanel;

        PanelDeleteHandler(LocationPanelViewModel panel) {
            mDeletedPanel = panel;
        }

        void deletePanel() {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDataPosition = mDataset.indexOf(mDeletedPanel);
                    remove(mDeletedPanel);

                    // If only a single favorite location is left, revert the deletion
                    if ((hasGPSPanel && mDataset.size() <= 1) || (!hasGPSPanel && hasSearchPanel && mDataset.size() <= 0)) {
                        performUndoAction(mDataPosition, mDeletedPanel);
                        return;
                    }

                    showUndoSnackbar();
                }
            });
        }

        private void showUndoSnackbar() {
            final LocationPanelViewModel pendingVMForRemoval = mDeletedPanel;
            final int dataPosition = mDataPosition;
            if (mParentRecyclerView != null && isFragmentAlive) {
                // Make SnackBar
                Snackbar snackbar = Snackbar.make(R.string.message_locationremoved, Snackbar.Duration.SHORT);
                snackbar.setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDeletedPanel = null;

                        performUndoAction(dataPosition, pendingVMForRemoval);
                    }
                });
                com.google.android.material.snackbar.Snackbar.Callback callback = new com.google.android.material.snackbar.Snackbar.Callback() {
                    @Override
                    public void onDismissed(com.google.android.material.snackbar.Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);

                        if (event != DISMISS_EVENT_ACTION) {
                            AsyncTask.run(new Runnable() {
                                @Override
                                public void run() {
                                    if (pendingVMForRemoval == null)
                                        return;

                                    String key = pendingVMForRemoval.getLocationData().getQuery();
                                    Settings.deleteLocation(key);
                                }
                            });
                        }
                    }
                };

                if (mSnackMgr == null)
                    mSnackMgr = new SnackbarManager(mParentRecyclerView);
                mSnackMgr.show(snackbar, callback);
            }
        }

        private void performUndoAction(final int dataPosition, final LocationPanelViewModel pendingVMForRemoval) {
            if (pendingVMForRemoval != null && !mDataset.contains(pendingVMForRemoval)) {
                add(dataPosition, pendingVMForRemoval);
                // End active removal animations if we're undoing the action
                RecyclerView.ViewHolder holder = mParentRecyclerView.findViewHolderForAdapterPosition(getViewPosition(pendingVMForRemoval));
                if (mParentRecyclerView.getItemAnimator() != null) {
                    if (holder != null) {
                        mParentRecyclerView.getItemAnimator().endAnimation(holder);
                    } else {
                        mParentRecyclerView.getItemAnimator().endAnimations();
                    }
                }
            }
        }
    }
}