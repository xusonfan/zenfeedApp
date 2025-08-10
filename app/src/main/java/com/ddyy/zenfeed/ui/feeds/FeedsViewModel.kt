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
    data class Success(val feeds: List<Feed>) : FeedsUiState
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
            feedsUiState = if (result.isSuccess) {
                FeedsUiState.Success(result.getOrNull()?.feeds ?: emptyList())
            } else {
                FeedsUiState.Error
            }
        }
    }
}