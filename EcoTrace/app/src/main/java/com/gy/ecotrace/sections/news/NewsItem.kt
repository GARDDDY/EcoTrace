package com.gy.ecotrace.sections.news

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

data class NewsItem(
    @SerializedName("source") val source: String = "",
    @SerializedName("fetch_time") val fetchTime: Timestamp? = null,
    @SerializedName("url") val postLink: String = "",
    @SerializedName("image_url") val postImage: String? = null,
    @SerializedName("title") val postTitle: String = "",
    @SerializedName("is_russian") val isRu: Boolean = false
)
