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

import androidx.recyclerview.widget.GridLayoutManager;

/**
 * A {@link InsetItemDecoration.SpanSizeLookupHelper} backed by a {@link androidx.recyclerview.widget.GridLayoutManager}
 */
public class GridSpanSizeLookupHelper extends InsetItemDecoration.SpanSizeLookupHelper<GridLayoutManager> {
    private final GridLayoutManager.SpanSizeLookup spanSizeLookup;

    public GridSpanSizeLookupHelper(GridLayoutManager layoutManager) {
        super(layoutManager);
        spanSizeLookup = layoutManager.getSpanSizeLookup();
        spanSizeLookup.setSpanIndexCacheEnabled(true);
    }

    @Override
    public int getSpanCount() {
        return layoutManager.getSpanCount();
    }

    @Override
    public int getSpanIndex(int position) {
        return spanSizeLookup.getSpanIndex(position, getSpanCount());
    }

    @Override
    public int getSpanSize(int position) {
        return spanSizeLookup.getSpanSize(position);
    }

    @Override
    public int getOrientation() {
        return layoutManager.getOrientation();
    }
}