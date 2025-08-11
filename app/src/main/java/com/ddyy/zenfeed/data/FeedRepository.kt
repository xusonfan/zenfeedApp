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
        private const val CACHE_KEY_READ_FEEDS = "read_feeds" // 已读文章ID集合
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
            
            // 检查是否是SSL错误，提供更有用的错误信息
            val errorMessage = when {
                e is javax.net.ssl.SSLException && e.message?.contains("Unable to parse TLS packet header") == true -> {
                    "SSL连接失败：服务器可能不支持HTTPS协议，请检查API地址是否应该使用HTTP协议"
                }
                e is javax.net.ssl.SSLException -> {
                    "SSL连接失败：${e.message}"
                }
                e is java.net.ConnectException -> {
                    "连接失败：无法连接到服务器，请检查网络和API地址"
                }
                e is java.net.SocketTimeoutException -> {
                    "连接超时：服务器响应超时，请检查网络连接"
                }
                else -> {
                    "网络请求失败：${e.message}"
                }
            }
            
            android.util.Log.e("FeedRepository", errorMessage)
            
            // 如果网络请求失败且有缓存数据，返回缓存数据
            val cachedFeeds = getCachedFeeds()
            if (cachedFeeds != null) {
                android.util.Log.d("FeedRepository", "网络请求失败，返回缓存数据")
                return Result.success(FeedResponse(feeds = cachedFeeds))
            }
            
            // 返回包含详细错误信息的失败结果
            Result.failure(Exception(errorMessage, e))
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
            .remove(CACHE_KEY_READ_FEEDS)
            .apply()
        android.util.Log.d("FeedRepository", "Feed 缓存已清除")
    }
    
    /**
     * 保存已读文章ID集合
     */
    fun saveReadFeedIds(readFeedIds: Set<String>) {
        try {
            val readFeedsJson = gson.toJson(readFeedIds.toList())
            sharedPreferences.edit()
                .putString(CACHE_KEY_READ_FEEDS, readFeedsJson)
                .apply()
            android.util.Log.d("FeedRepository", "已读状态已保存，共 ${readFeedIds.size} 条")
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "保存已读状态失败", e)
        }
    }
    
    /**
     * 获取已读文章ID集合
     */
    fun getReadFeedIds(): Set<String> {
        return try {
            val readFeedsJson = sharedPreferences.getString(CACHE_KEY_READ_FEEDS, null)
            if (readFeedsJson != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val readFeedsList = gson.fromJson<List<String>>(readFeedsJson, type)
                readFeedsList.toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "读取已读状态失败", e)
            emptySet()
        }
    }
    
    /**
     * 添加已读文章ID
     */
    fun addReadFeedId(feedId: String) {
        val currentReadIds = getReadFeedIds().toMutableSet()
        currentReadIds.add(feedId)
        saveReadFeedIds(currentReadIds)
    }
    
    /**
     * 移除已读文章ID
     */
    fun removeReadFeedId(feedId: String) {
        val currentReadIds = getReadFeedIds().toMutableSet()
        if (currentReadIds.remove(feedId)) {
            saveReadFeedIds(currentReadIds)
        }
    }
    
    /**
     * 检查文章是否已读
     */
    fun isFeedRead(feedId: String): Boolean {
        return getReadFeedIds().contains(feedId)
    }
}