package com.thewizrd.weather_api.meteofrance.weather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class AlertsResponse(

    @field:SerializedName("domain_id")
    var domainId: String? = null,

    @field:SerializedName("update_time")
    var updateTime: Long? = null,

    @field:SerializedName("text_avalanche")
    var textAvalanche: Any? = null,

    @field:SerializedName("comments")
    var comments: Comments? = null,

    @field:SerializedName("consequences")
    var consequences: Any? = null,

    @field:SerializedName("advices")
    var advices: Any? = null,

    @field:SerializedName("text")
    var text: Any? = null,

    @field:SerializedName("phenomenons_items")
    var phenomenonsItems: List<PhenomenonsItemsItem?>? = null,

    @field:SerializedName("timelaps")
    var timelaps: List<TimelapsItem?>? = null,

    @field:SerializedName("max_count_items")
    var maxCountItems: Any? = null,

    @field:SerializedName("end_validity_time")
    var endValidityTime: Long? = null,

    @field:SerializedName("color_max")
    var colorMax: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class TextBlocItemItem(

    @field:SerializedName("title_html")
    var titleHtml: String? = null,

    @field:SerializedName("text")
    var text: List<String?>? = null,

    @field:SerializedName("title")
    var title: String? = null,

    //@field:SerializedName("text_html")
    //var textHtml: List<Any?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class PhenomenonsItemsItem(

    @field:SerializedName("phenomenon_max_color_id")
    var phenomenonMaxColorId: Int? = null,

    @field:SerializedName("phenomenon_id")
    var phenomenonId: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class TimelapsItemsItem(

    @field:SerializedName("color_id")
    var colorId: Int? = null,

    @field:SerializedName("begin_time")
    var beginTime: Long? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class TimelapsItem(

    @field:SerializedName("timelaps_items")
    var timelapsItems: List<TimelapsItemsItem?>? = null,

    @field:SerializedName("phenomenon_id")
    var phenomenonId: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Comments(

    @field:SerializedName("end_time")
    var endTime: Long? = null,

    @field:SerializedName("begin_time")
    var beginTime: Long? = null,

    @field:SerializedName("text_bloc_item")
    var textBlocItem: List<TextBlocItemItem?>? = null
)
