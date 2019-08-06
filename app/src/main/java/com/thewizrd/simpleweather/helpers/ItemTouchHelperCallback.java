package com.thewizrd.simpleweather.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private boolean dragEnabled = true;
    private boolean swipeEnabled = true;

    private ItemTouchHelperAdapterInterface mAdapter;

    private Drawable deleteIcon;
    private Drawable deleteBackground;
    private int iconMargin;

    @Override
    public boolean isLongPressDragEnabled() {
        return dragEnabled;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return swipeEnabled;
    }

    public ItemTouchHelperCallback(ItemTouchHelperAdapterInterface adapter) {
        Context context = App.getInstance().getAppContext();

        mAdapter = adapter;
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white_24dp);
        deleteBackground = new ColorDrawable(ContextCompat.getColor(context, R.color.bg_swipe_delete));
        iconMargin = (int) context.getResources().getDimension(R.dimen.delete_icon_margin);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = 0;
        int swipeFlags = 0;

        if (viewHolder.getItemViewType() != LocationPanelAdapter.LocationPanelItemType.SEARCH_PANEL) {
            dragFlags = 0;
            swipeFlags = 0;
        } else if (dragEnabled && swipeEnabled) {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        } else if (dragEnabled)
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        else if (swipeEnabled)
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
        if (!(target instanceof LocationPanelAdapter.ViewHolder))
            return false;

        LocationPanelAdapter adapter = (LocationPanelAdapter) recyclerView.getAdapter();
        return adapter == null || !adapter.hasGPSHeader() || !adapter.hasSearchHeader() || target.getAdapterPosition() != 1;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (viewHolder.getAdapterPosition() == -1)
            return;

        try {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View itemView = viewHolder.itemView;

                int iconLeft;
                int iconRight;
                int iconTop = itemView.getTop() + (itemView.getBottom() - itemView.getTop() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) {
                    deleteBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());

                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                } else {
                    deleteBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());

                    iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicHeight();
                    iconRight = itemView.getRight() - iconMargin;
                }

                deleteBackground.draw(c);

                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);
            } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                float topY = viewHolder.itemView.getTop() + dY;
                float bottomY = topY + viewHolder.itemView.getHeight();
                float upperLimit = 0;
                LocationPanelAdapter adapter = (LocationPanelAdapter) recyclerView.getAdapter();

                if (adapter != null && adapter.hasSearchHeader() && adapter.hasGPSHeader()) {
                    upperLimit = adapter.getGPSViewHolder().itemView.getHeight() + viewHolder.itemView.getHeight() + adapter.getFavViewHolder().itemView.getHeight();
                } else if (adapter != null && adapter.hasSearchHeader()) {
                    upperLimit = adapter.getFavViewHolder().itemView.getHeight();
                }

                if (topY < upperLimit) {
                    dY = 0;
                } else if (bottomY > recyclerView.getHeight()) {
                    dY = recyclerView.getHeight() - viewHolder.itemView.getHeight() - viewHolder.itemView.getTop();
                }
            }
        } catch (Exception ex) {
            Logger.writeLine(Log.INFO, ex, "SimpleWeather: ItemTouchHelperCallback: object disposed error");
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
        if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS)
            return 350; // Default is 250
        else
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
    }

    public void setLongPressDragEnabled(boolean value) {
        dragEnabled = value;
    }

    public void setItemViewSwipeEnabled(boolean value) {
        swipeEnabled = value;
    }
}
