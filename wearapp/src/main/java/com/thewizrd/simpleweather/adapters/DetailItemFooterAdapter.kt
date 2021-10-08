package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.R

class DetailItemFooterAdapter : RecyclerView.Adapter<DetailItemFooterAdapter.FooterViewHolder>() {
    private interface HeaderSetterInterface {
        fun setHeader()
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        HeaderSetterInterface {
        private val header: TextView = itemView.findViewById(R.id.header)

        init {
            header.setTextColor(Colors.WHITE)
        }

        override fun setHeader() {
            val context = header.context
            val creditPrefix = context.getString(R.string.credit_prefix)
            val weatherAPI = WeatherManager.instance.getWeatherAPI()

            val entry =
                WeatherAPI.APIs.find { wapi: ProviderEntry? -> wapi != null && weatherAPI == wapi.value }
            val credit = String.format(
                "%s %s",
                creditPrefix,
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
        holder.setHeader()
    }

    override fun getItemCount(): Int {
        return 1
    }
}