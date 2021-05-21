package com.thewizrd.shared_resources.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.databinding.LocationQueryViewBinding
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import com.thewizrd.shared_resources.weatherdata.WeatherManager.Companion.instance
import java.util.*

class LocationQueryAdapter(myDataset: List<LocationQueryViewModel>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mDataset: MutableList<LocationQueryViewModel>
    private var onClickListener: RecyclerOnClickListenerInterface? = null

    fun setOnClickListener(onClickListener: RecyclerOnClickListenerInterface?) {
        this.onClickListener = onClickListener
    }

    val dataset: MutableList<LocationQueryViewModel>
        get() = mDataset

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    internal inner class ViewHolder(private val binding: LocationQueryViewBinding) :
            RecyclerView.ViewHolder(binding.root) {
        fun bind(model: LocationQueryViewModel?) {
            binding.viewModel = model
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnClickListener { v -> if (onClickListener != null) onClickListener!!.onClick(v, adapterPosition) }
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        mDataset = ArrayList(myDataset)
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> FooterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.locations_header, parent, false))
            else -> {
                // create a new view
                val inflater = LayoutInflater.from(parent.context)
                val binding = LocationQueryViewBinding.inflate(inflater)
                binding.root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                ViewHolder(binding)
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderSetterInterface) {
            (holder as HeaderSetterInterface).setHeader()
        } else {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val vh = holder as ViewHolder
            vh.bind(mDataset[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == itemCount - 1)
            return 1

        return 0
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        if (mDataset.size == 0)
            return 0

        return mDataset.size + 1
    }

    val dataCount: Int
        get() = mDataset.size

    fun setLocations(myDataset: List<LocationQueryViewModel>) {
        mDataset.clear()
        mDataset.addAll(myDataset)
        notifyDataSetChanged()
    }

    interface HeaderSetterInterface {
        fun setHeader()
    }

    inner class FooterViewHolder internal constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView), HeaderSetterInterface {
        var header: TextView = itemView.findViewById(R.id.header)

        override fun setHeader() {
            val context = header.context
            @LocationProviders val locationAPI = instance.getLocationProvider().getLocationAPI()

            val entry = WeatherAPI.LocationAPIs.find { lapi -> locationAPI == lapi.value }
            val credit = String.format("%s %s",
                    context.getString(R.string.credit_prefix),
                    entry?.toString() ?: WeatherIcons.EM_DASH)

            header.text = credit
        }

    }
}