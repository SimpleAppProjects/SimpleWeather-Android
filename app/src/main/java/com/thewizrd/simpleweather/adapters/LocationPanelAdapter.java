package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.LocationPanel;
import com.thewizrd.simpleweather.controls.LocationPanelViewModel;
import com.thewizrd.simpleweather.helpers.ColorsUtils;
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapterInterface;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreator;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class LocationPanelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ItemTouchHelperAdapterInterface {

    public static class LocationPanelItemType {
        public static final int GPS_PANEL = LocationType.GPS.getValue();
        public static final int SEARCH_PANEL = LocationType.SEARCH.getValue();
        public static final int HEADER_GPS = -2;
        public static final int HEADER_FAV = -3;
    }

    public interface HeaderSetterInterface {
        void setHeader();
    }

    private ObservableArrayList<LocationPanelViewModel> mDataset;
    private RequestManager mGlide;
    private Handler mMainHandler;

    private GPSHeaderViewHolder gpsVH;
    private FavHeaderViewHolder favVH;
    private boolean hasGPSPanel;
    private boolean hasSearchPanel;

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
    public class ViewHolder extends RecyclerView.ViewHolder {
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
    public LocationPanelAdapter(RequestManager glide) {
        this.mGlide = glide;
        mMainHandler = new Handler(Looper.getMainLooper());
        mDataset = new ObservableArrayList<>();
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case LocationPanelItemType.HEADER_GPS:
                gpsVH = new GPSHeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.locations_header, parent, false));
                return gpsVH;
            case LocationPanelItemType.HEADER_FAV:
                favVH = new FavHeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.locations_header, parent, false));
                return favVH;
            default:
                // create a new view
                LocationPanel v = new LocationPanel(parent.getContext());
                // set the view's size, margins, paddings and layout parameters
                RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, parent.getContext().getResources().getDisplayMetrics());
                layoutParams.setMargins(0, margin, 0, margin); // l, t, r, b
                v.setLayoutParams(layoutParams);
                return new LocationPanelAdapter.ViewHolder(v);
        }
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderSetterInterface) {
            ((HeaderSetterInterface) holder).setHeader();
        } else {
            final ViewHolder vHolder = (ViewHolder) holder;
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final LocationPanelViewModel panelView = getPanelViewModel(position);

            // Background
            if (panelView != null && !StringUtils.isNullOrWhitespace(panelView.getBackground())) {
                mGlide.asBitmap()
                        .load(panelView.getBackground())
                        .apply(new RequestOptions()
                                .centerCrop()
                                .error(vHolder.mLocView.getColorDrawable())
                                .placeholder(vHolder.mLocView.getColorDrawable()))
                        .into(new BitmapImageViewTarget(vHolder.mBgImageView) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                super.onResourceReady(resource, transition);
                                Palette p = Palette.from(resource).generate();
                                int textColor = Colors.WHITE;
                                if (ColorsUtils.isSuperLight(p))
                                    textColor = Colors.BLACK;
                                vHolder.mLocView.setTextColor(textColor);
                            }
                        });
            } else {
                mGlide.clear(vHolder.mBgImageView);
                vHolder.mBgImageView.setImageDrawable(vHolder.mLocView.getColorDrawable());
                vHolder.mLocView.setTextColor(Colors.WHITE);
            }

            vHolder.mLocView.setWeather(panelView);
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
            return LocationPanelItemType.HEADER_GPS;
        else if (hasSearchPanel && position == 0)
            return LocationPanelItemType.HEADER_FAV;
        else if (hasGPSPanel && hasSearchPanel && position == 2)
            return LocationPanelItemType.HEADER_FAV;

        return getPanelViewModel(position).getLocationType();
    }

    public int getViewPosition(LocationPanelViewModel item) {
        int position = mDataset.indexOf(item);

        if (position == 0)
            position++;
        else if (hasSearchPanel && hasGPSPanel && position > 0)
            position += 2;
        else if (hasSearchPanel && position > 0)
            position++;

        return position;
    }

    public int getDataPosition(int position) {
        if (position == 1)
            position--;
        else if (hasSearchPanel && hasGPSPanel && position > 1)
            position -= 2;
        else if (hasSearchPanel && position > 1)
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

        return size;
    }

    public int getDataCount() {
        return mDataset.size();
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
        mDataset.add(index, item);
        notifyItemInserted(getViewPosition(item));

        if (!hasGPSPanel && item.getLocationType() == LocationType.GPS.getValue())
            hasGPSPanel = true;
        else if (!hasSearchPanel)
            hasSearchPanel = true;
    }

    public void remove(int position) {
        LocationPanelViewModel panel = getPanelViewModel(position);

        int dataPosition = getDataPosition(position);
        int viewPosition = getViewPosition(panel);

        mDataset.remove(dataPosition);
        notifyItemRemoved(viewPosition);

        if (panel.getLocationType() == LocationType.GPS.getValue())
            hasGPSPanel = false;
    }

    public void removeAll() {
        int size = getItemCount();
        mDataset.clear();
        notifyItemRangeRemoved(0, size);

        hasGPSPanel = false;
        hasSearchPanel = false;
    }

    public LocationPanelViewModel getPanelViewModel(int position) {
        if (position >= getItemCount())
            return null;

        return mDataset.get(getDataPosition(position));
    }

    public LocationData getPanelData(int position) {
        if (position >= getItemCount())
            return null;

        return getPanelViewModel(position).getLocationData();
    }

    private void removeLocation(final int position) {
        // Remove location from list
        Settings.deleteLocation(
                getPanelData(position).getQuery());

        // Remove panel
        if (Thread.currentThread() != mMainHandler.getLooper().getThread()) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    remove(position);
                }
            });
        } else {
            remove(position);
        }
    }

    public void onItemMove(int fromPosition, int toPosition) {
        mDataset.move(getDataPosition(fromPosition), getDataPosition(toPosition));
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

    public class GPSHeaderViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        GPSHeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
        }

        public void setHeader() {
            header.setText(R.string.label_currentlocation);
        }
    }

    public class FavHeaderViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        FavHeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
        }

        public void setHeader() {
            header.setText(R.string.label_favoritelocations);
        }
    }
}