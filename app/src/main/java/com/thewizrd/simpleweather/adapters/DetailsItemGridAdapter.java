package com.thewizrd.simpleweather.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.simpleweather.controls.DetailCard;

import java.util.ArrayList;
import java.util.List;

public class DetailsItemGridAdapter extends BaseAdapter {
    private List<DetailItemViewModel> mDataset;

    @Override
    public int getCount() {
        return mDataset != null ? mDataset.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mDataset != null ? mDataset.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return mDataset != null && mDataset.size() > 0 ? mDataset.get(position).getDetailsType().getValue() : 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DetailCard view = convertView != null ? (DetailCard) convertView : new DetailCard(parent.getContext());
        view.setStrokeWidth((int) ContextUtils.dpToPx(parent.getContext(), 1));

        view.bindModel(mDataset.get(position));

        return view;
    }

    public void updateItems(final List<DetailItemViewModel> dataset) {
        if (mDataset == null || !Iterables.elementsEqual(mDataset, dataset)) {
            mDataset = new ArrayList<>(dataset);
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
