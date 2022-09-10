package com.thewizrd.simpleweather.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.FavoritesPanelAdapter;
import com.thewizrd.simpleweather.adapters.LocationPanelItemType;

import java.util.ArrayList;
import java.util.List;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private boolean swipeEnabled = true;

    private final ItemTouchHelperAdapter mAdapter;

    private final Drawable deleteIcon;
    private final Drawable deleteBackground;
    private final int iconMargin;

    /* Callback Listener */
    private final List<ItemTouchCallbackListener> mCallbacks;

    public ItemTouchHelperCallback(@NonNull Context context, @NonNull ItemTouchHelperAdapter adapter) {
        mCallbacks = new ArrayList<>();

        mAdapter = adapter;
        Drawable deleteIcoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_delete_outline_24dp);
        deleteIcon = DrawableCompat.wrap(deleteIcoDrawable);
        DrawableCompat.setTint(deleteIcon, ContextUtils.getAttrColor(context, R.attr.colorOnError));
        deleteBackground = ContextCompat.getDrawable(context, R.drawable.swipe_delete);
        iconMargin = context.getResources().getDimensionPixelSize(R.dimen.delete_icon_margin);
    }

    public void addItemTouchHelperCallbackListener(@NonNull ItemTouchCallbackListener listener) {
        mCallbacks.add(listener);
    }

    public void removeItemTouchHelperCallbackListener(@NonNull ItemTouchCallbackListener listener) {
        mCallbacks.remove(listener);
    }

    private void notifyOnMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onMove(recyclerView, viewHolder, target);
        }
    }

    private void notifyOnSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int direction) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onSwiped(viewHolder, direction);
        }
    }

    private void notifyOnClearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onClearView(recyclerView, viewHolder);
        }
    }
    /* Callbacks end */

    @Override
    public boolean isLongPressDragEnabled() {
        // We handle the long press on our side for better touch feedback.
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return swipeEnabled;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = 0;
        int swipeFlags = 0;

        if (viewHolder.getItemViewType() == LocationPanelItemType.SEARCH_PANEL) {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                dragFlags |= ItemTouchHelper.START | ItemTouchHelper.END;
            }

            if (swipeEnabled) {
                swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            }
        }

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
        notifyOnMove(recyclerView, viewHolder, target);
        return true;
    }

    @Override
    public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int direction) {
        notifyOnSwiped(viewHolder, direction);
        mAdapter.onItemDismiss(viewHolder.getBindingAdapterPosition());
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
        if (!(target instanceof FavoritesPanelAdapter.LocationPanelViewHolder))
            return false;

        final FavoritesPanelAdapter adapter = (FavoritesPanelAdapter) target.getBindingAdapter();
        return adapter == null || target.getBindingAdapterPosition() != 1;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (viewHolder.getBindingAdapterPosition() == -1)
            return;

        try {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View itemView = viewHolder.itemView;

                int iconLeft;
                int iconRight;
                int iconTop = itemView.getTop() + (itemView.getBottom() - itemView.getTop() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) {
                    deleteBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + itemView.getWidth(), itemView.getBottom());

                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                } else if (dX < 0) {
                    deleteBackground.setBounds(itemView.getRight() - itemView.getWidth(), itemView.getTop(), itemView.getRight(), itemView.getBottom());

                    iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicHeight();
                    iconRight = itemView.getRight() - iconMargin;
                } else {
                    deleteBackground.setBounds(itemView.getRight(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
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
                final FavoritesPanelAdapter adapter = (FavoritesPanelAdapter) viewHolder.getBindingAdapter();

                if (adapter != null && adapter.getHeaderViewHolder() != null) {
                    upperLimit = adapter.getHeaderViewHolder().itemView.getTop();
                }

                if (topY < upperLimit) {
                    dY = 0;
                } else if (bottomY > recyclerView.getHeight()) {
                    dY = recyclerView.getHeight() - viewHolder.itemView.getHeight() - viewHolder.itemView.getTop();
                }

                if (isCurrentlyActive && viewHolder.itemView instanceof MaterialCardView) {
                    ((MaterialCardView) viewHolder.itemView).setDragged(true);
                }
            } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                if (isCurrentlyActive && viewHolder.itemView instanceof MaterialCardView) {
                    ((MaterialCardView) viewHolder.itemView).setDragged(false);
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

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (viewHolder.itemView instanceof MaterialCardView) {
            ((MaterialCardView) viewHolder.itemView).setDragged(false);
        }
        notifyOnClearView(recyclerView, viewHolder);
    }

    public void setItemViewSwipeEnabled(boolean value) {
        swipeEnabled = value;
    }
}
