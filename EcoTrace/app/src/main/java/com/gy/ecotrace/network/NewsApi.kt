package com.gy.ecotrace.network

import com.gy.ecotrace.sections.news.NewsItem
import retrofit2.http.GET

interface NewsApi {
    @GET("news")
    suspend fun getNews(): ApiResponse<List<NewsItem>>
}
