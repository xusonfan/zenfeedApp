package com.ddyy.zenfeed.ui.feeds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItem(
    feed: Feed,
    onClick: () -> Unit,
    onPlayPodcastList: (() -> Unit)? = null,
    onTogglePlayPause: (() -> Unit)? = null,
    isCurrentlyPlaying: Boolean = false,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 简化卡片设计，减少重绘开销
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp, // 减少阴影计算
            pressedElevation = 6.dp,
            hoveredElevation = 5.dp
        ),
        shape = RoundedCornerShape(12.dp), // 简化圆角
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(16.dp) // 统一内边距
            ) {
                // 来源信息区域 - 简化设计，恢复原始布局
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 简化图标
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = "来源图标",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = feed.labels.source,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = feed.formattedTimeShort,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            
                Spacer(modifier = Modifier.height(8.dp))
                
                // 标题 - 根据阅读状态调整透明度
                Text(
                    text = feed.labels.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (feed.isRead) 0.6f else 1.0f // 已读文章标题淡化
                    )
                )
            
                // 摘要内容处理
                val displayContent = remember(feed.labels.summaryHtmlSnippet, feed.labels.summary) {
                    if (feed.labels.summaryHtmlSnippet.isNotBlank()) {
                        feed.labels.summaryHtmlSnippet
                            .replace(Regex("<[^>]*>"), "")
                            .replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .trim()
                    } else {
                        feed.labels.summary.trim()
                    }
                }
                
                // 显示摘要
                if (displayContent.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3, // 减少行数
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (feed.isRead) 0.5f else 1.0f // 已读文章摘要也淡化
                        )
                    )
                }
                
            }

            // 播客播放按钮 - 绝对定位在右上角
            if (feed.labels.podcastUrl.isNotBlank() && (onPlayPodcastList != null || onTogglePlayPause != null)) {
                FilledTonalButton(
                    onClick = {
                        if (isCurrentlyPlaying) {
                            onTogglePlayPause?.invoke()
                        } else {
                            onPlayPodcastList?.invoke()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .height(28.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isCurrentlyPlaying) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        },
                        contentColor = if (isCurrentlyPlaying) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyPlaying && isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isCurrentlyPlaying && isPlaying) "暂停播客" else "播放播客",
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isCurrentlyPlaying && isPlaying) "暂停" else "播放",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryTabs(
    pagerState: PagerState,
    categories: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategories = remember { listOf("全部") + categories }

    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (pagerState.currentPage < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        allCategories.forEachIndexed { index, category ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = category,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@Stable
fun FeedsScreen(
    feedsViewModel: FeedsViewModel,
    onFeedClick: (Feed) -> Unit,
    onSettingsClick: () -> Unit = {},
    onPlayPodcastList: ((List<Feed>, Int) -> Unit)? = null,
    playerViewModel: PlayerViewModel? = null,
    modifier: Modifier = Modifier
) {
    val feedsUiState = feedsViewModel.feedsUiState
    val selectedCategory = feedsViewModel.selectedCategory
    val isRefreshing = feedsViewModel.isRefreshing
    val isBackgroundRefreshing = feedsViewModel.isBackgroundRefreshing
    val onRefresh = { feedsViewModel.refreshFeeds() }
    val onCategorySelected = { category: String -> feedsViewModel.selectCategory(category) }

    // 为每个类别维护一个列表状态，并与ViewModel关联以持久化
    val listStates = remember { mutableMapOf<String, LazyStaggeredGridState>() }
    DisposableEffect(Unit) {
        onDispose {
            listStates.forEach { (category, state) ->
                feedsViewModel.scrollPositions[category] =
                    state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
            }
        }
    }

    FeedsScreenContent(
        feedsUiState = feedsUiState,
        selectedCategory = selectedCategory,
        isRefreshing = isRefreshing,
        isBackgroundRefreshing = isBackgroundRefreshing,
        onFeedClick = onFeedClick,
        onCategorySelected = onCategorySelected,
        onRefresh = onRefresh,
        onSettingsClick = onSettingsClick,
        onPlayPodcastList = onPlayPodcastList,
        playerViewModel = playerViewModel,
        listStates = listStates,
        scrollPositions = feedsViewModel.scrollPositions,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedsScreenContent(
    feedsUiState: FeedsUiState,
    selectedCategory: String,
    isRefreshing: Boolean,
    isBackgroundRefreshing: Boolean,
    onFeedClick: (Feed) -> Unit,
    onCategorySelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlayPodcastList: ((List<Feed>, Int) -> Unit)?,
    playerViewModel: PlayerViewModel?,
    listStates: MutableMap<String, LazyStaggeredGridState>,
    scrollPositions: Map<String, Pair<Int, Int>>,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

    // Pager 状态
    val pagerCategories = (feedsUiState as? FeedsUiState.Success)?.let { listOf("") + it.categories } ?: listOf("")
    val pagerState = rememberPagerState(pageCount = { pagerCategories.size })

    // 当 selectedCategory 改变时，滚动到对应的页面
    LaunchedEffect(selectedCategory, pagerCategories) {
        val page = pagerCategories.indexOf(selectedCategory)
        if (page != -1 && page != pagerState.currentPage) {
            pagerState.animateScrollToPage(page)
        }
    }

    // 当用户滑动 Pager 时，更新 selectedCategory
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerCategories.isNotEmpty()) {
            val newCategory = pagerCategories[pagerState.currentPage]
            if (newCategory != selectedCategory) {
                onCategorySelected(newCategory)
            }
        }
    }

    // 双击检测状态
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val doubleTapThreshold = 300L // 双击时间间隔阈值（毫秒）

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                // 现代化的顶部应用栏
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Zenfeed",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime <= doubleTapThreshold) {
                                    // 双击事件：滚动到顶部
                                    coroutineScope.launch {
                                        // 根据 pagerState 获取当前可见的列表状态并滚动
                                        val categoryToScroll = pagerCategories.getOrNull(pagerState.currentPage)
                                        val stateToScroll = categoryToScroll?.let { listStates[it] }
                                        stateToScroll?.animateScrollToItem(0)
                                    }
                                    lastClickTime = 0L // 重置时间避免三击
                                } else {
                                    lastClickTime = currentTime
                                }
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // 背景刷新进度条
                if (isBackgroundRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (feedsUiState) {
                is FeedsUiState.Loading -> ModernLoadingScreen(Modifier.fillMaxSize())
                is FeedsUiState.Error -> ModernErrorScreen(Modifier.fillMaxSize())
                is FeedsUiState.Success -> {
                    val allCategories = remember(feedsUiState.categories) { listOf("") + feedsUiState.categories }
                    
                    // 分类 Tab
                    CategoryTabs(
                        pagerState = pagerState,
                        categories = feedsUiState.categories,
                        onTabSelected = { page ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    )
                    
                    // 观察播放状态
                    val isPlaying by playerViewModel?.isPlaying?.observeAsState(false) ?: remember { mutableStateOf(false) }
                    val playlistInfo by playerViewModel?.playlistInfo?.observeAsState() ?: remember { mutableStateOf(null) }
                    val currentPlaylist = remember(playlistInfo) {
                        playerViewModel?.getCurrentPlaylist() ?: emptyList()
                    }

                    // 预先按分类对 feeds 进行分组，避免在 Pager 内部进行昂贵的过滤操作
                    val categorizedFeeds = remember(feedsUiState.feeds) {
                        val grouped = feedsUiState.feeds.groupBy { it.labels.category }
                        // 将“全部”类别也添加进去，通过调换合并顺序，确保“全部”列表覆盖任何可能存在的、分类为空字符串的列表
                        grouped + ("" to feedsUiState.feeds)
                    }
                    
                    // 内容区域
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { allCategories[it] }
                    ) { page ->
                        val category = allCategories[page]
                        // 直接从预先计算好的 Map 中获取数据，这是一个非常快速的操作
                        val feedsForCategory = categorizedFeeds[category] ?: emptyList()
                        val listState = listStates.getOrPut(category) {
                            val (initialIndex, initialOffset) = scrollPositions[category] ?: (0 to 0)
                            LazyStaggeredGridState(initialIndex, initialOffset)
                        }
                        val pullToRefreshState = rememberPullToRefreshState()

                        PullToRefreshBox(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Adaptive(minSize = 200.dp),
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalItemSpacing = 12.dp,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = feedsForCategory,
                                    key = { feed -> "${feed.labels.title}-${feed.time}" },
                                    contentType = { "FeedItem" }
                                ) { feed ->
                                    val isCurrentlyPlaying = currentPlaylist.any {
                                        it.labels.podcastUrl == feed.labels.podcastUrl && feed.labels.podcastUrl.isNotBlank()
                                    } && playlistInfo?.let { info ->
                                        info.currentIndex >= 0 &&
                                                info.currentIndex < currentPlaylist.size &&
                                                currentPlaylist[info.currentIndex].labels.podcastUrl == feed.labels.podcastUrl
                                    } == true

                                    FeedItem(
                                        feed = feed,
                                        onClick = { onFeedClick(feed) },
                                        onPlayPodcastList = if (feed.labels.podcastUrl.isNotBlank()) {
                                            { onPlayPodcastList?.invoke(feedsUiState.feeds, feedsUiState.feeds.indexOf(feed)) }
                                        } else null,
                                        onTogglePlayPause = if (feed.labels.podcastUrl.isNotBlank()) {
                                            { playerViewModel?.togglePlayPause() }
                                        } else null,
                                        isCurrentlyPlaying = isCurrentlyPlaying,
                                        isPlaying = isPlaying
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernLoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 现代化加载动画
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "精彩内容加载中...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请稍候片刻",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModernErrorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 现代化错误图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "错误图标",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "内容加载失败",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请检查网络连接后重试",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 现代化重试按钮
        FilledTonalButton(
            onClick = { /* TODO: 实现重试逻辑 */ },
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 120.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "重新加载",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// 保持原有的预览组件，但使用新的设计
@Preview(showBackground = true)
@Composable
fun FeedItemPreview() {
    MaterialTheme {
        Column {
            // 未读文章示例
            FeedItem(
                feed = Feed(
                    labels = Labels(
                        title = "这是一个现代化的示例标题，它展示了全新的设计风格和视觉效果",
                        summary = "这是一个更加精美的摘要展示，采用了现代化的排版和间距设计，提供更好的阅读体验。新的设计包含了渐变背景、圆角卡片和优雅的动画效果，让整个界面看起来更加专业和吸引人。",
                        source = "现代化来源",
                        category = "",
                        content = "",
                        link = "",
                        podcastUrl = "",
                        pubTime = "",
                        summaryHtmlSnippet = "",
                        tags = "",
                        type = ""
                    ),
                    time = "2023-10-27T12:00:00Z",
                    isRead = false
                ),
                onClick = {}
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 已读文章示例（淡化效果）
            FeedItem(
                feed = Feed(
                    labels = Labels(
                        title = "这是一篇已读文章的标题，显示淡化效果",
                        summary = "这是已读文章的摘要，文字会显示为淡化状态，便于用户区分已读和未读内容。",
                        source = "示例来源",
                        category = "",
                        content = "",
                        link = "",
                        podcastUrl = "",
                        pubTime = "",
                        summaryHtmlSnippet = "",
                        tags = "",
                        type = ""
                    ),
                    time = "2023-10-27T11:00:00Z",
                    isRead = true
                ),
                onClick = {}
            )
        }
    }
}

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
                            tags = "",
                            type = ""
                        ),
                        time = "2023-10-27T12:00:00Z",
                        isRead = it % 3 == 0 // 每三个中有一个是已读状态，用于展示淡化效果
                    )
                },
                categories = listOf("科技", "新闻", "生活")
            ),
            selectedCategory = "",
            isRefreshing = false,
            isBackgroundRefreshing = false,
            onFeedClick = {},
            onCategorySelected = {},
            onRefresh = {},
            onSettingsClick = {},
            onPlayPodcastList = null,
            playerViewModel = null,
            listStates = remember { mutableMapOf() },
            scrollPositions = emptyMap()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ModernLoadingScreenPreview() {
    MaterialTheme {
        ModernLoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ModernErrorScreenPreview() {
    MaterialTheme {
        ModernErrorScreen()
    }
}