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

import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * A {@link InsetItemDecoration.SpanSizeLookupHelper} backed by a {@link androidx.recyclerview.widget.LinearLayoutManager}
 */
public class LinearSpanSizeLookupHelper extends InsetItemDecoration.SpanSizeLookupHelper<LinearLayoutManager> {

    public LinearSpanSizeLookupHelper(LinearLayoutManager layoutManager) {
        super(layoutManager);
    }

    @Override
    public int getSpanCount() {
        return 1;
    }

    /**
     * As the {@link androidx.recyclerview.widget.LinearLayoutManager} only has one column, this method always returns 0;
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int getSpanIndex(int position) {
        return 0;
    }

    /**
     * As the {@link androidx.recyclerview.widget.LinearLayoutManager} only has one column, this method always returns 1;
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int getSpanSize(int position) {
        return 1;
    }

    @Override
    public int getOrientation() {
        return layoutManager.getOrientation();
    }
}