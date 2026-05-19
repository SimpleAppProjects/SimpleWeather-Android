package com.thewizrd.simpleweather.controls.viewmodels

import android.content.Context
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.utils.AirQualityUtils
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.R
import com.thewizrd.simpleweather.controls.graphs.BarGraphData
import com.thewizrd.simpleweather.controls.graphs.BarGraphDataSet
import com.thewizrd.simpleweather.controls.graphs.BarGraphEntry
import com.thewizrd.simpleweather.controls.graphs.ForecastRangeBarEntry
import com.thewizrd.simpleweather.controls.graphs.ForecastRangeBarGraphData
import com.thewizrd.simpleweather.controls.graphs.ForecastRangeBarGraphDataSet
import com.thewizrd.simpleweather.controls.graphs.GraphData
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

            if (aqiIndexData.getDataSet() == null) {
                aqiIndexData.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            aqiIndexData.getDataSet()?.addEntry(BarGraphEntry().apply {
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

fun List<AirQuality>?.createGraphData(context: Context): List<GraphData<*>> {
    val graphDataList = mutableListOf<GraphData<*>>()
    var aqiIndexData: GraphData<*>? = null
    var pm25Data: GraphData<*>? = null
    var pm10Data: GraphData<*>? = null
    var o3Data: GraphData<*>? = null
    var coData: GraphData<*>? = null
    var no2Data: GraphData<*>? = null
    var so2Data: GraphData<*>? = null

    val pm25UseRangeGraph =
        this?.all { it.extras != null && it.extras.pm25Max != null && it.extras.pm25Min != null }
            ?: false
    val pm10UseRangeGraph =
        this?.all { it.extras != null && it.extras.pm10Max != null && it.extras.pm10Min != null }
            ?: false
    val o3UseRangeGraph =
        this?.all { it.extras != null && it.extras.o3Max != null && it.extras.o3Min != null }
            ?: false

    this?.forEach { aqi ->
        if (aqi.index != null) {
            if (aqiIndexData == null) {
                aqiIndexData = BarGraphData().apply {
                    graphLabel = context.getString(R.string.label_airquality)
                }
            }

            if ((aqiIndexData as BarGraphData).getDataSet() == null) {
                aqiIndexData.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            aqiIndexData.getDataSet()?.addEntry(BarGraphEntry().apply {
                xLabel =
                    aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                entryData = YEntryData(aqi.index.toFloat(), aqi.index.toString())
                fillColor = AirQualityUtils.getColorFromIndex(aqi.index)
            })
        }

        if (aqi.pm25 != null) {
            if (pm25Data == null) {
                pm25Data = if (pm25UseRangeGraph) {
                    ForecastRangeBarGraphData().apply {
                        graphLabel = context.getString(R.string.units_pm25_formatted)
                    }
                } else {
                    BarGraphData().apply {
                        graphLabel = context.getString(R.string.units_pm25_formatted)
                    }
                }
            }

            if (pm25Data is ForecastRangeBarGraphData) {
                if (pm25Data.getDataSet() == null) {
                    pm25Data.setDataSet(ForecastRangeBarGraphDataSet(mutableListOf()))
                }

                pm25Data.getDataSet()?.addEntry(ForecastRangeBarEntry().apply {
                    xLabel =
                        aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    if (aqi.extras?.pm25Max != null && aqi.extras?.pm25Min != null) {
                        hiTempData =
                            YEntryData(aqi.extras.pm25Max.toFloat(), aqi.extras.pm25Max.toString())
                        loTempData =
                            YEntryData(aqi.extras.pm25Min.toFloat(), aqi.extras.pm25Min.toString())
                        setFillColors(
                            AirQualityUtils.getColorFromIndex(aqi.extras.pm25Max),
                            AirQualityUtils.getColorFromIndex(aqi.extras.pm25Min)
                        )
                    } else if (aqi.extras?.pm25Max != null) {
                        hiTempData =
                            YEntryData(aqi.extras.pm25Max.toFloat(), aqi.extras.pm25Max.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.extras.pm25Max))
                    } else if (aqi.extras?.pm25Min != null) {
                        loTempData =
                            YEntryData(aqi.extras.pm25Min.toFloat(), aqi.extras.pm25Min.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.extras.pm25Min))
                    } else {
                        hiTempData = YEntryData(aqi.pm25.toFloat(), aqi.pm25.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.pm25))
                    }
                })
            } else if (pm25Data is BarGraphData) {
                if (pm25Data.getDataSet() == null) {
                    pm25Data.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                pm25Data.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel =
                        aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.pm25.toFloat(), aqi.pm25.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.pm25)
                })
            }
        }

        if (aqi.pm10 != null) {
            if (pm10Data == null) {
                pm10Data = if (pm10UseRangeGraph) {
                    ForecastRangeBarGraphData().apply {
                        graphLabel = context.getString(R.string.units_pm10_formatted)
                    }
                } else {
                    BarGraphData().apply {
                        graphLabel = context.getString(R.string.units_pm10_formatted)
                    }
                }
            }

            if (pm10Data is ForecastRangeBarGraphData) {
                if (pm10Data.getDataSet() == null) {
                    pm10Data.setDataSet(ForecastRangeBarGraphDataSet(mutableListOf()))
                }

                pm10Data.getDataSet()?.addEntry(ForecastRangeBarEntry().apply {
                    xLabel =
                        aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    if (aqi.extras?.pm10Max != null && aqi.extras?.pm10Min != null) {
                        hiTempData =
                            YEntryData(aqi.extras.pm10Max.toFloat(), aqi.extras.pm10Max.toString())
                        loTempData =
                            YEntryData(aqi.extras.pm10Min.toFloat(), aqi.extras.pm10Min.toString())
                        setFillColors(
                            AirQualityUtils.getColorFromIndex(aqi.extras.pm10Max),
                            AirQualityUtils.getColorFromIndex(aqi.extras.pm10Min)
                        )
                    } else if (aqi.extras?.pm10Max != null) {
                        hiTempData =
                            YEntryData(aqi.extras.pm10Max.toFloat(), aqi.extras.pm10Max.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.extras.pm10Max))
                    } else if (aqi.extras?.pm10Min != null) {
                        loTempData =
                            YEntryData(aqi.extras.pm10Min.toFloat(), aqi.extras.pm10Min.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.extras.pm10Min))
                    } else {
                        hiTempData = YEntryData(aqi.pm10.toFloat(), aqi.pm10.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.pm10))
                    }
                })
            } else if (pm10Data is BarGraphData) {
                if (pm10Data.getDataSetByIndex(0) == null) {
                    pm10Data.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                pm10Data.getDataSetByIndex(0)?.addEntry(BarGraphEntry().apply {
                    xLabel =
                        aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.pm10.toFloat(), aqi.pm10.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.pm10)
                })
            }
        }

        if (aqi.o3 != null) {
            if (o3Data == null) {
                o3Data = if (o3UseRangeGraph) {
                    ForecastRangeBarGraphData().apply {
                        graphLabel = context.getString(R.string.units_o3_formatted)
                    }
                } else {
                    BarGraphData().apply {
                        graphLabel = context.getString(R.string.units_o3_formatted)
                    }
                }
            }

            if (o3Data is ForecastRangeBarGraphData) {
                if (o3Data.getDataSet() == null) {
                    o3Data.setDataSet(ForecastRangeBarGraphDataSet(mutableListOf()))
                }

                o3Data.getDataSet()?.addEntry(ForecastRangeBarEntry().apply {
                    xLabel =
                        aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    if (aqi.extras?.o3Max != null && aqi.extras?.o3Min != null) {
                        hiTempData =
                            YEntryData(aqi.extras.o3Max.toFloat(), aqi.extras.o3Max.toString())
                        loTempData =
                            YEntryData(aqi.extras.o3Min.toFloat(), aqi.extras.o3Min.toString())
                        setFillColors(
                            AirQualityUtils.getColorFromIndex(aqi.extras.o3Max),
                            AirQualityUtils.getColorFromIndex(aqi.extras.o3Min)
                        )
                    } else if (aqi.extras?.o3Max != null) {
                        hiTempData =
                            YEntryData(aqi.extras.o3Max.toFloat(), aqi.extras.o3Max.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.extras.o3Max))
                    } else if (aqi.extras?.o3Min != null) {
                        loTempData =
                            YEntryData(aqi.extras.o3Min.toFloat(), aqi.extras.o3Min.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.extras.o3Min))
                    } else {
                        hiTempData = YEntryData(aqi.o3.toFloat(), aqi.o3.toString())
                        setFillColor(AirQualityUtils.getColorFromIndex(aqi.o3))
                    }
                })
            } else if (o3Data is BarGraphData) {
                if (o3Data.getDataSetByIndex(0) == null) {
                    o3Data.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                o3Data.getDataSetByIndex(0)?.addEntry(BarGraphEntry().apply {
                    xLabel =
                        aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.o3.toFloat(), aqi.o3.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.o3)
                })
            }
        }

        if (aqi.co != null) {
            if (coData == null) {
                coData = BarGraphData().apply {
                    graphLabel = context.getString(R.string.units_co)
                }
            }

            if ((coData as BarGraphData).getDataSet() == null) {
                coData.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            coData.getDataSetByIndex(0)?.addEntry(BarGraphEntry().apply {
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

            if ((no2Data as BarGraphData).getDataSet() == null) {
                no2Data.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            no2Data.getDataSetByIndex(0)?.addEntry(BarGraphEntry().apply {
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

            if ((so2Data as BarGraphData).getDataSet() == null) {
                so2Data.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                    setMinMax(0f)
                })
            }

            so2Data.getDataSetByIndex(0)?.addEntry(BarGraphEntry().apply {
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