package com.thewizrd.simpleweather.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.LocationPanel;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapterInterface;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

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

    public interface ViewHolderLongClickListener {
        void onLongClick(RecyclerView.ViewHolder holder);
    }

    private final ObservableArrayList<LocationPanelViewModel> mDataset;
    private final ObservableArrayList<LocationPanelViewModel> mSelectedItems;
    private Handler mMainHandler;

    private GPSHeaderViewHolder gpsVH;
    private FavHeaderViewHolder favVH;
    private boolean hasGPSPanel;
    private boolean hasSearchPanel;

    private RecyclerView mParentRecyclerView;
    private SnackbarManager mSnackMgr;
    private boolean isFragmentAlive;
    private boolean isInEditMode;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;
    private RecyclerOnClickListenerInterface onLongClickListener;
    private ViewHolderLongClickListener onLongClickToDragListener;
    private OnListChangedListener<LocationPanelViewModel> onListChangedCallback;
    private OnListChangedListener<LocationPanelViewModel> onSelectionChangedCallback;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(RecyclerOnClickListenerInterface onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void setOnListChangedCallback(OnListChangedListener<LocationPanelViewModel> onListChangedCallback) {
        if (this.onListChangedCallback != null) {
            mDataset.removeOnListChangedCallback(this.onListChangedCallback);
        }

        this.onListChangedCallback = onListChangedCallback;

        if (onListChangedCallback != null)
            mDataset.addOnListChangedCallback(onListChangedCallback);
    }

    public void setOnSelectionChangedCallback(OnListChangedListener<LocationPanelViewModel> onSelectionChangedCallback) {
        if (this.onSelectionChangedCallback != null) {
            mSelectedItems.removeOnListChangedCallback(this.onSelectionChangedCallback);
        }

        this.onSelectionChangedCallback = onSelectionChangedCallback;

        if (onSelectionChangedCallback != null)
            mSelectedItems.addOnListChangedCallback(onSelectionChangedCallback);
    }

    public List<LocationPanelViewModel> getDataset() {
        // Create copy of list
        return new ArrayList<>(mDataset);
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
        private LocationPanel mLocView;
        private LocationPanelViewModel model;

        LocationPanelViewHolder(LocationPanel v) {
            super(v);
            mLocView = v;

            mLocView.showLoading(true);
            mLocView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, getAdapterPosition());
                    if (model != null && getItemViewType() == ItemType.SEARCH_PANEL && model.isEditMode()) {
                        if (model.isChecked()) {
                            model.setChecked(false);
                            mSelectedItems.remove(model);
                        } else {
                            model.setChecked(true);
                            mSelectedItems.add(model);
                        }
                        notifyItemChanged(getViewPosition(model));
                    }
                }
            });
            mLocView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (model.getLocationType() == LocationType.SEARCH.getValue()) {
                        if (onLongClickToDragListener != null)
                            onLongClickToDragListener.onLongClick(LocationPanelViewHolder.this);

                        if (!model.isEditMode() && !model.isChecked() && !mSelectedItems.contains(model)) {
                            mSelectedItems.add(model);
                        }
                    }
                    if (onLongClickListener != null)
                        onLongClickListener.onClick(v, getAdapterPosition());
                    return true;
                }
            });
        }

        public void bind(LocationPanelViewModel model) {
            this.model = model;
            if (!model.isEditMode())
                model.setChecked(false);
            mLocView.bindModel(model);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationPanelAdapter(ViewHolderLongClickListener longClickListener) {
        mMainHandler = new Handler(Looper.getMainLooper());
        mDataset = new ObservableArrayList<>(Settings.getMaxLocations());
        mSelectedItems = new ObservableArrayList<>(Settings.getMaxLocations());
        onLongClickToDragListener = longClickListener;
    }

    public void clearSelection() {
        mSelectedItems.clear();
    }

    public List<LocationPanelViewModel> getSelectedItems() {
        return Collections.unmodifiableList(mSelectedItems);
    }

    public void setInEditMode(boolean value) {
        isInEditMode = value;
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
                int height = context.getResources().getDimensionPixelSize(R.dimen.fab_size);
                spacer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (height * 1.5f)));
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
                    if (vHolder != null && vHolder.mLocView != null) {
                        updatePanelBackground(vHolder, panelView, imageUpdateOnly);
                    }
                }
            });

            if (!imageUpdateOnly) {
                vHolder.bind(panelView);
            }
        }
    }

    private void updatePanelBackground(final LocationPanelViewHolder vHolder, final LocationPanelViewModel panelView, boolean skipCache) {
        if (panelView != null && panelView.getImageData() != null) {
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
    public synchronized int getItemViewType(int position) {
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

        LocationPanelViewModel model = getPanelViewModel(position);
        return model != null ? model.getLocationType() : 0;
    }

    public synchronized int getViewPosition(LocationPanelViewModel item) {
        int position = mDataset.indexOf(item);

        if (position == 0)
            position++;
        else if (hasSearchPanel && hasGPSPanel && position > 0)
            position += 2;
        else if ((hasSearchPanel || hasGPSPanel) && position > 0)
            position++;

        return position;
    }

    public synchronized int getDataPosition(int position) {
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
    public synchronized int getItemCount() {
        int size = mDataset.size();

        if (hasGPSPanel)
            size++;
        if (hasSearchPanel)
            size++;

        size++; // For Footer

        return size;
    }

    public synchronized int getDataCount() {
        return mDataset.size();
    }

    public synchronized int getFavoritesCount() {
        int size = mDataset.size();

        if (hasGPSPanel)
            size--;

        return size;
    }

    public LocationPanelViewModel getGPSPanel() {
        if (hasGPSPanel) {
            LocationData data = getPanelData(0);
            if (data != null && data.getLocationType() == LocationType.GPS) {
                return getPanelViewModel(0);
            }
        }
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

    @Nullable
    public synchronized LocationPanelViewModel getPanelViewModel(int position) {
        if (position >= getItemCount() || position < 0 || mDataset.size() == 0)
            return null;

        int dataPosition = getDataPosition(position);

        if (dataPosition >= mDataset.size()) {
            return null;
        }

        return mDataset.get(dataPosition);
    }

    @Nullable
    public synchronized LocationData getPanelData(int position) {
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
        AnalyticsLogger.logEvent("LocationPanelAdapter: onItemDismiss");

        final LocationPanelViewModel dismissedPanel = getPanelViewModel(position);
        if (dismissedPanel == null) return;

        dismissedPanel.setChecked(false);
        mSelectedItems.remove(dismissedPanel);

        AsyncTask.create(new Callable<Void>() {
            @Override
            public Void call() {
                if (mParentRecyclerView != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            new PanelDeleteHandler(dismissedPanel).deletePanels();
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
                ShortcutCreatorWorker.requestUpdateShortcuts(App.getInstance().getAppContext());
            }
        });
    }

    public static class GPSHeaderViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
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

    public static class FavHeaderViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
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

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public void removeSelectedItems() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                new PanelDeleteHandler(mSelectedItems).deletePanels();
            }
        });
    }

    private class PanelDeleteHandler {
        // For undo
        private List<Pair<Integer, LocationPanelViewModel>> panelPairs;

        PanelDeleteHandler(LocationPanelViewModel panel) {
            panelPairs = Collections.singletonList(new Pair<>(mDataset.indexOf(panel), panel));
        }

        PanelDeleteHandler(List<LocationPanelViewModel> panels) {
            panelPairs = new ArrayList<>(panels.size());

            for (LocationPanelViewModel panel : panels) {
                panelPairs.add(new Pair<>(mDataset.indexOf(panel), panel));
            }
        }

        Runnable undoAction = new Runnable() {
            @Override
            public void run() {
                Collections.sort(panelPairs, new Comparator<Pair<Integer, LocationPanelViewModel>>() {
                    @Override
                    public int compare(Pair<Integer, LocationPanelViewModel> o1, Pair<Integer, LocationPanelViewModel> o2) {
                        return o1.first.compareTo(o2.first);
                    }
                });
                for (Pair<Integer, LocationPanelViewModel> panelPair : panelPairs) {
                    if (panelPair.second != null && !mDataset.contains(panelPair.second)) {
                        panelPair.second.setEditMode(isInEditMode);
                        if (panelPair.first >= mDataset.size()) {
                            add(panelPair.second);
                        } else {
                            add(panelPair.first, panelPair.second);
                        }

                        // End active removal animations if we're undoing the action
                        RecyclerView.ViewHolder holder = mParentRecyclerView.findViewHolderForAdapterPosition(getViewPosition(panelPair.second));
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
        };

        void deletePanels() {
            if (panelPairs.isEmpty()) return;

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (Pair<Integer, LocationPanelViewModel> panelPair : panelPairs) {
                        panelPair.second.setEditMode(false);
                        panelPair.second.setChecked(false);
                        mSelectedItems.remove(panelPair.second);
                        remove(panelPair.second);
                    }

                    // If only a single favorite location is left, revert the deletion
                    if (getFavoritesCount() <= 0) {
                        undoAction.run();
                        if (mParentRecyclerView != null && isFragmentAlive) {
                            if (mSnackMgr == null)
                                mSnackMgr = new SnackbarManager(mParentRecyclerView);
                            mSnackMgr.show(Snackbar.make(R.string.message_needfavorite, Snackbar.Duration.SHORT), null);
                        }
                        return;
                    }

                    showUndoSnackbar();
                }
            });
        }

        private void showUndoSnackbar() {
            if (mParentRecyclerView != null && isFragmentAlive) {
                // Make SnackBar
                final Snackbar snackbar = Snackbar.make(R.string.message_locationremoved, Snackbar.Duration.SHORT);
                snackbar.setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undoAction.run();
                    }
                });
                final com.google.android.material.snackbar.Snackbar.Callback callback = new com.google.android.material.snackbar.Snackbar.Callback() {
                    @Override
                    public void onDismissed(com.google.android.material.snackbar.Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);

                        if (event != DISMISS_EVENT_ACTION) {
                            AsyncTask.run(new Runnable() {
                                @Override
                                public void run() {
                                    for (Pair<Integer, LocationPanelViewModel> panelPair : panelPairs) {
                                        if (panelPair.second == null) return;
                                        String key = panelPair.second.getLocationData().getQuery();
                                        Settings.deleteLocation(key);
                                    }
                                }
                            });
                        }
                    }
                };

                if (mSnackMgr == null)
                    mSnackMgr = new SnackbarManager(mParentRecyclerView);

                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mSnackMgr.show(snackbar, callback);
                    }
                });
            }
        }
    }
}