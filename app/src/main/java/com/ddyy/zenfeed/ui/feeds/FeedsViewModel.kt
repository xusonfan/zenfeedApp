package com.ddyy.zenfeed.ui.feeds

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.FeedRepository
import kotlinx.coroutines.launch

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
        loadCachedFeeds()
        getFeeds()
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
                android.util.Log.d("FeedsViewModel", "已加载缓存数据，共 ${cachedFeeds.size} 条")
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
                    android.util.Log.d("FeedsViewModel", "已更新网络数据，共 ${newFeeds.size} 条")
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
                android.util.Log.d("FeedsViewModel", "刷新完成，共 ${newFeeds.size} 条")
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
        val feedId = "${feed.labels.title}-${feed.time}"
        readFeedIds.add(feedId)
        updateFilteredFeeds() // 重新更新UI以反映阅读状态变化
        android.util.Log.d("FeedsViewModel", "标记文章为已读: ${feed.labels.title}")
    }
    
    /**
     * 检查文章是否已读
     */
    private fun isFeedRead(feed: Feed): Boolean {
        val feedId = "${feed.labels.title}-${feed.time}"
        return readFeedIds.contains(feedId)
    }
    
    /**
     * 更新筛选后的Feed列表
     */
    private fun updateFilteredFeeds() {
        // 为所有Feed设置正确的阅读状态
        val feedsWithReadStatus = allFeeds.map { feed ->
            feed.copy(isRead = isFeedRead(feed))
        }
        
        val categories = allFeeds
            .map { it.labels.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        
        feedsUiState = FeedsUiState.Success(
            feeds = feedsWithReadStatus, // 始终返回完整的列表
            categories = categories
        )
    }
}