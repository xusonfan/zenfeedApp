package com.ddyy.zenfeed.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ddyy.zenfeed.ui.SharedViewModel
import com.ddyy.zenfeed.ui.feeds.FeedDetailScreen
import com.ddyy.zenfeed.ui.feeds.FeedsScreen
import com.ddyy.zenfeed.ui.feeds.FeedsViewModel
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import com.ddyy.zenfeed.ui.settings.SettingsScreen
import com.ddyy.zenfeed.ui.settings.SettingsViewModel
import com.ddyy.zenfeed.ui.webview.WebViewScreen

@Composable
fun AppNavigation(sharedViewModel: SharedViewModel) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current

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
            val feedsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<FeedsViewModel>()
            FeedsScreen(
                feedsViewModel = feedsViewModel,
                onFeedClick = { feed ->
                    // 标记文章为已读
                    feedsViewModel.markFeedAsRead(feed)
                    sharedViewModel.selectFeed(feed)
                    navController.navigate("feedDetail")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onPlayPodcastList = { feeds, startIndex ->
                    // 过滤出有播客URL的Feed
                    val podcastFeeds = feeds.filter { it.labels.podcastUrl.isNotBlank() }
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
                playerViewModel = playerViewModel
            )
        }
        composable("feedDetail") {
            val feed = sharedViewModel.selectedFeed
            if (feed != null) {
                FeedDetailScreen(
                    feed = feed,
                    onBack = { navController.popBackStack() },
                    onOpenWebView = { url, title ->
                        sharedViewModel.setWebViewData(url, title)
                        navController.navigate("webview")
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