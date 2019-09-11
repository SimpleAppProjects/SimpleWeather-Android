package com.thewizrd.simpleweather.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public interface ItemTouchCallbackListener {
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction);
}
