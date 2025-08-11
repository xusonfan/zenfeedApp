package com.ddyy.zenfeed.ui.feeds

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.FeedRepository
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface FeedsUiState {
    data class Success(val feeds: List<Feed>, val categories: List<String>) : FeedsUiState
    data object Error : FeedsUiState
    data object Loading : FeedsUiState
}

/**
 * Feeds页面的ViewModel
 * 继承AndroidViewModel以获取Application context用于API配置
 */
class FeedsViewModel(application: Application) : AndroidViewModel(application) {

    // 用于存储每个分类列表的滚动位置 <Category, Pair<Index, Offset>>
    val scrollPositions = mutableMapOf<String, Pair<Int, Int>>()
    
    private val feedRepository = FeedRepository(application.applicationContext)

    var feedsUiState: FeedsUiState by mutableStateOf(FeedsUiState.Loading)
        private set
    
    // 下拉刷新状态
    var isRefreshing: Boolean by mutableStateOf(false)
        private set
    
    // 背景刷新状态（用于在有缓存数据时显示顶部刷新指示器）
    var isBackgroundRefreshing: Boolean by mutableStateOf(false)
        private set
    
    // 原始的完整Feed列表
    private var allFeeds: List<Feed> = emptyList()
    
    // 已读文章的标识符集合（使用标题+时间作为唯一标识）
    private val readFeedIds = mutableSetOf<String>()
    
    // 当前选中的分类，空字符串表示显示全部
    var selectedCategory: String by mutableStateOf("")
        private set

    init {
        loadReadFeedIds()
        loadCachedFeeds()
        getFeeds()
    }

    /**
     * 从持久化存储加载已读文章ID集合
     */
    private fun loadReadFeedIds() {
        val persistedReadIds = feedRepository.getReadFeedIds()
        readFeedIds.clear()
        readFeedIds.addAll(persistedReadIds)
        Log.d("FeedsViewModel", "已加载已读状态，共 ${readFeedIds.size} 条")
    }
    
    /**
     * 加载缓存的Feed列表（应用启动时调用）
     */
    private fun loadCachedFeeds() {
        viewModelScope.launch {
            feedsUiState = FeedsUiState.Loading
            val cachedFeeds = feedRepository.getCachedFeeds()
            if (cachedFeeds != null && cachedFeeds.isNotEmpty()) {
                allFeeds = cachedFeeds
                updateFilteredFeeds()
                Log.d("FeedsViewModel", "已加载缓存数据，共 ${cachedFeeds.size} 条")
            }
        }
    }
    
    /**
     * 获取Feed列表（从网络获取新数据）
     */
    fun getFeeds() {
        viewModelScope.launch {
            // 如果当前没有数据，显示加载状态
            if (allFeeds.isEmpty()) {
                feedsUiState = FeedsUiState.Loading
            } else {
                // 如果有缓存数据，显示背景刷新状态
                isBackgroundRefreshing = true
            }
            
            val result = feedRepository.getFeeds(useCache = false) // 强制从网络获取
            if (result.isSuccess) {
                val newFeeds = result.getOrNull()?.feeds ?: emptyList()
                if (newFeeds != allFeeds) { // 只有数据不同时才更新
                    allFeeds = newFeeds
                    updateFilteredFeeds()
                    Log.d("FeedsViewModel", "已更新网络数据，共 ${newFeeds.size} 条")
                }
            } else {
                // 如果网络获取失败但有缓存数据，不改变当前状态
                if (allFeeds.isEmpty()) {
                    feedsUiState = FeedsUiState.Error
                }
            }
            
            // 关闭背景刷新状态
            isBackgroundRefreshing = false
        }
    }
    
    /**
     * 下拉刷新获取Feed列表
     */
    fun refreshFeeds() {
        viewModelScope.launch {
            isRefreshing = true
            val result = feedRepository.getFeeds(useCache = false) // 强制从网络获取
            if (result.isSuccess) {
                val newFeeds = result.getOrNull()?.feeds ?: emptyList()
                allFeeds = newFeeds
                updateFilteredFeeds()
                Log.d("FeedsViewModel", "刷新完成，共 ${newFeeds.size} 条")
            } else {
                // 刷新失败时保持当前数据不变
                if (allFeeds.isEmpty()) {
                    feedsUiState = FeedsUiState.Error
                }
            }
            isRefreshing = false
        }
    }
    
