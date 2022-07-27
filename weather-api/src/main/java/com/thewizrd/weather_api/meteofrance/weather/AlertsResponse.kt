package com.thewizrd.weather_api.meteofrance.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlertsResponse(

    @field:Json(name = "domain_id")
    var domainId: String? = null,

    @field:Json(name = "update_time")
    var updateTime: Long? = null,

    @field:Json(name = "text_avalanche")
    var textAvalanche: Any? = null,

    @field:Json(name = "comments")
    var comments: Comments? = null,

    @field:Json(name = "consequences")
    var consequences: Any? = null,

    @field:Json(name = "advices")
    var advices: Any? = null,

    @field:Json(name = "text")
    var text: Any? = null,

    @field:Json(name = "phenomenons_items")
    var phenomenonsItems: List<PhenomenonsItemsItem?>? = null,

    @field:Json(name = "timelaps")
    var timelaps: List<TimelapsItem?>? = null,

    @field:Json(name = "max_count_items")
    var maxCountItems: Any? = null,

    @field:Json(name = "end_validity_time")
    var endValidityTime: Long? = null,

    @field:Json(name = "color_max")
    var colorMax: Int? = null
)

@JsonClass(generateAdapter = true)
data class TextBlocItemItem(

    @field:Json(name = "title_html")
    var titleHtml: String? = null,

    @field:Json(name = "text")
    var text: List<String?>? = null,

    @field:Json(name = "title")
    var title: String? = null,

    //@field:Json(name = "text_html")
    //var textHtml: List<Any?>? = null
)

@JsonClass(generateAdapter = true)
data class PhenomenonsItemsItem(

    @field:Json(name = "phenomenon_max_color_id")
    var phenomenonMaxColorId: Int? = null,

    @field:Json(name = "phenomenon_id")
    var phenomenonId: Int? = null
)

@JsonClass(generateAdapter = true)
data class TimelapsItemsItem(

    @field:Json(name = "color_id")
    var colorId: Int? = null,

    @field:Json(name = "begin_time")
    var beginTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class TimelapsItem(

    @field:Json(name = "timelaps_items")
    var timelapsItems: List<TimelapsItemsItem?>? = null,

    @field:Json(name = "phenomenon_id")
    var phenomenonId: Int? = null
)

@JsonClass(generateAdapter = true)
data class Comments(

    @field:Json(name = "end_time")
    var endTime: Long? = null,

    @field:Json(name = "begin_time")
    var beginTime: Long? = null,

    @field:Json(name = "text_bloc_item")
    var textBlocItem: List<TextBlocItemItem?>? = null
)
