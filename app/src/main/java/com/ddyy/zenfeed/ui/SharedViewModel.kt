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
    
    // 存储全部feeds列表用于播放列表功能
    var allFeeds by mutableStateOf<List<Feed>>(emptyList())
        private set
    
    // 当前选中的feed在allFeeds中的索引
    var selectedFeedIndex by mutableStateOf(0)
        private set
    
    var webViewData by mutableStateOf<Pair<String, String>?>(null)
        private set
    
    // 导航到详情页的标志
    var shouldNavigateToDetail by mutableStateOf(false)
        private set
    
    // 记录进入详情页时的分类，用于返回时定位
    var detailEntryCategory by mutableStateOf("")
        private set
    
    // 记录最后浏览的文章，用于返回时滚动定位
    var lastViewedFeed by mutableStateOf<Feed?>(null)
        private set
    
    // 标记是否需要在返回列表时滚动到指定位置
    var shouldScrollToLastViewed by mutableStateOf(false)
        private set
    
    // 标记是否在详情页中（用于检测返回）
    var isInDetailPage by mutableStateOf(false)
        private set

    fun selectFeed(feed: Feed) {
        selectedFeed = feed
        // 更新当前选中feed的索引
        selectedFeedIndex = allFeeds.indexOfFirst { it == feed }.coerceAtLeast(0)
    }
    
    /**
     * 更新全部feeds列表
     */
    fun updateAllFeeds(feeds: List<Feed>) {
        allFeeds = feeds
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
    
    /**
     * 根据索引选择Feed
     */
    fun selectFeedByIndex(index: Int) {
        if (index >= 0 && index < allFeeds.size) {
            selectedFeedIndex = index
            selectedFeed = allFeeds[index]
        }
    }
    
    /**
     * 获取当前选中Feed在allFeeds中的索引
     */
    fun getCurrentFeedIndex(): Int {
        return if (selectedFeed != null) {
            // 先尝试对象引用匹配
            val objectRefIndex = allFeeds.indexOfFirst { it == selectedFeed }
            if (objectRefIndex != -1) {
                objectRefIndex
            } else {
                // 对象引用匹配失败时，使用内容匹配（适用于从Intent创建的Feed对象）
                allFeeds.indexOfFirst { feed ->
                    feed.labels.title == selectedFeed?.labels?.title &&
                    feed.time == selectedFeed?.time
                }.coerceAtLeast(0)
            }
        } else {
            0
        }
    }
    
    /**
     * 设置进入详情页时的分类
     */
    fun setEntryCategory(category: String) {
        detailEntryCategory = category
    }
    
    /**
     * 更新最后浏览的文章（在详情页滑动时调用）
     */
    fun updateLastViewedFeed(feed: Feed) {
        lastViewedFeed = feed
    }
    
    /**
     * 设置是否需要滚动到最后浏览的文章
     */
    fun setScrollToLastViewed(shouldScroll: Boolean) {
        shouldScrollToLastViewed = shouldScroll
    }
    
    /**
     * 设置是否在详情页中
     */
    fun updateDetailPageStatus(inDetail: Boolean) {
        isInDetailPage = inDetail
    }
    
    /**
     * 获取最后浏览的文章在指定分类中的索引
     */
    fun getLastViewedFeedIndexInCategory(categoryFeeds: List<Feed>): Int {
        return if (lastViewedFeed != null) {
            categoryFeeds.indexOfFirst { feed ->
                feed.labels.title == lastViewedFeed?.labels?.title &&
                feed.time == lastViewedFeed?.time
            }.coerceAtLeast(0)
        } else {
            0
        }
    }
    
    /**
     * 清除滚动相关的状态
     */
    fun clearScrollState() {
        shouldScrollToLastViewed = false
        lastViewedFeed = null
        detailEntryCategory = ""
        isInDetailPage = false
    }
}