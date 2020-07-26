package com.thewizrd.simpleweather.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public interface ItemTouchCallbackListener {
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction);

    public void onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target);

    public void onClearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder);
}
