package com.ddyy.zenfeed.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.ddyy.zenfeed.BuildConfig
import com.ddyy.zenfeed.ZenFeedApplication
import com.ddyy.zenfeed.data.model.GithubRelease
import com.ddyy.zenfeed.data.network.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val faviconManager: FaviconManager by lazy {
        (context.applicationContext as ZenFeedApplication).faviconManager
    }

    companion object {
        private const val CACHE_KEY_FEEDS = "cached_feeds"
        private const val CACHE_KEY_TIMESTAMP = "cache_timestamp"
        private const val CACHE_KEY_READ_FEEDS = "read_feeds" // 已读文章ID集合
        private const val CACHE_KEY_SEARCH_HISTORY = "search_history" // 搜索历史记录
        private const val CACHE_EXPIRY_HOURS = 1 // 缓存过期时间（小时）
    }

    /**
     * 获取Feed列表
     * @return Feed响应结果
     */
    suspend fun getFeeds(useCache: Boolean = true, hours: Int = 24, query: String = "", threshold: Float? = null, limit: Int = 500): Result<FeedResponse> {
        return try {
            // 如果允许使用缓存且缓存有效，则返回缓存数据
            if (useCache && isCacheValid()) {
                val cachedFeeds = getCachedFeeds()
                if (cachedFeeds != null) {
                    Log.d("FeedRepository", "从缓存加载 Feed 数据")
                    return Result.success(
                        FeedResponse(
                            feeds = cachedFeeds,
                            count = cachedFeeds.size
                        )
                    )
                }
            }

            // 从网络获取新数据
            val now = Date()
            val start = Date(now.time - hours * 60 * 60 * 1000L) // 根据传入的小时数计算开始时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val requestBody = FeedRequest(
                start = dateFormat.format(start),
                end = dateFormat.format(now),
                limit = limit,
                query = query,
                threshold = threshold,
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
            Log.d("FeedRepository", "从网络获取并缓存 Feed 数据")

            Result.success(response)
        } catch (e: Exception) {
            Log.e("FeedRepository", "获取摘要失败", e)

            // 检查是否是SSL错误，提供更有用的错误信息
            val errorMessage = when {
                e is javax.net.ssl.SSLException && e.message?.contains("Unable to parse TLS packet header") == true -> {
                    "SSL连接失败：服务器可能不支持HTTPS协议，请检查API地址是否应该使用HTTP协议"
                }
                e is javax.net.ssl.SSLException -> {
                    "SSL连接失败：${e.message}"
                }
                e is java.net.ConnectException -> {
                    "连接失败：无法连接到服务器，请检查网络、代理设置和API地址"
                }
                e is java.net.SocketTimeoutException -> {
                    "连接超时：服务器响应超时，请检查网络连接"
                }
                else -> {
                    "网络请求失败：${e.message}"
                }
            }

            Log.e("FeedRepository", errorMessage)

            // 如果网络请求失败且有缓存数据，返回缓存数据但仍然通知UI有网络错误
            val cachedFeeds = getCachedFeeds()
            if (cachedFeeds != null) {
                Log.d("FeedRepository", "网络请求失败，返回缓存数据")
                // 创建包含缓存数据和错误信息的复合结果
                return Result.success(
                    FeedResponse(
                        feeds = cachedFeeds,
                        count = cachedFeeds.size,
                        error = errorMessage
                    )
                )
            }

            // 返回包含详细错误信息的失败结果
            Result.failure(Exception(errorMessage, e))
        }
    }

    /**
     * 获取缓存的 Feed 数据
     */
    fun getCachedFeeds(): List<Feed>? {
        return try {
            val feedsJson = sharedPreferences.getString(CACHE_KEY_FEEDS, null)
            if (feedsJson != null) {
                val type = object : TypeToken<List<Feed>>() {}.type
                gson.fromJson(feedsJson, type)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FeedRepository", "读取缓存失败", e)
            null
        }
    }

    /**
     * 缓存 Feed 数据
     */
    private fun cacheFeeds(feeds: List<Feed>) {
        try {
            val feedsJson = gson.toJson(feeds)
            sharedPreferences.edit {
                putString(CACHE_KEY_FEEDS, feedsJson)
                    .putLong(CACHE_KEY_TIMESTAMP, System.currentTimeMillis())
            }
            Log.d("FeedRepository", "Feed 数据已缓存，共 ${feeds.size} 条")
        } catch (e: Exception) {
            Log.e("FeedRepository", "缓存数据失败", e)
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
     * 获取总缓存大小（单位：字节）
     * 包括 SharedPreferences、图标缓存和网络缓存
     */
    fun getCacheSize(): Long {
        var totalSize = 0L
        try {
            // 1. SharedPreferences 缓存大小
            val feedsJson = sharedPreferences.getString(CACHE_KEY_FEEDS, null)
            val readFeedsJson = sharedPreferences.getString(CACHE_KEY_READ_FEEDS, null)
            val searchHistoryJson = sharedPreferences.getString(CACHE_KEY_SEARCH_HISTORY, null)
            feedsJson?.let { totalSize += it.toByteArray().size }
            readFeedsJson?.let { totalSize += it.toByteArray().size }
            searchHistoryJson?.let { totalSize += it.toByteArray().size }

            // 2. 图标缓存大小
            totalSize += faviconManager.getCacheSize()

            // 3. OkHttp 缓存大小
            val okhttpCacheDir = File(context.cacheDir, "okhttp")
            if (okhttpCacheDir.exists()) {
                totalSize += okhttpCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }

            // 4. 播客媒体缓存大小
            val mediaCacheDir = File(context.cacheDir, "media_cache")
            if (mediaCacheDir.exists()) {
                totalSize += mediaCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }

        } catch (e: Exception) {
            Log.e("FeedRepository", "计算缓存大小失败", e)
        }
        return totalSize
    }

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        // 1. 清除 SharedPreferences 缓存
        sharedPreferences.edit {
            remove(CACHE_KEY_FEEDS)
                .remove(CACHE_KEY_TIMESTAMP)
                .remove(CACHE_KEY_READ_FEEDS)
                .remove(CACHE_KEY_SEARCH_HISTORY)
        }
        Log.d("FeedRepository", "SharedPreferences 缓存已清除")

        // 2. 清除图标缓存
        faviconManager.clearCache()
        Log.d("FeedRepository", "图标缓存已清除")

        // 3. 清除 OkHttp 缓存
        val okhttpCacheDir = File(context.cacheDir, "okhttp")
        if (okhttpCacheDir.exists()) {
            if (okhttpCacheDir.deleteRecursively()) {
                Log.d("FeedRepository", "OkHttp 缓存已清除")
            } else {
                Log.e("FeedRepository", "清除 OkHttp 缓存失败")
            }
        }

        // 4. 清除播客媒体缓存
        val mediaCacheDir = File(context.cacheDir, "media_cache")
        if (mediaCacheDir.exists()) {
            if (mediaCacheDir.deleteRecursively()) {
                Log.d("FeedRepository", "播客媒体缓存已清除")
            } else {
                Log.e("FeedRepository", "清除播客媒体缓存失败")
            }
        }
    }

    /**
     * 保存已读文章ID集合
     */
    fun saveReadFeedIds(readFeedIds: Set<String>) {
        try {
            val readFeedsJson = gson.toJson(readFeedIds.toList())
            sharedPreferences.edit {
                putString(CACHE_KEY_READ_FEEDS, readFeedsJson)
            }
            Log.d("FeedRepository", "已读状态已保存，共 ${readFeedIds.size} 条")
        } catch (e: Exception) {
            Log.e("FeedRepository", "保存已读状态失败", e)
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
            Log.e("FeedRepository", "读取已读状态失败", e)
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

    /**
     * 保存搜索历史
     */
    fun saveSearchHistory(searchHistory: List<String>) {
        try {
            val searchHistoryJson = gson.toJson(searchHistory)
            sharedPreferences.edit {
                putString(CACHE_KEY_SEARCH_HISTORY, searchHistoryJson)
            }
            Log.d("FeedRepository", "搜索历史已保存，共 ${searchHistory.size} 条")
        } catch (e: Exception) {
            Log.e("FeedRepository", "保存搜索历史失败", e)
        }
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(): List<String> {
        return try {
            val searchHistoryJson = sharedPreferences.getString(CACHE_KEY_SEARCH_HISTORY, null)
            if (searchHistoryJson != null) {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(searchHistoryJson, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("FeedRepository", "读取搜索历史失败", e)
            emptyList()
        }
    }
    /**
     * 检查应用更新
     * @return 如果有新版本，则返回 GithubRelease 对象，否则返回 null
     */
    suspend fun checkForUpdate(): GithubRelease? {
        return try {
            val apiService = ApiClient.getApiService(context)
            // 替换为你的 GitHub 用户名和仓库名
            val url = "https://api.github.com/repos/xusonfan/zenfeedApp/releases/latest"
            val latestRelease = apiService.getLatestRelease(url)

            // 比较版本号
            // 我们移除 'v' 前缀来进行比较
            val latestVersion = latestRelease.tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            if (isNewerVersion(latestVersion, currentVersion)) {
                latestRelease
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FeedRepository", "检查更新失败", e)
            null
        }
    }

    /**
     * 比较两个版本字符串
     * 例如： "1.0.1" > "1.0.0"
     */
    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val partCount = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until partCount) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}