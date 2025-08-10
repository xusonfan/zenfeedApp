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
    
    private val feedRepository = FeedRepository(application.applicationContext)

    var feedsUiState: FeedsUiState by mutableStateOf(FeedsUiState.Loading)
        private set
    
    // 下拉刷新状态
    var isRefreshing: Boolean by mutableStateOf(false)
        private set
    
    // 原始的完整Feed列表
    private var allFeeds: List<Feed> = emptyList()
    
    // 当前选中的分类，空字符串表示显示全部
    var selectedCategory: String by mutableStateOf("")
        private set

    init {
        getFeeds()
    }

    /**
     * 获取Feed列表
     */
    fun getFeeds() {
        viewModelScope.launch {
            feedsUiState = FeedsUiState.Loading
            val result = feedRepository.getFeeds()
            if (result.isSuccess) {
                allFeeds = result.getOrNull()?.feeds ?: emptyList()
                updateFilteredFeeds()
            } else {
                feedsUiState = FeedsUiState.Error
            }
        }
    }
    
    /**
     * 下拉刷新获取Feed列表
     */
    fun refreshFeeds() {
        viewModelScope.launch {
            isRefreshing = true
            val result = feedRepository.getFeeds()
            if (result.isSuccess) {
                allFeeds = result.getOrNull()?.feeds ?: emptyList()
                updateFilteredFeeds()
            } else {
                feedsUiState = FeedsUiState.Error
            }
            isRefreshing = false
        }
    }
    
    /**
     * 选择分类进行筛选
     */
    fun selectCategory(category: String) {
        selectedCategory = category
        updateFilteredFeeds()
    }
    
    /**
     * 更新筛选后的Feed列表
     */
    private fun updateFilteredFeeds() {
        val filteredFeeds = if (selectedCategory.isEmpty()) {
            allFeeds
        } else {
            allFeeds.filter { it.labels.category == selectedCategory }
        }
        
        val categories = allFeeds
            .map { it.labels.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        
        feedsUiState = FeedsUiState.Success(
            feeds = filteredFeeds,
            categories = categories
        )
    }
}