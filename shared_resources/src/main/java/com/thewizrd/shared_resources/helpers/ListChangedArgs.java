package com.thewizrd.shared_resources.helpers;

import java.util.List;

public final class ListChangedArgs<T> {
    public final ListChangedAction action;
    public final int newStartingIndex;
    public final int oldStartingIndex;
    public final List<T> oldItems;
    public final List<T> newItems;

    public ListChangedArgs(ListChangedAction action, int newStartingIndex, int oldStartingIndex) {
        this.action = action;
        this.newStartingIndex = newStartingIndex;
        this.oldStartingIndex = oldStartingIndex;
        this.oldItems = null;
        this.newItems = null;
    }

    public ListChangedArgs(ListChangedAction action, int newStartingIndex, int oldStartingIndex, List<T> oldItems, List<T> newItems) {
        this.action = action;
        this.newStartingIndex = newStartingIndex;
        this.oldStartingIndex = oldStartingIndex;
        this.oldItems = oldItems;
        this.newItems = newItems;
    }
}
