package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels
import com.ddyy.zenfeed.ui.feeds.FeedsScreenContent
import com.ddyy.zenfeed.ui.feeds.FeedsUiState

@Preview(showBackground = true)
@Composable
fun FeedsScreenSuccessPreview() {
    MaterialTheme {
        FeedsScreenContent(
            feedsUiState = FeedsUiState.Success(
                feeds = List(8) {
                    Feed(
                        labels = Labels(
                            title = "现代化标题 $it - 展示新的设计风格",
                            summary = "这是第 $it 条内容的现代化摘要信息，采用了全新的视觉设计和排版风格，提供更好的用户体验。",
                            source = "精选来源 $it",
                            category = if (it % 3 == 0) "科技" else if (it % 3 == 1) "新闻" else "生活",
                            content = "",
                            link = "",
                            podcastUrl = "",
                            pubTime = "",
                            summaryHtmlSnippet = "",
                            tags = when (it % 3) {
                                0 -> "科技,AI,创新"
                                1 -> "新闻,时事,热点"
                                else -> "生活,健康,娱乐"
                            },
                            type = ""
                        ),
                        time = "2023-10-27T12:00:00Z",
                        isRead = it % 3 == 0 // 每三个中有一个是已读状态，用于展示淡化效果
                    )
                },
                categories = listOf("科技", "新闻", "生活")
            ),
            selectedCategory = "",
            selectedTimeRangeHours = 24,
            isRefreshing = false,
            isBackgroundRefreshing = false,
            shouldScrollToTop = false,
            newContentCount = 0,
            shouldShowNoNewContent = false,
            errorMessage = "",
            onFeedClick = {},
            onCategorySelected = {},
            onRefresh = {},
            onScrollToTopHandled = {},
            onNoNewContentHandled = {},
            onErrorMessageHandled = {},
            onSettingsClick = {},
            onLoggingClick = {},
            onAboutClick = {},
            onPlayPodcastList = null,
            playerViewModel = null,
            listStates = remember { mutableMapOf() },
            scrollPositions = emptyMap(),
            sharedViewModel = null,
            onTimeRangeSelected = {},
            searchQuery = "",
            onSearchQueryChanged = {},
            searchHistory = listOf("示例搜索1", "示例搜索2", "示例搜索3"),
            onSearchHistoryClick = {},
            onClearSearchHistory = {},
            searchThreshold = 0.55f,
            onSearchThresholdChanged = {},
            searchLimit = 500,
            onSearchLimitChanged = {}
        )
    }
}