package com.thewizrd.shared_resources.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public abstract class ListDiffUtilCallback<T> extends DiffUtil.Callback {
    private List<T> oldList;
    private List<T> newList;

    public ListDiffUtilCallback(@NonNull List<T> oldList, @NonNull List<T> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    public List<T> getOldList() {
        return oldList;
    }

    public List<T> getNewList() {
        return newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public abstract boolean areItemsTheSame(int oldItemPosition, int newItemPosition);

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}