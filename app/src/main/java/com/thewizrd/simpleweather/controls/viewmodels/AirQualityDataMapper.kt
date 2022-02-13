package com.thewizrd.simpleweather.controls.viewmodels

import android.content.Context
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.utils.AirQualityUtils
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.BarGraphData
import com.thewizrd.simpleweather.controls.graphs.BarGraphDataSet
import com.thewizrd.simpleweather.controls.graphs.BarGraphEntry
import com.thewizrd.simpleweather.controls.graphs.YEntryData

fun List<AirQuality>?.createAQIGraphData(context: Context): BarGraphData? {
    var aqiIndexData: BarGraphData? = null

    this?.forEach { aqi ->
        if (aqi.index != null) {
            if (aqiIndexData == null) {
                aqiIndexData = BarGraphData().apply {
                    graphLabel = context.getString(R.string.label_airquality)
                }
            }

            if (aqiIndexData?.getDataSet() == null) {
                aqiIndexData?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            aqiIndexData?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.index.toFloat(), aqi.index.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.index)
            })
        }
    }

    aqiIndexData?.notifyDataChanged()

    return aqiIndexData
}

fun List<AirQuality>?.createGraphData(context: Context): List<BarGraphData> {
    val graphDataList = mutableListOf<BarGraphData>()
    var aqiIndexData: BarGraphData? = null
    var pm25Data: BarGraphData? = null
    var pm10Data: BarGraphData? = null
    var o3Data: BarGraphData? = null
    var coData: BarGraphData? = null
    var no2Data: BarGraphData? = null
    var so2Data: BarGraphData? = null

    this?.forEach { aqi ->
        if (aqi.index != null) {
            if (aqiIndexData == null) {
                aqiIndexData = BarGraphData().apply {
                    graphLabel = context.getString(R.string.label_airquality)
                }
            }

            if (aqiIndexData?.getDataSet() == null) {
                aqiIndexData?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            aqiIndexData?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.index.toFloat(), aqi.index.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.index)
            })
        }

        if (aqi.pm25 != null) {
            if (pm25Data == null) {
                pm25Data = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_pm25_formatted)
                }
            }

            if (pm25Data?.getDataSet() == null) {
                pm25Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            pm25Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.pm25.toFloat(), aqi.pm25.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.pm25)
            })
        }

        if (aqi.pm10 != null) {
            if (pm10Data == null) {
                pm10Data = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_pm10_formatted)
                }
            }

            if (pm10Data?.getDataSet() == null) {
                pm10Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            pm10Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.pm10.toFloat(), aqi.pm10.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.pm10)
            })
        }

        if (aqi.o3 != null) {
            if (o3Data == null) {
                o3Data = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_o3_formatted)
                }
            }

            if (o3Data?.getDataSet() == null) {
                o3Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            o3Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.o3.toFloat(), aqi.o3.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.o3)
            })
        }

        if (aqi.co != null) {
            if (coData == null) {
                coData = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_co)
                }
            }

            if (coData?.getDataSet() == null) {
                coData?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            coData?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.co.toFloat(), aqi.co.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.co)
            })
        }

        if (aqi.no2 != null) {
            if (no2Data == null) {
                no2Data = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_no2_formatted)
                }
            }

            if (no2Data?.getDataSet() == null) {
                no2Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            no2Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.no2.toFloat(), aqi.no2.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.no2)
            })
        }

        if (aqi.so2 != null) {
            if (so2Data == null) {
                so2Data = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_so2_formatted)
                }
            }

            if (so2Data?.getDataSet() == null) {
                so2Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            so2Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.so2.toFloat(), aqi.so2.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.so2)
            })
        }
    }

    aqiIndexData?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
    pm25Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
    pm10Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
    o3Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
    coData?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
    no2Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
    so2Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }

    return graphDataList
}