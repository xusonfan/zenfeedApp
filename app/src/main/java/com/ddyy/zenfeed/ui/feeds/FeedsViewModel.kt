package com.ddyy.zenfeed.ui.feeds

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.FeedRepository
import kotlinx.coroutines.launch

sealed interface FeedsUiState {
    data class Success(val feeds: List<Feed>) : FeedsUiState
    data object Error : FeedsUiState
    data object Loading : FeedsUiState
}

class FeedsViewModel(private val feedRepository: FeedRepository = FeedRepository()) : ViewModel() {

    var feedsUiState: FeedsUiState by mutableStateOf(FeedsUiState.Loading)
        private set

    init {
        getFeeds()
    }

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