    /**
     * 选择分类进行筛选
     */
    fun selectCategory(category: String) {
        selectedCategory = category
        // 这里不再调用 updateFilteredFeeds()，因为UI层会处理筛选
    }
    
    /**
     * 标记文章为已读
     */
    fun markFeedAsRead(feed: Feed) {
        val feedId = "${feed.labels.title ?: ""}-${feed.time}"
        if (!readFeedIds.contains(feedId)) {
            readFeedIds.add(feedId)
            // 立即持久化到存储
            feedRepository.addReadFeedId(feedId)
            updateFilteredFeeds() // 重新更新UI以反映阅读状态变化
            Log.d("FeedsViewModel", "标记文章为已读: ${feed.labels.title ?: "未知标题"}")
        }
    }
    
    /**
     * 标记文章为未读
     */
    fun markFeedAsUnread(feed: Feed) {
        val feedId = "${feed.labels.title ?: ""}-${feed.time}"
        if (readFeedIds.contains(feedId)) {
            readFeedIds.remove(feedId)
            // 立即从持久化存储中移除
            feedRepository.removeReadFeedId(feedId)
            updateFilteredFeeds() // 重新更新UI以反映阅读状态变化
            Log.d("FeedsViewModel", "标记文章为未读: ${feed.labels.title ?: "未知标题"}")
        }
    }
    
    /**
     * 检查文章是否已读
     */
    private fun isFeedRead(feed: Feed): Boolean {
        val feedId = "${feed.labels.title ?: ""}-${feed.time}"
        return readFeedIds.contains(feedId)
    }
    
    /**
     * 更新筛选后的Feed列表
     */
    private fun updateFilteredFeeds() {
        // 为所有Feed设置正确的阅读状态并按时间倒序排序
        val feedsWithReadStatus = allFeeds
            .map { feed ->
                feed.copy(isRead = isFeedRead(feed))
            }
            .sortedWith(compareByDescending<Feed> { feed ->
                // 主要排序：按时间倒序
                parseTimeToLong(feed.time)
            }.thenBy { feed ->
                // 次要排序：相同时间时按标题字母顺序，确保稳定性
                feed.labels.title ?: "未知标题"
            })
        
        val categories = allFeeds
            .mapNotNull { it.labels.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        
        feedsUiState = FeedsUiState.Success(
            feeds = feedsWithReadStatus, // 返回按时间倒序排序的列表
            categories = categories
        )
        
        Log.d("FeedsViewModel", "Feed列表已按时间倒序排序，共 ${feedsWithReadStatus.size} 条")
    }
    
    /**
     * 解析时间字符串为长整型时间戳，支持多种格式包括纳秒和时区
     */
    @OptIn(ExperimentalTime::class)
    private fun parseTimeToLong(timeString: String): Long {
        return try {
            // kotlinx-datetime 的 Instant.parse() 可以自动处理各种 ISO 8601 格式
            // 包括纳秒精度和时区信息，如: 2025-08-11T08:14:51.583598089+08:00
            Instant.parse(timeString).toEpochMilliseconds()
        } catch (e: Exception) {
            Log.d("FeedsViewModel", "kotlinx-datetime 解析失败: $timeString, 尝试备用方案")
            
            // 备用方案1：尝试使用 Android Time 类
            try {
                val time = android.text.format.Time()
                if (time.parse3339(timeString)) {
                    val millis = time.toMillis(false)
                    Log.d("FeedsViewModel", "时间解析成功: $timeString -> $millis (使用 Time.parse3339)")
                    return millis
                }
            } catch (e2: Exception) {
                Log.d("FeedsViewModel", "Time.parse3339 也解析失败: $timeString")
            }
            
            // 备用方案2：基于数字提取的排序值
            try {
                val cleanTime = timeString.replace(Regex("[^\\d]"), "").take(14)
                val result = if (cleanTime.length >= 8) {
                    cleanTime.toLongOrNull() ?: timeString.hashCode().toLong()
                } else {
                    timeString.hashCode().toLong()
                }
                Log.d("FeedsViewModel", "时间解析使用数字提取: $timeString -> 清理后: $cleanTime -> 结果: $result")
                result
            } catch (e3: Exception) {
                val hashResult = timeString.hashCode().toLong()
                Log.w("FeedsViewModel", "时间解析最终使用散列值: $timeString -> $hashResult")
                hashResult
            }
        }
    }
}