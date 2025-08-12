package com.ddyy.zenfeed.ui.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ddyy.zenfeed.data.SettingsDataStore
import com.ddyy.zenfeed.ui.SharedViewModel
import com.ddyy.zenfeed.ui.about.AboutScreen
import com.ddyy.zenfeed.ui.feeds.FeedDetailScreen
import com.ddyy.zenfeed.ui.feeds.FeedsScreen
import com.ddyy.zenfeed.ui.feeds.FeedsUiState
import com.ddyy.zenfeed.ui.feeds.FeedsViewModel
import com.ddyy.zenfeed.ui.logging.LoggingScreen
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import com.ddyy.zenfeed.ui.settings.SettingsScreen
import com.ddyy.zenfeed.ui.settings.SettingsViewModel
import com.ddyy.zenfeed.ui.webview.WebViewScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(sharedViewModel: SharedViewModel) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val feedsViewModel: FeedsViewModel = viewModel() // 共享的FeedsViewModel
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 创建SettingsDataStore实例
    val settingsDataStore = remember { SettingsDataStore(context) }
    
    // 监听主题模式变化
    val currentThemeMode by settingsDataStore.themeMode.collectAsState(initial = "system")
    
    // 监听代理启用状态
    val isProxyEnabled by settingsDataStore.proxyEnabled.collectAsState(initial = false)

    // 确保播放器服务在应用启动时就绑定，使用稳定的key避免主题切换时重新绑定
    DisposableEffect(playerViewModel) {
        Log.d("AppNavigation", "绑定播放器服务")
        playerViewModel.bindService(context)
        onDispose {
            Log.d("AppNavigation", "准备解绑播放器服务")
            playerViewModel.unbindService(context)
        }
    }
    
    // 监听导航标志，处理从通知栏跳转到详情页
    LaunchedEffect(sharedViewModel.shouldNavigateToDetail, feedsViewModel.feedsUiState) {
        if (sharedViewModel.shouldNavigateToDetail) {
            // 确保feeds数据已经加载完成再导航
            val feedsState = feedsViewModel.feedsUiState
            if (feedsState is FeedsUiState.Success && feedsState.feeds.isNotEmpty()) {
                // feeds数据已加载，更新SharedViewModel中的allFeeds
                sharedViewModel.updateAllFeeds(feedsState.feeds)
                
                // 导航到feedDetail页面
                navController.navigate("feedDetail") {
                    // 如果当前不在feeds页面，先导航到feeds，然后到详情页
                    popUpTo("feeds") { inclusive = false }
                }
                // 重置导航标志
                sharedViewModel.setNavigateToDetail(false)
            } else {
                // feeds数据还没加载完成，触发加载
                feedsViewModel.getFeeds()
                // 保持导航标志，等待下次检查
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "feeds"
    ) {
        composable(
            "feeds",
            popEnterTransition = {
                // 从详情页返回时的进入动画：快速淡入，避免闪烁
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        ) {
            FeedsScreen(
                feedsViewModel = feedsViewModel,
                onFeedClick = { feed ->
                    // 先保存当前的feeds列表到SharedViewModel
                    val currentFeedsState = feedsViewModel.feedsUiState
                    if (currentFeedsState is FeedsUiState.Success) {
                        sharedViewModel.updateAllFeeds(currentFeedsState.feeds)
                    }
                    // 记录进入详情页时的分类
                    Log.d("AppNavigation", "进入详情页，分类: ${feedsViewModel.selectedCategory}, 文章: ${feed.labels.title}")
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
                onLoggingClick = {
                    navController.navigate("logging")
                },
                onAboutClick = {
                    navController.navigate("about")
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

                        // 【后台播放修复】播放播客列表时传入context参数
                        // 原因：支持PlayerViewModel启动前台服务，确保后台播放功能
                        playerViewModel.playPodcastPlaylist(podcastFeeds, correctedIndex, context)
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
        composable(
            "feedDetail",
            enterTransition = {
                // 进入时：从右向左滑入，延迟淡入以减少闪烁
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 150,
                        delayMillis = 50,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                // 退出时：先淡出再滑出，避免背景色闪烁
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = FastOutSlowInEasing
                    )
                ) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = 250,
                        delayMillis = 50,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            popEnterTransition = {
                // 返回时列表页的进入动画：快速淡入
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            popExitTransition = {
                // 返回时详情页的退出动画：先淡出再滑出
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = FastOutSlowInEasing
                    )
                ) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = 250,
                        delayMillis = 50,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        ) {
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
                        Log.d("AppNavigation", "从详情页返回，最后浏览: ${sharedViewModel.lastViewedFeed?.labels?.title}, 分类: ${sharedViewModel.detailEntryCategory}")
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

                            // 【后台播放修复】播放播客列表时传入context参数
                            // 原因：支持PlayerViewModel启动前台服务，确保后台播放功能
                            playerViewModel.playPodcastPlaylist(podcastFeeds, correctedIndex, context)
                        }
                    },
                    onFeedChanged = { newFeed ->
                        // 当滑动到新的Feed时，更新SharedViewModel中的选中Feed并标记为已读
                        Log.d("AppNavigation", "详情页切换文章: ${newFeed.labels.title}")
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
        composable(
            "webview",
            enterTransition = {
                // 组合滑动和淡入动画，减少闪烁
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                // 组合滑动和淡出动画，减少闪烁
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        ) {
            val webViewData = sharedViewModel.webViewData
            if (webViewData != null) {
                WebViewScreen(
                    url = webViewData.first,
                    title = webViewData.second,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(
            "settings",
            enterTransition = {
                // 组合滑动和淡入动画，减少闪烁
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                // 组合滑动和淡出动画，减少闪烁
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        ) {
            val settingsViewModel = viewModel<SettingsViewModel>()
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
        composable(
            "logging",
            enterTransition = {
                // 组合滑动和淡入动画，减少闪烁
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                // 组合滑动和淡出动画，减少闪烁
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        ) {
            LoggingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "about",
            enterTransition = {
                // 组合滑动和淡入动画，减少闪烁
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                // 组合滑动和淡出动画，减少闪烁
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        ) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}