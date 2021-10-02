package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager

class LocationQueryFooterAdapter :
    RecyclerView.Adapter<LocationQueryFooterAdapter.FooterViewHolder>() {
    inner class FooterViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var header: TextView = itemView.findViewById(R.id.header)

        fun bind() {
            val context = header.context
            @WeatherAPI.LocationProviders val locationAPI =
                WeatherManager.instance.getLocationProvider().getLocationAPI()

            val entry = WeatherAPI.LocationAPIs.find { lapi -> locationAPI == lapi.value }
            val credit = String.format(
                "%s %s",
                context.getString(R.string.credit_prefix),
                entry?.toString() ?: WeatherIcons.EM_DASH
            )

            header.text = credit
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        return FooterViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.locations_header, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return 1
    }
}