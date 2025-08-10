package com.ddyy.zenfeed.ui

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels

class SharedViewModel : ViewModel() {
    var selectedFeed by mutableStateOf<Feed?>(null)
        private set
    
    var webViewData by mutableStateOf<Pair<String, String>?>(null)
        private set
    
    // 导航到详情页的标志
    var shouldNavigateToDetail by mutableStateOf(false)
        private set

    fun selectFeed(feed: Feed) {
        selectedFeed = feed
    }
    
    /**
     * 设置是否需要导航到详情页
     */
    fun setNavigateToDetail(navigate: Boolean) {
        shouldNavigateToDetail = navigate
    }
    
    fun setWebViewData(url: String, title: String) {
        webViewData = Pair(url, title)
    }
    
    /**
     * 从Intent中的数据创建Feed对象并设置为选中的Feed
     */
    fun selectFeedFromIntent(intent: Intent) {
        val title = intent.getStringExtra("FEED_TITLE") ?: ""
        val source = intent.getStringExtra("FEED_SOURCE") ?: ""
        val content = intent.getStringExtra("FEED_CONTENT") ?: ""
        val link = intent.getStringExtra("FEED_LINK") ?: ""
        val summary = intent.getStringExtra("FEED_SUMMARY") ?: ""
        val summaryHtmlSnippet = intent.getStringExtra("FEED_SUMMARY_HTML_SNIPPET") ?: ""
        val pubTime = intent.getStringExtra("FEED_PUB_TIME") ?: ""
        val category = intent.getStringExtra("FEED_CATEGORY") ?: ""
        val tags = intent.getStringExtra("FEED_TAGS") ?: ""
        val type = intent.getStringExtra("FEED_TYPE") ?: ""
        val podcastUrl = intent.getStringExtra("FEED_PODCAST_URL") ?: ""
        val time = intent.getStringExtra("FEED_TIME") ?: ""
        val isRead = intent.getBooleanExtra("FEED_IS_READ", false)
        
        val labels = Labels(
            category = category,
            content = content,
            link = link,
            podcastUrl = podcastUrl,
            pubTime = pubTime,
            source = source,
            summary = summary,
            summaryHtmlSnippet = summaryHtmlSnippet,
            tags = tags,
            title = title,
            type = type
        )
        
        val feed = Feed(
            labels = labels,
            time = time,
            isRead = isRead
        )
        
        selectedFeed = feed
    }
}