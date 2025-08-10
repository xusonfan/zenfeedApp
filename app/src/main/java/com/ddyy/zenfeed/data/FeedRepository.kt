package com.ddyy.zenfeed.data

import android.content.Context
import android.content.SharedPreferences
import com.ddyy.zenfeed.data.network.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Feed数据仓库
 * 负责从API获取Feed数据，支持动态配置API地址和后端URL
 */
class FeedRepository(private val context: Context) {
    
    private val settingsDataStore = SettingsDataStore(context)
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("feed_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val CACHE_KEY_FEEDS = "cached_feeds"
        private const val CACHE_KEY_TIMESTAMP = "cache_timestamp"
        private const val CACHE_EXPIRY_HOURS = 1 // 缓存过期时间（小时）
    }

    /**
     * 获取Feed列表
     * @return Feed响应结果
     */
    suspend fun getFeeds(useCache: Boolean = true): Result<FeedResponse> {
        return try {
            // 如果允许使用缓存且缓存有效，则返回缓存数据
            if (useCache && isCacheValid()) {
                val cachedFeeds = getCachedFeeds()
                if (cachedFeeds != null) {
                    android.util.Log.d("FeedRepository", "从缓存加载 Feed 数据")
                    return Result.success(FeedResponse(feeds = cachedFeeds))
                }
            }
            
            // 从网络获取新数据
            val now = Date()
            val start = Date(now.time - 24 * 60 * 60 * 1000) // 24 hours ago
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val requestBody = FeedRequest(
                start = dateFormat.format(start),
                end = dateFormat.format(now),
                limit = 500,
                query = "",
                summarize = false
            )
            
            // 使用动态API服务和后端URL
            val apiService = ApiClient.getApiService(context)
            val backendUrl = settingsDataStore.backendUrl.first()
            val response = apiService.getFeeds(
                backendUrl = backendUrl,
                body = requestBody
            )
            
            // 缓存新获取的数据
            cacheFeeds(response.feeds)
            android.util.Log.d("FeedRepository", "从网络获取并缓存 Feed 数据")
            
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "获取摘要失败", e)
            
            // 如果网络请求失败且有缓存数据，返回缓存数据
            val cachedFeeds = getCachedFeeds()
            if (cachedFeeds != null) {
                android.util.Log.d("FeedRepository", "网络请求失败，返回缓存数据")
                return Result.success(FeedResponse(feeds = cachedFeeds))
            }
            
            Result.failure(e)
        }
    }
    
    /**
     * 获取缓存的 Feed 数据
     */
    suspend fun getCachedFeeds(): List<Feed>? {
        return try {
            val feedsJson = sharedPreferences.getString(CACHE_KEY_FEEDS, null)
            if (feedsJson != null) {
                val type = object : TypeToken<List<Feed>>() {}.type
                gson.fromJson<List<Feed>>(feedsJson, type)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "读取缓存失败", e)
            null
        }
    }
    
    /**
     * 缓存 Feed 数据
     */
    private fun cacheFeeds(feeds: List<Feed>) {
        try {
            val feedsJson = gson.toJson(feeds)
            sharedPreferences.edit()
                .putString(CACHE_KEY_FEEDS, feedsJson)
                .putLong(CACHE_KEY_TIMESTAMP, System.currentTimeMillis())
                .apply()
            android.util.Log.d("FeedRepository", "Feed 数据已缓存，共 ${feeds.size} 条")
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "缓存数据失败", e)
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        val cacheTimestamp = sharedPreferences.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - cacheTimestamp
        val cacheExpiryTime = CACHE_EXPIRY_HOURS * 60 * 60 * 1000 // 转换为毫秒
        
        return cacheAge < cacheExpiryTime
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        sharedPreferences.edit()
            .remove(CACHE_KEY_FEEDS)
            .remove(CACHE_KEY_TIMESTAMP)
            .apply()
        android.util.Log.d("FeedRepository", "Feed 缓存已清除")
    }
}