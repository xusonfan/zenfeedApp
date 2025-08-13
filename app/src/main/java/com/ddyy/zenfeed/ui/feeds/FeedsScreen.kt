package com.ddyy.zenfeed.ui.feeds

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels
import com.ddyy.zenfeed.extension.getLastReadFeedIndex
import com.ddyy.zenfeed.extension.groupByCategory
import com.ddyy.zenfeed.extension.hasValidPodcast
import com.ddyy.zenfeed.ui.feeds.components.common.FeedItem
import com.ddyy.zenfeed.ui.feeds.components.list.JumpToLastReadButton
import com.ddyy.zenfeed.ui.feeds.components.list.ModernErrorScreen
import com.ddyy.zenfeed.ui.feeds.components.list.ModernLoadingScreen
import com.ddyy.zenfeed.ui.feeds.components.navigation.CategoryTabs
import com.ddyy.zenfeed.ui.feeds.components.navigation.DrawerContent
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val selectedTimeRangeHours = feedsViewModel.selectedTimeRangeHours
    val searchQuery = feedsViewModel.searchQuery
    val isRefreshing = feedsViewModel.isRefreshing
    val isBackgroundRefreshing = feedsViewModel.isBackgroundRefreshing
    val shouldScrollToTop = feedsViewModel.shouldScrollToTop
    val newContentCount = feedsViewModel.newContentCount
    val shouldShowNoNewContent = feedsViewModel.shouldShowNoNewContent
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
        selectedTimeRangeHours = selectedTimeRangeHours,
        isRefreshing = isRefreshing,
        isBackgroundRefreshing = isBackgroundRefreshing,
        shouldScrollToTop = shouldScrollToTop,
        newContentCount = newContentCount,
        shouldShowNoNewContent = shouldShowNoNewContent,
        onFeedClick = onFeedClick,
        onCategorySelected = onCategorySelected,
        onRefresh = onRefresh,
        onScrollToTopHandled = { feedsViewModel.clearScrollToTopState() },
        onNoNewContentHandled = { feedsViewModel.clearNoNewContentState() },
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
        onTimeRangeSelected = { hours -> feedsViewModel.selectTimeRange(hours) },
        searchQuery = searchQuery,
        onSearchQueryChanged = { query -> feedsViewModel.searchFeeds(query) },
        searchHistory = feedsViewModel.searchHistory,
        onSearchHistoryClick = { historyQuery -> feedsViewModel.searchFeeds(historyQuery) },
        onClearSearchHistory = { feedsViewModel.clearSearchHistory() },
        searchThreshold = feedsViewModel.searchThreshold,
        onSearchThresholdChanged = { threshold ->
            feedsViewModel.searchThreshold = threshold
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedsScreenContent(
    feedsUiState: FeedsUiState,
    selectedCategory: String,
    selectedTimeRangeHours: Int,
    isRefreshing: Boolean,
    isBackgroundRefreshing: Boolean,
    shouldScrollToTop: Boolean,
    newContentCount: Int,
    shouldShowNoNewContent: Boolean,
    onFeedClick: (Feed) -> Unit,
    onCategorySelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onScrollToTopHandled: () -> Unit,
    onNoNewContentHandled: () -> Unit,
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
    onProxyToggle: () -> Unit = {},
    onTimeRangeSelected: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    searchHistory: List<String> = emptyList(),
    onSearchHistoryClick: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    searchThreshold: Float,
    onSearchThresholdChanged: (Float) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSearchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(searchQuery) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 搜索历史记录显示状态
    var showSearchHistory by remember { mutableStateOf(false) }

    // 搜索前的查询状态，用于退出搜索时恢复
    var searchQueryBeforeSearch by remember { mutableStateOf("") }
    
    // 阈值设置对话框显示状态
    var showThresholdDialog by remember { mutableStateOf(false) }
    
    // 当前编辑的阈值
    var currentEditThreshold by remember { mutableStateOf(searchThreshold) }

    // 抽屉状态管理
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // 返回键拦截状态
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var hasScrolledToTop by remember { mutableStateOf(false) }
    val backPressThreshold = 2000L // 两次返回键间隔阈值（2秒）

    // Pager 状态
    val pagerCategories =
        (feedsUiState as? FeedsUiState.Success)?.let { listOf("") + it.categories } ?: listOf("")
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
        if (isSearchActive) {
            // 如果搜索框为空，退出搜索模式并恢复搜索前的状态
            if (searchText.isEmpty()) {
                isSearchActive = false
                showSearchHistory = false
                keyboardController?.hide()
                // 恢复搜索前的查询
                onSearchQueryChanged(searchQueryBeforeSearch)
            } else {
                // 如果有搜索内容，只清空搜索内容但不退出搜索模式
                searchText = ""
                showSearchHistory = false
                // 重新聚焦到搜索框并显示键盘
                coroutineScope.launch {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        } else {
            val currentTime = System.currentTimeMillis()
            val currentCategory = pagerCategories.getOrNull(pagerState.currentPage) ?: ""
            val currentListState = listStates[currentCategory]

            // 检查当前是否在列表顶部
            val isAtTop = currentListState?.firstVisibleItemIndex == 0 &&
                    currentListState.firstVisibleItemScrollOffset == 0

            if (!isAtTop) {
                // 如果不在顶部，滚动到顶部 - 智能选择滚动方式
                coroutineScope.launch {
                    try {
                        val currentIndex = currentListState?.firstVisibleItemIndex ?: 0
                        val animationThreshold = 20 // 跳转距离阈值

                        if (currentIndex <= animationThreshold) {
                            // 距离较短，使用动画滚动提供流畅体验
                            Log.d("FeedsScreen", "返回键滚动距离: $currentIndex，使用动画滚动")
                            currentListState?.animateScrollToItem(0)
                        } else {
                            // 距离较长，直接跳转提升性能
                            Log.d("FeedsScreen", "返回键滚动距离: $currentIndex，直接跳转")
                            currentListState?.scrollToItem(0)
                        }
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
                    val hasValidPodcast =
                        currentPlayingFeed?.labels?.podcastUrl?.isNotBlank() == true ||
                                (feedsUiState as? FeedsUiState.Success)?.feeds?.hasValidPodcast() == true

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
                                                val firstPodcastFeed =
                                                    feeds.find { !it.labels.podcastUrl.isNullOrBlank() }
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
                            if (isSearchActive) {
                                Box {
                                    TextField(
                                        value = searchText,
                                        onValueChange = {
                                            searchText = it
                                            showSearchHistory =
                                                it.isNotEmpty() && searchHistory.isNotEmpty()
                                        },
                                        placeholder = { Text("语义搜索，长句效果更佳") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            onSearchQueryChanged(searchText)
                                            showSearchHistory = false
                                            keyboardController?.hide()
                                        })
                                    )

                                    // 搜索历史记录下拉菜单
                                    DropdownMenu(
                                        expanded = showSearchHistory && searchHistory.isNotEmpty(),
                                        onDismissRequest = { showSearchHistory = false },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusable(false), // 防止抢夺焦点
                                        properties = androidx.compose.ui.window.PopupProperties(
                                            focusable = false, // 禁止焦点抢夺
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true
                                        )
                                    ) {
                                        // 标题行
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "搜索历史",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            // 清除历史按钮
                                            IconButton(
                                                onClick = {
                                                    onClearSearchHistory()
                                                    showSearchHistory = false
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "清除历史记录",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // 历史记录列表
                                        searchHistory.forEach { historyItem ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AccessTime,
                                                            contentDescription = "历史记录",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.6f
                                                            )
                                                        )

                                                        Spacer(modifier = Modifier.width(12.dp))

                                                        Text(
                                                            text = historyItem,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    searchText = historyItem
                                                    onSearchHistoryClick(historyItem)
                                                    showSearchHistory = false
                                                    keyboardController?.hide()
                                                }
                                            )
                                        }
                                    }
                                }
                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                    // 延迟显示搜索历史，确保键盘先弹出
                                    kotlinx.coroutines.delay(200)
                                    if (searchText.isEmpty() && searchHistory.isNotEmpty()) {
                                        showSearchHistory = true
                                        // 搜索历史显示后重新聚焦到搜索框
                                        kotlinx.coroutines.delay(50)
                                        focusRequester.requestFocus()
                                    }
                                }
                            } else {
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
                                            // 双击事件：滚动到顶部 - 智能选择滚动方式
                                            coroutineScope.launch {
                                                try {
                                                    // 根据 pagerState 获取当前可见的列表状态并滚动
                                                    val categoryToScroll =
                                                        pagerCategories.getOrNull(pagerState.currentPage)
                                                    val stateToScroll =
                                                        categoryToScroll?.let { listStates[it] }
                                                    val currentIndex =
                                                        stateToScroll?.firstVisibleItemIndex ?: 0
                                                    val animationThreshold = 20 // 跳转距离阈值

                                                    if (currentIndex <= animationThreshold) {
                                                        // 距离较短，使用动画滚动提供流畅体验
                                                        Log.d(
                                                            "FeedsScreen",
                                                            "双击标题滚动距离: $currentIndex，使用动画滚动"
                                                        )
                                                        stateToScroll?.animateScrollToItem(0)
                                                    } else {
                                                        // 距离较长，直接跳转提升性能
                                                        Log.d(
                                                            "FeedsScreen",
                                                            "双击标题滚动距离: $currentIndex，直接跳转"
                                                        )
                                                        stateToScroll?.scrollToItem(0)
                                                    }
                                                } catch (e: kotlinx.coroutines.CancellationException) {
                                                    // 检查是否是LeftCompositionCancellationException
                                                    if (e.message?.contains("left the composition") == true) {
                                                        Log.w(
                                                            "FeedsScreen",
                                                            "组合已离开，双击标题滚动操作被取消",
                                                            e
                                                        )
                                                        // 不重新抛出LeftCompositionCancellationException
                                                    } else {
                                                        Log.w(
                                                            "FeedsScreen",
                                                            "协程被取消，双击标题滚动操作终止",
                                                            e
                                                        )
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
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "菜单",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (isSearchActive) {
                                // 阈值调整按钮（放在叉号左边）
                                IconButton(onClick = {
                                    currentEditThreshold = searchThreshold
                                    showThresholdDialog = true
                                }) {
                                    Icon(Icons.Default.Tune, contentDescription = "调整搜索阈值")
                                }
                                // 关闭按钮
                                IconButton(onClick = {
                                    if (searchText.isNotEmpty()) {
                                        // 只清空输入框，不触发搜索重载
                                        searchText = ""
                                        showSearchHistory = false
                                        // 清空后重新聚焦到搜索框并显示键盘
                                        coroutineScope.launch {
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        }
                                    } else {
                                        // 输入框为空时退出搜索模式，恢复搜索前的状态
                                        isSearchActive = false
                                        showSearchHistory = false
                                        keyboardController?.hide()
                                        // 恢复搜索前的查询
                                        onSearchQueryChanged(searchQueryBeforeSearch)
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                                }
                            } else {
                                IconButton(onClick = {
                                    isSearchActive = true
                                    // 保存搜索前的查询状态
                                    searchQueryBeforeSearch = searchQuery
                                    // 触发键盘弹出
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(300) // 等待搜索框完全激活
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "搜索")
                                }
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
                    is FeedsUiState.Error -> ModernErrorScreen(
                        modifier = Modifier.fillMaxSize(),
                        onRetry = onRefresh
                    )

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
                                            delay(300)
                                        }

                                        // 获取对应分类的列表状态并滚动到顶部
                                        val allCategories = listOf("") + feedsUiState.categories
                                        val category = if (page < allCategories.size) {
                                            allCategories[page]
                                        } else {
                                            "" // 默认为全部分类
                                        }
                                        val listState = listStates[category]
                                        val currentIndex = listState?.firstVisibleItemIndex ?: 0
                                        val animationThreshold = 20 // 跳转距离阈值

                                        if (currentIndex <= animationThreshold) {
                                            // 距离较短，使用动画滚动提供流畅体验
                                            Log.d(
                                                "FeedsScreen",
                                                "双击Tab滚动距离: $currentIndex，使用动画滚动"
                                            )
                                            listState?.animateScrollToItem(0)
                                        } else {
                                            // 距离较长，直接跳转提升性能
                                            Log.d(
                                                "FeedsScreen",
                                                "双击Tab滚动距离: $currentIndex，直接跳转"
                                            )
                                            listState?.scrollToItem(0)
                                        }
                                    } catch (e: CancellationException) {
                                        // 检查是否是LeftCompositionCancellationException
                                        if (e.message?.contains("left the composition") == true) {
                                            Log.w(
                                                "FeedsScreen",
                                                "组合已离开，双击Tab滚动操作被取消",
                                                e
                                            )
                                            // 不重新抛出LeftCompositionCancellationException
                                        } else {
                                            Log.w(
                                                "FeedsScreen",
                                                "协程被取消，双击Tab滚动操作终止",
                                                e
                                            )
                                            throw e
                                        }
                                    } catch (e: Exception) {
                                        Log.e("FeedsScreen", "双击Tab滚动失败", e)
                                    }
                                }
                            },
                            onTimeRangeSelected = onTimeRangeSelected,
                            selectedTimeRangeHours = selectedTimeRangeHours
                        )

                        // 观察播放状态
                        val isPlaying by playerViewModel?.isPlaying?.observeAsState(false)
                            ?: remember { mutableStateOf(false) }
                        val playlistInfo by playerViewModel?.playlistInfo?.observeAsState()
                            ?: remember { mutableStateOf(null) }
                        val currentPlaylist = remember(playlistInfo) {
                            playerViewModel?.getCurrentPlaylist() ?: emptyList()
                        }

                        // 预先按分类对 feeds 进行分组，避免在 Pager 内部进行昂贵的过滤操作
                        val categorizedFeeds = remember(feedsUiState.feeds) {
                            feedsUiState.feeds.groupByCategory()
                        }

                        // 处理返回时的滚动定位 - 检测页面重新进入
                        LaunchedEffect(Unit) {
                            Log.d("FeedsScreen", "FeedsScreen 页面进入，检查是否需要滚动")

                            // 检查是否有待处理的滚动任务
                            if (sharedViewModel?.lastViewedFeed != null && !sharedViewModel.lastViewedFeed?.labels?.title.isNullOrEmpty()) {
                                val targetCategory = sharedViewModel.detailEntryCategory
                                val lastViewedFeed = sharedViewModel.lastViewedFeed!!

                                Log.d(
                                    "FeedsScreen",
                                    "检测到从详情页返回，准备滚动到文章: ${lastViewedFeed.labels.title}, 分类: '$targetCategory', 当前分类: '$selectedCategory'"
                                )

                                // 等待UI完全加载
                                kotlinx.coroutines.delay(200)

                                // 确保切换到正确的分类
                                if (targetCategory != selectedCategory) {
                                    Log.d(
                                        "FeedsScreen",
                                        "切换分类从 '$selectedCategory' 到 '$targetCategory'"
                                    )
                                    onCategorySelected(targetCategory)
                                    // 等待分类切换和Pager动画完成
                                    kotlinx.coroutines.delay(800)
                                } else {
                                    // 即使在同一分类，也等待一下确保UI稳定
                                    kotlinx.coroutines.delay(300)
                                }

                                // 滚动到指定文章
                                val targetFeeds = categorizedFeeds[targetCategory] ?: emptyList()
                                val targetIndex =
                                    sharedViewModel.getLastViewedFeedIndexInCategory(targetFeeds)

                                Log.d(
                                    "FeedsScreen",
                                    "目标索引: $targetIndex, 总数: ${targetFeeds.size}"
                                )
                                Log.d(
                                    "FeedsScreen",
                                    "目标文章标题: ${targetFeeds.getOrNull(targetIndex)?.labels?.title}"
                                )

                                if (targetIndex >= 0 && targetIndex < targetFeeds.size) {
                                    // 确保listState存在并且是当前正确的状态
                                    val listState = listStates[targetCategory]
                                    if (listState != null) {
                                        try {
                                            val currentIndex = listState.firstVisibleItemIndex
                                            val jumpDistance = abs(targetIndex - currentIndex)
                                            val animationThreshold = 20 // 跳转距离阈值

                                            if (jumpDistance <= animationThreshold) {
                                                // 距离较短，使用动画滚动提供流畅体验
                                                Log.d(
                                                    "FeedsScreen",
                                                    "从详情页返回滚动距离: $jumpDistance，使用动画滚动到索引: $targetIndex"
                                                )
                                                listState.animateScrollToItem(targetIndex)
                                            } else {
                                                // 距离较长，直接跳转提升性能
                                                Log.d(
                                                    "FeedsScreen",
                                                    "从详情页返回滚动距离: $jumpDistance，直接跳转到索引: $targetIndex"
                                                )
                                                listState.scrollToItem(targetIndex)
                                            }
                                            Log.d("FeedsScreen", "滚动完成")
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

                        // 监听feedsUiState变化，同步更新SharedViewModel中的allFeeds
                        LaunchedEffect(feedsUiState) {
                            val currentFeedsState = feedsUiState
                            Log.d(
                                "FeedsScreen",
                                "feedsUiState变化，同步更新SharedViewModel中的allFeeds，共 ${currentFeedsState.feeds.size} 条"
                            )
                            sharedViewModel?.updateAllFeeds(currentFeedsState.feeds)
                        }

                        // 监听新增内容状态并处理滚动到顶部和toast提醒
                        LaunchedEffect(shouldScrollToTop, newContentCount) {
                            if (shouldScrollToTop && newContentCount > 0) {
                                Log.d(
                                    "FeedsScreen",
                                    "检测到新增内容：$newContentCount 条，准备滚动到顶部"
                                )

                                // 显示toast提醒
                                Toast.makeText(
                                    context,
                                    "已更新 $newContentCount 条新内容",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // 等待一下确保UI稳定，然后滚动到当前分类的列表顶部
                                delay(100)

                                try {
                                    val currentListState = listStates[selectedCategory]
                                    if (currentListState != null) {
                                        // 直接跳转到顶部，因为是新内容刷新
                                        currentListState.scrollToItem(0)
                                        Log.d("FeedsScreen", "滚动到顶部完成")
                                    } else {
                                        Log.w(
                                            "FeedsScreen",
                                            "当前分类 '$selectedCategory' 的ListState不存在"
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("FeedsScreen", "滚动到顶部失败", e)
                                }

                                // 通知ViewModel清除状态
                                onScrollToTopHandled()
                            }
                        }

                        // 监听"没有新内容"状态并显示toast提醒
                        LaunchedEffect(shouldShowNoNewContent) {
                            if (shouldShowNoNewContent) {
                                Log.d("FeedsScreen", "刷新完成但没有新内容")

                                // 显示toast提醒
                                Toast.makeText(
                                    context,
                                    "没有获取到新的内容",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // 通知ViewModel清除状态
                                onNoNewContentHandled()
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
                                val (initialIndex, initialOffset) = scrollPositions[category]
                                    ?: (0 to 0)
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
                                feedsForCategory.getLastReadFeedIndex()
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
                                            val isCurrentlyPlaying = remember(
                                                feed.labels.podcastUrl,
                                                playlistInfo,
                                                currentPlaylist
                                            ) {
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
                                                    {
                                                        onPlayPodcastList?.invoke(
                                                            feedsUiState.feeds,
                                                            feedsUiState.feeds.indexOf(feed)
                                                        )
                                                    }
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
                                                    // 计算跳转距离，决定使用动画还是直接跳转
                                                    val currentIndex =
                                                        listState.firstVisibleItemIndex
                                                    val jumpDistance =
                                                        abs(lastReadIndex - currentIndex)
                                                    val animationThreshold = 20 // 跳转距离阈值，超过20条直接跳转

                                                    if (jumpDistance <= animationThreshold) {
                                                        // 距离较短，使用动画滚动提供流畅体验
                                                        Log.d(
                                                            "FeedsScreen",
                                                            "跳转距离: $jumpDistance，使用动画滚动到最近阅读"
                                                        )
                                                        listState.animateScrollToItem(lastReadIndex)
                                                    } else {
                                                        // 距离较长，直接跳转提升性能
                                                        Log.d(
                                                            "FeedsScreen",
                                                            "跳转距离: $jumpDistance，直接跳转到最近阅读"
                                                        )
                                                        listState.scrollToItem(lastReadIndex)
                                                    }
                                                } catch (e: kotlinx.coroutines.CancellationException) {
                                                    // 检查是否是LeftCompositionCancellationException
                                                    if (e.message?.contains("left the composition") == true) {
                                                        Log.w(
                                                            "FeedsScreen",
                                                            "组合已离开，跳转到最近阅读操作被取消",
                                                            e
                                                        )
                                                        // 不重新抛出LeftCompositionCancellationException
                                                    } else {
                                                        Log.w(
                                                            "FeedsScreen",
                                                            "协程被取消，跳转到最近阅读操作终止",
                                                            e
                                                        )
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
                            
                            // 搜索阈值设置对话框
                            if (showThresholdDialog) {
                                AlertDialog(
                                    onDismissRequest = { showThresholdDialog = false },
                                    title = { Text("设置搜索阈值") },
                                    text = {
                                        Column {
                                            Text(
                                                text = "阈值范围：0.0 - 1.0\n数值越高，搜索结果越精确\n默认值：0.55",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                                            Slider(
                                                value = currentEditThreshold,
                                                onValueChange = { currentEditThreshold = it },
                                                valueRange = 0f..1f,
                                                steps = 19, // 20个间隔，步长0.05
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "0.0",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = String.format("%.2f", currentEditThreshold),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "1.0",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                onSearchThresholdChanged(currentEditThreshold)
                                                showThresholdDialog = false
                                            }
                                        ) {
                                            Text("确定")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { showThresholdDialog = false }
                                        ) {
                                            Text("取消")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
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
            selectedTimeRangeHours = 24,
            isRefreshing = false,
            isBackgroundRefreshing = false,
            shouldScrollToTop = false,
            newContentCount = 0,
            shouldShowNoNewContent = false,
            onFeedClick = {},
            onCategorySelected = {},
            onRefresh = {},
            onScrollToTopHandled = {},
            onNoNewContentHandled = {},
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
            onSearchThresholdChanged = {}
        )
    }
}