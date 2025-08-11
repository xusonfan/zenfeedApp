package com.ddyy.zenfeed.ui.feeds

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddyy.zenfeed.BuildConfig
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedItem(
    feed: Feed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayPodcastList: (() -> Unit)? = null,
    onTogglePlayPause: (() -> Unit)? = null,
    isCurrentlyPlaying: Boolean = false,
    isPlaying: Boolean = false
) {
    // 使用 remember 缓存不变的属性，减少重组开销
    val hasValidPodcast = remember(feed.labels.podcastUrl) {
        !feed.labels.podcastUrl.isNullOrBlank()
    }
    val feedTitle = remember(feed.labels.title) {
        feed.labels.title ?: "未知标题"
    }
    val feedSource = remember(feed.labels.source) {
        feed.labels.source ?: "未知来源"
    }
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
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp) // 减少底部内边距
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
                        text = feedSource,
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
                    text = feedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (feed.isRead) 0.6f else 1.0f // 已读文章标题淡化
                    )
                )
            
                // 摘要内容处理 - 优化HTML处理性能
                val displayContent = remember(feed.labels.summaryHtmlSnippet, feed.labels.summary) {
                    val content = feed.labels.summaryHtmlSnippet?.takeIf { it.isNotBlank() }
                        ?: feed.labels.summary?.takeIf { it.isNotBlank() }
                        ?: ""
                    
                    if (content.contains('<')) {
                        // 只有包含HTML标签时才进行处理
                        content.replace(Regex("<[^>]*>"), "")
                            .replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .trim()
                    } else {
                        content.trim()
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
                
                // Tags 展示区域 - 放在卡片左下角
                val displayTags = remember(feed.labels.tags) {
                    feed.labels.tags?.takeIf { it.isNotBlank() }
                        ?.split(",", "，", ";", "；") // 支持多种分隔符
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.take(3) // 最多显示3个标签
                        ?: emptyList()
                }
                
                if (displayTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        displayTags.forEachIndexed { index, tag ->
                            // 根据标签内容生成颜色
                            val colorIndex = tag.hashCode().let { if (it < 0) -it else it } % 6
                            val (backgroundColor, borderColor, textColor) = when (colorIndex) {
                                0 -> Triple(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary
                                )
                                1 -> Triple(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondary
                                )
                                2 -> Triple(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiary
                                )
                                3 -> Triple(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.error
                                )
                                4 -> Triple(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                else -> Triple(
                                    MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.inversePrimary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = backgroundColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp
                                    ),
                                    color = textColor.copy(
                                        alpha = if (feed.isRead) 0.6f else 0.8f
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
            }

            // 播客播放按钮 - 绝对定位在右上角
            if (hasValidPodcast && (onPlayPodcastList != null || onTogglePlayPause != null)) {
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
    modifier: Modifier = Modifier,
    onTabDoubleClick: (Int) -> Unit = {}
) {
    // 使用remember监听categories变化，确保tab栏与数据同步
    val allCategories = remember(categories) { listOf("全部") + categories }
    
    // 双击检测状态 - 为每个tab维护独立的双击状态
    var lastClickTimes by remember { mutableStateOf(emptyMap<Int, Long>()) }
    val doubleTapThreshold = 300L // 双击时间间隔阈值（毫秒）

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
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    val lastTime = lastClickTimes[index] ?: 0L
                    
                    if (currentTime - lastTime <= doubleTapThreshold) {
                        // 双击事件：滚动到列表顶部
                        onTabDoubleClick(index)
                        lastClickTimes = lastClickTimes + (index to 0L) // 重置时间避免三击
                    } else {
                        // 单击事件：切换tab
                        onTabSelected(index)
                        lastClickTimes = lastClickTimes + (index to currentTime)
                    }
                },
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

/**
 * 获取最近阅读的条目索引
 */
fun getLastReadFeedIndex(feeds: List<Feed>): Int? {
    // 找到最后一个已读的条目（按列表顺序）
    return feeds.indexOfLast { it.isRead }.takeIf { it >= 0 }
}

/**
 * 跳转到最近阅读按钮组件
 */
@Composable
fun JumpToLastReadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.LastPage,
                contentDescription = "跳转到最近阅读",
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "最近阅读",
                style = MaterialTheme.typography.labelMedium
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
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onLoggingClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onPlayPodcastList: ((List<Feed>, Int) -> Unit)? = null,
    playerViewModel: PlayerViewModel? = null,
    sharedViewModel: com.ddyy.zenfeed.ui.SharedViewModel? = null,
    currentThemeMode: String = "system",
    onThemeToggle: () -> Unit = {},
    isProxyEnabled: Boolean = false,
    onProxyToggle: () -> Unit = {}
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
        onLoggingClick = onLoggingClick,
        onAboutClick = onAboutClick,
        onPlayPodcastList = onPlayPodcastList,
        playerViewModel = playerViewModel,
        listStates = listStates,
        scrollPositions = feedsViewModel.scrollPositions,
        sharedViewModel = sharedViewModel,
        currentThemeMode = currentThemeMode,
        onThemeToggle = onThemeToggle,
        isProxyEnabled = isProxyEnabled,
        onProxyToggle = onProxyToggle,
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
    onLoggingClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPlayPodcastList: ((List<Feed>, Int) -> Unit)?,
    playerViewModel: PlayerViewModel?,
    listStates: MutableMap<String, LazyStaggeredGridState>,
    scrollPositions: Map<String, Pair<Int, Int>>,
    modifier: Modifier = Modifier,
    sharedViewModel: com.ddyy.zenfeed.ui.SharedViewModel? = null,
    currentThemeMode: String = "system",
    onThemeToggle: () -> Unit = {},
    isProxyEnabled: Boolean = false,
    onProxyToggle: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 抽屉状态管理
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    
    // 返回键拦截状态
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var hasScrolledToTop by remember { mutableStateOf(false) }
    val backPressThreshold = 2000L // 两次返回键间隔阈值（2秒）

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
    
    // 返回键处理逻辑
    BackHandler {
        val currentTime = System.currentTimeMillis()
        val currentCategory = pagerCategories.getOrNull(pagerState.currentPage) ?: ""
        val currentListState = listStates[currentCategory]
        
        // 检查当前是否在列表顶部
        val isAtTop = currentListState?.firstVisibleItemIndex == 0 &&
                     currentListState.firstVisibleItemScrollOffset == 0
        
        if (!isAtTop) {
            // 如果不在顶部，滚动到顶部 - 使用 scrollToItem 替代 animateScrollToItem 提升性能
            coroutineScope.launch {
                try {
                    currentListState?.animateScrollToItem(0) // 使用动画滚动
                    hasScrolledToTop = true
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // 检查是否是LeftCompositionCancellationException
                    if (e.message?.contains("left the composition") == true) {
                        Log.w("FeedsScreen", "组合已离开，返回键滚动操作被取消", e)
                        // 不重新抛出LeftCompositionCancellationException
                    } else {
                        Log.w("FeedsScreen", "协程被取消，返回键滚动操作终止", e)
                        throw e
                    }
                } catch (e: Exception) {
                    Log.e("FeedsScreen", "返回键滚动失败", e)
                }
            }
            Toast.makeText(context, "再次按返回键退出应用", Toast.LENGTH_SHORT).show()
        } else {
            // 如果已经在顶部，检查是否在时间阈值内
            if (currentTime - lastBackPressTime <= backPressThreshold) {
                // 在阈值内，真正退出应用
                (context as? androidx.activity.ComponentActivity)?.finish()
            } else {
                // 超过阈值，显示提示并更新时间
                lastBackPressTime = currentTime
                hasScrolledToTop = false
                Toast.makeText(context, "再次按返回键退出应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 使用 ModalNavigationDrawer 包装整个内容
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    onSettingsClick()
                },
                onLoggingClick = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    onLoggingClick()
                },
                onAboutClick = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    onAboutClick()
                },
                currentThemeMode = currentThemeMode,
                onThemeToggle = onThemeToggle,
                isProxyEnabled = isProxyEnabled,
                onProxyToggle = onProxyToggle
            )
        }
    ) {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            floatingActionButton = {
                // 全局播放控制悬浮按钮
                if (playerViewModel != null) {
                    val isPlaying by playerViewModel.isPlaying.observeAsState(false)
                    val playlistInfo by playerViewModel.playlistInfo.observeAsState()
                    
                    // 获取当前播放的播客或者第一个有效播客作为默认播客
                    val currentPlayingFeed = remember(playlistInfo) {
                        playlistInfo?.let { info ->
                            val currentPlaylist = playerViewModel.getCurrentPlaylist()
                            if (info.currentIndex >= 0 && info.currentIndex < currentPlaylist.size) {
                                currentPlaylist[info.currentIndex]
                            } else null
                        }
                    }
                    
                    // 如果有当前播放的或者列表中有播客，显示悬浮按钮
                    val hasValidPodcast = currentPlayingFeed?.labels?.podcastUrl?.isNotBlank() == true ||
                            (feedsUiState as? FeedsUiState.Success)?.feeds?.any { !it.labels.podcastUrl.isNullOrBlank() } == true
                    
                    if (hasValidPodcast) {
                        FloatingActionButton(
                            onClick = {
                                playerViewModel.playerService?.let { service ->
                                    if (isPlaying) {
                                        service.pause()
                                    } else {
                                        if (currentPlayingFeed != null) {
                                            // 如果有当前播放的播客，恢复播放
                                            service.resume()
                                        } else {
                                            // 否则播放第一个有效播客
                                            (feedsUiState as? FeedsUiState.Success)?.feeds?.let { feeds ->
                                                val firstPodcastFeed = feeds.find { !it.labels.podcastUrl.isNullOrBlank() }
                                                if (firstPodcastFeed != null) {
                                                    val feedIndex = feeds.indexOf(firstPodcastFeed)
                                                    onPlayPodcastList?.invoke(feeds, feedIndex)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停播客" else "播放播客"
                            )
                        }
                    }
                }
            },
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
                                    // 双击事件：滚动到顶部 - 优化滚动性能
                                    coroutineScope.launch {
                                        try {
                                            // 根据 pagerState 获取当前可见的列表状态并滚动
                                            val categoryToScroll = pagerCategories.getOrNull(pagerState.currentPage)
                                            val stateToScroll = categoryToScroll?.let { listStates[it] }
                                            stateToScroll?.animateScrollToItem(0) // 使用动画滚动
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            // 检查是否是LeftCompositionCancellationException
                                            if (e.message?.contains("left the composition") == true) {
                                                Log.w("FeedsScreen", "组合已离开，双击标题滚动操作被取消", e)
                                                // 不重新抛出LeftCompositionCancellationException
                                            } else {
                                                Log.w("FeedsScreen", "协程被取消，双击标题滚动操作终止", e)
                                                throw e
                                            }
                                        } catch (e: Exception) {
                                            Log.e("FeedsScreen", "双击标题滚动失败", e)
                                        }
                                    }
                                    lastClickTime = 0L // 重置时间避免三击
                                } else {
                                    lastClickTime = currentTime
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单",
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
                    // 确保 allCategories 与 pagerCategories 保持一致
                    val allCategories = remember(feedsUiState.categories) {
                        pagerCategories
                    }
                    
                    // 分类 Tab
                    CategoryTabs(
                        pagerState = pagerState,
                        categories = feedsUiState.categories,
                        onTabSelected = { page ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        onTabDoubleClick = { page ->
                            // 双击tab时滚动到对应分类的列表顶部
                            coroutineScope.launch {
                                try {
                                    // 确保pager已经切换到正确页面
                                    if (pagerState.currentPage != page) {
                                        pagerState.animateScrollToPage(page)
                                        // 等待页面切换完成
                                        kotlinx.coroutines.delay(300)
                                    }
                                    
                                    // 获取对应分类的列表状态并滚动到顶部
                                    val allCategories = listOf("") + feedsUiState.categories
                                    val category = if (page < allCategories.size) {
                                        allCategories[page]
                                    } else {
                                        "" // 默认为全部分类
                                    }
                                    val listState = listStates[category]
                                    listState?.animateScrollToItem(0) // 使用动画滚动
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    // 检查是否是LeftCompositionCancellationException
                                    if (e.message?.contains("left the composition") == true) {
                                        Log.w("FeedsScreen", "组合已离开，双击Tab滚动操作被取消", e)
                                        // 不重新抛出LeftCompositionCancellationException
                                    } else {
                                        Log.w("FeedsScreen", "协程被取消，双击Tab滚动操作终止", e)
                                        throw e
                                    }
                                } catch (e: Exception) {
                                    Log.e("FeedsScreen", "双击Tab滚动失败", e)
                                }
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
                        val grouped = feedsUiState.feeds.groupBy { it.labels.category ?: "" }.toMutableMap()
                        // 将"全部"类别也添加进去，确保"全部"列表覆盖任何可能存在的、分类为空字符串的列表
                        grouped[""] = feedsUiState.feeds
                        grouped.toMap()
                    }
                    
                    // 处理返回时的滚动定位 - 检测页面重新进入
                    LaunchedEffect(Unit) {
                        Log.d("FeedsScreen", "FeedsScreen 页面进入，检查是否需要滚动")
                        
                        // 检查是否有待处理的滚动任务
                        if (sharedViewModel?.lastViewedFeed != null && !sharedViewModel.lastViewedFeed?.labels?.title.isNullOrEmpty()) {
                            val targetCategory = sharedViewModel.detailEntryCategory
                            val lastViewedFeed = sharedViewModel.lastViewedFeed!!
                            
                            Log.d("FeedsScreen", "检测到从详情页返回，准备滚动到文章: ${lastViewedFeed.labels.title}, 分类: '$targetCategory', 当前分类: '$selectedCategory'")
                            
                            // 等待UI完全加载
                            kotlinx.coroutines.delay(200)
                            
                            // 确保切换到正确的分类
                            if (targetCategory != selectedCategory) {
                                Log.d("FeedsScreen", "切换分类从 '$selectedCategory' 到 '$targetCategory'")
                                onCategorySelected(targetCategory)
                                // 等待分类切换和Pager动画完成
                                kotlinx.coroutines.delay(800)
                            } else {
                                // 即使在同一分类，也等待一下确保UI稳定
                                kotlinx.coroutines.delay(300)
                            }
                            
                            // 滚动到指定文章
                            val targetFeeds = categorizedFeeds[targetCategory] ?: emptyList()
                            val targetIndex = sharedViewModel.getLastViewedFeedIndexInCategory(targetFeeds)
                            
                            Log.d("FeedsScreen", "目标索引: $targetIndex, 总数: ${targetFeeds.size}")
                            Log.d("FeedsScreen", "目标文章标题: ${targetFeeds.getOrNull(targetIndex)?.labels?.title}")
                            
                            if (targetIndex >= 0 && targetIndex < targetFeeds.size) {
                                // 确保listState存在并且是当前正确的状态
                                val listState = listStates[targetCategory]
                                if (listState != null) {
                                    try {
                                        Log.d("FeedsScreen", "开始动画滚动到索引: $targetIndex")
                                        listState.animateScrollToItem(targetIndex) // 使用动画滚动
                                        Log.d("FeedsScreen", "动画滚动完成")
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        // 检查是否是LeftCompositionCancellationException
                                        if (e.message?.contains("left the composition") == true) {
                                            Log.w("FeedsScreen", "组合已离开，滚动操作被取消", e)
                                            // 不重新抛出LeftCompositionCancellationException
                                        } else {
                                            Log.w("FeedsScreen", "协程被取消，滚动操作终止", e)
                                            throw e
                                        }
                                    } catch (e: Exception) {
                                        Log.e("FeedsScreen", "滚动失败", e)
                                    }
                                } else {
                                    Log.w("FeedsScreen", "ListState 不存在: '$targetCategory'")
                                }
                            } else {
                                Log.w("FeedsScreen", "无效的目标索引: $targetIndex")
                            }
                            
                            // 清除滚动状态，避免重复触发
                            sharedViewModel.clearScrollState()
                        }
                    }
                    
                    // 内容区域
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { index ->
                            // 添加边界检查，防止索引越界
                            if (index < allCategories.size) {
                                allCategories[index]
                            } else {
                                "unknown_$index"
                            }
                        }
                    ) { page ->
                        // 添加边界检查，防止索引越界
                        val category = if (page < allCategories.size) {
                            allCategories[page]
                        } else {
                            "" // 默认为全部分类
                        }
                        // 直接从预先计算好的 Map 中获取数据，这是一个非常快速的操作
                        val feedsForCategory = categorizedFeeds[category] ?: emptyList()
                        val listState = listStates.getOrPut(category) {
                            val (initialIndex, initialOffset) = scrollPositions[category] ?: (0 to 0)
                            LazyStaggeredGridState(initialIndex, initialOffset)
                        }
                        val pullToRefreshState = rememberPullToRefreshState()

                        // 检测是否在列表顶部
                        val isAtTop by remember {
                            derivedStateOf {
                                listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0
                            }
                        }
                        
                        // 获取最近阅读的条目索引
                        val lastReadIndex = remember(feedsForCategory) {
                            getLastReadFeedIndex(feedsForCategory)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            PullToRefreshBox(
                                state = pullToRefreshState,
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(1), // 使用单列布局
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalItemSpacing = 12.dp,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = feedsForCategory,
                                        key = { feed -> "${feed.time}-${feed.labels.title?.hashCode() ?: 0}" }, // 确保 key 唯一性
                                        contentType = { "FeedItem" }
                                    ) { feed ->
                                        // 优化播客状态计算 - 减少重复计算
                                        val isCurrentlyPlaying = remember(feed.labels.podcastUrl, playlistInfo, currentPlaylist) {
                                            if (feed.labels.podcastUrl.isNullOrBlank()) {
                                                false
                                            } else {
                                                playlistInfo?.let { info ->
                                                    info.currentIndex >= 0 &&
                                                            info.currentIndex < currentPlaylist.size &&
                                                            currentPlaylist[info.currentIndex].labels.podcastUrl == feed.labels.podcastUrl
                                                } == true
                                            }
                                        }

                                        FeedItem(
                                            feed = feed,
                                            onClick = { onFeedClick(feed) },
                                            onPlayPodcastList = if (!feed.labels.podcastUrl.isNullOrBlank()) {
                                                { onPlayPodcastList?.invoke(feedsUiState.feeds, feedsUiState.feeds.indexOf(feed)) }
                                            } else null,
                                            onTogglePlayPause = if (!feed.labels.podcastUrl.isNullOrBlank()) {
                                                { playerViewModel?.togglePlayPause() }
                                            } else null,
                                            isCurrentlyPlaying = isCurrentlyPlaying,
                                            isPlaying = isPlaying
                                        )
                                    }
                                }
                            }
                            
                            // 显示"跳转到最近阅读"按钮
                            if (isAtTop && lastReadIndex != null && lastReadIndex > 0) {
                                JumpToLastReadButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                listState.animateScrollToItem(lastReadIndex) // 使用动画滚动
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                // 检查是否是LeftCompositionCancellationException
                                                if (e.message?.contains("left the composition") == true) {
                                                    Log.w("FeedsScreen", "组合已离开，跳转到最近阅读操作被取消", e)
                                                    // 不重新抛出LeftCompositionCancellationException
                                                } else {
                                                    Log.w("FeedsScreen", "协程被取消，跳转到最近阅读操作终止", e)
                                                    throw e
                                                }
                                            } catch (e: Exception) {
                                                Log.e("FeedsScreen", "跳转到最近阅读失败", e)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
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

/**
 * 菜单项卡片组件
 */
@Composable
fun MenuItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标背景
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 抽屉菜单内容组件
 */
@Composable
fun DrawerContent(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLoggingClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    currentThemeMode: String = "system",
    onThemeToggle: () -> Unit = {},
    isProxyEnabled: Boolean = false,
    onProxyToggle: () -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = modifier.widthIn(max = 280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // 抽屉头部 - 扩展到状态栏区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 增加高度以覆盖状态栏区域
                .background(
                    MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp) // 为状态栏留出空间
            ) {
                Text(
                    text = "Zenfeed",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "精选资讯阅读",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 菜单项分组
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 主题切换菜单项
            MenuItemCard(
                icon = when (currentThemeMode) {
                    "light" -> Icons.Default.LightMode
                    "dark" -> Icons.Default.DarkMode
                    else -> Icons.Default.AutoMode // 跟随系统使用自动模式图标
                },
                title = "主题模式",
                subtitle = when (currentThemeMode) {
                    "light" -> "日间模式"
                    "dark" -> "夜间模式"
                    "system" -> "跟随系统"
                    else -> "未知"
                },
                onClick = onThemeToggle
            )
            
            // 代理切换菜单项
            MenuItemCard(
                icon = Icons.Default.Security,
                title = "代理设置",
                subtitle = if (isProxyEnabled) "代理已启用" else "代理已禁用",
                onClick = onProxyToggle
            )
            
            // 日志记录菜单项
            MenuItemCard(
                icon = Icons.Default.BugReport,
                title = "日志记录",
                subtitle = "记录应用日志，排查问题",
                onClick = onLoggingClick
            )
            
            // 设置菜单项
            MenuItemCard(
                icon = Icons.Default.Settings,
                title = "设置",
                subtitle = "应用设置和配置",
                onClick = onSettingsClick
            )
            
            // 关于菜单项
            MenuItemCard(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "应用信息和版本详情",
                onClick = onAboutClick
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 底部信息
        Text(
            text = "版本 ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
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
                        tags = "新闻,时事",
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
                        tags = "新闻,时事",
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
            isRefreshing = false,
            isBackgroundRefreshing = false,
            onFeedClick = {},
            onCategorySelected = {},
            onRefresh = {},
            onSettingsClick = {},
            onLoggingClick = {},
            onAboutClick = {},
            onPlayPodcastList = null,
            playerViewModel = null,
            listStates = remember { mutableMapOf() },
            scrollPositions = emptyMap(),
            sharedViewModel = null
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