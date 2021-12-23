/*
 * Copyright 2015 Johannes Homeier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.twoid.ui.decoration;

import android.view.View;

import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * A {@link InsetItemDecoration.SpanSizeLookupHelper} backed by a {@link androidx.recyclerview.widget.StaggeredGridLayoutManager}
 */
public class StaggeredGridSpanSizeLookupHelper extends InsetItemDecoration.SpanSizeLookupHelper<StaggeredGridLayoutManager> {

    protected StaggeredGridSpanSizeLookupHelper(StaggeredGridLayoutManager layoutManager) {
        super(layoutManager);
    }

    @Override
    public int getSpanCount() {
        return layoutManager.getSpanCount();
    }

    @Override
    public int getSpanIndex(int position) {
        StaggeredGridLayoutManager.LayoutParams params = getLayoutParams(position);
        return params.getSpanIndex();
    }

    @Override
    public int getSpanSize(int position) {
        StaggeredGridLayoutManager.LayoutParams params = getLayoutParams(position);
        return params.isFullSpan() ? getSpanCount() : 1;
    }

    @Override
    public int getOrientation() {
        return layoutManager.getOrientation();
    }

    private StaggeredGridLayoutManager.LayoutParams getLayoutParams(int position) {
        View view = layoutManager.getChildAt(position);
        return (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
    }
}