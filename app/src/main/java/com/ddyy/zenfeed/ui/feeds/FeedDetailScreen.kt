package com.ddyy.zenfeed.ui.feeds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.extension.orDefaultSource
import com.ddyy.zenfeed.extension.orDefaultTitle
import com.ddyy.zenfeed.ui.feeds.components.detail.FeedDetailPage
import com.ddyy.zenfeed.ui.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedDetailScreen(
    modifier: Modifier = Modifier,
    allFeeds: List<Feed>,
    initialFeedIndex: Int = 0,
    onBack: () -> Unit,
    onOpenWebView: (String, String) -> Unit = { _, _ -> },
    onPlayPodcastList: ((List<Feed>, Int) -> Unit)? = null,
    onFeedChanged: (Feed) -> Unit = {},
) {
    // 如果没有feeds数据，显示空状态
    if (allFeeds.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("文章详情") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("没有可显示的文章")
            }
        }
        return
    }

    val playerViewModel: PlayerViewModel = viewModel()
    val context = LocalContext.current
    val isPlaying by playerViewModel.isPlaying.observeAsState(false)
    val playlistInfo by playerViewModel.playlistInfo.observeAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }
    
    // 用于接收滚动进度的状态
    var scrollProgress by remember { mutableStateOf(0f) }
    
    // HorizontalPager状态
    val pagerState = rememberPagerState(
        initialPage = initialFeedIndex.coerceIn(0, allFeeds.size - 1),
        pageCount = { allFeeds.size }
    )
    
    // 当前显示的Feed
    val currentFeed = allFeeds[pagerState.currentPage]
    
    // 监听页面变化，通知父组件当前Feed已改变
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < allFeeds.size) {
            onFeedChanged(allFeeds[pagerState.currentPage])
        }
    }

    DisposableEffect(Unit) {
        playerViewModel.bindService(context)
        onDispose {
            playerViewModel.unbindService(context)
        }
    }

    Scaffold(
        floatingActionButton = {
            // 播放控制按钮组
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 主播放按钮
                FloatingActionButton(onClick = {
                    playerViewModel.playerService?.let { it ->
                        if (isPlaying) {
                            it.pause()
                        } else {
                            if (it.isSameTrack(currentFeed.labels.podcastUrl ?: "")) {
                                it.resume()
                            } else {
                                // 优先使用播放列表功能，如果没有提供则使用单曲播放
                                if (onPlayPodcastList != null) {
                                    // 使用全部feeds作为播放列表，从当前feed开始播放
                                    val currentIndex = allFeeds.indexOfFirst {
                                        it.labels.podcastUrl == currentFeed.labels.podcastUrl && !currentFeed.labels.podcastUrl.isNullOrBlank()
                                    }.takeIf { it >= 0 } ?: pagerState.currentPage
                                    onPlayPodcastList(allFeeds, currentIndex)
                                } else {
                                    it.play(currentFeed)
                                }
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放"
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // 根据滚动进度在来源和标题之间过渡
                        Text(
                            text = if (scrollProgress < 0.5f) {
                                currentFeed.labels.source.orDefaultSource()
                            } else {
                                currentFeed.labels.title.orDefaultTitle()
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${allFeeds.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onOpenWebView(currentFeed.labels.link ?: "", currentFeed.labels.title.orDefaultTitle())
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "打开原网页"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(innerPadding)
        ) { page ->
            if (page < allFeeds.size) {
                FeedDetailPage(
                    feed = allFeeds[page],
                    playerViewModel = playerViewModel,
                    playlistInfo = playlistInfo,
                    showPlaylistDialog = showPlaylistDialog,
                    onShowPlaylistDialog = { showPlaylistDialog = it },
                    onScrollProgressChanged = { progress ->
                        scrollProgress = progress
                    }
                )
            }
        }
    }
}

