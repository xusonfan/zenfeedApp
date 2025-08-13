package com.ddyy.zenfeed.ui.feeds

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    
    // 新增内容数量状态（用于toast提醒和触发滚动）
    var newContentCount: Int by mutableStateOf(0)
        private set
    
    // 是否需要滚动到顶部的状态（刷新获得新内容时触发）
    var shouldScrollToTop: Boolean by mutableStateOf(false)
        private set
    
    // 刷新完成但没有新内容的状态（用于显示"没有新内容"提示）
    var shouldShowNoNewContent: Boolean by mutableStateOf(false)
        private set
    
    // 网络请求错误消息状态（用于显示toast提示）
    var errorMessage: String by mutableStateOf("")
        private set
    
    // 原始的完整Feed列表
    private var allFeeds: List<Feed> = emptyList()
    
    // 已读文章的标识符集合（使用标题+时间作为唯一标识）
    private val readFeedIds = mutableSetOf<String>()
    
    // 当前选中的分类，空字符串表示显示全部
    var selectedCategory: String by mutableStateOf("")
        private set

    // 当前选中的时间范围（小时）
    var selectedTimeRangeHours: Int by mutableStateOf(24) // 默认24小时
        private set

    // 当前的搜索查询
    var searchQuery: String by mutableStateOf("")
        private set

    // 搜索历史记录
    var searchHistory: List<String> by mutableStateOf(emptyList())
        private set

    // 搜索阈值，用于控制语义搜索的相关性要求
    var searchThreshold: Float by mutableFloatStateOf(0.55f)
    
    // 搜索结果限制数量，默认500，范围1-500
    var searchLimit: Int by mutableIntStateOf(500)

    init {
        loadReadFeedIds()
        loadSearchHistory() // 加载搜索历史
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
    fun getFeeds(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // 清除之前的错误消息状态
            errorMessage = ""
            
            // 如果当前没有数据或强制刷新，显示加载状态
            if (allFeeds.isEmpty() || forceRefresh) {
                feedsUiState = FeedsUiState.Loading
            } else {
                // 如果有缓存数据，显示背景刷新状态
                isBackgroundRefreshing = true
            }

            val result = feedRepository.getFeeds(useCache = !forceRefresh, hours = selectedTimeRangeHours, query = searchQuery, threshold = if (searchQuery.isNotEmpty()) searchThreshold else null, limit = searchLimit)
            if (result.isSuccess) {
                val feedResponse = result.getOrNull()
                val newFeeds = feedResponse?.feeds ?: emptyList()
                // 检查是否有网络错误信息（即使请求成功返回了缓存数据）
                if (feedResponse?.error != null) {
                    errorMessage = feedResponse.error
                }
                if (newFeeds != allFeeds || forceRefresh) { // 数据不同或强制刷新时更新
                    allFeeds = newFeeds
                    updateFilteredFeeds()
                    Log.d("FeedsViewModel", "已更新网络数据，共 ${newFeeds.size} 条")
                }
            } else {
                // 网络请求失败，设置错误消息状态
                errorMessage = result.exceptionOrNull()?.message ?: "网络请求失败"
                
                // 如果网络获取失败但没有缓存数据，显示错误状态
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
            // 清除之前的错误消息状态
            errorMessage = ""
            isRefreshing = true
            val result = feedRepository.getFeeds(useCache = false, hours = selectedTimeRangeHours, query = searchQuery, threshold = if (searchQuery.isNotEmpty()) searchThreshold else null, limit = searchLimit) // 强制从网络获取
            if (result.isSuccess) {
                val feedResponse = result.getOrNull()
                val newFeeds = feedResponse?.feeds ?: emptyList()
                
                // 检查是否有网络错误信息（即使请求成功返回了缓存数据）
                if (feedResponse?.error != null) {
                    errorMessage = feedResponse.error
                }

                // 检测新增内容数量
                val newContentCount = detectNewContent(allFeeds, newFeeds)

                // 更新Feed列表
                allFeeds = newFeeds
                updateFilteredFeeds()

                // 如果有新内容，设置滚动到顶部状态和新增数量
                if (newContentCount > 0) {
                    this@FeedsViewModel.newContentCount = newContentCount
                    shouldScrollToTop = true
                    shouldShowNoNewContent = false
                    Log.d("FeedsViewModel", "刷新完成，发现 $newContentCount 条新内容，总共 ${newFeeds.size} 条")
                } else {
                    // 没有新内容时，设置显示"没有新内容"提示的状态
                    shouldShowNoNewContent = true
                    Log.d("FeedsViewModel", "刷新完成，无新内容，总共 ${newFeeds.size} 条")
                }
            } else {
                // 刷新失败，设置错误消息状态
                errorMessage = result.exceptionOrNull()?.message ?: "刷新失败"
                
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
     * 选择时间范围
     */
    fun selectTimeRange(hours: Int) {
        if (selectedTimeRangeHours != hours) {
            selectedTimeRangeHours = hours
            // 强制刷新数据
            getFeeds(forceRefresh = true)
            Log.d("FeedsViewModel", "时间范围已更改为: $hours 小时")
        }
    }

    /**
     * 搜索Feeds
     */
    fun searchFeeds(query: String) {
        if (searchQuery != query || query.isNotEmpty()) { // 允许重复搜索相同内容
            searchQuery = query
            addSearchHistory(query) // 添加到历史记录
            getFeeds(forceRefresh = true)
            Log.d("FeedsViewModel", "开始搜索: $query, 阈值: $searchThreshold")
        }
    }


    /**
     * 添加到搜索历史
     */
    private fun addSearchHistory(query: String) {
        if (query.isBlank()) return
        val newHistory = searchHistory.toMutableList()
        newHistory.remove(query) // 移除旧的重复项
        newHistory.add(0, query) // 添加到最前面
        searchHistory = newHistory.take(10) // 最多保存10条
        feedRepository.saveSearchHistory(searchHistory)
        Log.d("FeedsViewModel", "添加搜索历史: $query, 当前历史: $searchHistory")
    }

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        searchHistory = emptyList()
        feedRepository.saveSearchHistory(emptyList())
        Log.d("FeedsViewModel", "搜索历史已清除")
    }

    /**
     * 加载搜索历史
     */
    private fun loadSearchHistory() {
        searchHistory = feedRepository.getSearchHistory()
        Log.d("FeedsViewModel", "已加载搜索历史: $searchHistory")
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
     * 检测新增内容数量
     * 通过比较新旧Feed列表，找出真正新增的内容
     */
    private fun detectNewContent(oldFeeds: List<Feed>, newFeeds: List<Feed>): Int {
        if (oldFeeds.isEmpty()) return 0 // 初次加载不算新增
        
        // 创建旧Feed的唯一标识符集合（使用标题+时间作为唯一标识）
        val oldFeedIds = oldFeeds.map { "${it.labels.title ?: ""}-${it.time}" }.toSet()
        
        // 计算新Feed中不在旧Feed集合中的数量
        val newContentCount = newFeeds.count { feed ->
            val feedId = "${feed.labels.title ?: ""}-${feed.time}"
            !oldFeedIds.contains(feedId)
        }
        
        return newContentCount
    }
    
    /**
     * 清除滚动到顶部状态（UI处理完滚动后调用）
     */
    fun clearScrollToTopState() {
        shouldScrollToTop = false
        newContentCount = 0
    }
    
    /**
     * 清除"没有新内容"提示状态（UI显示完提示后调用）
     */
    fun clearNoNewContentState() {
        shouldShowNoNewContent = false
    }
    
    /**
     * 清除错误消息状态（UI显示完错误提示后调用）
     */
    fun clearErrorMessage() {
        errorMessage = ""
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