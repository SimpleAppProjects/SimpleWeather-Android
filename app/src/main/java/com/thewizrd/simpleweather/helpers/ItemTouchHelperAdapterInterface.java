package com.thewizrd.simpleweather.helpers;

public interface ItemTouchHelperAdapterInterface {
    void onItemMove(int fromPosition, int toPosition);

    void onItemMoved(int fromPosition, int toPosition);

    void onItemDismiss(int position);
}
