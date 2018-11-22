package com.thewizrd.shared_resources.helpers;

import java.util.ArrayList;

public abstract class OnListChangedListener<T> {
    /**
     * Called whenever a change of unknown type has occurred, such as the entire list being
     * set to new values.
     *
     * @param sender The changing list.
     * @param args   The data for the onChanged event.
     */
    public abstract void onChanged(ArrayList<T> sender, ListChangedArgs args);
}
