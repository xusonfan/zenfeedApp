package com.ddyy.zenfeed.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ddyy.zenfeed.data.SettingsDataStore
import com.ddyy.zenfeed.ui.SharedViewModel
import com.ddyy.zenfeed.ui.feeds.FeedDetailScreen
import com.ddyy.zenfeed.ui.feeds.FeedsScreen
import com.ddyy.zenfeed.ui.feeds.FeedsUiState
import com.ddyy.zenfeed.ui.feeds.FeedsViewModel
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import com.ddyy.zenfeed.ui.settings.SettingsScreen
import com.ddyy.zenfeed.ui.settings.SettingsViewModel
import com.ddyy.zenfeed.ui.webview.WebViewScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(sharedViewModel: SharedViewModel) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val feedsViewModel: FeedsViewModel = androidx.lifecycle.viewmodel.compose.viewModel() // 共享的FeedsViewModel
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 创建SettingsDataStore实例
    val settingsDataStore = remember { SettingsDataStore(context) }
    
    // 监听主题模式变化
    val currentThemeMode by settingsDataStore.themeMode.collectAsState(initial = "system")
    
    // 监听代理启用状态
    val isProxyEnabled by settingsDataStore.proxyEnabled.collectAsState(initial = false)

    // 确保播放器服务在应用启动时就绑定
    DisposableEffect(Unit) {
        playerViewModel.bindService(context)
        onDispose {
            playerViewModel.unbindService(context)
        }
    }
    
    // 监听导航标志，处理从通知栏跳转到详情页
    LaunchedEffect(sharedViewModel.shouldNavigateToDetail) {
        if (sharedViewModel.shouldNavigateToDetail) {
            // 导航到feedDetail页面
            navController.navigate("feedDetail") {
                // 如果当前不在feeds页面，先导航到feeds，然后到详情页
                popUpTo("feeds") { inclusive = false }
            }
            // 重置导航标志
            sharedViewModel.setNavigateToDetail(false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = "feeds"
    ) {
        composable("feeds") {
            FeedsScreen(
                feedsViewModel = feedsViewModel,
                onFeedClick = { feed ->
                    // 先保存当前的feeds列表到SharedViewModel
                    val currentFeedsState = feedsViewModel.feedsUiState
                    if (currentFeedsState is FeedsUiState.Success) {
                        sharedViewModel.updateAllFeeds(currentFeedsState.feeds)
                    }
                    // 记录进入详情页时的分类
                    android.util.Log.d("AppNavigation", "进入详情页，分类: ${feedsViewModel.selectedCategory}, 文章: ${feed.labels.title}")
                    sharedViewModel.setEntryCategory(feedsViewModel.selectedCategory)
                    // 然后选择Feed（这样getCurrentFeedIndex能找到正确的索引）
                    sharedViewModel.selectFeed(feed)
                    // 设置初始的最后浏览文章
                    sharedViewModel.updateLastViewedFeed(feed)
                    // 标记进入详情页
                    sharedViewModel.updateDetailPageStatus(true)
                    // 标记文章为已读
                    feedsViewModel.markFeedAsRead(feed)
                    navController.navigate("feedDetail")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onPlayPodcastList = { feeds, startIndex ->
                    // 过滤出有播客URL的Feed
                    val podcastFeeds = feeds.filter { !it.labels.podcastUrl.isNullOrBlank() }
                    if (podcastFeeds.isNotEmpty()) {
                        // 找到当前Feed在过滤后列表中的正确索引
                        val targetFeed = feeds[startIndex]
                        val correctedIndex = podcastFeeds.indexOfFirst {
                            it.labels.podcastUrl == targetFeed.labels.podcastUrl
                        }.coerceAtLeast(0)

                        // 播放播客列表
                        playerViewModel.playPodcastPlaylist(podcastFeeds, correctedIndex)
                    }
                },
                playerViewModel = playerViewModel,
                sharedViewModel = sharedViewModel,
                currentThemeMode = currentThemeMode,
                onThemeToggle = {
                    // 循环切换主题：system -> light -> dark -> system
                    val nextTheme = when (currentThemeMode) {
                        "system" -> "light"
                        "light" -> "dark"
                        "dark" -> "system"
                        else -> "system"
                    }
                    coroutineScope.launch {
                        settingsDataStore.saveThemeMode(nextTheme)
                    }
                },
                isProxyEnabled = isProxyEnabled,
                onProxyToggle = {
                    // 切换代理启用状态
                    coroutineScope.launch {
                        // 获取当前代理设置
                        val currentHost = settingsDataStore.proxyHost.first()
                        val currentPort = settingsDataStore.proxyPort.first()
                        val currentType = settingsDataStore.proxyType.first()
                        val currentUsername = settingsDataStore.proxyUsername.first()
                        val currentPassword = settingsDataStore.proxyPassword.first()
                        
                        // 保存新的代理设置，只切换启用状态
                        settingsDataStore.saveProxySettings(
                            enabled = !isProxyEnabled,
                            type = currentType,
                            host = currentHost,
                            port = currentPort,
                            username = currentUsername,
                            password = currentPassword
                        )
                    }
                }
            )
        }
        composable("feedDetail") {
            val allFeeds = sharedViewModel.allFeeds
            val selectedFeed = sharedViewModel.selectedFeed
            if (selectedFeed != null && allFeeds.isNotEmpty()) {
                // 找到当前选中Feed在allFeeds中的索引
                val initialIndex = sharedViewModel.getCurrentFeedIndex()
                
                FeedDetailScreen(
                    allFeeds = allFeeds,
                    initialFeedIndex = initialIndex,
                    onBack = {
                        // 设置滚动标志，准备回到列表时滚动到最后浏览的文章
                        android.util.Log.d("AppNavigation", "从详情页返回，最后浏览: ${sharedViewModel.lastViewedFeed?.labels?.title}, 分类: ${sharedViewModel.detailEntryCategory}")
                        sharedViewModel.setScrollToLastViewed(true)
                        // 标记离开详情页
                        sharedViewModel.updateDetailPageStatus(false)
                        navController.popBackStack()
                    },
                    onOpenWebView = { url, title ->
                        sharedViewModel.setWebViewData(url, title)
                        navController.navigate("webview")
                    },
                    onPlayPodcastList = { feeds, startIndex ->
                        // 过滤出有播客URL的Feed
                        val podcastFeeds = feeds.filter { !it.labels.podcastUrl.isNullOrBlank() }
                        if (podcastFeeds.isNotEmpty()) {
                            // 找到当前Feed在过滤后列表中的正确索引
                            val targetFeed = feeds[startIndex]
                            val correctedIndex = podcastFeeds.indexOfFirst {
                                it.labels.podcastUrl == targetFeed.labels.podcastUrl
                            }.coerceAtLeast(0)

                            // 播放播客列表
                            playerViewModel.playPodcastPlaylist(podcastFeeds, correctedIndex)
                        }
                    },
                    onFeedChanged = { newFeed ->
                        // 当滑动到新的Feed时，更新SharedViewModel中的选中Feed并标记为已读
                        android.util.Log.d("AppNavigation", "详情页切换文章: ${newFeed.labels.title}")
                        sharedViewModel.selectFeed(newFeed)
                        // 更新最后浏览的文章
                        sharedViewModel.updateLastViewedFeed(newFeed)
                        feedsViewModel.markFeedAsRead(newFeed)
                        
                        // 同步更新SharedViewModel中的allFeeds，以反映已读状态的变化
                        val currentFeedsState = feedsViewModel.feedsUiState
                        if (currentFeedsState is FeedsUiState.Success) {
                            sharedViewModel.updateAllFeeds(currentFeedsState.feeds)
                        }
                    }
                )
            }
        }
        composable("webview") {
            val webViewData = sharedViewModel.webViewData
            if (webViewData != null) {
                WebViewScreen(
                    url = webViewData.first,
                    title = webViewData.second,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("settings") {
            val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>()
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
    }
}