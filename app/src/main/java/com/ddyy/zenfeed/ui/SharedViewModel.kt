package com.ddyy.zenfeed.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ddyy.zenfeed.data.Feed

class SharedViewModel : ViewModel() {
    var selectedFeed by mutableStateOf<Feed?>(null)
        private set

    fun selectFeed(feed: Feed) {
        selectedFeed = feed
    }
}