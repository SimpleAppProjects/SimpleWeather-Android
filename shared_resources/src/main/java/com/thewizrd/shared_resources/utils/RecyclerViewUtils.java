package com.thewizrd.shared_resources.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class RecyclerViewUtils {

    private static int getFirstVisibleItem(int[] firstVisibleItemPositions) {
        int minSize = 0;
        for (int i = 0; i < firstVisibleItemPositions.length; i++) {
            if (i == 0) {
                minSize = firstVisibleItemPositions[i];
            } else if (firstVisibleItemPositions[i] < minSize) {
                minSize = firstVisibleItemPositions[i];
            }
        }
        return minSize;
    }

    private static int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    public static int findFirstVisibleItemPosition(@NonNull RecyclerView recyclerView) {
        RecyclerView.LayoutManager mLayoutMgr = recyclerView.getLayoutManager();
        int firstVisibleItemPosition = 0;

        if (mLayoutMgr instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager sGLayoutMgr = ((StaggeredGridLayoutManager) mLayoutMgr);
            int[] firstVisibleItemPositions = sGLayoutMgr.findFirstVisibleItemPositions(null);
            // get min and maximum element within the list
            firstVisibleItemPosition = getFirstVisibleItem(firstVisibleItemPositions);
        } else if (mLayoutMgr instanceof LinearLayoutManager) {
            LinearLayoutManager linLayoutMgr = ((LinearLayoutManager) mLayoutMgr);
            // get min and maximum element within the list
            firstVisibleItemPosition = linLayoutMgr.findFirstVisibleItemPosition();
        }

        return firstVisibleItemPosition;
    }

    public static int findLastVisibleItemPosition(@NonNull RecyclerView recyclerView) {
        RecyclerView.LayoutManager mLayoutMgr = recyclerView.getLayoutManager();
        int lastVisibleItemPosition = 0;

        if (mLayoutMgr instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager sGLayoutMgr = ((StaggeredGridLayoutManager) mLayoutMgr);
            int[] lastVisibleItemPositions = sGLayoutMgr.findLastVisibleItemPositions(null);
            // get min and maximum element within the list
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
        } else if (mLayoutMgr instanceof LinearLayoutManager) {
            LinearLayoutManager linLayoutMgr = ((LinearLayoutManager) mLayoutMgr);
            // get min and maximum element within the list
            lastVisibleItemPosition = linLayoutMgr.findLastVisibleItemPosition();
        }

        return lastVisibleItemPosition;
    }

    public static int getItemCount(@NonNull RecyclerView recyclerView) {
        RecyclerView.LayoutManager mLayoutMgr = recyclerView.getLayoutManager();
        return mLayoutMgr != null ? mLayoutMgr.getItemCount() : 0;
    }
}
