package com.thewizrd.simpleweather.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.thewizrd.shared_resources.controls.DetailItemViewModel
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.utils.sequenceEqual
import com.thewizrd.simpleweather.controls.DetailCard
import java.util.*

class DetailsItemGridAdapter : BaseAdapter() {
    private var mDataset: List<DetailItemViewModel>? = null

    override fun getCount(): Int {
        return mDataset?.size ?: 0
    }

    override fun getItem(position: Int): Any? {
        return mDataset?.get(position)
    }

    override fun getItemId(position: Int): Long {
        return mDataset?.get(position)?.detailsType?.value?.toLong() ?: 0L
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView as? DetailCard ?: DetailCard(parent.context)
        view.setStrokeWidth(ContextUtils.dpToPx(parent.context, 1f))

        view.bindModel(mDataset!![position])

        return view
    }

    fun updateItems(dataset: List<DetailItemViewModel>?) {
        if (mDataset == null || !sequenceEqual(mDataset!!, dataset!!)) {
            mDataset = ArrayList(dataset)
            notifyDataSetChanged()
        }
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}