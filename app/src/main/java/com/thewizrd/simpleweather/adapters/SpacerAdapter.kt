package com.thewizrd.simpleweather.adapters

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Space
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.simpleweather.R

class SpacerAdapter(@Px private val spacerSize: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(
            Space(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    MATCH_PARENT, spacerSize
                )
            }
        ) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // no-op
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun getItemViewType(position: Int): Int {
        return R.id.spacer
    }
}