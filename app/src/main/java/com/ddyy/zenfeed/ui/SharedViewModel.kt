package com.ddyy.zenfeed.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.FeedRepository
import com.ddyy.zenfeed.data.Labels
import com.ddyy.zenfeed.data.model.GithubRelease
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val feedRepository = FeedRepository(application)

    var selectedFeed by mutableStateOf<Feed?>(null)
        private set

    // 应用更新信息
    var updateInfo by mutableStateOf<GithubRelease?>(null)
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
        Log.d("SharedViewModel", "更新allFeeds数据，从 ${allFeeds.size} 条更新到 ${feeds.size} 条")
        allFeeds = feeds
    }
    
    /**
     * 根据标题在allFeeds中查找文章索引
     */
    fun findFeedIndexByTitle(title: String): Int {
        return allFeeds.indexOfFirst { it.labels?.title == title }
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
                Log.d("SharedViewModel", "通过对象引用找到选中文章索引: $objectRefIndex")
                objectRefIndex
            } else {
                // 对象引用匹配失败时，使用内容匹配（适用于从Intent创建的Feed对象）
                val contentIndex = allFeeds.indexOfFirst { feed ->
                    feed.labels.title == selectedFeed?.labels?.title &&
                    feed.time == selectedFeed?.time
                }.coerceAtLeast(0)
                Log.d("SharedViewModel", "通过内容匹配找到选中文章索引: $contentIndex, 标题: '${selectedFeed?.labels?.title}'")
                contentIndex
            }
        } else {
            Log.d("SharedViewModel", "没有选中文章，返回索引0")
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
    /**
     * 检查应用更新
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            updateInfo = feedRepository.checkForUpdate()
            if (updateInfo != null) {
                Log.d("SharedViewModel", "发现新版本: ${updateInfo?.tagName}")
            } else {
                Log.d("SharedViewModel", "未发现新版本")
            }
        }
    }

    /**
     * 清除更新信息（用户关闭对话框时调用）
     */
    fun clearUpdateInfo() {
        updateInfo = null
    }
}