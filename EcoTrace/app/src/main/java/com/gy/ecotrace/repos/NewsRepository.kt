package com.gy.ecotrace.repos

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import com.gy.ecotrace.network.NewsApi
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.sections.news.NewsItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val api: NewsApi,
    @ApplicationContext private val context: Context
) {

    suspend fun fetchNews(): List<NewsItem> {
        return try {
            val news = apiCall { api.getNews() }
            saveToCache(news)
            news
        } catch (e: Exception) {
            getCachedNews()
        }
    }

    private suspend fun saveToCache(news: List<NewsItem>) = withContext(Dispatchers.IO) {
        val json = Gson().toJson(news)
        context.getSharedPreferences("cache", Context.MODE_PRIVATE)
            .edit {
                putString("cached_news", json)
            }
    }

    private suspend fun getCachedNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        val json = context.getSharedPreferences("cache", Context.MODE_PRIVATE)
            .getString("cached_news", null) ?: return@withContext emptyList()
        try {
            val type = object : TypeToken<List<NewsItem>>() {}.type
            Gson().fromJson<List<NewsItem>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
