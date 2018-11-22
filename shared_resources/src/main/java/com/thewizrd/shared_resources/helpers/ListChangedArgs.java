package com.thewizrd.shared_resources.helpers;

public final class ListChangedArgs {
    public final ListChangedAction action;
    public final int newStartingIndex;
    public final int oldStartingIndex;

    public ListChangedArgs(ListChangedAction action, int newStartingIndex, int oldStartingIndex) {
        this.action = action;
        this.newStartingIndex = newStartingIndex;
        this.oldStartingIndex = oldStartingIndex;
    }
}
